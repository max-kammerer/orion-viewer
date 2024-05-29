package universe.constellation.orion.viewer.view

sealed class Operation {

    data object PINCH_ZOOM : Operation()

    data object DEFAULT : Operation()
}