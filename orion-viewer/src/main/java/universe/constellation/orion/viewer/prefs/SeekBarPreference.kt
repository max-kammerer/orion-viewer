package universe.constellation.orion.viewer.prefs


import android.content.Context
import android.preference.DialogPreference
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View

import android.widget.SeekBar
import android.widget.TextView
import universe.constellation.orion.viewer.R

class SeekBarPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
        DialogPreference(context, attrs), SeekBar.OnSeekBarChangeListener, OrionBookPreference {

    override val orionState = State()
    // Real defaults
    var defaultSeekValue: Int = 50

    var maxValue: Int = 1
    var minValue: Int = 100

    // Current value
    private var mCurrentValue: Int = 0

    // View elements
    private var mSeekBar: SeekBar? = null
    private var mValueText: TextView? = null
    override var isCurrentBookOption: Boolean = false

    init {

        // Read parameters from attributes
        attrs?.let {
            minValue = attrs.getAttributeIntValue(PREFERENCE_NS, ATTR_MIN_VALUE, DEFAULT_MIN_VALUE)
            maxValue = attrs.getAttributeIntValue(PREFERENCE_NS, ATTR_MAX_VALUE, DEFAULT_MAX_VALUE)
            defaultSeekValue = attrs.getAttributeIntValue(ANDROID_NS, ATTR_DEFAULT_VALUE, DEFAULT_CURRENT_VALUE)
            isCurrentBookOption = attrs.getAttributeBooleanValue(PREFERENCE_NS, "isBook", false)
        }
    }

    override fun onCreateDialogView(): View {
        // Get current value from preferences
        mCurrentValue = getPersistedInt(defaultSeekValue)

        // Inflate layout
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.dialog_slider, null)

        // Setup minimum and maximum text labels
        (view.findViewById(R.id.min_value) as TextView).text = Integer.toString(minValue)
        (view.findViewById(R.id.max_value) as TextView).text = Integer.toString(maxValue)

        // Setup SeekBar
        mSeekBar = view.findViewById(R.id.seek_bar) as SeekBar
        mSeekBar!!.max = maxValue - minValue
        mSeekBar!!.progress = mCurrentValue - minValue
        mSeekBar!!.setOnSeekBarChangeListener(this)

        // Setup text label for current value
        mValueText = view.findViewById(R.id.current_value) as TextView
        mValueText!!.text = Integer.toString(mCurrentValue)

        return view
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        super.onDialogClosed(positiveResult)

        // Return if change was cancelled
        if (!positiveResult) {
            return
        }

        // Persist current value if needed
        if (shouldPersist()) {
            persistInt(mCurrentValue)
        }

        // Notify activity about changes (to update preference summary line)
        notifyChanged()
    }

    override fun getSummary(): CharSequence {
        // Format summary string with current value
        val summary = super.getSummary().toString()
        val value = getPersistedInt(defaultSeekValue)
        return String.format(summary, value)
    }

    override fun onSetInitialValue(restoreValue: Boolean, defaultValue: Any?) {
        super.onSetInitialValue(if (isCurrentBookOption) true else restoreValue, defaultValue)
    }

    override fun persistInt(value: Int): Boolean {
        if (isCurrentBookOption) {
            return OrionPreferenceUtil.persistValue(this, "" + value)
        } else {
            return super.persistInt(value)
        }
    }

    override fun getPersistedInt(defaultReturnValue: Int): Int {
        if (isCurrentBookOption) {
            return OrionPreferenceUtil.getPersistedInt(this, defaultReturnValue)
        } else {
            return super.getPersistedInt(defaultReturnValue)
        }
    }

    override fun onProgressChanged(seek: SeekBar, value: Int, fromTouch: Boolean) {
        // Update current value
        mCurrentValue = value + minValue
        // Update label with current value
        mValueText!!.text = Integer.toString(mCurrentValue)
    }

    override fun onStartTrackingTouch(seek: SeekBar) {
        // Not used
    }

    override fun onStopTrackingTouch(seek: SeekBar) {
        // Not used
    }

    companion object {

        // Namespaces to read attributes
        private val PREFERENCE_NS = "http://schemas.android.com/apk/res/universe.constellation.orion.viewer"
        private val ANDROID_NS = "http://schemas.android.com/apk/res/android"

        // Attribute names
        private val ATTR_DEFAULT_VALUE = "defaultValue"
        private val ATTR_MIN_VALUE = "minValue"
        private val ATTR_MAX_VALUE = "maxValue"

        // Default values for defaults
        private val DEFAULT_CURRENT_VALUE = 50
        private val DEFAULT_MIN_VALUE = 0
        private val DEFAULT_MAX_VALUE = 100
    }
}