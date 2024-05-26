package universe.constellation.orion.viewer.test.framework

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import universe.constellation.orion.viewer.BuildConfig
import universe.constellation.orion.viewer.OrionViewerActivity
import universe.constellation.orion.viewer.filemanager.fileExtension
import universe.constellation.orion.viewer.prefs.GlobalOptions

fun createTestViewerIntent(body: Intent.() -> Unit): Intent {
    return Intent(Intent.ACTION_VIEW).apply {
        setClassName(
            BuildConfig.APPLICATION_ID,
            OrionViewerActivity::class.qualifiedName!!
        )
        addCategory(Intent.CATEGORY_DEFAULT)
        body()
        if (!hasExtra(GlobalOptions.SHOW_TAP_HELP)) {
            putExtra(GlobalOptions.SHOW_TAP_HELP, false)
        }
        putExtra(GlobalOptions.OPEN_AS_TEMP_BOOK, true)
    }
}

fun createContentIntentWithGenerated(fileName: String): Intent {
    return createTestViewerIntent {
        val uri = Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
            .authority(universe.constellation.orion.viewer.test.BuildConfig.APPLICATION_ID + ".fileprovider")
            .encodedPath(fileName).appendQueryParameter("displayName", fileName).build()

        instrumentationContext.grantUriPermission(
            BuildConfig.APPLICATION_ID,
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileName.fileExtension)
        setDataAndType(uri, mimeType)
    }
}

fun BookFile.toOpenIntentWithNewUI(): Intent {
    return toOpenIntent {
        putExtra(GlobalOptions.OLD_UI, false)
    }
}