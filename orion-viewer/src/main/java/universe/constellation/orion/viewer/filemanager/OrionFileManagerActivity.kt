/*
 * Orion Viewer - pdf, djvu, xps and cbz file viewer for android devices
 *
 * Copyright (C) 2011-2017 Michael Bogdanov & Co
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

package universe.constellation.orion.viewer.filemanager

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.app.ListFragment
import android.support.v4.view.ViewPager
import android.view.*
import android.widget.AdapterView
import android.widget.ListView
import android.widget.TextView
import androidx.core.widget.toast
import universe.constellation.orion.viewer.*
import universe.constellation.orion.viewer.prefs.GlobalOptions
import java.io.File
import java.io.FilenameFilter

/**
 * User: mike
 * Date: 24.12.11
 * Time: 16:41
 */

open class OrionFileManagerActivity @JvmOverloads constructor(
    private val showRecentsAndSavePath: Boolean = true,
    private val addToolbar: Boolean = true,
    private val fileNameFilter: FilenameFilter = FileChooserAdapter.DEFAULT_FILTER
) : OrionBaseActivity() {

    private var prefs: SharedPreferences? = null

    private var globalOptions: GlobalOptions? = null

    private var justCreated: Boolean = false

    private val startFolder: String
        get() {
            val lastOpenedDir = globalOptions!!.lastOpenedDirectory

            if (lastOpenedDir != null && File(lastOpenedDir).exists()) {
                return lastOpenedDir
            }

            val path = Environment.getExternalStorageDirectory().path + "/"
            if (File(path).exists()) {
                return path
            }

            val path1 = "/system/media/sdcard/"
            return if (File(path1).exists()) {
                path1
            } else Environment.getRootDirectory().absolutePath

        }

    class FoldersFragment : Fragment() {

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            return inflater.inflate(R.layout.folder_view, container, false)

        }

        override fun onActivityCreated(savedInstanceState: Bundle?) {
            super.onActivityCreated(savedInstanceState)
            (activity as OrionFileManagerActivity).createFileView(
                activity!!.findViewById<View>(R.id.listView) as ListView,
                activity!!.findViewById<View>(R.id.path) as TextView
            )
        }
    }

    class RecentListFragment : ListFragment() {
        override fun onActivityCreated(savedInstanceState: Bundle?) {
            super.onActivityCreated(savedInstanceState)
            (activity as OrionFileManagerActivity).createRecentView(this)
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onCreate(savedInstanceState: Bundle?) {
        onOrionCreate(savedInstanceState, R.layout.file_manager, addToolbar)
        log("Creating file manager")

        prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        globalOptions = orionContext.options

        initFileManager()

        justCreated = true

        Permissions.checkReadPermission(this)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (Permissions.ORION_ASK_PERMISSION_CODE == requestCode) {
            println("Permission callback...")
            val list = findViewById<View>(R.id.listView) as ListView
            val adapter = list.adapter
            if (adapter is FileChooserAdapter) {
                val currentFolder = adapter.currentFolder
                println("Refreshing view")
                adapter.changeFolder(File(currentFolder.absolutePath))
            }
        }
    }


    override fun onNewIntent(intent: Intent) {
        log("OrionFileManager: On new intent $intent")

        if (intent.getBooleanExtra(OPEN_RECENTS_TAB, false)) {
            findViewById<ViewPager>(R.id.viewpager).setCurrentItem(1, false)
            return
        }


        val dontStartRecent = intent.getBooleanExtra(DONT_OPEN_RECENT_FILE, false)
        if (!dontStartRecent && globalOptions!!.isOpenRecentBook) {
            if (!globalOptions!!.recentFiles.isEmpty()) {
                val entry = globalOptions!!.recentFiles[0]
                val book = File(entry.path)
                if (book.exists()) {
                    log("Opening recent book $book")
                    openFile(book)
                }
            }
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

    private fun createRecentView(list: ListFragment) {
        val recent = list.listView
        recent.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            val entry = parent.getItemAtPosition(position) as GlobalOptions.RecentEntry
            val file = File(entry.path)
            if (file.exists()) {
                openFile(file)
            } else {
                parent.context.toast(getString(R.string.recent_book_not_found))
            }
        }

        list.listAdapter = RecentListAdapter(this, globalOptions!!.recentFiles)
    }

    private fun createFileView(list: ListView, path: TextView) {
        list.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            val file = parent.getItemAtPosition(position) as File
            if (file.isDirectory) {
                val newFolder = (parent.adapter as FileChooserAdapter).changeFolder(file)
                path.text = newFolder.absolutePath
            } else {
                if (showRecentsAndSavePath) {
                    val editor = prefs!!.edit()
                    editor.putString(LAST_OPENED_DIRECTORY, file.parentFile.absolutePath)
                    editor.commit()
                }
                openFile(file)
            }
        }

        path.text = startFolder
        list.adapter = FileChooserAdapter(this, startFolder, fileNameFilter)
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
        val viewPager = findViewById<ViewPager>(R.id.viewpager)
        viewPager.adapter = pagerAdapter
        val tabLayout = findViewById<TabLayout>(R.id.sliding_tabs)
        tabLayout.setupWithViewPager(viewPager)

        val folderTab = tabLayout.getTabAt(0)
        folderTab?.setIcon(R.drawable.folder)
        if (showRecentsAndSavePath) {
            val recentTab = tabLayout.getTabAt(1)
            recentTab?.setIcon(R.drawable.book)
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

        const val OPEN_RECENTS_TAB = "OPEN_RECENTS_FILE"

        const val DONT_OPEN_RECENT_FILE = "DONT_OPEN_RECENT_FILE"

        const val LAST_OPENED_DIRECTORY = "LAST_OPENED_DIR"

        private const val LAST_FOLDER = "LAST_FOLDER"

        private const val FILE_FILTER_EXTENSION = "FILE_FILTER_EXTENSION"
    }
}


internal class SimplePagerAdapter(fm: FragmentManager, private val pageCount: Int) : FragmentStatePagerAdapter(fm) {

    override fun getItem(i: Int): Fragment {
        return if (i == 0)
            OrionFileManagerActivity.FoldersFragment()
        else
            OrionFileManagerActivity.RecentListFragment()
    }

    override fun getCount(): Int {
        return pageCount
    }

}
