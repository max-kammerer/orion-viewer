import documents.DjvuDocument
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

                val djvuDocument = DjvuDocument(openedFile!!)
                println(djvuDocument.pageCount)
                val pageInfo = djvuDocument.getPageInfo(0, 0)
                println(pageInfo)
                djvuDocument.destroy()
            }
        }

        val scroll: TextArea = textarea {
            readonly = true
            stretchy = true
        }
    }
}