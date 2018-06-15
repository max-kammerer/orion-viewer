/*
 * Orion Viewer - pdf, djvu, xps and cbz file viewer for android devices
 *
 * Copyright (C) 2011-2013  Michael Bogdanov & Co
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package universe.constellation.orion.viewer

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.os.Debug
import android.support.v4.internal.view.SupportMenuItem
import android.support.v7.app.AppCompatDialog
import android.text.method.PasswordTransformationMethod
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.android.UI
import universe.constellation.orion.viewer.android.FileUtils
import universe.constellation.orion.viewer.device.Device
import universe.constellation.orion.viewer.dialog.SearchDialog
import universe.constellation.orion.viewer.dialog.TapHelpDialog
import universe.constellation.orion.viewer.dialog.create
import universe.constellation.orion.viewer.document.StubDocument
import universe.constellation.orion.viewer.layout.SimpleLayoutStrategy
import universe.constellation.orion.viewer.prefs.GlobalOptions
import universe.constellation.orion.viewer.selection.NewTouchProcessor
import universe.constellation.orion.viewer.selection.NewTouchProcessorWithScale
import universe.constellation.orion.viewer.selection.SelectionAutomata
import universe.constellation.orion.viewer.view.FullScene
import universe.constellation.orion.viewer.view.OrionStatusBarHelper
import universe.constellation.orion.viewer.view.Renderer
import java.io.File

class OrionViewerActivity : OrionBaseActivity(viewerType = Device.VIEWER_ACTIVITY) {

    private var dialog: AppCompatDialog? = null

    internal val subscriptionManager = SubscriptionManager()

    private var animator: ViewAnimator? = null

    private var lastPageInfo: LastPageInfo? = null

    var controller: Controller? = null
        private set

    private val operation = OperationHolder()

    val globalOptions: GlobalOptions by lazy {
        orionContext.options
    }

    private var myIntent: Intent? = null

    @JvmField
    var _isResumed: Boolean = false

    private val selectionAutomata: SelectionAutomata by lazy {
        SelectionAutomata(this)
    }

    private var newTouchProcessor: NewTouchProcessor? = null

    private var hasActionBar: Boolean = false

    lateinit var fullScene: FullScene
        private set

    private var hasReadPermissions = false

    private var lastIntent: Intent? = null

    private var zoomInternal = 0

    override val view: OrionScene
        get() = fullScene.drawView

    val statusBarHelper: OrionStatusBarHelper
        get() = fullScene.statusBarHelper

    val bookId: Long
        get() {
            log("Selecting book id...")
            val info = lastPageInfo as ShortFileInfo
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
        log("Creating file manager")

        orionContext.viewActivity = this
        OptionActions.FULL_SCREEN.doAction(this, !globalOptions.isFullScreen, globalOptions.isFullScreen)
        onOrionCreate(savedInstanceState, R.layout.main_view)

        hasActionBar = globalOptions.isActionBarVisible
        OptionActions.SHOW_ACTION_BAR.doAction(this, !hasActionBar, hasActionBar)

        val view = findViewById<View>(R.id.view) as OrionScene
        fullScene = FullScene(findViewById<View>(R.id.orion_full_scene) as ViewGroup, view, findViewById<View>(R.id.orion_status_bar) as ViewGroup, orionContext)

        OptionActions.SHOW_STATUS_BAR.doAction(this, !globalOptions.isStatusBarVisible, globalOptions.isStatusBarVisible)
        OptionActions.SHOW_OFFSET_ON_STATUS_BAR.doAction(this, !globalOptions.isShowOffsetOnStatusBar, globalOptions.isShowOffsetOnStatusBar)
        fullScene.setDrawOffPage(globalOptions.isDrawOffPage)

        initDialogs()

        myIntent = intent
        newTouchProcessor = if (orionContext.sdkVersion >= Build.VERSION_CODES.FROYO)
            NewTouchProcessorWithScale(view, this)
        else
            NewTouchProcessor(view, this)

        hasReadPermissions = Permissions.checkReadPermission(this)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (Permissions.ORION_ASK_PERMISSION_CODE == requestCode && !hasReadPermissions) {
            println("Permission callback...")
            var i = 0
            while (i < permissions.size) {
                if (android.Manifest.permission.READ_EXTERNAL_STORAGE == permissions[i] && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    hasReadPermissions = true
                    onNewIntent(lastIntent ?: return)
                    lastIntent = null
                    return
                }
                i++
            }
        }
    }


    private fun initDialogs() {
        initOptionDialog()
        initRotationScreen()

        //page chooser
        initPagePeekerScreen()

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

    override fun onNewIntent(intent: Intent) {
        log("Runtime.getRuntime().totalMemory(): ${Runtime.getRuntime().totalMemory()}")
        log("Debug.getNativeHeapSize(): ${Debug.getNativeHeapSize()}")
        log("Trying to open new intent: $intent...")
        if (!hasReadPermissions) {
            log("Waiting for read permissions")
            lastIntent = intent
            return
        }

        val uri = intent.data
        if (uri != null) {
            log("Try to open file by " + uri.toString())
            try {
                val intentPath: String =
                        if ("content".equals(uri.scheme, ignoreCase = true)) {
                            try {
                                FileUtils.getPath(this, uri)
                            } catch (e: Exception) {
                                log(e)
                                null
                            } ?: run {
                                SaveNotification().apply {
                                    Bundle().apply {
                                        putString(SaveNotification.URI, uri.toString())
                                        putString(SaveNotification.TYPE, intent.type)
                                        arguments = this
                                    }


                                    show(supportFragmentManager, "save_notification")
                                }
                                return
                            }
                        } else uri.path


                if (controller != null && lastPageInfo != null) {
                    if (lastPageInfo!!.openingFileName == intentPath) {
                        controller!!.drawPage()
                        return
                    }
                }

                openFileAndDestroyOldController(intentPath)

            } catch (e: Exception) {
                showAlertWithExceptionThrow(intent, e)
            }

        } else
        /*if (intent.getAction().endsWith("MAIN"))*/ {
            //TODO error
        }
    }

    @Throws(Exception::class)
    fun openFileAndDestroyOldController(filePath: String) {
        log("openFileAndDestroyOldController")
        destroyController(controller).also { controller = null }
        AndroidLogger.stopLogger()
        openFile(filePath)
    }

    @Throws(Exception::class)
    private fun openFile(filePath: String) {
        val stubDocument = StubDocument(filePath, "Loading...")
        val stubRenderer = Renderer.EMPTY
        val stubController = Controller(this, stubDocument, SimpleLayoutStrategy.create(stubDocument), stubRenderer)
        val drawView = fullScene.drawView
        val stubInfo = LastPageInfo.createDefaultLastPageInfo(this)
        stubController.changeOrinatation(stubInfo.screenOrientation)
        stubController.init(stubInfo, Point(drawView.sceneWidth, drawView.sceneHeight))

        view.setDimensionAware(stubController)
        stubController.drawPage()
        controller = stubController
        updateViewOnNewBook(stubDocument.title)

        launch(UI) {
            log("Trying to open file: $filePath")
            val rootJob = Job()
            val newDocument = try {
                async(CommonPool, parent = rootJob) {
                    FileUtil.openFile(filePath)
                }.await()
            } catch (e: Exception) {
                log(e)
                stubDocument.bodyText = e.message
                stubDocument.title = e.message
                updateViewOnNewBook(stubDocument.title)
                return@launch
            }

            try {
                val lastPageInfo1 = async(CommonPool, parent = rootJob) { LastPageInfo.loadBookParameters(this@OrionViewerActivity, filePath) }.await()
                lastPageInfo = lastPageInfo1
                orionContext.currentBookParameters = lastPageInfo1
                OptionActions.DEBUG.doAction(this@OrionViewerActivity, false, globalOptions.getBooleanProperty("DEBUG", false))

                val layoutStrategy = SimpleLayoutStrategy.create(newDocument)

                val renderer = RenderThread(this@OrionViewerActivity, layoutStrategy, newDocument, fullScene)

                val controller1 = Controller(this@OrionViewerActivity, newDocument, layoutStrategy, renderer, rootJob)
                controller = controller1
                stubController.destroy()
                controller1.changeOrinatation(lastPageInfo1!!.screenOrientation)

                val drawView = fullScene.drawView
                controller1.init(lastPageInfo1, Point(drawView.sceneWidth, drawView.sceneHeight))

                subscriptionManager.sendDocOpenedNotification(controller1)

                view.setDimensionAware(controller1)

                controller1.drawPage()

                val title = newDocument.title?.takeIf { it.isNotBlank() } ?: filePath.substringAfter('/').substringBefore(".")

                updateViewOnNewBook(title)
                globalOptions.addRecentEntry(GlobalOptions.RecentEntry(File(filePath).absolutePath))

                lastPageInfo1.totalPages = newDocument.pageCount
                device!!.onNewBook(lastPageInfo1, newDocument)

                askPassword(controller1)
                orionContext.onNewBook(filePath)

            } catch (e: Exception) {
                log(e)
                throw e
            }
        }
    }

    private fun updateViewOnNewBook(title: String?) {
        fullScene.onNewBook(title, controller!!.pageCount)
        if (supportActionBar != null) {
            supportActionBar!!.title = title
        }
    }

    private fun showAlertWithExceptionThrow(intent: Intent, e: Exception) {
        val themedAlertBuilder = createThemedAlertBuilder().setMessage("Error while opening " + intent + ": " + e.message + " cause of " + e.cause)
        themedAlertBuilder.setPositiveButton("OK") { dialog, which ->
            finish()
            throw RuntimeException("Exception on processing $intent", e)
        }
        themedAlertBuilder.setOnCancelListener {
            finish()
            throw RuntimeException("Exception on processing $intent", e)
        }
        themedAlertBuilder.create().show()
    }


    public override fun onPause() {
        _isResumed = false
        super.onPause()
        if (controller != null) {
            controller!!.onPause()
            saveData()
        }
    }

    private fun initPagePeekerScreen() {
        val pageSeek = findMyViewById(R.id.page_picker_seeker) as SeekBar

        subscriptionManager.addDocListeners(object : DocumentViewAdapter() {
            override fun documentOpened(controller: Controller) {
                pageSeek.max = controller.pageCount - 1
                pageSeek.progress = controller.currentPage
            }

            override fun pageChanged(newPage: Int, pageCount: Int) {
                pageSeek.progress = newPage
            }
        })


        val pageNumberText = findMyViewById(R.id.page_picker_message) as TextView
        //initial state
        pageNumberText.text = Integer.toString(1)

        pageSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                pageNumberText.text = "${progress + 1}"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        val closePagePeeker = findMyViewById(R.id.page_picker_close) as ImageButton

        val plus = findMyViewById(R.id.page_picker_plus) as ImageButton
        plus.setOnClickListener { pageSeek.incrementProgressBy(1) }

        val minus = findMyViewById(R.id.page_picker_minus) as ImageButton
        minus.setOnClickListener {
            if (pageSeek.progress != 0) {
                pageSeek.incrementProgressBy(-1)
            }
        }

        closePagePeeker.setOnClickListener {
            //controller.drawPage(Integer.valueOf(pageNumberText.getText().toString()) - 1);
            //main menu
            onAnimatorCancel()
            updatePageSeeker()
            //animator.setDisplayedChild(MAIN_SCREEN);
        }

        val pagePreview = findMyViewById(R.id.page_preview) as ImageButton
        pagePreview.setOnClickListener {
            onApplyAction()
            if (pageNumberText.text.isNotEmpty()) {
                try {
                    val parsedInput = Integer.valueOf(pageNumberText.text.toString())
                    controller!!.drawPage(parsedInput - 1)
                } catch (ex: NumberFormatException) {
                    showError("Couldn't parse " + pageNumberText.text, ex)
                }

            }
        }
    }

    private fun updatePageSeeker() {
        val pageSeek = findMyViewById(R.id.page_picker_seeker) as SeekBar
        pageSeek.progress = controller!!.currentPage
        val view = findMyViewById(R.id.page_picker_message) as TextView
        view.text = "${controller!!.currentPage + 1}"
        view.clearFocus()
        view.requestFocus()

    }

    private fun initZoomScreen() {
        //zoom screen

        val sp = findMyViewById(R.id.zoom_spinner) as Spinner

        val zoomText = findMyViewById(R.id.zoom_picker_message) as EditText

        val zoomSeek = findMyViewById(R.id.zoom_picker_seeker) as SeekBar

        zoomSeek.max = 300
        zoomSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (zoomInternal != 1) {
                    zoomText.setText("$progress")
                    if (sp.selectedItemPosition != 0) {
                        val oldInternal = zoomInternal
                        zoomInternal = 2
                        sp.setSelection(0)
                        zoomInternal = oldInternal
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        subscriptionManager.addDocListeners(object : DocumentViewAdapter() {
            override fun documentOpened(controller: Controller) {
                updateZoom()
            }
        })

        val zplus = findMyViewById(R.id.zoom_picker_plus) as ImageButton
        zplus.setOnClickListener { zoomSeek.incrementProgressBy(1) }

        val zminus = findMyViewById(R.id.zoom_picker_minus) as ImageButton
        zminus.setOnClickListener {
            if (zoomSeek.progress != 0) {
                zoomSeek.incrementProgressBy(-1)
            }
        }

        val closeZoomPeeker = findMyViewById(R.id.zoom_picker_close) as ImageButton
        closeZoomPeeker.setOnClickListener {
            //main menu
            onAnimatorCancel()
            //updateZoom();
        }

        val zoomPreview = findMyViewById(R.id.zoom_preview) as ImageButton
        zoomPreview.setOnClickListener {
            onApplyAction()
            val index = sp.selectedItemPosition
            controller!!.changeZoom(if (index == 0) (java.lang.Float.parseFloat(zoomText.text.toString()) * 100).toInt() else -1 * (index - 1))
            updateZoom()
        }

        sp.adapter = MyArrayAdapter(applicationContext)
        sp.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val disable = position != 0
                val oldZoomInternal = zoomInternal
                if (zoomInternal != 2) {
                    zoomInternal = 1
                    if (disable) {
                        zoomText.setText(parent.adapter.getItem(position) as String)
                    } else {
                        zoomText.setText("${(controller!!.currentPageZoom * 10000).toInt() / 100f}")
                        zoomSeek.progress = (controller!!.currentPageZoom * 100).toInt()
                    }
                    zoomInternal = oldZoomInternal
                }

                zminus.visibility = if (disable) View.GONE else View.VISIBLE
                zplus.visibility = if (disable) View.GONE else View.VISIBLE

                zoomText.isFocusable = !disable
                zoomText.isFocusableInTouchMode = !disable

                val parent1 = zoomText.parent as LinearLayout

                parent1.post { parent1.requestLayout() }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
            }
        }

        //by width
        sp.setSelection(1)

    }

    fun updateZoom() {
        val zoomSeek = findMyViewById(R.id.zoom_picker_seeker) as SeekBar
        val textView = findMyViewById(R.id.zoom_picker_message) as TextView

        val sp = findMyViewById(R.id.zoom_spinner) as Spinner
        val spinnerIndex: Int
        zoomInternal = 1
        try {
            var zoom = controller!!.zoom10000Factor
            if (zoom <= 0) {
                spinnerIndex = -zoom + 1
                zoom = (10000 * controller!!.currentPageZoom).toInt()
            } else {
                spinnerIndex = 0
                textView.text = (zoom / 100f).toString()
            }
            zoomSeek.progress = zoom / 100
            sp.setSelection(spinnerIndex)
        } finally {
            zoomInternal = 0
        }
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
        if (myIntent != null) {
            //starting creation intent
            onNewIntent(myIntent!!)
            myIntent = null
        } else {
            if (controller != null) {
                controller!!.processPendingEvents()
                //controller.startRenderer();
                controller!!.drawPage()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        log("onDestroy")
        AndroidLogger.stopLogger()

        destroyController(controller).also { controller = null }

        if (dialog != null) {
            dialog!!.dismiss()
        }
        orionContext.destroyDb()
    }

    private fun saveData() {
        if (controller != null) {
            try {
                controller!!.serialize(lastPageInfo!!)
                lastPageInfo!!.save(this)
            } catch (ex: Exception) {
                log(ex)
            }

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
            val action = Action.getAction(actionCode)
            when (action) {
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

        if (device!!.onKeyUp(keyCode, event.isLongPress, operation)) {
            changePage(operation.value)
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
        menuInflater.inflate(R.menu.menu, menu)
        if (!hasActionBar) {
            for (i in 0 until menu.size()) {
                val item = menu.getItem(i)
                item.setShowAsAction(SupportMenuItem.SHOW_AS_ACTION_NEVER)
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var action = Action.NONE //will open help

        when (item.itemId) {
            R.id.exit_menu_item -> {
                finish()
                return true
            }

            R.id.search_menu_item -> action = Action.SEARCH
            R.id.crop_menu_item -> action = Action.CROP
            R.id.zoom_menu_item -> action = Action.ZOOM
            R.id.add_bookmark_menu_item -> action = Action.ADD_BOOKMARK
            R.id.goto_menu_item -> action = Action.GOTO
            R.id.select_text_menu_item -> action = Action.SELECT_TEXT
        //            case R.id.navigation_menu_item: showOrionDialog(PAGE_LAYOUT_SCREEN, null, null);
        //                return true;

        //            case R.id.rotation_menu_item: action = Action.ROTATION; break;

            R.id.options_menu_item -> action = Action.OPTIONS

            R.id.book_options_menu_item -> action = Action.BOOK_OPTIONS

        //            case R.id.tap_menu_item:
        //                Intent tap = new Intent(this, OrionTapActivity.class);
        //                startActivity(tap);
        //                return true;

            R.id.outline_menu_item -> action = Action.SHOW_OUTLINE
            R.id.open_menu_item -> action = Action.OPEN_BOOK
            R.id.open_dictionary_menu_item -> action = Action.DICTIONARY

            R.id.bookmarks_menu_item -> action = Action.OPEN_BOOKMARKS
            R.id.help_menu_item -> {
                val intent = Intent()
                intent.setClass(this, OrionHelpActivity::class.java)
                startActivity(intent)
            }
        }

        if (Action.NONE !== action) {
            doAction(action)
        } else {
            //            Intent intent = new Intent();
            //            intent.setClass(this, OrionHelpActivity.class);
            //            startActivity(intent);
            return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun initOptionDialog() {
        dialog = AppCompatDialog(this)
        dialog!!.supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog!!.setContentView(R.layout.options_dialog)
        animator = dialog!!.findViewById<View>(R.id.viewanim) as ViewAnimator?

        view.setOnTouchListener(View.OnTouchListener { v, event -> newTouchProcessor!!.onTouch(event) })

        dialog!!.setCanceledOnTouchOutside(true)
    }

    fun doAction(code: Int) {
        val action = Action.getAction(code)
        doAction(action)
        log("Code action $code")
    }


    private fun doAction(action: Action) {
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

        list.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            val check = view as CheckedTextView
            check.isChecked = !check.isChecked
        }

        val ORIENTATION_ARRAY = resources.getTextArray(R.array.screen_orientation_full)

        list.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            onApplyAction(true)
            val orientation = ORIENTATION_ARRAY[position].toString()
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
        val info = lastPageInfo as ShortFileInfo
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
                SelectionAutomata.getSelectionRectangle(x, y, 0, 0, true)
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == OPEN_BOOKMARK_ACTIVITY_RESULT && resultCode == Activity.RESULT_OK) {
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
                ZOOM_SCREEN -> updateZoom()
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

    private class MyArrayAdapter constructor(context: Context) :
            ArrayAdapter<CharSequence>(context, R.layout.support_simple_spinner_dropdown_item, context.resources.getTextArray(R.array.fits)), SpinnerAdapter {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
                convertView ?: TextView(parent.context).apply {
                    text = " % "
                }
    }

    private fun askPassword(controller: Controller) {
        if (controller.needPassword()) {
            val builder = createThemedAlertBuilder()
            builder.setTitle("Password")

            val input = EditText(this)
            input.inputType = EditorInfo.TYPE_TEXT_VARIATION_PASSWORD
            input.transformationMethod = PasswordTransformationMethod()
            builder.setView(input)

            builder.setPositiveButton("OK") { dialog, which ->
                if (controller.authenticate(input.text.toString())) {
                    dialog.dismiss()
                } else {
                    askPassword(controller)
                    showWarning("Wrong password!")
                }
            }

            builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            builder.create().show()
        }
    }

    //big hack
    fun myprocessOnActivityVisible() {
        if (globalOptions.isShowTapHelp && !orionContext.isTesting) {
            globalOptions.saveBooleanProperty(GlobalOptions.SHOW_TAP_HELP, false)
            TapHelpDialog(this).showDialog()
        }
    }

    fun startSearch() {
        SearchDialog.newInstance().show(supportFragmentManager, "search")
    }

    private fun destroyController(controller: Controller?) {
        controller?.let {
            val currentPage = it.currentPage
            val pageCount = it.document.pageCount
            launch(CommonPool) {
                it.rootJob.cancelAndJoin()
                it.destroy()
                device!!.onBookClose(currentPage, pageCount)
            }
        }
    }

    companion object {

        const val OPEN_BOOKMARK_ACTIVITY_RESULT = 1

        const val SAVE_FILE_RESULT = 2

        const val ROTATION_SCREEN = 0

        const val PAGE_SCREEN = 1

        const val ZOOM_SCREEN = 2

        const val CROP_SCREEN = 3

        const val PAGE_LAYOUT_SCREEN = 4

        const val ADD_BOOKMARK_SCREEN = 5

        const val CROP_RESTRICTION_MIN = -10

        const val CROP_RESTRICTION_MAX = 40
    }
}
