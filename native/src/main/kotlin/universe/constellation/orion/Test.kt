package universe.constellation.orion

import universe.constellation.orion.viewer.OperationHolder
import universe.constellation.orion.viewer.device.Device

fun main(args: Array<String>) {
    object : Device {
        override fun onKeyUp(keyCode: Int, isLongPress: Boolean, operation: OperationHolder): Boolean {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onPause() {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onWindowGainFocus() {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onUserInteraction() {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override val isDefaultDarkTheme: Boolean
            get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    }

}

