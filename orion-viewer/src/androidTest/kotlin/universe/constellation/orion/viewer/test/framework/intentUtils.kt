package universe.constellation.orion.viewer.test.framework

import android.content.Intent
import universe.constellation.orion.viewer.BuildConfig
import universe.constellation.orion.viewer.OrionViewerActivity

fun openOrionIntent(body: Intent.() -> Unit): Intent {
    return Intent(Intent.ACTION_VIEW).apply {
        setClassName(
            BuildConfig.APPLICATION_ID,
            OrionViewerActivity::class.qualifiedName!!
        )
        addCategory(Intent.CATEGORY_DEFAULT)
        body()
    }
}