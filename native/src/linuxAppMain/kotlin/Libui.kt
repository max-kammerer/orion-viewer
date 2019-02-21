import documents.DjvuDocument
import libui.ktx.*
import libui.ktx.draw.Brush
import libui.ktx.draw.Color
import libui.ktx.draw.brush
import libui.ktx.draw.fill
import universe.constellation.orion.viewer.Bitmap

fun graphWidth(clientWidth: Double): Double = clientWidth
fun graphHeight(clientHeight: Double): Double = clientHeight

val width = 600
val height = 800

fun main(args: Array<String>) {
    appWindow(
        title = "Hello",
        width = width,
        height = height
    ) {

        vbox {
            lateinit var canvas: DrawArea
            lateinit var brush: Brush

            button("Open File") {

                action {
                    val openedFile = OpenFileDialog()
                    println(openedFile)

                    val djvuDocument = DjvuDocument(openedFile!!)
                    println(djvuDocument.pageCount)
                    val pageInfo = djvuDocument.getPageInfo(0, 0)
                    println(pageInfo)
                    val bitmap = Bitmap(width, height)


                    djvuDocument.renderPage(0, bitmap, 0.3, 0, 0, width, height)

                    canvas.draw {
                        val graphWidth = graphWidth(it.AreaWidth)
                        val graphHeight = graphHeight(it.AreaHeight)

                        val data = bitmap.pixels.get()
                        for ((index, value) in data.withIndex()) {
                            val value = value
                            val b = ((value shr 16) and 255).toDouble() / 255
                            val g = ((value shr 8) and 255).toDouble() / 255
                            val r = ((value) and 255).toDouble() / 255
                            val a = ((value shr 24) and 255).toDouble() / 255
                            val c = Color(r, g, b, a)

                            fill(brush.solid(c)) {
                                val x = index % bitmap.getWidth()
                                val y = index / bitmap.getWidth()
                                rectangle(x.toDouble(), y.toDouble(), 1.0, 1.0)

                            }
                        }
//                        fill(brush.solid(colorWhite)) {
//                            rectangle(0.0, 0.0, it.AreaWidth, it.AreaHeight)
//                        }
                    }
                    canvas.redraw()
                    djvuDocument.destroy()
                }
            }

            canvas = drawarea {
                stretchy = true
                brush = brush()
            }
        }
    }
}