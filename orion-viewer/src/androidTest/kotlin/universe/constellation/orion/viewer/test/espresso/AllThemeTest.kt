package universe.constellation.orion.viewer.test.espresso

import androidx.test.core.app.launchActivity
import org.junit.Assert
import org.junit.Test
import universe.constellation.orion.viewer.OrionViewerActivity
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.prefs.GlobalOptions
import universe.constellation.orion.viewer.test.framework.BaseInstrumentationTest
import universe.constellation.orion.viewer.test.framework.BookDescription
import universe.constellation.orion.viewer.test.framework.appContext

class AllThemeTest : BaseInstrumentationTest() {

    @Test
    fun testThemes() {
        val themes = appContext.resources.getStringArray(R.array.application_theme)

        Assert.assertEquals(5, themes.size)

        themes.forEach { theme ->
            val activity = launchActivity<OrionViewerActivity>(BookDescription.SICP.toOpenIntent {
                this.putExtra(GlobalOptions.APPLICATION_THEME, theme)
            })
            try {
                with(this) {
                    activity.openCropDialog()
                    closeDialog()
                    //TODO check colors
                }
            } finally {
                activity.close()
            }
        }
    }

}