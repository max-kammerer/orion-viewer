package universe.constellation.orion.viewer.test.screenshots

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.takeScreenshot
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.hamcrest.Matchers.endsWith
import org.hamcrest.Matchers.hasToString
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import universe.constellation.orion.viewer.BuildConfig
import universe.constellation.orion.viewer.OrionViewerActivity
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.filemanager.OrionFileManagerActivity
import universe.constellation.orion.viewer.filemanager.OrionFileManagerActivityBase.Companion.DONT_OPEN_RECENT_FILE
import universe.constellation.orion.viewer.outline.showOutline
import universe.constellation.orion.viewer.prefs.GlobalOptions
import universe.constellation.orion.viewer.prefs.OrionApplication
import universe.constellation.orion.viewer.test.framework.BaseInstrumentationTest
import universe.constellation.orion.viewer.test.framework.LONG_TIMEOUT
import universe.constellation.orion.viewer.test.framework.appContext
import universe.constellation.orion.viewer.test.framework.dumpBitmap

/**
 * Walks through the main screens, mostly the reader, under every application theme and stores
 * a screenshot of each into the failures folder, from where gradle pulls them into build/failures.
 * Not a check of anything, a tool for eyeballing themes, so it isn't part of the regular suite:
 * the source set is compiled only for the `tour` test build type (`-Porion.tour=true`) and the
 * "Screenshots" workflow runs it on demand.
 *
 * Themes can be limited with the instrumentation argument `tour.themes`, a comma-separated list:
 * `-Pandroid.testInstrumentationRunnerArguments.tour.themes=LIBRARY,CLASSIC`.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
@RunWith(Parameterized::class)
class ScreenTourTest(private val theme: String) : BaseInstrumentationTest() {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun themes(): List<String> {
            val all = appContext.resources.getStringArray(R.array.application_theme).toList()
            val requested = InstrumentationRegistry.getArguments().getString("tour.themes")
                .orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }
            return if (requested.isEmpty()) all else all.filter { it in requested }
        }

        /** A page with a body of text, the title page says little about colors. */
        private const val TEXT_PAGE = 40

        private val PREFS_TO_RESTORE = listOf(
            GlobalOptions.APPLICATION_THEME,
            OrionFileManagerActivity.LAST_OPENED_DIRECTORY,
            GlobalOptions.SHOW_TAP_HELP,
            GlobalOptions.OLD_UI
        )
    }

    private val prefs by lazy { PreferenceManager.getDefaultSharedPreferences(appContext) }

    private var savedPrefs: Map<String, Any?> = emptyMap()

    @Before
    fun setUpTheme() {
        savedPrefs = PREFS_TO_RESTORE.associateWith { prefs.all[it] }
        prefs.edit()
            .putString(GlobalOptions.APPLICATION_THEME, theme)
            .putString(OrionFileManagerActivity.LAST_OPENED_DIRECTORY, testDataFolder.absolutePath)
            .putBoolean(GlobalOptions.SHOW_TAP_HELP, false)
            .putBoolean(GlobalOptions.OLD_UI, false)
            .commit()
    }

    @After
    fun tearDown() {
        finishAllActivities()
        val editor = prefs.edit()
        savedPrefs.forEach { (key, value) ->
            when (value) {
                null -> editor.remove(key)
                is String -> editor.putString(key, value)
                is Boolean -> editor.putBoolean(key, value)
            }
        }
        editor.commit()
    }

    @Test
    fun tour() {
        val intent = Intent(appContext, OrionFileManagerActivity::class.java)
            .putExtra(DONT_OPEN_RECENT_FILE, true)
        ActivityScenario.launch<OrionFileManagerActivity>(intent).use { fileManager ->
            onView(withId(R.id.folderList)).check(matches(isDisplayed()))
            /* onData scrolls to the book: a device folder with extra files pushes it below the fold */
            onData(hasToString(endsWith(SICP))).inAdapterView(withId(R.id.folderList)).check(matches(isDisplayed()))
            shot("01_file_manager")

            fileManager.onActivity { it.drawer.openDrawer(GravityCompat.START, false) }
            device.wait(Until.hasObject(By.res(BuildConfig.APPLICATION_ID, "nav_view")), LONG_TIMEOUT)
            shot("02_drawer")

            fileManager.onActivity { it.drawer.closeDrawer(GravityCompat.START, false) }
            await("Drawer wasn't closed") {
                var closed = false
                fileManager.onActivity { closed = !it.drawer.isDrawerOpen(GravityCompat.START) }
                closed
            }

            onView(withText(R.string.file_manager_history)).perform(click())
            Espresso.onIdle()
            shot("03_recent")
            onView(withText(R.string.file_manager_fodlers)).perform(click())
            Espresso.onIdle()

            onData(hasToString(endsWith(SICP))).inAdapterView(withId(R.id.folderList)).perform(click())
            onView(withId(R.id.view)).check(matches(isCompletelyDisplayed()))
            Espresso.onIdle()
            val viewer = orionApplication.viewActivity ?: error("Viewer activity wasn't started")
            shot("04_book")

            runOnMain { viewer.controller!!.goToPage(TEXT_PAGE) }
            Espresso.onIdle()
            shot("05_page")

            viewer.openMenu()
            shot("06_menu")

            viewer.openMenuItem(R.id.zoom_menu_item)
            shot("07_zoom")
            closeDialog()

            viewer.openMenuItem(R.id.crop_menu_item)
            shot("08_crop")
            closeDialog()

            runOnMain { showOutline(viewer.controller!!, viewer) }
            assertNotNull(
                "Outline dialog did not appear",
                device.wait(Until.findObject(By.res(BuildConfig.APPLICATION_ID, "mainTreeView")), LONG_TIMEOUT)
            )
            shot("09_outline")
            device.pressBack()
            Espresso.onIdle()

            viewer.openMenuItem(R.id.search_menu_item)
            Espresso.closeSoftKeyboard()
            shot("10_search")
            viewer.back()

            viewer.openMenuItem(R.id.add_bookmark_menu_item)
            Espresso.closeSoftKeyboard()
            shot("11_add_bookmark")
            viewer.back()

            viewer.openMenuItem(R.id.bookmarks_menu_item)
            shot("12_bookmarks")
            viewer.back()

            viewer.openMenuItem(R.id.book_options_menu_item)
            shot("13_book_options")
            viewer.back()

            viewer.openMenuItem(R.id.options_menu_item)
            shot("14_settings")
            viewer.back()
        }
    }

    private fun shot(name: String) {
        device.waitForIdle()
        dumpBitmap("tour_${theme}_", name, takeScreenshot())
    }

    private fun OrionViewerActivity.openMenu() {
        runOnMain { showMenu() }
        Espresso.onIdle()
    }

    /** The menu may need a second attempt right after another activity was closed. */
    private fun OrionViewerActivity.openMenuItem(id: Int) {
        val resName = appContext.resources.getResourceEntryName(id)
        repeat(3) {
            openMenu()
            if (this@ScreenTourTest.device.wait(Until.hasObject(By.res(BuildConfig.APPLICATION_ID, resName)), LONG_TIMEOUT / 5)) {
                onView(withId(id)).perform(click())
                Espresso.onIdle()
                return
            }
        }
        throw AssertionError("Menu item $resName didn't show up")
    }

    /** Leaves a dialog or an activity opened from the viewer and waits until the viewer is in front again. */
    private fun OrionViewerActivity.back() {
        Espresso.pressBack()
        await("Viewer wasn't resumed after going back") {
            var resumed = false
            runOnMain {
                resumed = this in ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
            }
            resumed
        }
        Espresso.onIdle()
    }

    private val orionApplication: OrionApplication
        get() = appContext.applicationContext as OrionApplication

    private val OrionFileManagerActivity.drawer: DrawerLayout
        get() = findViewById(R.id.drawer_layout)

    private fun runOnMain(body: () -> Unit) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(body)
    }

    private fun finishAllActivities() {
        runOnMain {
            val registry = ActivityLifecycleMonitorRegistry.getInstance()
            listOf(Stage.RESUMED, Stage.PAUSED, Stage.STOPPED)
                .flatMap { registry.getActivitiesInStage(it) }
                .distinct()
                .forEach(Activity::finish)
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    private fun await(message: String, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + LONG_TIMEOUT
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition()) return
            SystemClock.sleep(50)
        }
        throw AssertionError("$message in $LONG_TIMEOUT ms")
    }
}
