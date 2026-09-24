package universe.constellation.orion.viewer.test.espresso

import org.junit.Assert.assertEquals
import org.junit.Test
import universe.constellation.orion.viewer.Action
import universe.constellation.orion.viewer.test.framework.BookDescription
import universe.constellation.orion.viewer.test.framework.onActivity

/** The crop actions meant for hardware keys: hidden from the action picker, still bound on old devices. */
class CropActionsTest : BaseViewerActivityTest(BookDescription.SICP) {

    @Test
    fun keyCropActionsChangeTheMargins() {
        val start = margins()
        doAction(Action.CROP_LEFT)
        doAction(Action.CROP_LEFT)
        doAction(Action.CROP_TOP)
        doAction(Action.UNCROP_RIGHT)
        margins().let {
            assertEquals(start.left + 2, it.left)
            assertEquals(start.top + 1, it.top)
            assertEquals(start.right - 1, it.right)
            assertEquals(start.bottom, it.bottom)
        }

        //inverted: crop actions widen the margin back
        doAction(Action.INVERSE_CROP)
        doAction(Action.CROP_LEFT)
        assertEquals(start.left + 1, margins().left)
        doAction(Action.INVERSE_CROP)

        //big step from the "long crop" option
        val longCrop = onActivity { it.globalOptions.longCrop }
        doAction(Action.SWITCH_CROP)
        doAction(Action.CROP_BOTTOM)
        assertEquals(start.bottom + longCrop, margins().bottom)
        doAction(Action.SWITCH_CROP)
        doAction(Action.UNCROP_BOTTOM)
        assertEquals(start.bottom + longCrop - 1, margins().bottom)
    }

    private fun doAction(action: Action) = onActivity { it.doAction(action) }

    private fun margins() = onActivity { it.controller!!.margins }
}
