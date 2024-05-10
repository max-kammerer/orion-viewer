package universe.constellation.orion.viewer.prefs

import android.content.Context
import android.util.AttributeSet

class IntListPreferenceWithIcons @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ListPreferenceWithIcons(context, attrs) {

    override fun persistString(value: String): Boolean {
        return super.persistInt(value.toInt())
    }

    override fun getPersistedString(defaultReturnValue: String?): String {
        return super.getPersistedInt(defaultReturnValue?.toInt() ?: 0).toString()
    }
}