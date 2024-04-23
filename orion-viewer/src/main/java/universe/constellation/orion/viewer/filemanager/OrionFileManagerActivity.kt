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
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.fragment.app.ListFragment
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
import universe.constellation.orion.viewer.R.id.file_manager_goto_documents
import universe.constellation.orion.viewer.R.id.file_manager_goto_downloads
import universe.constellation.orion.viewer.R.id.file_manager_goto_sdcard
import universe.constellation.orion.viewer.android.isAtLeastKitkat
import universe.constellation.orion.viewer.filemanager.OrionFileManagerActivity.Companion.LAST_OPENED_DIRECTORY
import universe.constellation.orion.viewer.getVectorDrawable
import universe.constellation.orion.viewer.log
import universe.constellation.orion.viewer.prefs.GlobalOptions
import java.io.File
import java.io.FilenameFilter

open class OrionFileManagerActivity : OrionFileManagerActivityBase(
    true,
    FileChooserAdapter.DEFAULT_FILTER
) {
    companion object {
        const val OPEN_RECENTS_TAB = "OPEN_RECENTS_FILE"
        const val LAST_OPENED_DIRECTORY = "LAST_OPENED_DIR"
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        log("OrionFileManager: On new intent $intent")

        if (intent.getBooleanExtra(OPEN_RECENTS_TAB, false)) {
            findViewById<ViewPager>(R.id.viewpager).setCurrentItem(1, false)
            return
        }


        val dontStartRecent = intent.getBooleanExtra(DONT_OPEN_RECENT_FILE, false)
        if (!dontStartRecent && globalOptions.isOpenRecentBook) {
            if (!globalOptions.recentFiles.isEmpty()) {
                val entry = globalOptions.recentFiles[0]
                val book = File(entry.path)
                if (book.exists()) {
                    log("Opening recent book $book")
                    openFile(book)
                }
            }
        }
    }
}

private val DIRECTORY_DOCUMENTS = if (isAtLeastKitkat()) Environment.DIRECTORY_DOCUMENTS else "Documents"

private val possibleStartFolders = listOf(
    Environment.getExternalStoragePublicDirectory(DIRECTORY_DOCUMENTS) to R.string.file_manager_documents,
    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) to R.string.file_manager_downloads,
    Environment.getExternalStorageDirectory() to R.string.file_manager_sdcard
)

abstract class OrionFileManagerActivityBase @JvmOverloads constructor(
    private val showRecentsAndSavePath: Boolean = true,
    private val fileNameFilter: FilenameFilter = FileChooserAdapter.DEFAULT_FILTER
) : OrionBaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout

    private lateinit var drawerLayoutListener: ActionBarDrawerToggle

    private var prefs: SharedPreferences? = null

    protected lateinit var globalOptions: GlobalOptions

    private var justCreated: Boolean = false

    class FileManagerFragment : Fragment(R.layout.folder_view) {

        private val startFolder: File
            @SuppressLint("SdCardPath")
            get() {
                val lastOpenedDir =
                    (activity as OrionFileManagerActivityBase).globalOptions.lastOpenedDirectory

                if (lastOpenedDir != null && File(lastOpenedDir).exists()) {
                    return File(lastOpenedDir)
                }

                val startFolder =
                    (possibleStartFolders.map { it.first } + File("/system/media/sdcard/")).firstOrNull { it.exists() }
                if (startFolder != null) {
                    return startFolder
                }

                return Environment.getRootDirectory()
            }

        private lateinit var listView: ListView
        private lateinit var pathView: TextView

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            listView = view.findViewById(R.id.folderList)
            pathView = view.findViewById(R.id.path)

            val myActivity = requireActivity() as OrionFileManagerActivityBase

            listView.onItemClickListener =
                AdapterView.OnItemClickListener { parent, _, position, _ ->
                    val file = parent.getItemAtPosition(position) as File
                    if (file.isDirectory) {
                        changeFolder(file)
                    } else {
                        if (myActivity.showRecentsAndSavePath) {
                            val absolutePath = file.parentFile?.absolutePath
                            myActivity.prefs!!.edit().putString(LAST_OPENED_DIRECTORY, absolutePath)
                                .apply()
                        }
                        myActivity.openFile(file)
                    }
                }
            pathView.text = startFolder.absolutePath
            listView.adapter =
                FileChooserAdapter(requireActivity(), startFolder, myActivity.fileNameFilter)

            if (!setupGotoButton(file_manager_goto_documents, Environment.getExternalStoragePublicDirectory(DIRECTORY_DOCUMENTS)) or
                !setupGotoButton(file_manager_goto_downloads, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
            ) {
                setupGotoButton(file_manager_goto_sdcard,Environment.getExternalStorageDirectory())
            }
            val grant = requireView().findViewById<Button>(R.id.file_manager_grant)
            grant.setOnClickListener {
                requireActivity().checkAndRequestStorageAccessPermissionOrReadOne(Permissions.ASK_READ_PERMISSION_FOR_FILE_MANAGER)
            }
        }

        private fun setupGotoButton(id: Int, file: File): Boolean {
            val goto = requireView().findViewById<Button>(id)
            if (file.exists()) {
                goto.setOnClickListener {
                    changeFolder(file)
                }
                goto.visibility = View.VISIBLE
                return true
            } else {
                goto.visibility = View.GONE
                return false
            }
        }

        internal fun changeFolder(
            file: File
        ) {
            val newFolder = (listView.adapter as FileChooserAdapter).changeFolder(file)
            pathView.text = newFolder.absolutePath
        }

        override fun onResume() {
            super.onResume()
            if (activity !is OrionFileManagerActivity) {
                view?.findViewById<TextView>(R.id.file_manager_tip)?.visibility = View.GONE
            }
            view?.findViewById<TextView>(R.id.file_manager_grant)?.visibility =
                if (hasReadStoragePermission(requireActivity())) View.GONE else View.VISIBLE
        }
    }

    class RecentListFragment : ListFragment() {
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            listView.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
                val entry = parent.getItemAtPosition(position) as GlobalOptions.RecentEntry
                val file = File(entry.path)
                if (file.exists()) {
                    (requireActivity() as OrionFileManagerActivityBase).openFile(file)
                } else {
                    Toast.makeText(parent.context, getString(R.string.recent_book_not_found), LENGTH_SHORT).show()
                }
            }
        }

        override fun onResume() {
            super.onResume()
            updateRecentListAdapter()
        }

        private fun updateRecentListAdapter() {
            listAdapter = RecentListAdapter(requireActivity(), (requireActivity() as OrionFileManagerActivityBase).globalOptions.recentFiles)
        }

    }

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
        val navView = findViewById<NavigationView>(R.id.nav_view)
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
        menu.findItem(R.id.nav_system)?.setVisible(isAtLeastKitkat())
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
        if (requestCode == Permissions.ASK_READ_PERMISSION_FOR_FILE_MANAGER) {
            actualizePermissions()
        }
    }

    private fun actualizePermissions() {
        val hasReadStoragePermission = hasReadStoragePermission(this)
        if (hasReadStoragePermission) {
            refreshFolder()
        }
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

    protected open fun openFile(file: File) {
        log("Opening new book: " + file.path)

        startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setClass(applicationContext, OrionViewerActivity::class.java)
                data = Uri.fromFile(file)
                addCategory(Intent.CATEGORY_DEFAULT)
            }
        )
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


internal class SimplePagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {

    private val fragments: MutableList<Fragment> = arrayListOf(OrionFileManagerActivityBase.FileManagerFragment())

    fun addFragment(fragment: Fragment) {
        fragments.add(fragment)
    }

    override fun getItem(i: Int): Fragment {
        return fragments[i]
    }

    override fun getCount(): Int {
        return fragments.size
    }

}
