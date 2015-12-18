package universe.constellation.orion.viewer

/**
 * Created by mike on 12/18/15.
 */

class CropMargins(
        @JvmField val left: Int,
        @JvmField val right: Int,
        @JvmField val top: Int,
        @JvmField val bottom: Int,
        @JvmField val evenLeft: Int,
        @JvmField val evenRight: Int,
        @JvmField val evenCrop: Boolean,
        @JvmField val autoCrop: Boolean)
