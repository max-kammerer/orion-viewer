package universe.constellation.orion.viewer.prefs

import android.content.Context
import android.util.AttributeSet
import androidx.preference.SeekBarPreference

class SeekBarPreferenceAsText : SeekBarPreference {
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int = 0
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null
    ) : super(context, attrs)

    override fun persistInt(value: Int): Boolean {
        return persistString(value.toString())
    }

    override fun getPersistedInt(defaultReturnValue: Int): Int {
        return super.getPersistedString(defaultReturnValue.toString()).toIntOrNull() ?: defaultReturnValue
    }
}