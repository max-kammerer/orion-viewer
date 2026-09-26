package universe.constellation.orion.viewer.test

import android.content.Intent
import android.os.Build
import android.widget.EditText
import android.widget.ListView
import androidx.test.core.app.ActivityScenario
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import universe.constellation.orion.viewer.Action
import universe.constellation.orion.viewer.ActionGroup
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.prefs.ActionListActivity
import java.io.File

/** The action picker of tap zones and keys: "None" first, groups with headers, folding and search. */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
class ActionPickerTest {

    private fun ActionListActivity.rows(): List<Any> {
        val list = findViewById<ListView>(R.id.actionsGroup)
        return (0 until list.adapter.count).map { list.adapter.getItem(it)!! }
    }

    @Test
    fun groupsFoldingAndSearch() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, ActionListActivity::class.java).putExtra("type", 0)
        ActivityScenario.launch<ActionListActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val rows = activity.rows()
                assertEquals(Action.NONE, rows.first())
                assertEquals(ActionGroup.entries.toList(), rows.filterIsInstance<ActionGroup>())
                //every action but the folded key cropping ones, once, grouped, in declaration order inside a group
                val expected = ActionGroup.entries.filter { it != ActionGroup.CROP_KEYS }
                    .flatMap { group -> Action.entries.filter { it.group == group } }
                assertEquals(expected, rows.filterIsInstance<Action>().drop(1))
                assertTrue(rows.indexOf(ActionGroup.NAVIGATION) < rows.indexOf(Action.NEXT))
            }
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            device.waitForIdle()
            device.takeScreenshot(File("/sdcard/Download/orion/action_picker.png"))

            //a header tap unfolds the group
            scenario.onActivity { activity ->
                val list = activity.findViewById<ListView>(R.id.actionsGroup)
                val position = activity.rows().indexOf(ActionGroup.CROP_KEYS)
                list.performItemClick(list, position, list.adapter.getItemId(position))
                assertTrue(activity.rows().containsAll(Action.entries))
            }

            //a query shows the matching actions of every group, folded or not
            scenario.onActivity { activity ->
                activity.findViewById<EditText>(R.id.actions_search).setText("crop")
                val rows = activity.rows()
                val actions = rows.filterIsInstance<Action>()
                assertTrue(actions.contains(Action.CROP) && actions.contains(Action.CROP_LEFT) && actions.contains(Action.SWITCH_CROP))
                assertTrue(!actions.contains(Action.NEXT) && !rows.contains(Action.NONE))
                assertEquals(listOf(ActionGroup.VIEW, ActionGroup.CROP_KEYS), rows.filterIsInstance<ActionGroup>())
            }
        }
    }

    @Test
    fun currentChoiceIsShownUnfolded() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, ActionListActivity::class.java).putExtra("type", 0).putExtra("code", Action.CROP_TOP.code)
        ActivityScenario.launch<ActionListActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(activity.rows().contains(Action.CROP_TOP))
            }
        }
    }
}
