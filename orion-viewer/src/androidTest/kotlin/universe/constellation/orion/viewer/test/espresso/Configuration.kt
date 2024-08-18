package universe.constellation.orion.viewer.test.espresso

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers
import universe.constellation.orion.viewer.R

open class Configuration(private val name: String, val body: BaseViewerActivityTest.() -> Unit) {

    override fun toString(): String {
        return name
    }
}

object AutoCropConfig : Configuration ("crop", {
    activityScenarioRule.scenario.openCropDialog()
    Espresso.onView(ViewMatchers.withText("Auto")).perform(click())
    Espresso.onView(ViewMatchers.withId(R.id.auto)).perform(click())
    applyCrop()
})

object ContrastConfig : Configuration ("contrast", {
    activityScenarioRule.scenario.openBookOptions()
    Espresso.onView(ViewMatchers.withText(R.string.pref_book_contrast)).perform(click())
    Espresso.onView(ViewMatchers.withText("40")).perform(click())
    Espresso.pressBack()
})

object DefaultConfig : Configuration("empty", {})

fun allConfigs(): List<Configuration> {
    return listOf(DefaultConfig, AutoCropConfig, /*ContrastConfig*/)
}

fun decartMult(vararg data: List<Any>): List<Array<Any>> {
    var cur = listOf<List<Any>>()
    for (i in data) {
        cur = if (cur.isEmpty()) {
            i.map { listOf(it) }
        } else {
            i.flatMap { el ->
                cur.map { it + el }
            }
        }
    }
    return cur.map { it.toTypedArray<Any>() }
}