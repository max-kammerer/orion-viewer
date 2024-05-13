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
import universe.constellation.orion.viewer.layout.SimpleLayoutStrategy
import universe.constellation.orion.viewer.prefs.GlobalOptions
import universe.constellation.orion.viewer.prefs.initalizer
import universe.constellation.orion.viewer.selection.NewTouchProcessor
import universe.constellation.orion.viewer.selection.NewTouchProcessorWithScale
import universe.constellation.orion.viewer.selection.SelectionAutomata
import universe.constellation.orion.viewer.view.FullScene
import universe.constellation.orion.viewer.view.OrionDrawScene
import universe.constellation.orion.viewer.view.OrionStatusBarHelper
import java.io.File
import java.util.concurrent.Executors
import kotlin.coroutines.resume

enum class MyState {
    PROCESSING_INTENT,
    WAITING_ACTION,
    FINISHED
}

class OrionViewerActivity : OrionBaseActivity(viewerType = Device.VIEWER_ACTIVITY) {

    private var dialog: AppCompatDialog? = null

    internal val subscriptionManager = SubscriptionManager()

    private var animator: ViewAnimator? = null

    private var lastPageInfo: LastPageInfo? = null

    var controller: Controller? = null
        private set

    val globalOptions: GlobalOptions by lazy {
        orionContext.options
    }

    private var myState: MyState = MyState.PROCESSING_INTENT

    @JvmField
    var _isResumed: Boolean = false

    private val selectionAutomata: SelectionAutomata by lazy {
        SelectionAutomata(this)
    }

    private var newTouchProcessor: NewTouchProcessor? = null

    private var hasActionBar: Boolean = false

    lateinit var fullScene: FullScene
        private set

    val view: OrionDrawScene
        get() = fullScene.drawView

    val statusBarHelper: OrionStatusBarHelper
        get() = fullScene.statusBarHelper

    private var openAsTempTestBook = false

    internal lateinit var mainMenu: MainMenu

    var isNewUI: Boolean = false
        private set

    val bookId: Long
        get() {
            log("Selecting book id...")
            val info = lastPageInfo!!
            var bookId: Long? = orionContext.tempOptions!!.bookId
            if (bookId == null || bookId == -1L) {
                bookId = orionContext.getBookmarkAccessor().selectBookId(info.simpleFileName, info.fileSize)
                orionContext.tempOptions!!.bookId = bookId
            }
            log("...book id = $bookId")
            return bookId
        }

    @SuppressLint("MissingSuperCall")
    public override fun onCreate(savedInstanceState: Bundle?) {
        log("Creating OrionViewerActivity...")
        updateGlobalOptionsFromIntent(intent)
        isNewUI = globalOptions.isNewUI
        orionContext.viewActivity = this
        OptionActions.FULL_SCREEN.doAction(this, !globalOptions.isFullScreen, globalOptions.isFullScreen)
        onOrionCreate(savedInstanceState, R.layout.main_view, !isNewUI)

        hasActionBar = globalOptions.isActionBarVisible
        if (!isNewUI) {
            OptionActions.SHOW_ACTION_BAR.doAction(this, !hasActionBar, hasActionBar)
            findViewById<ViewGroup>(R.id.main_menu)?.visibility = View.GONE
        } else {
            findViewById<View>(R.id.toolbar)?.visibility = View.GONE
        }

        val view = findViewById<OrionDrawScene>(R.id.view)

        fullScene = FullScene(findViewById<View>(R.id.orion_full_scene) as ViewGroup, view, findViewById<View>(R.id.orion_status_bar) as ViewGroup, orionContext)

        OptionActions.SHOW_STATUS_BAR.doAction(this, !globalOptions.isStatusBarVisible, globalOptions.isStatusBarVisible)
        OptionActions.SHOW_OFFSET_ON_STATUS_BAR.doAction(this, !globalOptions.isShowOffsetOnStatusBar, globalOptions.isShowOffsetOnStatusBar)
        fullScene.setDrawOffPage(globalOptions.isDrawOffPage)

        initDialogs()

        newTouchProcessor = NewTouchProcessorWithScale(view, this)
        view.setOnTouchListener{ _, event ->
            newTouchProcessor!!.onTouch(event)
        }
        processIntentAndCheckPermission(intent, true)

        mainMenu = MainMenu(findViewById<LinearLayout>(R.id.main_menu)!!, this)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (ASK_READ_PERMISSION_FOR_BOOK_OPEN == requestCode) {
            println("Permission callback $requestCode...")
            processIntentAndCheckPermission(intent ?: return)
        }
    }

    private fun initDialogs() {
        initOptionDialog()
        initRotationScreen()

        //page chooser
        initGoToPageScreen()

        initZoomScreen()

        initPageLayoutScreen()

        initAddBookmarkScreen()
    }

    fun updatePageLayout() {
        val walkOrder = controller!!.direction
        val lid = controller!!.layout
        (findMyViewById(R.id.layoutGroup) as RadioGroup).check(if (lid == 0) R.id.layout1 else if (lid == 1) R.id.layout2 else R.id.layout3)
        //((RadioGroup) findMyViewById(R.id.directionGroup)).check(did == 0 ? R.id.direction1 : did == 1 ? R.id.direction2 : R.id.direction3);

        val group = findMyViewById(R.id.directionGroup) as RadioGroup
        for (i in 0 until group.childCount) {
            val child = group.getChildAt(i)
            if (child is universe.constellation.orion.viewer.android.RadioButton) {
                if (walkOrder == child.walkOrder) {
                    group.check(child.id)
                }
            }
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
                val fileInfo = getFileInfo(this, uri)
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
                            controller!!.drawPage(pageNumber, newOffsetX, newOffsetY)
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
                        intent, null
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

        } else
        /*if (intent.getAction().endsWith("MAIN"))*/ {
            //TODO error
        }
    }

    @Throws(Exception::class)
    private fun openFile(file: File) {
        log("Runtime.getRuntime().totalMemory(): ${Runtime.getRuntime().totalMemory()}")
        log("Debug.getNativeHeapSize(): ${Debug.getNativeHeapSize()}")
        log("openFileAndDestroyOldController")

        orionContext.idlingRes.busy()

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
                orionContext.idlingRes.free()
                analytics.errorDuringInitialFileOpen()
                return@launch
            }

            try {
                val layoutStrategy = SimpleLayoutStrategy.create()
                val controller1 = Controller(this@OrionViewerActivity, newDocument, layoutStrategy, rootJob, context = executor)

                if (!askPassword(controller1)) {
                    return@launch
                }

                val lastPageInfo1 = loadBookParameters(rootJob, file)
                log("Read LastPageInfo for page ${lastPageInfo1.pageNumber}")
                lastPageInfo = lastPageInfo1
                orionContext.currentBookParameters = lastPageInfo1

                controller = controller1
                bind(view, controller1)
                controller1.changeOrinatation(lastPageInfo1.screenOrientation)

                updateViewOnNewBook((newDocument.title?.takeIf { it.isNotBlank() } ?: file.name.substringBeforeLast(".")))

                val drawView = fullScene.drawView
                controller1.init(lastPageInfo1, drawView.sceneWidth, drawView.sceneHeight)

                subscriptionManager.sendDocOpenedNotification(controller1)

                globalOptions.addRecentEntry(GlobalOptions.RecentEntry(file.absolutePath))

                lastPageInfo1.totalPages = newDocument.pageCount
                orionContext.onNewBook(file.name)
                invalidateOrHideMenu()
                doOnLayout(lastPageInfo1)
                analytics.fileOpenedSuccessfully(file)
            } catch (e: Exception) {
                analytics.errorDuringInitialFileOpen()
                log(e)
                throw e
            } finally {
                orionContext.idlingRes.free()
            }
        }
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
    }

    private fun initGoToPageScreen() {
        val pageSeek = findMyViewById(R.id.page_picker_seeker) as SeekBar

        subscriptionManager.addDocListeners(object : DocumentViewAdapter() {
            override fun documentOpened(controller: Controller) {
                pageSeek.max = controller.pageCount - 1
            }

            override fun pageChanged(newPage: Int, pageCount: Int) {
                pageSeek.progress = newPage
            }
        })


        val pageNumberText = findMyViewById(R.id.page_picker_message) as TextView
        pageNumberText.text = 1.toString()

        pageSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            @SuppressLint("SetTextI18n")
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                pageNumberText.text = (progress + 1).toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        val plus = findMyViewById(R.id.page_picker_plus) as ImageButton
        plus.setOnClickListener {
            if (pageSeek.progress != pageSeek.max) {
                pageSeek.incrementProgressBy(1)
            }
        }

        val minus = findMyViewById(R.id.page_picker_minus) as ImageButton
        minus.setOnClickListener {
            if (pageSeek.progress != 0) {
                pageSeek.incrementProgressBy(-1)
            }
        }

        val closePagePeeker = findMyViewById(R.id.page_picker_close) as ImageButton
        closePagePeeker.setOnClickListener {
            onAnimatorCancel()
        }

        val pagePreview = findMyViewById(R.id.page_preview) as ImageButton
        pagePreview.setOnClickListener {
            onApplyAction()
            if (pageNumberText.text.isNotEmpty()) {
                try {
                    val userPage = Integer.valueOf(pageNumberText.text.toString())
                    val newPage = MathUtils.clamp(userPage, 1, controller!!.pageCount)
                    controller!!.drawPage(newPage - 1)
                } catch (ex: NumberFormatException) {
                    showAndLogError(this, "Couldn't parse " + pageNumberText.text, ex)
                }
            }
        }
    }

    private fun updatePageSeeker() {
        val pageSeek = findMyViewById(R.id.page_picker_seeker) as SeekBar
        pageSeek.progress = controller!!.currentPage
        val view = findMyViewById(R.id.page_picker_message) as TextView
        view.text = (controller!!.currentPage + 1).toString()
        view.clearFocus()
        view.requestFocus()
    }

    private fun initZoomScreen() {
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

        subscriptionManager.addDocListeners(object : DocumentViewAdapter() {
            override fun documentOpened(controller: Controller) {
                actualizeZoomOptions()
            }
        })

        val zoomPlus = findMyViewById(R.id.zoom_picker_plus) as ImageButton
        zoomPlus.setOnClickListener { zoomSeek.incrementProgressBy(1) }

        val zoomMinus = findMyViewById(R.id.zoom_picker_minus) as ImageButton
        zoomMinus.setOnClickListener {
            if (zoomSeek.progress != 0) {
                zoomSeek.incrementProgressBy(-1)
            }
        }

        val closeZoomPicker = findMyViewById(R.id.zoom_picker_close) as ImageButton
        closeZoomPicker.setOnClickListener {
            onAnimatorCancel()
        }

        val zoomPreview = findMyViewById(R.id.zoom_preview) as ImageButton
        zoomPreview.setOnClickListener {
            onApplyAction()
            val index = spinner.selectedItemPosition
            controller!!.changeZoom(if (index == 0) (java.lang.Float.parseFloat(zoomValueAsText.text.toString()) * 100).toInt() else -1 * (index - 1))
        }

        spinner.adapter = MyArrayAdapter(applicationContext)
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
    }

    private fun actualizeZoomOptions() {
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


    private fun initPageLayoutScreen() {
        val close = findMyViewById(R.id.options_close) as ImageButton
        close.setOnClickListener {
            onAnimatorCancel()
            updatePageLayout()
        }


        val view = findMyViewById(R.id.options_apply) as ImageButton
        view.setOnClickListener {
            onApplyAction()
            val group = findMyViewById(R.id.directionGroup) as RadioGroup
            val walkOrderButtonId = group.checkedRadioButtonId
            val button = group.findViewById<View>(walkOrderButtonId) as universe.constellation.orion.viewer.android.RadioButton
            val lid = (findMyViewById(R.id.layoutGroup) as RadioGroup).checkedRadioButtonId
            controller!!.setDirectionAndLayout(button.walkOrder, if (lid == R.id.layout1) 0 else if (lid == R.id.layout2) 1 else 2)
        }

        subscriptionManager.addDocListeners(object : DocumentViewAdapter() {
            override fun documentOpened(controller: Controller) {
                updatePageLayout()
            }
        })
    }


    private fun initAddBookmarkScreen() {
        val close = findMyViewById(R.id.add_bookmark_close) as ImageButton
        close.setOnClickListener {
            //main menu
            onAnimatorCancel()
        }


        val view = findMyViewById(R.id.add_bookmark_apply) as ImageButton
        view.setOnClickListener {
            val text = findMyViewById(R.id.add_bookmark_text) as EditText
            try {
                insertBookmark(controller!!.currentPage, text.text.toString())
                onApplyAction(true)
            } catch (e: Exception) {
                e.printStackTrace()
                val activity = this@OrionViewerActivity
                val buider = createThemedAlertBuilder()
                buider.setTitle(activity.resources.getString(R.string.ex_msg_operation_failed))

                val input = EditText(activity)
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
            //controller!!.drawPage(lastPageInfo!!.pageNumber, lastPageInfo!!.newOffsetX, lastPageInfo!!.newOffsetY)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        log("onDestroy")
        destroyController()

        if (dialog != null) {
            dialog!!.dismiss()
        }
        orionContext.destroyMainActivity()
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
        saveGlobalOptions()
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        log("onKeyUp key = " + keyCode + " " + event.isCanceled + " " + doTrack(keyCode))
        if (event.isCanceled) {
            log("Tracking = $keyCode")
            return super.onKeyUp(keyCode, event)
        }

        return processKey(keyCode, event, false) || super.onKeyUp(keyCode, event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        log("onKeyDown = " + keyCode + " " + event.isCanceled + " " + doTrack(keyCode))
        if (doTrack(keyCode)) {
            log("Tracking = $keyCode")
            event.startTracking()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun processKey(keyCode: Int, event: KeyEvent, isLong: Boolean): Boolean {
        log("key = $keyCode isLong = $isLong")

        val actionCode = orionContext.keyBinding.getInt(getPrefKey(keyCode, isLong), -1)
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
        val landscape = width > height || controller!!.rotation != 0 /*second condition for nook and alex*/
        if (controller != null) {
            if (operation == Device.NEXT && (!landscape || !swapKeys) || swapKeys && operation == Device.PREV && landscape) {
                controller!!.drawNext()
            } else {
                controller!!.drawPrev()
            }
        }
    }

    private fun saveGlobalOptions() {
        log("Saving global options...")
        globalOptions.saveRecents()
        log("Done!")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (!isNewUI) {
            val disable = controller == null
            if (!disable) {
                menuInflater.inflate(R.menu.menu, menu)
            } else {
                menuInflater.inflate(R.menu.menu_disabled, menu)
            }
            if (!hasActionBar) {
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
            R.id.help_menu_item, R.id.about_menu_item -> {
                openHelpActivity(id)
                return true
            }
            else -> return false
        }
        doAction(action)
        return true
    }

    private fun initOptionDialog() {
        dialog = AppCompatDialog(this)
        dialog!!.supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog!!.setContentView(R.layout.options_dialog)
        animator = dialog!!.findViewById<View>(R.id.viewanim) as ViewAnimator?
        dialog!!.setCanceledOnTouchOutside(true)
    }

    fun doAction(actionCode: Int) {
        val action = Action.getAction(actionCode)
        doAction(action)
        log("Code action $actionCode")
    }


    internal fun doAction(action: Action) {
        action.doAction(controller, this, null)
    }


    override fun findMyViewById(id: Int): View {
        return dialog!!.findViewById<View>(id) as View
    }

    public override fun onAnimatorCancel() {
        dialog!!.cancel()
    }

    override fun onApplyAction() {
        onApplyAction(false)
    }

    private fun onApplyAction(close: Boolean) {
        if (close || globalOptions.isApplyAndClose) {
            onAnimatorCancel()
        }
    }

    private fun initRotationScreen() {
        val rotationGroup = findMyViewById(R.id.rotationGroup) as RadioGroup
        rotationGroup.visibility = View.GONE

        val list = findMyViewById(R.id.rotationList) as ListView

        //set choices and replace 0 one with Application Default
        val isLevel9 = orionContext.sdkVersion >= 9
        val values = resources.getTextArray(if (isLevel9) R.array.screen_orientation_full_desc else R.array.screen_orientation_desc)
        val newValues = arrayOfNulls<CharSequence>(values.size)
        System.arraycopy(values, 0, newValues, 0, values.size)
        newValues[0] = resources.getString(R.string.orientation_default_rotation)

        list.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_single_choice, newValues)

        list.choiceMode = ListView.CHOICE_MODE_SINGLE
        list.setItemChecked(0, true)

        list.onItemClickListener = AdapterView.OnItemClickListener { _, view, _, _ ->
            val check = view as CheckedTextView
            check.isChecked = !check.isChecked
        }

        val orientationArray = resources.getTextArray(R.array.screen_orientation_full)

        list.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            onApplyAction(true)
            val orientation = orientationArray[position].toString()
            controller!!.changeOrinatation(orientation)
        }


        val apply = findMyViewById(R.id.rotation_apply) as ImageButton
        apply.visibility = View.GONE

        val cancel = findMyViewById(R.id.rotation_close) as ImageButton
        cancel.setOnClickListener {
            onAnimatorCancel()
            updateRotation()
        }
    }

    private fun updateRotation() {
        val rotationGroup = findMyViewById(R.id.rotationGroup) as? RadioGroup
        rotationGroup?.check(if (controller!!.rotation == 0) R.id.rotate0 else if (controller!!.rotation == -1) R.id.rotate90 else R.id.rotate270)

        val list = findMyViewById(R.id.rotationList) as? ListView ?: return
        val index = getScreenOrientationItemPos(controller!!.screenOrientation)
        list.setItemChecked(index, true)
        list.setSelection(index)
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
        var bookId: Long? = orionContext.tempOptions!!.bookId
        if (bookId == null || bookId == -1L) {
            bookId = orionContext.getBookmarkAccessor().insertOrUpdate(info.simpleFileName, info.fileSize)
            orionContext.tempOptions!!.bookId = bookId
        }
        return bookId.toInt().toLong()
    }

    private fun insertBookmark(page: Int, text: String): Boolean {
        val id = insertOrGetBookId()
        if (id != -1L) {
            val bokmarkId = orionContext.getBookmarkAccessor().insertOrUpdateBookmark(id, page, text)
            return bokmarkId != -1L
        }
        return false
    }

    fun doubleClickAction(x: Int, y: Int) {
        SelectionAutomata.selectText(
                this, true, true, selectionAutomata.dialog,
                SelectionAutomata.getSelectionRectangle(
                    x,
                    y,
                    0,
                    0,
                    true,
                    controller!!.pageLayoutManager
                )
        )
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
        if (screenId == CROP_SCREEN) {
            val cropDialog = create(this, controller!!.margins)
            cropDialog.show()
            return
        }
        if (screenId != -1) {
            when (screenId) {
                ROTATION_SCREEN -> updateRotation()
                PAGE_LAYOUT_SCREEN -> {
                    updatePageLayout()
                    updatePageSeeker()
                }
                PAGE_SCREEN -> updatePageSeeker()
                ZOOM_SCREEN -> actualizeZoomOptions()
            }

            if (action === Action.ADD_BOOKMARK) {
                val parameterText = parameter as String?

                val page = controller!!.currentPage
                val newText = orionContext.getBookmarkAccessor().selectExistingBookmark(bookId, page, parameterText)

                val notOverride = parameterText == null || parameterText == newText
                findMyViewById(R.id.warn_text_override).visibility = if (notOverride) View.GONE else View.VISIBLE

                (findMyViewById(R.id.add_bookmark_text) as EditText).setText(if (notOverride) newText else parameterText)
            }

            animator!!.displayedChild = screenId
            dialog!!.show()
        }
    }


    fun textSelectionMode(isSingleSelection: Boolean, translate: Boolean) {
        selectionAutomata.startSelection(isSingleSelection, translate)
    }

    private class MyArrayAdapter(context: Context) :
            ArrayAdapter<CharSequence>(context, R.layout.support_simple_spinner_dropdown_item, context.resources.getTextArray(R.array.fits)), SpinnerAdapter {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
                convertView ?: TextView(parent.context).apply {
                    text = " % "
                }
    }

    private suspend fun askPassword(controller: Controller): Boolean {
        if (controller.needPassword()) {
            val view = layoutInflater.inflate(R.layout.password, null)
            val builder = createThemedAlertBuilder()

            builder.setView(view)
                .setNegativeButton(R.string.string_cancel) { dialog, _ -> dialog.dismiss() }
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
            controller?.drawPage(lastPageInfo1.pageNumber, lastPageInfo1.newOffsetX, lastPageInfo1.newOffsetY, lastPageInfo1.isSinglePageMode)
            controller?.pageLayoutManager?.uploadNewPages()
        }
    }

    fun startSearch() {
        SearchDialog.newInstance().show(supportFragmentManager, "search")
    }

    private fun destroyController() {
        AndroidLogger.stopLogger()
        controller?.destroy()
        controller = null
        orionContext.currentBookParameters = null
    }

    private fun updateGlobalOptionsFromIntent(intent: Intent) {
        if (intent.hasExtra(GlobalOptions.SHOW_TAP_HELP)) {
            val showTapHelp = intent.getBooleanExtra(GlobalOptions.SHOW_TAP_HELP, false)
            globalOptions.saveBooleanProperty(GlobalOptions.SHOW_TAP_HELP, showTapHelp)
        }

        if (intent.hasExtra(GlobalOptions.NEW_UI)) {
            val newUI = intent.getBooleanExtra(GlobalOptions.NEW_UI, false)
            globalOptions.saveBooleanProperty(GlobalOptions.NEW_UI, newUI)
        }

        if (intent.hasExtra(GlobalOptions.TEST_SCREEN_WIDTH) && intent.hasExtra(GlobalOptions.TEST_SCREEN_HEIGHT)) {
            val newWidth = intent.getIntExtra(GlobalOptions.TEST_SCREEN_WIDTH, view.layoutParams.width)
            val newHeigth = intent.getIntExtra(GlobalOptions.TEST_SCREEN_HEIGHT, view.layoutParams.height)
            view.layoutParams.width = newWidth
            view.layoutParams.height = newHeigth
            view.requestLayout()
        }
        openAsTempTestBook = intent.getBooleanExtra(GlobalOptions.OPEN_AS_TEMP_BOOK, false)
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
        info: String? = null
    ) {
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

        if (exception != null) {
            log(exception)
            analytics.error(exception, "$message $intent")
        } else {
            logError("$message $intent")
            analytics.logWarning("$message $intent")
        }

        showErrorOrFallbackPanel(message, intent, info, exception = exception)
    }

    fun showErrorOrFallbackPanel(
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
        findViewById<View>(R.id.orion_full_scene)!!.visibility = if (!show) View.VISIBLE else View.INVISIBLE
        findViewById<View>(R.id.problem_view)!!.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            view.pageLayoutManager = null
        }
    }

    companion object {

        val BOOK_MENU_ITEMS = setOf(R.id.search_menu_item,
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

        const val ROTATION_SCREEN = 0

        const val PAGE_SCREEN = 1

        const val ZOOM_SCREEN = 2

        const val CROP_SCREEN = 3

        const val PAGE_LAYOUT_SCREEN = 4

        const val ADD_BOOKMARK_SCREEN = 5

        const val CROP_RESTRICTION_MIN = -10

        const val CROP_RESTRICTION_MAX = 40

        const val USER_INTENT = "USER_INTENT"
    }
}


