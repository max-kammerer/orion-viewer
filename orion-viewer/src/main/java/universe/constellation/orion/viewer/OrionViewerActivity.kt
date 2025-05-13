package universe.constellation.orion.viewer

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Debug
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDialog
import androidx.core.internal.view.SupportMenuItem
import androidx.core.math.MathUtils
import androidx.core.view.doOnLayout
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.*
import universe.constellation.orion.viewer.FallbackDialogs.Companion.saveFileByUri
import universe.constellation.orion.viewer.FileUtil.beautifyFileSize
import universe.constellation.orion.viewer.Permissions.ASK_READ_PERMISSION_FOR_BOOK_OPEN
import universe.constellation.orion.viewer.Permissions.hasReadStoragePermission
import universe.constellation.orion.viewer.analytics.SHOW_ERROR_PANEL_DIALOG
import universe.constellation.orion.viewer.analytics.TAP_HELP_DIALOG
import universe.constellation.orion.viewer.android.getFileInfo
import universe.constellation.orion.viewer.android.isRestrictedAccessPath
import universe.constellation.orion.viewer.device.Device
import universe.constellation.orion.viewer.dialog.SearchDialog
import universe.constellation.orion.viewer.dialog.TapHelpDialog
import universe.constellation.orion.viewer.dialog.create
import universe.constellation.orion.viewer.document.Document
import universe.constellation.orion.viewer.layout.SimpleLayoutStrategy
import universe.constellation.orion.viewer.prefs.GlobalOptions
import universe.constellation.orion.viewer.prefs.OrionApplication
import universe.constellation.orion.viewer.prefs.initalizer
import universe.constellation.orion.viewer.selection.NewTouchProcessor
import universe.constellation.orion.viewer.selection.NewTouchProcessorWithScale
import universe.constellation.orion.viewer.selection.SelectionAutomata
import universe.constellation.orion.viewer.test.resetSettingInTest
import universe.constellation.orion.viewer.test.updateGlobalOptionsFromIntent
import universe.constellation.orion.viewer.view.FullScene
import universe.constellation.orion.viewer.view.OrionDrawScene
import universe.constellation.orion.viewer.view.StatusBar
import java.io.File
import java.util.concurrent.Executors
import kotlin.coroutines.resume

enum class MyState {
    PROCESSING_INTENT,
    WAITING_ACTION,
    FINISHED
}

class OrionViewerActivity : OrionBaseActivity(viewerType = Device.VIEWER_ACTIVITY) {

    internal val subscriptionManager = SubscriptionManager()

    private var lastPageInfo: LastPageInfo? = null

    var controller: Controller? = null
        private set

    private var myState: MyState = MyState.PROCESSING_INTENT

    @JvmField
    var _isResumed: Boolean = false

     val selectionAutomata: SelectionAutomata by lazy {
        SelectionAutomata(this)
    }

    private var newTouchProcessor: NewTouchProcessor? = null

    lateinit var fullScene: FullScene
        private set

    val view: OrionDrawScene
        get() = fullScene.drawView

    private val statusBarHelper: StatusBar
        get() = fullScene.statusBarHelper

    private var openAsTempTestBook = false

    internal lateinit var mainMenu: MainMenu

    var isNewUI: Boolean = false
        private set

    val bookId: Long
        get() {
            log("Selecting book id...")
            val info = lastPageInfo!!
            var bookId: Long? = orionApplication.tempOptions!!.bookId
            if (bookId == null || bookId == -1L) {
                bookId = orionApplication.getBookmarkAccessor().selectBookId(info.simpleFileName, info.fileSize)
                orionApplication.tempOptions!!.bookId = bookId
            }
            log("...book id = $bookId")
            return bookId
        }

    @SuppressLint("MissingSuperCall")
    public override fun onCreate(savedInstanceState: Bundle?) {
        log("Creating OrionViewerActivity...")
        openAsTempTestBook = updateGlobalOptionsFromIntent(intent)
        isNewUI = globalOptions.isNewUI
        orionApplication.viewActivity = this
        globalOptions.FULL_SCREEN.observe(this) { flag ->
            OptionActions.FULL_SCREEN.doAction(this, flag)
        }
        onOrionCreate(savedInstanceState, R.layout.main_view, !isNewUI)

        val mainMenuLayout = findViewById<LinearLayout>(R.id.main_menu)

        if (!isNewUI) {
            globalOptions.SHOW_ACTION_BAR.observe(this) { flag ->
                OptionActions.SHOW_ACTION_BAR.doAction(this, flag)
            }
            mainMenuLayout?.visibility = View.GONE
        } else {
            findViewById<View>(R.id.toolbar)?.visibility = View.GONE
        }

        val view = findViewById<OrionDrawScene>(R.id.view)

        fullScene = FullScene(findViewById<View>(R.id.orion_full_scene) as ViewGroup, view, findViewById<View>(R.id.orion_status_bar) as ViewGroup, this)
        fullScene.setDrawOffPage(globalOptions.isDrawOffPage)

        newTouchProcessor = NewTouchProcessorWithScale(view, this)
        view.setOnTouchListener{ _, event ->
            newTouchProcessor!!.onTouch(event)
        }
        processIntentAndCheckPermission(intent, true)

        mainMenu = MainMenu(mainMenuLayout!!, this)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (ASK_READ_PERMISSION_FOR_BOOK_OPEN == requestCode) {
            log("Permission callback $requestCode...")
            processIntentAndCheckPermission(intent ?: return)
        }
    }

    internal fun onNewIntentInternal(intent: Intent) {
        onNewIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        processIntentAndCheckPermission(intent, intent.getBooleanExtra(USER_INTENT, true))
    }

    private fun askReadPermissionOrOpenExisting(fileInfo: FileInfo, intent: Intent) {
        log("Checking permissions for: $fileInfo")
        myState = MyState.WAITING_ACTION
        if (fileInfo.isRestrictedAccessPath() || hasReadStoragePermission(this)) {
            FallbackDialogs().createPrivateResourceFallbackDialog(this, fileInfo, intent).show()
        } else {
            FallbackDialogs().createGrantReadPermissionsDialog(
                this@OrionViewerActivity,
                fileInfo,
                intent
            ).show()
        }
    }

    internal fun processIntentAndCheckPermission(intent: Intent, isUserIntent: Boolean = false) {
        log("Trying to open document by $intent...")
        analytics.onNewIntent(contentResolver, intent, isUserIntent, isNewUI)
        showErrorPanel(false)

        if (!openAsTempTestBook) {
            //UGLY hack: otherwise Espresso can't recognize that it's test activity
            setIntent(intent)
        }
        myState = MyState.PROCESSING_INTENT

        val uri = intent.data
        if (uri != null) {
            log("Try to open file by $uri")
            try {
                val fileInfo = getFileInfo(this, uri, analytics)
                val filePath = fileInfo?.path

                if (fileInfo == null || filePath.isNullOrBlank()) {
                    FallbackDialogs().createBadIntentFallbackDialog(this, null, intent).show()
                    destroyController()
                    return
                }

                if (controller != null && lastPageInfo != null) {
                    lastPageInfo?.apply {
                        if (openingFileName == filePath) {
                            log("Fast processing")
                            controller!!.drawPage(pageNumber, newOffsetX, newOffsetY, controller!!.pageLayoutManager.isSinglePageMode)
                            return
                        }
                    }
                }
                destroyController()

                val fileToOpen = if (!fileInfo.file.canRead()) {
                    val cacheFileIfExists =
                        getStableTmpFileIfExists(fileInfo)?.takeIf { it.length() == fileInfo.size }

                    if (cacheFileIfExists == null) {
                        askReadPermissionOrOpenExisting(fileInfo, intent)
                        log("Waiting for read permissions for $intent")
                        return
                    } else {
                        cacheFileIfExists
                    }
                } else {
                    fileInfo.file
                }


                if (fileToOpen.length() == 0L) {
                    showErrorAndErrorPanel(
                        getString(R.string.crash_on_book_opening_title),
                        resources.getString(
                            R.string.fileopen_cant_open,
                            getString(R.string.fileopen_file_is_emppty)
                        ),
                        intent,
                        sendException = RuntimeException("Warning: empty file, host=" + fileInfo.uri.host)
                    )
                    return
                }

                openFile(fileToOpen)
                myState = MyState.FINISHED
            } catch (e: Exception) {
                showErrorAndErrorPanel(
                    R.string.crash_on_intent_opening_title,
                    R.string.crash_on_intent_opening_title,
                    intent, e
                )
            }

        } else {
            analytics.error(RuntimeException("Unexpected state $intent"))
        }
    }

    @Throws(Exception::class)
    private fun openFile(file: File) {
        logMemoryState()
        log("openFileAndDestroyOldController")
        orionApplication.idlingRes.busy()

        GlobalScope.launch(Dispatchers.Main) {
            log("Trying to open file: $file")
            val rootJob = Job()
            val executor = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
            val newDocument = try {
                withContext(executor + rootJob) {
                    FileUtil.openFile(file)
                }
            } catch (e: Exception) {
                showErrorAndErrorPanel(
                    getString(R.string.crash_on_book_opening_title),
                    resources.getString(
                        R.string.crash_on_book_opening_message_header_panel,
                        file.name
                    ),
                    intent, e
                )
                executor.close()
                orionApplication.idlingRes.free()
                analytics.errorDuringInitialFileOpen()
                return@launch
            }

            try {
                if (!askPassword(newDocument)) {
                    showErrorOnFallbackPanel(
                        getString(R.string.crash_on_book_opening_encrypted),
                        intent
                    )
                    return@launch
                }

                if (newDocument.pageCount == 0) {
                    showErrorAndErrorPanel(
                        getString(R.string.crash_on_book_opening_title),
                        resources.getString(
                            R.string.fileopen_cant_open,
                            getString(R.string.fileopen_no_pages)
                        ),
                        intent,
                        sendException = RuntimeException("Warning: no pages in doc, host=" + intent.data?.host)
                    )
                    newDocument.destroy()
                    return@launch
                }

                val layoutStrategy = SimpleLayoutStrategy.create()
                val controller1 = Controller(this@OrionViewerActivity, newDocument, layoutStrategy, rootJob, context = executor)


                val lastPageInfo1 = loadBookParameters(rootJob, file)
                log("Read LastPageInfo for page ${lastPageInfo1.pageNumber}")
                lastPageInfo = lastPageInfo1
                orionApplication.currentBookParameters = lastPageInfo1

                controller = controller1
                bind(view, controller1)
                controller1.changeOrinatation(lastPageInfo1.screenOrientation)

                updateViewOnNewBook((newDocument.title?.takeIf { it.isNotBlank() } ?: file.name.substringBeforeLast(".")))

                val drawView = fullScene.drawView
                controller1.init(lastPageInfo1, drawView.sceneWidth, drawView.sceneHeight)

                subscriptionManager.sendDocOpenedNotification(controller1)

                globalOptions.addRecentEntry(GlobalOptions.RecentEntry(file.absolutePath))

                lastPageInfo1.totalPages = newDocument.pageCount
                orionApplication.onNewBook(file.name)
                invalidateOrHideMenu()
                doOnLayout(lastPageInfo1)
                analytics.fileOpenedSuccessfully(file)
            } catch (e: Exception) {
                if (controller != null) {
                    destroyController()
                } else {
                    newDocument.destroy()
                }
                analytics.errorDuringInitialFileOpen()
                showErrorAndErrorPanel(
                    R.string.crash_on_book_opening_title,
                    R.string.crash_on_book_opening_title,
                    intent,
                    e
                )
            } finally {
                orionApplication.idlingRes.free()
            }
        }
    }

    private fun logMemoryState() {
        log("Runtime.getRuntime().totalMemory(): ${Runtime.getRuntime().totalMemory().beautifyFileSize()}")
        log("Debug.getNativeHeapSize(): ${Debug.getNativeHeapSize().beautifyFileSize()}")
        log("TotalMemory: ${OrionApplication.getTotalMemory(orionApplication)?.beautifyFileSize()}")
    }

    private fun invalidateOrHideMenu() {
        if (isNewUI) {
            mainMenu.hideMenu()
        } else {
            invalidateOptionsMenu()
        }
    }

    private suspend fun loadBookParameters(
        rootJob: CompletableJob,
        bookFile: File
    ): LastPageInfo {
        if (openAsTempTestBook) {
            return loadBookParameters(
                this@OrionViewerActivity,
                "temp-test-bookx",
                initalizer(globalOptions)
            )
        }

        return withContext(Dispatchers.Default + rootJob) {
            loadBookParameters(
                this@OrionViewerActivity,
                bookFile.absolutePath,
                initalizer(globalOptions)
            )
        }
    }

    private fun bind(view: OrionDrawScene, controller: Controller) {
        this.controller = controller
        view.setDimensionAware(controller)
    }

    private fun updateViewOnNewBook(title: String?) {
        fullScene.onNewBook(title, controller!!.pageCount)
        supportActionBar?.title = title
    }

    public override fun onPause() {
        log("Orion: onPause")
        _isResumed = false
        super.onPause()
        controller?.let {
            it.onPause()
            saveBookPositionAndRecentFiles()
        }
        statusBarHelper.onPause(this)
    }

    private fun AppCompatDialog.initGoToPageScreen() {
        val pageSeeker = findMyViewById(R.id.page_picker_seeker) as SeekBar
        val pageNumberText = findMyViewById(R.id.page_picker_message) as TextView
        val plus = findMyViewById(R.id.page_picker_plus) as ImageButton
        val minus = findMyViewById(R.id.page_picker_minus) as ImageButton
        initPageNavControls(this@OrionViewerActivity, pageSeeker, minus, plus, pageNumberText)
        initPageNavigationValues(controller, pageSeeker, pageNumberText)
        pageNumberText.text = ((controller?.currentPage ?: 0) + 1).toString()

        val closePagePeeker = findMyViewById(R.id.option_dialog_bottom_close) as ImageButton
        closePagePeeker.setOnClickListener {
            dismiss()
        }

        val pagePreview = findMyViewById(R.id.option_dialog_bottom_apply) as ImageButton
        pagePreview.setOnClickListener {
            if (pageNumberText.text.isNotEmpty()) {
                try {
                    val userPage = Integer.valueOf(pageNumberText.text.toString())
                    val newPage = MathUtils.clamp(userPage, 1, controller!!.pageCount)
                    if (newPage != controller?.currentPage) {
                        controller?.drawPage(newPage - 1, isTapNavigation = true)
                        pageSeeker.progress = newPage - 1
                    }
                    dismiss()
                } catch (ex: NumberFormatException) {
                    showAndLogError(this@OrionViewerActivity, "Couldn't parse " + pageNumberText.text, ex)
                }
            }
        }
        pageNumberText.requestFocus()
    }

    private fun AppCompatDialog.initZoomScreen() {
        //zoom screen
        val spinner = findMyViewById(R.id.zoom_spinner) as Spinner
        val zoomValueAsText = findMyViewById(R.id.zoom_picker_message) as EditText
        val zoomSeek = findMyViewById(R.id.zoom_picker_seeker) as SeekBar
        zoomSeek.max = 300
        zoomSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                zoomValueAsText.setText("$progress")
                if (spinner.selectedItemPosition != 0) {
                    spinner.setSelection(0)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        val zoomPlus = findMyViewById(R.id.zoom_picker_plus) as ImageButton
        zoomPlus.setOnClickListener { zoomSeek.incrementProgressBy(1) }

        val zoomMinus = findMyViewById(R.id.zoom_picker_minus) as ImageButton
        zoomMinus.setOnClickListener {
            if (zoomSeek.progress != 0) {
                zoomSeek.incrementProgressBy(-1)
            }
        }

        val closeZoomPicker = findMyViewById(R.id.option_dialog_bottom_close) as ImageButton
        closeZoomPicker.setOnClickListener {
            dismiss()
        }

        val zoomPreview = findMyViewById(R.id.option_dialog_bottom_apply) as ImageButton
        zoomPreview.setOnClickListener {
            onApplyAction()
            val index = spinner.selectedItemPosition
            controller!!.changeZoom(if (index == 0) (java.lang.Float.parseFloat(zoomValueAsText.text.toString()) * 100).toInt() else -1 * (index - 1))
        }

        spinner.adapter = MyArrayAdapter(context)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val disableChangeButtons = position != 0

                if (disableChangeButtons) {
                    zoomValueAsText.setText(parent.adapter.getItem(position) as String)
                } else {
                    zoomValueAsText.setText("${zoomSeek.progress}")
                }

                zoomMinus.visibility = if (disableChangeButtons) View.GONE else View.VISIBLE
                zoomPlus.visibility = if (disableChangeButtons) View.GONE else View.VISIBLE

                zoomValueAsText.isFocusable = !disableChangeButtons
                zoomValueAsText.isFocusableInTouchMode = !disableChangeButtons

                val parent1 = zoomValueAsText.parent as LinearLayout

                parent1.post { parent1.requestLayout() }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
            }
        }

        //by width
        spinner.setSelection(1)
        actualizeZoomOptions()
    }

    private fun AppCompatDialog.actualizeZoomOptions() {
        val zoomSeek = findMyViewById(R.id.zoom_picker_seeker) as SeekBar
        val textView = findMyViewById(R.id.zoom_picker_message) as TextView
        val spinner = findMyViewById(R.id.zoom_spinner) as Spinner

        var zoom = controller!!.zoom10000Factor
        val spinnerIndex: Int
        if (zoom in -3..0) {
            spinnerIndex = -zoom + 1
            zoom = (10000 * controller!!.currentPageZoom).toInt()
        } else {
            spinnerIndex = 0
            textView.text = (zoom / 100f).toString()
        }
        zoomSeek.progress = zoom / 100
        spinner.setSelection(spinnerIndex)
    }

    private fun AppCompatDialog.initAddBookmarkScreen() {
        val close = findMyViewById(R.id.option_dialog_bottom_close) as ImageButton
        close.setOnClickListener {
            //main menu
            dismiss()
        }


        val view = findMyViewById(R.id.option_dialog_bottom_apply) as ImageButton
        view.setOnClickListener {
            val text = findMyViewById(R.id.add_bookmark_text) as EditText
            try {
                insertBookmark(controller!!.currentPage, text.text.toString())
                dismiss()
            } catch (e: Exception) {
                e.printStackTrace()
                analytics.error(e)

                //TODO show common error dialog
                val buider = createThemedAlertBuilder()
                buider.setTitle(resources.getString(R.string.ex_msg_operation_failed))
                val input = EditText(this@OrionViewerActivity)
                input.setText(e.message)
                buider.setView(input)
                buider.setNeutralButton("OK") { dialog, _ -> dialog.dismiss() }
                buider.create().show()
            }
        }

    }

    override fun onResume() {
        _isResumed = true
        super.onResume()
        updateBrightness()
        log("onResume")

        if (controller != null) {
            analytics.action("onResumeOpenedBook")
            controller!!.processPendingEvents()
        }
        statusBarHelper.onResume(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        log("onDestroy")
        destroyController()
        orionApplication.destroyMainActivity()
        if (openAsTempTestBook) {
            resetSettingInTest(intent)
        }
    }

    private fun saveBookPositionAndRecentFiles() {
        try {
            lastPageInfo?.let {
                if (!openAsTempTestBook) {
                    controller?.serializeAndSave(it, this)
                }
            }
        } catch (ex: Exception) {
            log(ex)
        }
        globalOptions.saveRecentFiles()
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        log("onKeyUp key = " + keyCode + " " + event.isCanceled + " " + doKeyTrack(keyCode))
        if (event.isCanceled) {
            log("Tracking = $keyCode")
            return super.onKeyUp(keyCode, event)
        }

        return processKey(keyCode, event, false) || super.onKeyUp(keyCode, event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        log("onKeyDown = " + keyCode + " " + event.isCanceled + " " + doKeyTrack(keyCode))
        if (doKeyTrack(keyCode, orionApplication.keyBindingPrefs)) {
            log("Tracking = $keyCode")
            event.startTracking()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun processKey(keyCode: Int, event: KeyEvent, isLong: Boolean): Boolean {
        log("key = $keyCode isLong = $isLong")

        val actionCode = orionApplication.keyBindingPrefs.getInt(getPrefKey(keyCode, isLong), -1)
        if (actionCode != -1) {
            when (val action = Action.getAction(actionCode)) {
                Action.PREV, Action.NEXT -> {
                    changePage(if (action === Action.PREV) Device.PREV else Device.NEXT)
                    return true
                }
                Action.NONE -> {
                }
                else -> {
                    doAction(action)
                    return true
                }
            }
        }

        val resultHolder = OperationHolder()
        if (device!!.onKeyUp(keyCode, event.isLongPress, resultHolder)) {
            changePage(resultHolder.value)
            return true
        }
        return false
    }

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent): Boolean {
        return processKey(keyCode, event, true)
    }

    private fun changePage(operation: Int) {
        val swapKeys = globalOptions.isSwapKeys
        val width = view.sceneWidth
        val height = view.sceneHeight
        val controller1 = controller
        if (controller1 != null) {
            val landscape = width > height || controller1.rotation != 0 /*second condition for nook and alex*/
            if (operation == Device.NEXT && (!landscape || !swapKeys) || swapKeys && operation == Device.PREV && landscape) {
                controller1.drawNext()
            } else {
                controller1.drawPrev()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (!isNewUI) {
            val disable = controller == null
            if (!disable) {
                menuInflater.inflate(R.menu.menu, menu)
            } else {
                menuInflater.inflate(R.menu.menu_disabled, menu)
            }
            if (!globalOptions.isActionBarVisible) {
                for (i in 0 until menu.size()) {
                    val item = menu.getItem(i)
                    item.setShowAsAction(SupportMenuItem.SHOW_AS_ACTION_NEVER)
                }
            }
        }
        return !isNewUI
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (doMenuAction(item.itemId)) {
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    internal fun doMenuAction(id: Int): Boolean {
        val action = when (id) {
            R.id.exit_menu_item ->  Action.CLOSE_ACTION
            R.id.search_menu_item ->  Action.SEARCH
            R.id.crop_menu_item ->  Action.CROP
            R.id.zoom_menu_item ->  Action.ZOOM
            R.id.add_bookmark_menu_item ->  Action.ADD_BOOKMARK
            R.id.goto_menu_item ->  Action.GOTO
            R.id.select_text_menu_item ->  Action.SELECT_TEXT
            R.id.options_menu_item ->  Action.OPTIONS
            R.id.book_options_menu_item ->  Action.BOOK_OPTIONS
            R.id.outline_menu_item ->  Action.SHOW_OUTLINE
            R.id.open_menu_item ->  Action.OPEN_BOOK
            R.id.open_dictionary_menu_item ->  Action.DICTIONARY

            R.id.bookmarks_menu_item ->  Action.OPEN_BOOKMARKS
            R.id.share_menu_item -> Action.SHARE_FILE
            R.id.help_menu_item, R.id.about_menu_item -> {
                openHelpActivity(id)
                return true
            }
            else -> return false
        }
        doAction(action)
        return true
    }

    private fun createOptionDialog(screenId: Int): AppCompatDialog? {
        val dialog = if (CROP_SCREEN == screenId) {
            create(this, controller?.margins ?: return null)
        } else {
            val dialog = AppCompatDialog(this)
            dialog.supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setCanceledOnTouchOutside(true)

            when (screenId) {
                PAGE_SCREEN -> {
                    dialog.setContentView(R.layout.goto_dialog)
                    dialog.initGoToPageScreen()
                }

                ZOOM_SCREEN -> {
                    dialog.setContentView(R.layout.zoom_dialog)
                    dialog.initZoomScreen()
                }

                ADD_BOOKMARK_SCREEN -> {
                    dialog.setContentView(R.layout.add_bookmark_dialog)
                    dialog.initAddBookmarkScreen()
                }

                else -> errorInDebugOr("Unknown id = $screenId") { return dialog }
            }

            val displayWidth = resources.displayMetrics.widthPixels
            val limitWidth = dpToPixels(750f)
            val width = if (displayWidth > limitWidth) {
                dpToPixels(700f)
            } else {
                WindowManager.LayoutParams.MATCH_PARENT
            }

            dialog.window?.setLayout(
                width,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            dialog
        }

        return dialog
    }

    fun doAction(actionCode: Int) {
        val action = Action.getAction(actionCode)
        doAction(action)
        log("Code action $actionCode")
    }


    internal fun doAction(action: Action) {
        action.doAction(controller, this, null)
    }

    fun AppCompatDialog.findMyViewById(id: Int): View {
        return findViewById<View>(id) as View
    }

    fun AppCompatDialog.onApplyAction() {
        if (globalOptions.isApplyAndClose) {
            dismiss()
        }
    }

    private fun updateBrightness() {
        val params = window.attributes
        val oldBrightness = params.screenBrightness
        if (globalOptions.isCustomBrightness) {
            params.screenBrightness = globalOptions.brightness.toFloat() / 100
            window.attributes = params
        } else {
            if (oldBrightness >= 0) {
                params.screenBrightness = -1f
                window.attributes = params
            }
        }
    }

    private fun insertOrGetBookId(): Long {
        val info = lastPageInfo!!
        var bookId: Long? = orionApplication.tempOptions!!.bookId
        if (bookId == null || bookId == -1L) {
            bookId = orionApplication.getBookmarkAccessor()
                .insertOrUpdate(info.simpleFileName, info.fileSize)
            orionApplication.tempOptions!!.bookId = bookId
        }
        return bookId.toInt().toLong()
    }

    private fun insertBookmark(page: Int, text: String): Boolean {
        val id = insertOrGetBookId()
        if (id != -1L) {
            val bokmarkId =
                orionApplication.getBookmarkAccessor().insertOrUpdateBookmark(id, page, text)
            return bokmarkId != -1L
        }
        return false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        log("On activity result requestCode=$requestCode resultCode=$resultCode data=$data originalIntent=$intent")
        when (requestCode) {
            OPEN_BOOKMARK_ACTIVITY_RESULT -> {
                if (resultCode == Activity.RESULT_OK) {
                    if (controller != null) {
                        val page = data!!.getIntExtra(OrionBookmarkActivity.OPEN_PAGE, -1)
                        if (page != -1) {
                            controller!!.drawPage(page)
                        } else {
                            doAction(Action.GOTO)
                        }
                    }
                }
            }

            SAVE_FILE_RESULT -> {
                if (resultCode == Activity.RESULT_OK) {
                    val inputFileIntentData = intent.data
                    if (data?.data != null && inputFileIntentData != null) {
                        analytics.action("saveAs")
                        saveFileByUri(intent,
                            inputFileIntentData,
                            data.data!!,
                            CoroutineExceptionHandler { _, exception ->
                                showErrorAndErrorPanel(
                                    R.string.error_on_file_saving_title,
                                    R.string.error_on_file_saving_title,
                                    intent,
                                    exception
                                )
                            }) {
                            onNewIntent(data)
                        }
                        return
                    }
                }
                processIntentAndCheckPermission(intent ?: return)
            }

            PERMISSION_READ_RESULT ->
                processIntentAndCheckPermission(intent ?: return)
        }
    }

    fun showOrionDialog(screenId: Int, action: Action?, parameter: Any?) {
        if (screenId != -1) {
            val dialog = createOptionDialog(screenId) ?: return

            if (action === Action.ADD_BOOKMARK) {
                val parameterText = parameter as String?

                val page = controller!!.currentPage
                val newText = orionApplication.getBookmarkAccessor()
                    .selectExistingBookmark(bookId, page, parameterText)

                val notOverride = parameterText == null || parameterText == newText
                dialog.findMyViewById(R.id.warn_text_override).visibility =
                    if (notOverride) View.GONE else View.VISIBLE

                (dialog.findMyViewById(R.id.add_bookmark_text) as EditText).setText(if (notOverride) newText else parameterText)
            }

            dialog.show()
        }
    }


    fun textSelectionMode(isSingleSelection: Boolean, translate: Boolean) {
        selectionAutomata.startTextSelection(isSingleSelection, translate)
    }

    private class MyArrayAdapter(context: Context) :
        ArrayAdapter<CharSequence>(
            context,
            R.layout.support_simple_spinner_dropdown_item,
            context.resources.getTextArray(R.array.fits)
        ), SpinnerAdapter {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
            convertView ?: TextView(parent.context).apply {
                text = " % "
            }
    }

    private suspend fun askPassword(controller: Document): Boolean {
        if (controller.needPassword()) {
            val view = layoutInflater.inflate(R.layout.password, null)
            val builder = createThemedAlertBuilder()

            builder.setView(view)
                .setNegativeButton(R.string.string_cancel) { dialog, _ -> dialog.cancel() }
                .setPositiveButton(R.string.string_apply) { _, _ -> }


            val dialog = builder.create()
            dialog.setCanceledOnTouchOutside(false)
            dialog.show()

            return suspendCancellableCoroutine { continuation ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    lifecycleScope.launch {
                        val input = view.findViewById<TextInputEditText>(R.id.password)!!
                        if (controller.authenticate(input.text.toString())) {
                            dialog.dismiss()
                            continuation.resume(true)
                        } else {
                            input.error = getString(R.string.string_wrong_password)
                        }
                    }
                }

                dialog.setOnCancelListener {
                    continuation.resume(false)
                }

                continuation.invokeOnCancellation {
                    controller.destroy()
                    dialog.cancel()
                }
            }
        } else {
            return true
        }
    }

    private fun doOnLayout(lastPageInfo1: LastPageInfo) {
        (view as View).doOnLayout {
            if (globalOptions.isShowTapHelp) {
                TapHelpDialog().show(supportFragmentManager, "TAP_HELP")

                globalOptions.saveBooleanProperty(GlobalOptions.SHOW_TAP_HELP, false)
                analytics.dialog(TAP_HELP_DIALOG, true)
            }
            controller?.drawPage(
                lastPageInfo1.pageNumber,
                lastPageInfo1.newOffsetX,
                lastPageInfo1.newOffsetY,
                lastPageInfo1.isSinglePageMode
            )
            controller?.pageLayoutManager?.updateCacheAndRender()
        }
    }

    fun startSearch() {
        if (controller != null) {
            SearchDialog.newInstance().show(supportFragmentManager, "search")
        }
    }

    private fun destroyController() {
        log("Controller: destroy")
        view.setDimensionAware(null)
        controller?.destroy()
        controller = null
        orionApplication.currentBookParameters = null
    }

    fun showMenu() {
        if (isNewUI) {
            mainMenu.showMenu()
        } else {
            toolbar.showOverflowMenu()
        }
    }

    fun hideMenu() {
        if (isNewUI) {
            mainMenu.hideMenu()
        }
    }

    internal fun showErrorAndErrorPanel(
        dialogTitle: Int,
        messageTitle: Int,
        intent: Intent,
        exception: Throwable? = null
    ) {
        showErrorAndErrorPanel(
            resources.getString(dialogTitle),
            resources.getString(messageTitle),
            intent,
            exception
        )
    }

    private fun showErrorAndErrorPanel(
        dialogTitle: String,
        message: String,
        intent: Intent,
        exception: Throwable? = null,
        sendException: Throwable? = exception
    ) {
        if (sendException != null) {
            log(sendException)
            analytics.error(sendException, "$message $intent")
        } else {
            logError("$message $intent")
            analytics.logWarning("$message $intent")
        }
        showErrorOnFallbackPanel(message, intent, null, exception = exception)

        if (!isFinishing) {
            val dialog = createThemedAlertBuilder()
                .setPositiveButton(R.string.string_close) { dialog, _ ->
                    dialog.dismiss()
                }
                .setOnDismissListener {
                    analytics.dialog(SHOW_ERROR_PANEL_DIALOG, false)
                }
                .setTitle(dialogTitle)
                .setMessage(message + if (exception != null) "\n\n" + exception.message else "")
                .create()
            dialog.show()
        } else {
            analytics.logWarning("showErrorAndErrorPanel in finishing activity")
        }
    }

    fun showErrorOnFallbackPanel(
        message: String,
        intent: Intent,
        info: String? = null,
        cause: String? = null,
        exception: Throwable? = null
    ) {
        val problemView = findViewById<View>(R.id.problem_view)
        problemView.findViewById<TextView>(R.id.crash_message_header).text = message
        problemView.findViewById<TextView>(R.id.crash_intent_message).text = intent.toString()
        problemView.findViewById<TextView>(R.id.crash_cause_message).text =
            cause ?: prepareFullErrorMessage(
                intent,
                info,
                exception,
                false,
                false
            ).takeIf { it.isNotBlank() } ?: "<Absent>"

        problemView.findViewById<TextView>(R.id.crash_exception_message).text =
            exception?.stackTraceToString() ?: "<Absent>"

        showErrorPanel(true)

        problemView.findViewById<ImageView>(R.id.crash_close).setOnClickListener {
            Action.CLOSE_ACTION.doAction(this)
        }
        problemView.findViewById<ImageView>(R.id.crash_open_book).setOnClickListener {
            Action.OPEN_BOOK.doAction(this)
        }
    }

    private fun showErrorPanel(show: Boolean) {
        invalidateOptionsMenu()
        findViewById<View>(R.id.orion_full_scene)!!.visibility =
            if (!show) View.VISIBLE else View.INVISIBLE
        findViewById<View>(R.id.problem_view)!!.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            view.pageLayoutManager = null
        }
    }

    companion object {

        val BOOK_MENU_ITEMS = setOf(
            R.id.search_menu_item,
            R.id.crop_menu_item,
            R.id.zoom_menu_item,
            R.id.add_bookmark_menu_item,
            R.id.goto_menu_item,
            R.id.select_text_menu_item,
            R.id.book_options_menu_item,
            R.id.outline_menu_item,
            R.id.bookmarks_menu_item,
            R.id.open_dictionary_menu_item
        )

        const val OPEN_BOOKMARK_ACTIVITY_RESULT = 1

        const val SAVE_FILE_RESULT = 2

        const val PERMISSION_READ_RESULT = Permissions.ASK_READ_PERMISSION_FOR_BOOK_OPEN

        const val PAGE_SCREEN = 0

        const val ZOOM_SCREEN = 1

        const val CROP_SCREEN = 2

        const val ADD_BOOKMARK_SCREEN = 3

        const val CROP_RESTRICTION_MIN = -10

        const val CROP_RESTRICTION_MAX = 40

        const val USER_INTENT = "USER_INTENT"
    }
}
