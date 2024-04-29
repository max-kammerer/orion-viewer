package universe.constellation.orion.viewer.android

import android.content.Context
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceScreen
import androidx.preference.SeekBarPreference
import universe.constellation.orion.viewer.BuildConfig
import universe.constellation.orion.viewer.prefs.IntListPreferenceWithIcons
import universe.constellation.orion.viewer.prefs.ListPreferenceWithIcons
import universe.constellation.orion.viewer.prefs.ListPreferenceWithIconsLayoutDialog

abstract class DSLPreferenceFragment : PreferenceFragmentCompat(), PreferenceFragmentCompat.OnPreferenceDisplayDialogCallback {

    private val DIALOG_FRAGMENT_TAG = "orion.preference"

    override fun onPreferenceDisplayDialog(
        caller: PreferenceFragmentCompat,
        pref: Preference
    ): Boolean {
        if (pref is ListPreferenceWithIcons) {
            // check if dialog is already showing
            val dialogFragment =
                ListPreferenceWithIconsLayoutDialog.newInstance(pref.getKey())
            dialogFragment.setTargetFragment(this, 0)
            dialogFragment.show(getParentFragmentManager(), DIALOG_FRAGMENT_TAG)
            return true
        }
        return false
    }

    @PublishedApi
    internal var builderState: MutableList<Pair<PreferenceGroup, Preference>>? = null

    inline fun rootScreen(context: Context, isGeneral: Boolean = true, init: PreferenceScreen.() -> Unit): PreferenceScreen {
        if (BuildConfig.DEBUG && builderState != null) {
            error("rootScreen couldn't be nested")
        }
        return preferenceManager.createPreferenceScreen(context).also {
            it.nested(isGeneral, init)
            preferenceScreen = it
        }
    }

    inline fun PreferenceScreen.nested(isGeneral: Boolean = true, init: PreferenceScreen.() -> Unit) {
        builderState = arrayListOf()
        try {
            init()
            builderState!!.forEach { (parent, child) ->
                parent.addPreference(child)
            }
        } finally {
            builderState = null
        }
    }

    inline fun PreferenceGroup.category(init: PreferenceCategory.() -> Unit): PreferenceCategory =
        preference(PreferenceCategory(context), init)

    inline fun PreferenceGroup.list(init: ListPreference.() -> Unit) =
        preference(ListPreference(context), init)

    inline fun PreferenceGroup.listWithIcons(init: ListPreferenceWithIcons.() -> Unit) =
        preference(ListPreferenceWithIcons(context), init)

    inline fun PreferenceGroup.intListWithIcons(init: IntListPreferenceWithIcons.() -> Unit) =
        preference(IntListPreferenceWithIcons(context), init)

    inline fun PreferenceGroup.seekBar(init: SeekBarPreference.() -> Unit) =
        preference(SeekBarPreference(context), init)

    inline fun <T : Preference> PreferenceGroup.preference(preference: T, init: T.() -> Unit): T {
        return preference.also { newChild ->
            builderState!!.add(this to newChild)
            newChild.init()
            newChild.isIconSpaceReserved = false
        }
    }

    val Int.stringRes
        get() = resources.getString(this)

    val Int.intArray
        get() = resources.getIntArray(this)

    val Int.stringArray
        get() = resources.getStringArray(this)
}

