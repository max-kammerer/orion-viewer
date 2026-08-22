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
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.net.toUri
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import universe.constellation.orion.viewer.Permissions.checkWritePermission
import universe.constellation.orion.viewer.bookmarks.BookNameAndSize
import universe.constellation.orion.viewer.bookmarks.Bookmark
import universe.constellation.orion.viewer.bookmarks.ALL_BOOKMARKS_SUFFIX
import universe.constellation.orion.viewer.bookmarks.BOOKMARKS_SUFFIX
import universe.constellation.orion.viewer.bookmarks.BookmarkExporter
import universe.constellation.orion.viewer.bookmarks.BookmarkImporter
import universe.constellation.orion.viewer.bookmarks.bookmarksExportFile
import java.io.*
import java.util.*

class OrionBookmarkActivity : OrionBaseActivity() {

    private var bookId: Long = 0

    @SuppressLint("MissingSuperCall")
    public override fun onCreate(savedInstanceState: Bundle?) {
        onOrionCreate(savedInstanceState, R.layout.bookmarks,
            displayHomeAsUpEnabled = true
        )

        onNewIntent(intent)

        val view = findViewById<ListView>(R.id.bookmarks)
        view.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
            val bookmark = parent.getItemAtPosition(position) as Bookmark
            val result = Intent()
            result.putExtra(OPEN_PAGE, bookmark.page)
            log("bookmark id = " + bookmark.id + " page = " + bookmark.page)
            setResult(Activity.RESULT_OK, result)
            finish()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        bookId = intent.getLongExtra(BOOK_ID, -1)
        updateView(bookId)
    }

    private fun updateView(bookId: Long) {
        val accessor = orionApplication.getBookmarkAccessor()
        val bookmarks = accessor.selectBookmarks(bookId)
        val view = findViewById<ListView>(R.id.bookmarks)
        view.adapter = object : ArrayAdapter<Bookmark>(this, R.layout.bookmark_entry, R.id.bookmark_entry, bookmarks) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val convertView = super.getView(position, convertView, parent)

                val bookmark = getItem(position)
                val page = convertView.findViewById<View>(R.id.bookmark_entry_page) as TextView
                page.text = "${if (bookmark!!.page == -1) "*" else bookmark.page + 1}"

                val edit = convertView.findViewById<View>(R.id.bookmark_edit_entry) as ImageView
                //if (edit != null)
                edit.setOnClickListener {
                    if (position != 0) {
                        val item = getItem(position)!!

                        val builder = createThemedAlertBuilder().setIcon(R.drawable.edit_item)

                        builder.setTitle("Edit Bookmark")
                        val editText = EditText(this@OrionBookmarkActivity)
                        editText.setText(item.text)
                        builder.setView(editText)

                        builder.setPositiveButton("Save") { dialog, which ->
                            if (editText.text.isEmpty()) {
                                this@OrionBookmarkActivity.showAlert("Warning", "Coudn't save empty bookmark")
                            } else {
                                orionApplication.getBookmarkAccessor().insertOrUpdateBookmark(bookId, item.page, editText.text.toString())
                                item.text = editText.text.toString()
                                notifyDataSetChanged()
                            }
                            dialog.dismiss()
                        }

                        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

                        builder.setNeutralButton("Delete") { _, _ ->
                            orionApplication.getBookmarkAccessor().deleteBookmark(item.id.toLong())
                            updateView(bookId)
                        }

                        builder.create().show()
                    }
                }
                return convertView
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val result = super.onCreateOptionsMenu(menu)
        if (result) {
            menuInflater.inflate(R.menu.bookmarks_menu, menu)
        }
        return result
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            R.id.export_bookmarks_menu_item -> {
                //should be granted automatically
                checkWritePermission(this)
                if (bookId == -1L) {
                    showLongMessage("There is nothing to export!")
                } else {
                    exportBookmarks(bookId, BOOKMARKS_SUFFIX)
                }
                return true
            }

            R.id.export_all_bookmarks_menu_item -> {
                //should be granted automatically
                checkWritePermission(this)
                exportBookmarks(EXPORT_ALL_BOOKS, ALL_BOOKMARKS_SUFFIX)
                return true
            }

            R.id.import_current_bookmarks_menu_item -> {
                val intent = Intent(this, OrionFileSelectorActivity::class.java)
                startActivityForResult(intent, IMPORT_CURRRENT)
                return true
            }

            R.id.import_all_bookmarks_menu_item -> {
                intent = Intent(this, OrionFileSelectorActivity::class.java)
                startActivityForResult(intent, IMPORT_ALL)
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            val fileUri = data?.getStringExtra(OrionFileSelectorActivity.RESULT_FILE_URI)
                ?.takeIf { it.isNotEmpty() }?.toUri()
            if (fileUri == null) {
                showWarning("File name is empty")
                return
            } else {
                log("To import $fileUri")
                val books = listBooks(fileUri) ?: return

                if (books.isEmpty()) {
                    showWarning("There is no any bookmarks")
                    return
                }

                val importCurrent = requestCode == IMPORT_CURRRENT

                books.sort()

                val group = layoutInflater.inflate(R.layout.bookmark_book_list, null)
                val tree = group.findViewById<View>(R.id.book_list) as ListView

                val builder = createThemedAlertBuilder()
                builder.setTitle("Select source book").setCancelable(true).setView(group)
                builder.setPositiveButton("Import") { dialog, which ->
                    dialog.dismiss()
                    val currentBookParameters = orionApplication.currentBookParameters
                    if (importCurrent && currentBookParameters == null) {
                        showWarning("There is no opened book to import into")
                        return@setPositiveButton
                    }
                    val toBook = currentBookParameters?.let {
                        BookNameAndSize(it.simpleFileName, it.fileSize)
                    }
                    doImport(fileUri, getCheckedItems(tree), if (importCurrent) toBook else null)
                }
                builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }


                val dialog = builder.create()


                tree.choiceMode = if (!importCurrent) ListView.CHOICE_MODE_MULTIPLE else ListView.CHOICE_MODE_SINGLE
                tree.onItemClickListener = AdapterView.OnItemClickListener { _, _, _, _ -> dialog.getButton(Dialog.BUTTON_POSITIVE).isEnabled = tree.checkedItemPositions.size() != 0 }

                tree.adapter = object : ArrayAdapter<BookNameAndSize>(this, if (importCurrent) R.layout.select_book_item else R.layout.select_book_item_multi, R.id.title, books) {

                    private var positiveDisabled: Boolean = false

                    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                        val convertView = super.getView(position, convertView, parent)

                        val book = getItem(position)
                        var view = convertView.findViewById<View>(R.id.page) as TextView
                        view.text = book!!.beautifySize()

                        view = convertView.findViewById<View>(R.id.title) as TextView
                        view.text = book.name

                        if (!positiveDisabled) {
                            //android bug
                            positiveDisabled = true
                            if (importCurrent) {
                                dialog.getButton(Dialog.BUTTON_POSITIVE).isEnabled = false
                            }
                        }

                        return convertView
                    }
                }
                tree.itemsCanFocus = false

                if (!importCurrent) {
                    for (i in 0 until tree.adapter.count) {
                        tree.setItemChecked(i, true)
                    }
                }

                dialog.show()

            }
        }
    }

    private fun exportBookmarks(bookId: Long, suffix: String) {
        val target = bookmarksExportFile(orionApplication.currentBookParameters?.openingFileName, suffix)
        log("Bookmarks output file: $target")

        val exporter = BookmarkExporter(orionApplication.getBookmarkAccessor(), target.absolutePath)
        val exported = try {
            exporter.export(bookId)
        } catch (e: IOException) {
            showAndLogError(this, e)
            return
        }

        if (exported) {
            showLongMessage("Bookmarks exported to $target")
        } else {
            showLongMessage("There is nothing to export!")
        }
    }

    private fun doImport(fileUri: Uri, books: Set<BookNameAndSize>, toBook: BookNameAndSize?) {
        log("Import bookmarks " + books.size)

        val input = openImportStream(fileUri) ?: return
        val importer = BookmarkImporter(orionApplication.getBookmarkAccessor(), input, books, toBook)
        try {
            importer.doImport()
            val currentBookParameters = orionApplication.currentBookParameters
            val bookId = currentBookParameters?.let {
                orionApplication.getBookmarkAccessor().selectBookId(it.simpleFileName, it.fileSize)
            } ?: -1L
            updateView(bookId)
            showFastMessage("Imported successfully")
        } catch (e: OrionException) {
            showAlert("Error", e.message!!)
        }

    }

    private fun openImportStream(fileUri: Uri): InputStream? {
        return try {
            contentResolver.openInputStream(fileUri)
                ?: throw FileNotFoundException("No content for $fileUri")
        } catch (e: Exception) {
            showAndLogError(this, "Couldn't open file", e)
            null
        }
    }

    private fun listBooks(fileUri: Uri): MutableList<BookNameAndSize>? {
        val bookNames = ArrayList<BookNameAndSize>()
        var reader: InputStreamReader? = null
        try {
            reader = InputStreamReader(openImportStream(fileUri) ?: return null)
            val factory = XmlPullParserFactory.newInstance()

            val xpp = factory.newPullParser()
            xpp.setInput(reader)

            var eventType = xpp.eventType
            var wasBookmarks = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    val tagName = xpp.name
                    if ("bookmarks" == tagName) {
                        wasBookmarks = true
                    } else if (wasBookmarks) {
                        if ("book" == tagName) {
                            val name = xpp.getAttributeValue(NAMESPACE, "fileName")
                            val size = java.lang.Long.valueOf(xpp.getAttributeValue(NAMESPACE, "fileSize"))
                            val book = BookNameAndSize(name, size)
                            bookNames.add(book)
                        }
                    }
                }
                eventType = xpp.next()
            }
            return bookNames
        } catch (e: FileNotFoundException) {
            showAndLogError(this, "Couldn't open file", e)
        } catch (e: XmlPullParserException) {
            showAndLogError(this, "Couldn't parse book parameters", e)
        } catch (e: IOException) {
            showAndLogError(this, "Couldn't parse book parameters", e)
        } finally {
            if (reader != null) {
                try {
                    reader.close()
                } catch (e: IOException) {
                    log(e)
                }

            }
        }
        return null
    }

    private fun getCheckedItems(view: ListView): Set<BookNameAndSize> {
        val result = HashSet<BookNameAndSize>()
        val checked = view.checkedItemPositions
        for (i in 0 until checked.size()) {
            if (checked.valueAt(i)) {
                result.add(view.adapter.getItem(checked.keyAt(i)) as BookNameAndSize)
            }
        }
        return result
    }

    companion object {

        const val OPEN_PAGE = "open_page"

        const val OPENED_FILE = "opened_file"

        const val BOOK_ID = "book_id"

        const val NAMESPACE = ""

        const val IMPORT_CURRRENT = 1

        const val IMPORT_ALL = 2

        const val EXPORT_ALL_BOOKS = -1L
    }

}
