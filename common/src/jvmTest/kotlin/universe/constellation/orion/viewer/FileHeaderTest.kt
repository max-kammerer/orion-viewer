package universe.constellation.orion.viewer

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class FileHeaderTest {

    private fun describe(vararg bytes: Int, total: Int = 64): String {
        val file = File.createTempFile("header", ".djvu")
        try {
            file.writeBytes(ByteArray(total) { if (it < bytes.size) bytes[it].toByte() else 1 })
            return describeFileHeader(file.path)
        } finally {
            file.delete()
        }
    }

    private fun ascii(text: String) = text.map { it.code }.toIntArray()

    @Test
    fun djvuForm() {
        assertEquals(
            "header=AT&TFORM, form=DJVM, content=djvu",
            describe(*ascii("AT&TFORM"), 0, 0, 0, 0x34, *ascii("DJVM"))
        )
    }

    @Test
    fun damagedFormInHex() {
        assertEquals(
            "header=AT&TFORM, form=4B 00 1F A3, form_end=64, file_length=64, content=djvu",
            describe(*ascii("AT&TFORM"), 0, 0, 0, 0x34, 0x4B, 0x00, 0x1F, 0xA3)
        )
    }

    @Test
    fun printableForeignForm() {
        assertEquals(
            "header=AT&TFORM, form=K..., form_end=1048588, file_length=64, content=djvu",
            describe(*ascii("AT&TFORM"), 0, 0x10, 0, 0, *ascii("K..."))
        )
    }
}
