package universe.constellation.orion.viewer.filemanager

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.view.Menu
import android.view.MenuItem
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
import universe.constellation.orion.viewer.prefs.GlobalOptions
import java.io.File
import java.io.FilenameFilter

internal val DIRECTORY_DOCUMENTS = if (isAtLeastKitkat()) Environment.DIRECTORY_DOCUMENTS else "Documents"

internal val possibleStartFolders = listOf(
    Environment.getExternalStoragePublicDirectory(DIRECTORY_DOCUMENTS) to R.string.file_manager_documents,
    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) to R.string.file_manager_downloads,
    Environment.getExternalStorageDirectory() to R.string.file_manager_sdcard
)

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

    lateinit var globalOptions: GlobalOptions

    private var justCreated: Boolean = false

    val selectDocumentInSystem = if (isAtLeastKitkat()) registerForActivityResult(
        ActivityResultContracts.OpenDocument()
        ) { result ->
            if (result != null) {
                openFile(result)
            }
        }
    else null

    @SuppressLint("MissingSuperCall")
    override fun onCreate(savedInstanceState: Bundle?) {
        onOrionCreate(savedInstanceState, R.layout.file_manager, true)
        log("Creating file manager")

        prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        globalOptions = orionContext.options

        initFileManager()

        justCreated = true

        initDrawer()

        showPermissionRequestDialog()
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
        for (v in possibleStartFolders) {
            val folder = v.first
            if (folder.exists() && folder.isDirectory) {
                val item = locations.subMenu?.add(1, R.id.nav_locations, Menu.NONE, v.second)
                item?.setIcon(getVectorDrawable(R.drawable.new_folder_24))
                val viewPager = findViewById<ViewPager>(R.id.viewpager)
                item?.setOnMenuItemClickListener {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    if (viewPager.currentItem != 0) {
                        viewPager.setCurrentItem(0, false)
                    }
                    ((viewPager.adapter as? SimplePagerAdapter)?.getItem(0) as? FileManagerFragment)
                        ?.changeFolder(folder)?.run { true } ?: false
                }
            }
        }
        menu.findItem(R.id.nav_system_select)?.setVisible(enableSystemOptionAction && isAtLeastKitkat())
        menu.findItem(R.id.nav_permissions)?.setVisible(!hasReadStoragePermission(this))

        drawerLayoutListener.syncState()
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
        analytics.permissionEvent(this.javaClass.name, hasReadStoragePermission)
    }

    private fun showPermissionRequestDialog() {
        if (!hasReadStoragePermission(this)) {
            AlertDialog.Builder(this).setMessage(R.string.permission_directory_warning)
                .setPositiveButton(R.string.permission_grant) { _, _ ->
                    requestPermissions()
                }.setNegativeButton(R.string.permission_cancel) { d, _ -> d.dismiss() }.show()
        }
    }

    internal fun requestPermissions() {
        checkAndRequestStorageAccessPermissionOrReadOne(Permissions.ASK_READ_PERMISSION_FOR_FILE_MANAGER)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        justCreated = false
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(findViewById(R.id.nav_view))) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }

    }

    override fun onResume() {
        super.onResume()
        if (justCreated) {
            justCreated = false
            onNewIntent(intent)
        }
    }

    fun openFile(uri: Uri) {
        log("Opening new book: $uri")

        startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setClass(applicationContext, OrionViewerActivity::class.java)
                data = uri
                addCategory(Intent.CATEGORY_DEFAULT)
            }
        )
    }

    open fun openFile(file: File) {
        openFile(Uri.fromFile(file))
    }

    private fun initFileManager() {
        val pagerAdapter = SimplePagerAdapter(supportFragmentManager).apply {
            if (showRecentsAndSavePath) {
                addFragment(RecentListFragment())
            }
        }
        val viewPager = findViewById<ViewPager>(R.id.viewpager)
        viewPager.adapter = pagerAdapter
        val tabLayout = findViewById<TabLayout>(R.id.sliding_tabs)
        tabLayout.setupWithViewPager(viewPager)
        val folderTab = tabLayout.getTabAt(0)
        folderTab?.setIcon(getVectorDrawable(R.drawable.new_folder_24))
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
    }
}


