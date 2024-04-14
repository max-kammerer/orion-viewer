package universe.constellation.orion.viewer.filemanager

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.view.*
import android.widget.AdapterView
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.fragment.app.ListFragment
import com.google.android.material.tabs.TabLayout
import universe.constellation.orion.viewer.*
import universe.constellation.orion.viewer.Permissions.checkAndRequestStorageAccessPermissionOrReadOne
import universe.constellation.orion.viewer.Permissions.hasReadStoragePermission
import universe.constellation.orion.viewer.filemanager.OrionFileManagerActivity.Companion.LAST_OPENED_DIRECTORY
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
            findViewById<androidx.viewpager.widget.ViewPager>(R.id.viewpager).setCurrentItem(1, false)
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

abstract class OrionFileManagerActivityBase @JvmOverloads constructor(
    private val showRecentsAndSavePath: Boolean = true,
    private val fileNameFilter: FilenameFilter = FileChooserAdapter.DEFAULT_FILTER
) : OrionBaseActivity() {

    private var prefs: SharedPreferences? = null

    protected lateinit var globalOptions: GlobalOptions

    private var justCreated: Boolean = false

    private val startFolder: File
        @SuppressLint("SdCardPath")
        get() {
            val lastOpenedDir = globalOptions.lastOpenedDirectory

            if (lastOpenedDir != null && File(lastOpenedDir).exists()) {
                return File(lastOpenedDir)
            }

            val possibleStartFolders = listOf(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                Environment.getExternalStorageDirectory(),
                File("/system/media/sdcard/")
            )
            val startFolder = possibleStartFolders.firstOrNull { it.exists() }
            if (startFolder != null) {
                return startFolder
            }

            return Environment.getRootDirectory()
        }

    class FileManagerFragment : Fragment(R.layout.folder_view) {

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            (activity as OrionFileManagerActivityBase).initializeFileView(
                view.findViewById(R.id.listView),
                view.findViewById(R.id.path),
                view
            )
        }

        override fun onResume() {
            super.onResume()
            if (activity !is OrionFileManagerActivity) {
                view?.findViewById<TextView>(R.id.file_manager_tip)?.visibility = View.GONE
            }
            view?.findViewById<TextView>(R.id.file_manager_grant)?.visibility = if (hasReadStoragePermission(requireActivity())) View.GONE else View.VISIBLE
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

        showPermissionRequestDialog()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (Permissions.ASK_READ_PERMISSION_FOR_FILE_MANAGER == requestCode) {
            log("Permission callback: " + permissions.joinToString() + " " + grantResults.joinToString())
            if (checkPermissionGranted(grantResults, permissions, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                val list = findViewById<ListView>(R.id.listView)
                val adapter = list.adapter
                if (adapter is FileChooserAdapter) {
                    val currentFolder = adapter.currentFolder
                    log("Refreshing view")
                    adapter.changeFolder(File(currentFolder.absolutePath))
                }
            } else {
                showPermissionRequestDialog()
            }
        }
    }

    private fun showPermissionRequestDialog() {
        if (!hasReadStoragePermission(this)) {
            AlertDialog.Builder(this).setMessage(R.string.permission_directory_warning)
                .setPositiveButton(R.string.permission_grant) { _, _ ->
                    checkAndRequestStorageAccessPermissionOrReadOne(Permissions.ASK_READ_PERMISSION_FOR_FILE_MANAGER)
                }.setNegativeButton(R.string.permission_cancel) { d, _ -> d.dismiss() }.show()
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        justCreated = false
    }

    override fun onResume() {
        super.onResume()
        if (justCreated) {
            justCreated = false
            onNewIntent(intent)
        }
    }

    private fun initializeFileView(list: ListView, path: TextView, view: View) {
        list.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
            val file = parent.getItemAtPosition(position) as File
            if (file.isDirectory) {
                changeFolder(parent, file, path)
            } else {
                if (showRecentsAndSavePath) {
                    val absolutePath = file.parentFile?.absolutePath
                    prefs!!.edit().putString(LAST_OPENED_DIRECTORY, absolutePath).apply()
                }
                openFile(file)
            }
        }

        path.text = startFolder.absolutePath
        list.adapter = FileChooserAdapter(this, startFolder, fileNameFilter)

        if (!setupGotoButton(view, list, path, R.id.file_manager_goto_documents, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)) or
            !setupGotoButton(view, list, path, R.id.file_manager_goto_downloads, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))) {
            setupGotoButton(view, list, path, R.id.file_manager_goto_sdcard, Environment.getExternalStorageDirectory())
        }
        val grant = findViewById<Button>(R.id.file_manager_grant)
        grant.setOnClickListener {
            checkAndRequestStorageAccessPermissionOrReadOne(Permissions.ASK_READ_PERMISSION_FOR_FILE_MANAGER)
        }
    }

    private fun setupGotoButton(root: View, list: ListView, path: TextView,  id: Int, file: File): Boolean {
        val goto = root.findViewById<Button>(id)
        if (file.exists()) {
            goto.setOnClickListener {
                changeFolder(list, file, path)
            }
            goto.visibility = View.VISIBLE
            return true
        } else {
            goto.visibility = View.GONE
            return false
        }
    }

    private fun changeFolder(
        parent: AdapterView<*>,
        file: File,
        path: TextView
    ) {
        val newFolder = (parent.adapter as FileChooserAdapter).changeFolder(file)
        path.text = newFolder.absolutePath
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
        val pagerAdapter = SimplePagerAdapter(supportFragmentManager, if (showRecentsAndSavePath) 2 else 1)
        val viewPager = findViewById<androidx.viewpager.widget.ViewPager>(R.id.viewpager)
        viewPager.adapter = pagerAdapter
        val tabLayout = findViewById<TabLayout>(R.id.sliding_tabs)
        tabLayout.setupWithViewPager(viewPager)

        val folderTab = tabLayout.getTabAt(0)
        folderTab?.setIcon(R.drawable.folder)
        if (showRecentsAndSavePath) {
            val recentTab = tabLayout.getTabAt(1)
            recentTab?.setIcon(R.drawable.recent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val result = super.onCreateOptionsMenu(menu)
        if (result) {
            menuInflater.inflate(R.menu.file_manager_menu, menu)
        }
        return result
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.exit_menu_item -> {
                finish()
                return true
            }
        }
        return false
    }

    companion object {
        const val DONT_OPEN_RECENT_FILE = "DONT_OPEN_RECENT_FILE"
    }
}


internal class SimplePagerAdapter(fm: androidx.fragment.app.FragmentManager, private val pageCount: Int) : FragmentStatePagerAdapter(fm) {

    override fun getItem(i: Int): Fragment {
        return if (i == 0)
            OrionFileManagerActivityBase.FileManagerFragment()
        else
            OrionFileManagerActivityBase.RecentListFragment()
    }

    override fun getCount(): Int {
        return pageCount
    }

}
