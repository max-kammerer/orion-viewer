package universe.constellation.orion.viewer.view

sealed class Operation {

    object TOUCH_ZOOM : Operation()

    object DEFAULT : Operation()

}