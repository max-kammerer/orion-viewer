package universe.constellation.orion.viewer.device.texet

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.text.format.Time
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.experimental.runBlocking
import universe.constellation.orion.viewer.Common
import universe.constellation.orion.viewer.LastPageInfo
import universe.constellation.orion.viewer.device.EInkDevice
import universe.constellation.orion.viewer.document.Document
import universe.constellation.orion.viewer.document.DocumentWithCaching
import java.io.*

open class TexetDevice : EInkDevice() {

    override fun onNewBook(info: LastPageInfo, document: Document) {
        try {
            val coverFileName = getIconFileName(info.simpleFileName, info.fileSize)
            shtampTexetFile(info.openingFileName, info.simpleFileName, "", "" + info.totalPages, "" + info.pageNumber, coverFileName)
            rememberCover(coverFileName, document)
        } catch (e: Exception) {
            Common.d(e)
            Toast.makeText(activity, "Error on new book parameters update: " + e.message, Toast.LENGTH_SHORT).show()
        }

    }

    override fun onBookClose(info: LastPageInfo) {
        super.onBookClose(info)

        try {
            shtampTexetFile(null, null, null, "" + info.totalPages, "" + info.pageNumber, null)
        } catch (e: Exception) {
            Common.d(e)
            Toast.makeText(activity, "Error on parameters update on book close: " + e.message, Toast.LENGTH_SHORT).show()
        }

    }

    //code provided by texet
    @Throws(Exception::class)
    fun shtampTexetFile(cPath: String?, cTitle: String?, cAuthor: String?, cAllPage: String, cCurPage: String, cCover: String?) {
        if (cCover != null)
            Log.e("COVER", cCover)

        val myTime = Time(Time.getCurrentTimezone())
        myTime.setToNow()
        val cDate = String.format("%02d/%02d/%02d %02d:%02d",
                myTime.monthDay, myTime.month + 1, myTime.year, myTime.hour, myTime.minute, myTime.second)

        val bmkContext = activity.applicationContext.createPackageContext("com.android.systemui", Context.CONTEXT_IGNORE_SECURITY)


        val settings = bmkContext.getSharedPreferences("MyPrefsFile",
                Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE)
        val editor: SharedPreferences.Editor

        if (cPath == null) {
            editor = settings.edit()

            editor.putString("FirstRecentTotalPage", cAllPage)
            editor.putString("FirstRecentPage", cCurPage)
            editor.putString("FirstRecentReadDate", cDate)

            editor.commit()
            return
        }

        val curRecentReadingPath = settings.getString("FirstRecentPath", "")
        if (cPath.contentEquals(curRecentReadingPath!!)) {
            return
        }

        editor = settings.edit()

        var firstRecentPath = settings.getString("FirstRecentPath",
                "")
        var firstRecentTitle = settings.getString("FirstRecentTitle", "")
        var firstRecentAuthor = settings.getString("FirstRecentAuthor", "")
        var firstRecentTotalPage = settings.getString("FirstRecentTotalPage", "")
        var firstRecentPage = settings.getString("FirstRecentPage", "")
        var firstRecentDate = settings.getString("FirstRecentReadDate", "")
        var firstBookCover = settings.getString("FirstBookCover", "")

        var secondRecentPath = settings.getString("SecondRecentPath", "")
        var secondRecentTitle = settings.getString("SecondRecentTitle", "")
        var secondRecentAuthor = settings.getString("SecondRecentAuthor", "")
        var secondRecentTotalPage = settings.getString("SecondRecentTotalPage", "")
        var secondRecentPage = settings.getString("SecondRecentPage", "")
        var secondRecentDate = settings.getString("SecondRecentReadDate", "")
        var secondBookCover = settings.getString("SecondBookCover", "")

        var thirdRecentPath = settings.getString("ThirdRecentPath",
                "")
        var thirdRecentTitle = settings.getString("ThirdRecentTitle", "")
        var thirdRecentAuthor = settings.getString("ThirdRecentAuthor", "")
        var thirdRecentTotalPage = settings.getString("ThirdRecentTotalPage", "")
        var thirdRecentPage = settings.getString("ThirdRecentPage",
                "")
        var thirdRecentDate = settings.getString("ThirdRecentReadDate", "")
        var thirdBookCover = settings.getString("ThirdBookCover", "")

        if (cPath.contentEquals(secondRecentPath!!)) {
            secondRecentPath = firstRecentPath
            secondRecentTitle = firstRecentTitle
            secondRecentAuthor = firstRecentAuthor
            secondRecentTotalPage = firstRecentTotalPage
            secondRecentPage = firstRecentPage
            secondRecentDate = firstRecentDate
            secondBookCover = firstBookCover
        } else {
            thirdRecentPath = secondRecentPath
            thirdRecentTitle = secondRecentTitle
            thirdRecentAuthor = secondRecentAuthor
            thirdRecentTotalPage = secondRecentTotalPage
            thirdRecentPage = secondRecentPage
            thirdRecentDate = secondRecentDate
            thirdBookCover = secondBookCover

            secondRecentPath = firstRecentPath
            secondRecentTitle = firstRecentTitle
            secondRecentAuthor = firstRecentAuthor
            secondRecentTotalPage = firstRecentTotalPage
            secondRecentPage = firstRecentPage
            secondRecentDate = firstRecentDate
            secondBookCover = firstBookCover
        }

        firstRecentPath = cPath
        firstRecentTitle = cTitle
        firstRecentAuthor = cAuthor
        firstRecentTotalPage = cAllPage
        firstRecentPage = cCurPage
        firstRecentDate = cDate
        firstBookCover = cCover

        editor.putString("FirstRecentPath", firstRecentPath)
        editor.putString("FirstRecentTitle", firstRecentTitle)
        editor.putString("FirstRecentAuthor", firstRecentAuthor)
        editor.putString("FirstRecentTotalPage", firstRecentTotalPage)
        editor.putString("FirstRecentPage", firstRecentPage)
        editor.putString("FirstRecentReadDate", firstRecentDate)
        editor.putString("FirstBookCover", firstBookCover)

        editor.putString("SecondRecentPath", secondRecentPath)
        editor.putString("SecondRecentTitle", secondRecentTitle)
        editor.putString("SecondRecentAuthor", secondRecentAuthor)
        editor.putString("SecondRecentTotalPage", secondRecentTotalPage)
        editor.putString("SecondRecentPage", secondRecentPage)
        editor.putString("SecondRecentReadDate", secondRecentDate)
        editor.putString("SecondBookCover", secondBookCover)

        editor.putString("ThirdRecentPath", thirdRecentPath)
        editor.putString("ThirdRecentTitle", thirdRecentTitle)
        editor.putString("ThirdRecentAuthor", thirdRecentAuthor)
        editor.putString("ThirdRecentTotalPage", thirdRecentTotalPage)
        editor.putString("ThirdRecentPage", thirdRecentPage)
        editor.putString("ThirdRecentReadDate", thirdRecentDate)
        editor.putString("ThirdBookCover", thirdBookCover)

        editor.commit()
    }

    open fun getIconFileName(simpleFileName: String, fileSize: Long): String {
        return ""
    }

    @Throws(FileNotFoundException::class)
    private fun writeCover(originalBitmap: Bitmap, newFileName: String?) {
        var stream: OutputStream? = null
        try {
            stream = FileOutputStream(newFileName!!)
            originalBitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
        } finally {
            if (stream != null) {
                try {
                    stream.close()
                } catch (e: IOException) {
                    Common.d(e)
                }

            }
        }
    }

    private fun rememberCover(coverFileName: String?, doc: Document) {
        if (coverFileName != null && coverFileName.isNotEmpty() && !File(coverFileName).exists()) {
            Common.d("Writing cover to " + coverFileName)
            Thread(Runnable {
                try {
                    var originalDoc = doc
                    if (doc is DocumentWithCaching) {
                        originalDoc = doc.doc
                    }

                    if (originalDoc.pageCount <= 0) {
                        Common.d("No pages in document")
                        return@Runnable
                    }
                    val defaultHeight = 320

                    Common.d("Extracting cover info ...")
                    runBlocking {
                        val (_, width, height) = originalDoc.getPageInfo(0, 0)
                        if (width <= 0 || height <= 0) {
                            Common.d("Wrong page defaultHeight: " + width + "x" + height)
                        }

                        val zoom = Math.min(1.0f * defaultHeight / width, 1.0f * defaultHeight / height)
                        val sizeX = (zoom * width).toInt()
                        val sizeY = (zoom * height).toInt()
                        val bm = Bitmap.createBitmap(sizeX, sizeY, Bitmap.Config.ARGB_8888)
                        val xDelta = -(sizeX - (zoom * width).toInt()) / 2
                        val yDelta = -(sizeY - (zoom * height).toInt()) / 2
                        Common.d("Cover info " + zoom + " xD: " + xDelta + " yD: " + yDelta + " bm: " + sizeX + "x" + sizeY)
                        originalDoc.renderPage(0, bm, zoom.toDouble(), xDelta, yDelta, sizeX + xDelta, sizeY + yDelta)
                        writeCover(bm, coverFileName)
                    }
                } catch (e: FileNotFoundException) {
                    Common.d(e)
                }
            }).run()
        }
    }
}
