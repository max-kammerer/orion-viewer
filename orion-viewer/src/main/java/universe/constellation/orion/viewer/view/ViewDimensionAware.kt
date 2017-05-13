package universe.constellation.orion.viewer.view

interface ViewDimensionAware {
    fun onDimensionChanged(newWidth: Int, newHeight: Int)
}
