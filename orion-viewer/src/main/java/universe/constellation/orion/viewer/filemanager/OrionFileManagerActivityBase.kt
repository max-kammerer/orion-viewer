package universe.constellation.orion.viewer.filemanager

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import android.view.SubMenu
import android.widget.ListView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.viewpager.widget.ViewPager
import com.google.android.material.color.MaterialColors
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import universe.constellation.orion.viewer.OrionBaseActivity
import universe.constellation.orion.viewer.OrionViewerActivity
import universe.constellation.orion.viewer.Permissions
import universe.constellation.orion.viewer.Permissions.checkAndRequestStorageAccessPermissionOrReadOne
import universe.constellation.orion.viewer.Permissions.hasReadStoragePermission
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.android.isAtLeastKitkat
import universe.constellation.orion.viewer.getVectorDrawable
import universe.constellation.orion.viewer.log
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

        initFileManager()
        initDrawer()
        showPermissionRequestDialog()

        onNewIntent(intent)
    }

    private fun initDrawer() {
        supportActionBar?.setHomeButtonEnabled(true)

        drawerLayout = findViewById(R.id.drawer_layout)
        drawerLayoutListener = ActionBarDrawerToggle(this, drawerLayout, toolbar)
        drawerLayoutListener.drawerArrowDrawable.color = MaterialColors.getColor(drawerLayout, R.attr.appIconTint)
        drawerLayout.addDrawerListener(drawerLayoutListener)
        drawerLayoutListener.isDrawerIndicatorEnabled = true
        navView = findViewById(R.id.nav_view)
        navView.setNavigationItemSelectedListener(this)

        val menu = navView.menu
        val locations = menu.findItem(R.id.nav_locations)
        val viewPager = findViewById<ViewPager>(R.id.viewpager)



        for (storage in this.describeStorages()) {
            val folder = storage.file
            val subMenu = locations.subMenu!!//.addSubMenu(storage.description)
            addFodlerItem(
                subMenu,
                storage.description,
                viewPager,
                folder,
                R.drawable.sd_card
            )
            for (subFolder in storage.folders) {
                if (!subFolder.file.exists()) continue
                addFodlerItem(
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

        drawerLayoutListener.syncState()
    }

    private fun addFodlerItem(
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
        val list = findViewById<ListView>(R.id.folderList)
        val adapter = list.adapter
        if (adapter is FileChooserAdapter) {
            val currentFolder = adapter.currentFolder
            log("Refreshing view")
            adapter.changeFolder(File(currentFolder.absolutePath))
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

    private fun actualizePermissions() {
        val hasReadStoragePermission = hasReadStoragePermission(this)
        if (hasReadStoragePermission) {
            refreshFolder()
        }
        navView.menu.findItem(R.id.nav_permissions)?.setVisible(!hasReadStoragePermission(this))
        analytics.permissionEvent(this.javaClass.name, hasReadStoragePermission, globalOptions.isNewUI)
    }

    private fun showPermissionRequestDialog() {
        if (!hasReadStoragePermission(this)) {
            AlertDialog.Builder(this).setMessage(R.string.permission_directory_warning)
                .setPositiveButton(R.string.permission_grant) { _, _ ->
                    requestPermissions()
                }.setNegativeButton(R.string.permission_cancel) { d, _ ->
                    d.dismiss()
                    analytics.permissionEvent(this.javaClass.name, false, globalOptions.isNewUI)
                }.show()
        }
    }

    internal fun requestPermissions() {
        checkAndRequestStorageAccessPermissionOrReadOne(Permissions.ASK_READ_PERMISSION_FOR_FILE_MANAGER)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(findViewById(R.id.nav_view))) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    fun openFile(uri: Uri, isFromSystemFM: Boolean = false) {
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
        viewPager.adapter = SimplePagerAdapter(supportFragmentManager).apply {
            if (showRecentsAndSavePath) {
                addFragment(RecentListFragment())
            }
        }

        val tabLayout = findViewById<TabLayout>(R.id.sliding_tabs)
        tabLayout.setupWithViewPager(viewPager)

        val folderTab = tabLayout.getTabAt(0)
        folderTab?.setIcon(getVectorDrawable(R.drawable.new_folder))

        if (showRecentsAndSavePath) {
            val recentTab = tabLayout.getTabAt(1)
            recentTab?.setIcon(getVectorDrawable(R.drawable.new_history))
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
    }
}


