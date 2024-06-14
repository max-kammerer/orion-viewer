package universe.constellation.orion.viewer.view

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.BatteryManager
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.children
import androidx.core.widget.ImageViewCompat
import universe.constellation.orion.viewer.OrionBookListener
import universe.constellation.orion.viewer.OrionViewerActivity
import universe.constellation.orion.viewer.R
import universe.constellation.orion.viewer.dpToPixels
import universe.constellation.orion.viewer.util.ColorUtil

class StatusBar(
    private val fullScene: ViewGroup,
    private val statusBar: ViewGroup,
    private val activity: OrionViewerActivity
) : OrionBookListener {

    private val panel = statusBar.findViewById<ViewGroup>(R.id.orion_status_bar)
    private val title = statusBar.findViewById<TextView>(R.id.title)
    private val offset = statusBar.findViewById<TextView>(R.id.offset)
    private val clock = statusBar.findViewById<TextView>(R.id.clock)
    private val page = statusBar.findViewById<TextView>(R.id.page)
    private val totalPages = statusBar.findViewById<TextView>(R.id.totalPages)
    private val battery = statusBar.findViewById<ImageView>(R.id.batteryLevel)

    init {
        val globalOptions = activity.globalOptions
        globalOptions.SHOW_BATTERY_STATUS.observe(activity) {
            battery.setVisibleOrGone(it)
        }

        globalOptions.STATUS_BAR_POSITION.observe(activity) {
            updatePosition(it)
        }

        globalOptions.SHOW_OFFSET_ON_STATUS_BAR.observe(activity) {
            offset.setVisibleOrGone(it)
        }

        globalOptions.SHOW_TIME_ON_STATUS_BAR.observe(activity) {
            clock.setVisibleOrGone(it)
        }

        globalOptions.STATUS_BAR_SIZE.observe(activity) {
            updateSize(it)
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateBatteryLevel(intent)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onNewBook(title: String?, pageCount: Int) {
        this.title.text = title
        this.totalPages.text = "/$pageCount"
        this.page.text = "?"
        this.offset.text = "[?, ?]"
    }

    @SuppressLint("SetTextI18n")
    fun onPageUpdate(pageNum: Int, x: Int, y: Int) {
        offset.text = "[${pad(x)}:${pad(y)}]"
        page.text = "${pageNum + 1}"
    }

    private fun pad(value: Int): String {
        val pValue = abs(value)
        return when {
            pValue < 10 -> "  $value"
            pValue < 100 -> " $value"
            else -> "$value"
        }
    }

    private fun View.setVisibleOrGone(show: Boolean) {
        visibility = if (show) View.VISIBLE else View.GONE
    }

    fun setColorMatrix(colorMatrix: FloatArray?) {
        val transformedColor = ColorUtil.transformColor(Color.BLACK, colorMatrix)
        (0 until panel.childCount).forEach {
            when (val child = panel.getChildAt(it)) {
                is TextView -> {
                    child.setTextColor(transformedColor)
                }

                is ImageView -> {
                    ImageViewCompat.setImageTintList(
                        child,
                        ColorStateList.valueOf(transformedColor)
                    );
                }
            }
        }
    }

    fun onResume(context: Context) {
        updateBatteryLevel(
            context.registerReceiver(
                batteryReceiver,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
        )
    }

    fun onPause(context: Context) {
        context.unregisterReceiver(batteryReceiver)
    }

    private fun updateBatteryLevel(intent: Intent?) {
        if (intent == null) {
            battery.setImageResource(R.drawable.battery_unknown)
            return
        }

        val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val batteryPct = (level * 100 / scale.toFloat()).toInt()
        battery.setImageResource(
            when {
                batteryPct >= 95 -> R.drawable.battery_full
                batteryPct >= 85 -> R.drawable.battery_6_bar
                batteryPct >= 70 -> R.drawable.battery_5_bar
                batteryPct >= 55 -> R.drawable.battery_4_bar
                batteryPct >= 40 -> R.drawable.battery_3_bar
                batteryPct >= 25 -> R.drawable.battery_2_bar
                batteryPct >= 10 -> R.drawable.battery_1_bar
                else -> R.drawable.battery_alert
            }
        )
    }

    private fun updatePosition(value: String?) {
        statusBar.setVisibleOrGone(value != "HIDE")
        if (value == "HIDE") return

        val index = fullScene.children.indexOf(statusBar)
        when (value) {
            "BOTTOM" -> {
                if (index != fullScene.childCount - 1) {
                    fullScene.removeView(statusBar)
                    fullScene.addView(statusBar)
                }
            }

            else -> {
                if (index != 0) {
                    fullScene.removeView(statusBar)
                    fullScene.addView(statusBar, 0)
                }
            }
        }
    }


    private fun updateSize(value: String?) {
        val size = when (value) {
            "SMALL" -> 12
            "LARGE" -> 16
            else -> 14
        }
        statusBar.children.forEach {
            when (it) {
                is TextView -> {
                    it.textSize = size.toFloat()
                }

                is ImageView -> {
                    it.layoutParams =
                        it.layoutParams?.apply {
                            val newSize = activity.dpToPixels(size.toFloat())
                            width = newSize
                            height = newSize
                        }
                }
            }
        }
    }
}