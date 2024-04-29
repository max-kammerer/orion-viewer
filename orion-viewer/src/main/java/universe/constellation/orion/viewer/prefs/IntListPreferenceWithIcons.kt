package universe.constellation.orion.viewer.prefs

import android.content.Context
import android.util.AttributeSet

class IntListPreferenceWithIcons : ListPreferenceWithIcons {
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

    override fun persistString(value: String): Boolean {
        return super.persistInt(value.toInt())
    }

    override fun setDefaultValue(defaultValue: Any?) {
        super.setDefaultValue(defaultValue)
    }

    override fun getPersistedString(defaultReturnValue: String): String {
        return super.getPersistedInt(defaultReturnValue.toInt()).toString()
    }
}