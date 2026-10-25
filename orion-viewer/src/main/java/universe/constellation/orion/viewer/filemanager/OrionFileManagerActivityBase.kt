package universe.constellation.orion.viewer.filemanager

import universe.constellation.orion.viewer.prefs.SettingsTransferUi
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import android.view.SubMenu
import android.view.View
import android.widget.ListView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.viewpager.widget.ViewPager
import com.google.android.material.color.MaterialColors
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import universe.constellation.orion.viewer.OrionBaseActivity
import universe.constellation.orion.viewer.OrionHelpActivity
import universe.constellation.orion.viewer.OrionViewerActivity
import universe.constellation.orion.viewer.Permissions
import universe.constellation.orion.viewer.Permissions.checkAndRequestStorageAccessPermissionOrReadOne
import universe.constellation.orion.viewer.Permissions.hasReadStoragePermission
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.analytics.PERMISSION_ASKED
import universe.constellation.orion.viewer.analytics.PERMISSION_CANCELLED
import universe.constellation.orion.viewer.analytics.PERMISSION_DISMISSED
import universe.constellation.orion.viewer.analytics.PERMISSION_FROM_START
import universe.constellation.orion.viewer.analytics.PERMISSION_RESULT
import universe.constellation.orion.viewer.analytics.PERMISSION_SHOWN
import universe.constellation.orion.viewer.android.isAtLeastKitkat
import universe.constellation.orion.viewer.formats.FileFormats
import universe.constellation.orion.viewer.getVectorDrawable
import universe.constellation.orion.viewer.log
import universe.constellation.orion.viewer.padWithSystemBars
import java.io.File
import java.io.FilenameFilter

internal val DIRECTORY_DOCUMENTS = if (isAtLeastKitkat()) Environment.DIRECTORY_DOCUMENTS else "Documents"

abstract class OrionFileManagerActivityBase @JvmOverloads constructor(
    val showRecentsAndSavePath: Boolean = true,
    val fileNameFilter: FilenameFilter = FileChooserAdapter.DEFAULT_FILTER,
    private val enableSystemOptionAction: Boolean = false
) : OrionBaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout

    private lateinit var drawerLayoutListener: ActionBarDrawerToggle

    private lateinit var navView: NavigationView

    var prefs: SharedPreferences? = null
        private set

    /** Where the pending storage access request came from, for its result event; kept while the system settings recreate the activity. */
    private var permissionSource: String? = null

    val settingsTransfer = SettingsTransferUi(this, this) { this }

    val selectDocumentInSystem = if (isAtLeastKitkat()) registerForActivityResult(
        ActivityResultContracts.OpenDocument()
        ) { result ->
            if (result != null) {
                openFile(result, true)
            }
        }
    else null

    @SuppressLint("MissingSuperCall")
    override fun onCreate(savedInstanceState: Bundle?) {
        onOrionCreate(savedInstanceState, R.layout.file_manager, true)
        log("Creating file manager")

        prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(applicationContext)

        permissionSource = savedInstanceState?.getString(PERMISSION_SOURCE)
        initFileManager()
        initDrawer()
        //a recreation shows the dialog again, but it's the same sight of it
        showPermissionRequestDialog(isNewSight = savedInstanceState == null)

        onNewIntent(intent)
    }

    private fun initDrawer() {
        supportActionBar?.setHomeButtonEnabled(true)

        drawerLayout = findViewById(R.id.drawer_layout)
        drawerLayoutListener = ActionBarDrawerToggle(this, drawerLayout, toolbar)
        drawerLayoutListener.drawerArrowDrawable.color = MaterialColors.getColor(drawerLayout, R.attr.appIconTint)
        drawerLayout.addDrawerListener(drawerLayoutListener)
        closeDrawerOnBack()
        drawerLayoutListener.isDrawerIndicatorEnabled = true
        navView = findViewById(R.id.nav_view)
        navView.setNavigationItemSelectedListener(this)

        //DrawerLayout distributes the insets on its own, so android:fitsSystemWindows of the theme
        //doesn't reach this screen. NavigationView routes the top inset into its header layout, and
        //there is no header here, so it would be dropped and the first menu item would end up under
        //the status bar.
        navView.padWithSystemBars()

        val menu = navView.menu
        val locations = menu.findItem(R.id.nav_locations)
        val viewPager = findViewById<ViewPager>(R.id.viewpager)

        for (storage in this.describeStorages()) {
            val folder = storage.file
            val subMenu = locations.subMenu!!//.addSubMenu(storage.description)
            addFolderItem(
                subMenu,
                storage.description,
                viewPager,
                folder,
                R.drawable.sd_card
            )
            for (subFolder in storage.folders) {
                if (!subFolder.file.exists()) continue
                addFolderItem(
                    subMenu,
                    subFolder.description,
                    viewPager,
                    subFolder.file,
                    R.drawable.new_folder
                )
            }
        }
        menu.findItem(R.id.nav_system_select)?.setVisible(enableSystemOptionAction && isAtLeastKitkat())
        menu.findItem(R.id.nav_permissions)?.setVisible(!hasReadStoragePermission(this))
        menu.findItem(R.id.nav_export_settings)?.setVisible(settingsTransfer.isAvailable)
        menu.findItem(R.id.nav_import_settings)?.setVisible(settingsTransfer.isAvailable)

        drawerLayoutListener.syncState()
    }

    private fun addFolderItem(
        menu: SubMenu,
        name: String,
        viewPager: ViewPager,
        folder: File,
        icon: Int
    ): MenuItem {
        val item = menu.add(0, R.id.nav_locations, Menu.NONE, name)
        item.setIcon(getVectorDrawable(icon))
        item.setOnMenuItemClickListener {
            analytics.action("folderNavMenu")
            drawerLayout.closeDrawer(GravityCompat.START)
            if (viewPager.currentItem != 0) {
                viewPager.setCurrentItem(0, false)
            }
            (supportFragmentManager.fragments.getOrNull(viewPager.currentItem) as? FoldersFragment)
                ?.changeFolder(folder)?.run { true } ?: false
        }
        return item
    }

    private fun refreshFolder() {
        log("Refreshing folder")
        supportFragmentManager.fragments.filterIsInstance<FoldersFragment>().forEach {
            it.refreshFolder()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (Permissions.ASK_READ_PERMISSION_FOR_FILE_MANAGER == requestCode) {
            log("Permission callback: " + permissions.joinToString() + " " + grantResults.joinToString())
            actualizePermissions()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        log("FileManager: On activity result requestCode=$requestCode resultCode=$resultCode")
        when (requestCode) {
            Permissions.ASK_READ_PERMISSION_FOR_FILE_MANAGER -> {
                actualizePermissions()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(PERMISSION_SOURCE, permissionSource)
    }

    private fun actualizePermissions() {
        val hasReadStoragePermission = hasReadStoragePermission(this)
        if (hasReadStoragePermission) {
            refreshFolder()
        }
        navView.menu.findItem(R.id.nav_permissions)?.setVisible(!hasReadStoragePermission(this))
        reportPermission(PERMISSION_RESULT, permissionSource ?: "unknown", hasReadStoragePermission)
        permissionSource = null
    }

    /**
     * The dialog is shown on every start without the access. [isNewSight] false is a recreation
     * of the one already reported. It counts as the first one until it's answered.
     */
    private fun showPermissionRequestDialog(isNewSight: Boolean) {
        if (!hasReadStoragePermission(this)) {
            val first = !globalOptions.PERMISSION_DIALOG_SHOWN.value
            fun answered(step: String) {
                globalOptions.saveBooleanProperty(globalOptions.PERMISSION_DIALOG_SHOWN.key, true)
                reportPermission(step, PERMISSION_FROM_START, false, first)
            }
            if (isNewSight) reportPermission(PERMISSION_SHOWN, PERMISSION_FROM_START, false, first)
            AlertDialog.Builder(this).setMessage(R.string.permission_directory_warning)
                .setPositiveButton(R.string.permission_grant) { _, _ ->
                    globalOptions.saveBooleanProperty(globalOptions.PERMISSION_DIALOG_SHOWN.key, true)
                    requestPermissions(PERMISSION_FROM_START, first)
                }.setNegativeButton(R.string.permission_cancel) { d, _ ->
                    d.dismiss()
                    answered(PERMISSION_DISMISSED)
                }.setOnCancelListener {
                    //back or a tap outside
                    answered(PERMISSION_CANCELLED)
                }.show()
        }
    }

    /** [source] is one of PERMISSION_FROM_*: the start dialog, the button over the folders, the side menu. */
    internal fun requestPermissions(source: String, first: Boolean = false) {
        permissionSource = source
        reportPermission(PERMISSION_ASKED, source, false, first)
        checkAndRequestStorageAccessPermissionOrReadOne(Permissions.ASK_READ_PERMISSION_FOR_FILE_MANAGER)
    }

    private fun reportPermission(step: String, source: String, granted: Boolean, first: Boolean = false) {
        analytics.permissionEvent(
            javaClass.name, granted, globalOptions.isNewUI, step, source, first,
            Permissions.storageAccess(this), Permissions.allFilesAccessScreen(this)
        )
    }

    /**
     * Back closes the drawer, but only while it's open: the rest of the time the callback stays
     * disabled so the system runs its own back gesture, predictive animation included.
     */
    private fun closeDrawerOnBack() {
        val callback = object : OnBackPressedCallback(drawerLayout.isDrawerOpen(GravityCompat.START)) {
            override fun handleOnBackPressed() {
                drawerLayout.closeDrawer(GravityCompat.START)
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)

        drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerOpened(drawerView: View) {
                callback.isEnabled = true
            }

            override fun onDrawerClosed(drawerView: View) {
                callback.isEnabled = false
            }
        })
    }

    /**
     * The system picker greys out files whose type isn't listed. Providers of usb drives and sd
     * cards report octet-stream for extensions they don't know (cb7, cbt, cbz on old systems, djvu
     * on some devices), so it is accepted too: the format is checked on opening anyway.
     */
    open val systemSelectMimeTypes: Array<String>
        get() = FileFormats.supportedMimeTypes + "application/octet-stream"

    open fun openFile(uri: Uri, isFromSystemFM: Boolean = false) {
        log("Opening new book: $uri")

        startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setClass(applicationContext, OrionViewerActivity::class.java)
                data = uri
                addCategory(Intent.CATEGORY_DEFAULT)
                putExtra(SYSTEM_FILE_MANAGER, isFromSystemFM)
            }
        )
    }

    open fun openFile(file: File) {
        openFile(Uri.fromFile(file))
    }

    private fun initFileManager() {
        val viewPager = findViewById<ViewPager>(R.id.viewpager)
        val addSupportTab = this@OrionFileManagerActivityBase is OrionFileManagerActivity
        viewPager.adapter = SimplePagerAdapter(supportFragmentManager).apply {
            if (showRecentsAndSavePath) {
                addFragment(RecentListFragment())
            }

            if (addSupportTab) {
                addFragment(OrionHelpActivity.ContributionFragment())
            }
        }

        val tabLayout = findViewById<TabLayout>(R.id.sliding_tabs)
        tabLayout.setupWithViewPager(viewPager)

        tabLayout.getTabAt(0)?.apply {
            icon = getVectorDrawable(R.drawable.new_folder)
            setText(R.string.file_manager_fodlers)
        }

        if (showRecentsAndSavePath) {
            tabLayout.getTabAt(1)?.apply {
                icon = getVectorDrawable(R.drawable.new_history)
                setText(R.string.file_manager_history)
            }
        }

        if (addSupportTab) {
            tabLayout.getTabAt(2)?.let { tab ->
                tab.setText(R.string.file_manager_contribution)
                globalOptions.OPENED_SURVEY.observe(this) {
                    tab.icon = getVectorDrawable(OrionHelpActivity.contributionTabIcon(this, it))
                }
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (drawerLayoutListener.onOptionsItemSelected(item)) {
            drawerLayout.closeDrawer(GravityCompat.START)
            return true
        }
        return false
    }

    companion object {
        const val DONT_OPEN_RECENT_FILE = "DONT_OPEN_RECENT_FILE"
        const val SYSTEM_FILE_MANAGER = "SYSTEM_FILE_MANAGER"
        private const val PERMISSION_SOURCE = "PERMISSION_SOURCE"
    }
}


