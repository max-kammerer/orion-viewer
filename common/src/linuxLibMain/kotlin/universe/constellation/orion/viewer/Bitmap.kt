package universe.constellation.orion.viewer

actual class Bitmap(private val width1: Int, private val height1: Int) {

    actual fun getWidth() = width1

    actual fun getHeight() = height1

    private val pixels: IntArray = IntArray(width1 * height1)


    actual open fun getPixels(pixels: IntArray,
                              offset: Int,
                              stride: Int,
                              x: Int,
                              y: Int,
                              width: Int,
                              height: Int) {

    }
}