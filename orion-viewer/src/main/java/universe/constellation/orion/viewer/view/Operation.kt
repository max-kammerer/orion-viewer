package universe.constellation.orion.viewer.view

sealed class Operation {

    data object TOUCH_ZOOM : Operation()

    data object DEFAULT : Operation()
}