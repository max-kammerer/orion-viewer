package universe.constellation.orion.viewer.prefs

import android.content.Context
import android.util.AttributeSet
import androidx.preference.ListPreference
import universe.constellation.orion.viewer.Action

class ActionListPreference : ListPreference {

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

    init {
        val actions = getLongDoubleActions(key)
        entryValues = actions.map { it.code.toString() }.toTypedArray()
        entries = actions.map { it.getActionName(context) }.toTypedArray()
    }

    override fun persistString(value: String): Boolean {
        val intValue = value.toInt()
        return persistInt(intValue)
    }

    override fun getPersistedString(defaultReturnValue: String?): String {
        return getPersistedInt(defaultReturnValue?.toInt() ?: 0).toString()
    }

    companion object {
        fun getLongDoubleActions(key: String): List<Action> {
            if (key == "LONG_TAP_ACTION") {
                return listOf(
                    Action.SELECT_TEXT_NEW,
                    Action.SELECT_WORD_AND_TRANSLATE,
                    Action.TAP_ACTION
                )
            }
            return listOf(
                Action.SELECT_TEXT_NEW,
                Action.SELECT_WORD_AND_TRANSLATE
            )
        }
    }
}