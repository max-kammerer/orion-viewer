import djvu.djvu_DjvuDocument_initContext
import djvu.djvu_DjvuDocument_openFile
import kotlinx.cinterop.*
import libui.ktx.*

fun main(args: Array<String>) = appWindow(
    title = "Hello",
    width = 320,
    height = 240
) {


    vbox {

        button("Open File") {
            action {
                val openedFile = OpenFileDialog()
                println(openedFile)

                val context = djvu_DjvuDocument_initContext(null, null)
                println("Context: $context")

                memScoped {
                    val pageCount = this.alloc<LongVar>()
                    val book = djvu_DjvuDocument_openFile(null, null, openedFile!!.cstr.getPointer(this), pageCount.ptr, context)
                    println(
                        "Book: $book size: ${pageCount.value}")
                }
            }
        }

        val scroll: TextArea = textarea {
            readonly = true
            stretchy = true
        }
    }
}