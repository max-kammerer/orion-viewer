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
            }
        }

        val scroll: TextArea = textarea {
            readonly = true
            stretchy = true
        }
    }
}