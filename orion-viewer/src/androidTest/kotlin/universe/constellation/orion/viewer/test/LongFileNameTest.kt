package universe.constellation.orion.viewer.test

import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import universe.constellation.orion.viewer.FileInfo
import universe.constellation.orion.viewer.MAX_FILE_NAME_BYTES
import universe.constellation.orion.viewer.OrionViewerActivity
import universe.constellation.orion.viewer.createTmpFile
import universe.constellation.orion.viewer.getValidTmpCopy
import universe.constellation.orion.viewer.onTmpCopyComplete
import universe.constellation.orion.viewer.loadBookParameters
import universe.constellation.orion.viewer.save
import universe.constellation.orion.viewer.test.framework.BaseTest
import universe.constellation.orion.viewer.test.framework.BookDescription
import universe.constellation.orion.viewer.test.framework.onActivityRes
import universe.constellation.orion.viewer.utf8Size
import java.io.File

/**
 * File names are limited to 255 bytes, and a Cyrillic title takes two per letter, six when
 * percent-encoded. A book name that fits by itself went over with ".<size>.xml" appended for its
 * parameters, which then failed to save on every pause; the temp copy of a document with such a
 * title failed the same way.
 */
class LongFileNameTest : BaseTest() {

    /* 254 bytes: a valid file name, but not with the ".<size>.xml" of its parameters. */
    private val longName = "Т".repeat(125) + ".pdf"

    /* 264 bytes: a display name from the provider, not bound by any file system. */
    private val tooLongTitle = "Т".repeat(130) + ".pdf"

    @Test
    fun parametersOfBookWithLongNameAreSavedAndRestored() {
        ActivityScenario.launch<OrionViewerActivity>(BookDescription.SICP.toOpenIntent()).use { scenario ->
            Espresso.onIdle()
            val restored = scenario.onActivityRes { activity ->
                val path = File(activity.cacheDir, longName).absolutePath
                val info = loadBookParameters(activity, path) {}
                assertTrue(info.fileData!!, info.fileData!!.utf8Size() <= MAX_FILE_NAME_BYTES)
                info.pageNumber = 7
                info.save(activity)
                try {
                    loadBookParameters(activity, path) {}.pageNumber
                } finally {
                    activity.deleteFile(info.fileData)
                }
            }
            assertEquals(7, restored)
        }
    }

    @Test
    fun tempCopyOfDocumentWithLongTitleIsCreatedAndFound() {
        ActivityScenario.launch<OrionViewerActivity>(BookDescription.SICP.toOpenIntent()).use { scenario ->
            Espresso.onIdle()
            scenario.onActivityRes { activity ->
                val id = "primary:Download/$tooLongTitle"
                val uri = Uri.parse("content://com.android.externalstorage.documents/document/" + Uri.encode(id))
                val info = FileInfo(tooLongTitle, 1234, id, "", uri)

                val copy = activity.createTmpFile(info, "pdf")
                try {
                    copy.writeBytes(ByteArray(1234))
                    activity.onTmpCopyComplete(copy, info)
                    assertTrue(copy.name, copy.name.utf8Size() <= MAX_FILE_NAME_BYTES && copy.name.endsWith(".pdf"))
                    assertEquals(copy, activity.getValidTmpCopy(info))
                } finally {
                    copy.parentFile?.deleteRecursively()
                }
            }
        }
    }
}
