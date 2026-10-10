package universe.constellation.orion.viewer

import java.io.File
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FileContentTest {

    private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }

    private fun detect(text: String) = detectFileContent(text.toByteArray(Charsets.UTF_8))

    private fun detect(vararg prefix: Int, total: Int = 64) =
        detectFileContent(bytes(*prefix) + ByteArray(total - prefix.size) { 1 })

    @Test
    fun signatures() {
        assertEquals(FileContent.PDF, detect("%PDF-1.7\n%âã"))
        assertEquals(FileContent.DJVU, detect("AT&TFORM\u0000\u0000\u0001\u0000DJVM"))
        assertEquals(FileContent.ZIP, detect(0x50, 0x4B, 0x03, 0x04))
        assertEquals(FileContent.ZIP, detect(0x50, 0x4B, 0x05, 0x06))
        assertEquals(FileContent.RAR, detect("Rar!\u001A\u0007\u0001\u0000"))
        assertEquals(FileContent.SEVEN_ZIP, detect(0x37, 0x7A, 0xBC, 0xAF, 0x27, 0x1C))
        assertEquals(FileContent.PNG, detect(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
        assertEquals(FileContent.JPEG, detect(0xFF, 0xD8, 0xFF, 0xE0))
        assertEquals(FileContent.WEBP, detect("RIFF\u0010\u0000\u0000\u0000WEBPVP8 "))
        assertEquals(FileContent.TIFF, detect("II*\u0000\u0008\u0000"))
        assertEquals(FileContent.TIFF, detect("MM\u0000*\u0000\u0000"))
        assertEquals(FileContent.GIF, detect("GIF89a\u0001\u0000"))
        assertEquals(FileContent.BMP, detect(0x42, 0x4D, 0x36, 0x10, 0x0E, 0x00, 0x00, 0x00, 0x00, 0x00, 0x36, 0x00))
        assertEquals(FileContent.JPEG2000, detect(0x00, 0x00, 0x00, 0x0C, 0x6A, 0x50, 0x20, 0x20))
        assertEquals(FileContent.JPEG2000, detect(0xFF, 0x4F, 0xFF, 0x51))
        assertEquals(FileContent.ISO_MEDIA, detect(0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70, 0x68, 0x65, 0x69, 0x63))
    }

    @Test
    fun tarIsToldByItsMagicAtOffset257() {
        val header = ByteArray(512)
        "book/page1.jpg".toByteArray().copyInto(header)
        "ustar".toByteArray().copyInto(header, 257)
        assertEquals(FileContent.TAR, detectFileContent(header))
    }

    @Test
    fun formDataIsNotPdf() {
        assertEquals(FileContent.FDF, detect("%FDF-1.2\n1 0 obj <</FDF <</Fields []>>>>"))
        assertEquals(FileContent.FDF, detect("junk\r\n%FDF-1.2\n%PDF-1.4 inside a string"), "the first marker wins")
    }

    @Test
    fun pdfHeaderAfterJunkIsStillPdf() {
        assertEquals(FileContent.PDF, detect("\r\n<garbage from a mail gateway>\r\n%PDF-1.4\n1 0 obj"))
        assertEquals(FileContent.TEXT, detect(" ".repeat(1100) + "%PDF-1.4"), "mupdf looks only at the first kilobyte")
    }

    @Test
    fun textIsToldWithoutSignature() {
        assertEquals(FileContent.TEXT, detect("# Title\n\nSome *markdown* text.\n"))
        assertEquals(FileContent.TEXT, detect("Текст в UTF-8 и 中文\r\n\tс табуляцией"))
        val cp1251 = "Текст в Windows-1251".toByteArray(charset("windows-1251"))
        assertEquals(FileContent.TEXT, detectFileContent(cp1251))
        assertEquals(FileContent.TEXT, detectFileContent(bytes(0xFF, 0xFE) + "UTF-16".toByteArray(Charsets.UTF_16LE)))
    }

    @Test
    fun markupIsTextOfItsOwnKind() {
        assertEquals(FileContent.HTML, detect("﻿  \n<!DOCTYPE html><html><body>Not found</body></html>"))
        assertEquals(FileContent.HTML, detect("<HTML><HEAD><TITLE>Error</TITLE>"))
        assertEquals(FileContent.XML, detect("<?xml version=\"1.0\" encoding=\"UTF-8\"?><FictionBook>"))
    }

    @Test
    fun bmText() {
        assertEquals(FileContent.TEXT, detect("BMW manual, chapter one"))
    }

    @Test
    fun randomDataIsBinary() {
        /* what the reports showed: encrypted data under a .pdf name */
        val random = Random(7)
        repeat(20) {
            val data = random.nextBytes(FILE_CONTENT_SAMPLE_SIZE)
            /* exclude the rare draw that starts like a known signature */
            if (detectFileContent(data)?.isImage == true) return@repeat
            assertEquals(FileContent.BINARY, detectFileContent(data))
        }
    }

    @Test
    fun zerosAndEmpty() {
        assertEquals(FileContent.ZEROS, detectFileContent(ByteArray(FILE_CONTENT_SAMPLE_SIZE)))
        assertNull(detectFileContent(ByteArray(0)))
        assertNull(detectFileContent(ByteArray(10), size = 0))
    }

    @Test
    fun textHeuristic() {
        assertFalse(looksLikeText(bytes(0x41, 0x00, 0x42), 3), "a zero byte")
        val manyControls = ByteArray(100) { if (it % 10 == 0) 0x01 else 0x41 }
        assertFalse(looksLikeText(manyControls, manyControls.size), "10% control characters")
        val fewControls = ByteArray(200) { if (it == 0) 0x01 else 0x41 }
        assertTrue(looksLikeText(fewControls, fewControls.size), "a stray control character")
    }

    @Test
    fun tailMarker() {
        val file = File.createTempFile("tail", ".pdf")
        try {
            file.writeBytes("%PDF-1.4\n".toByteArray() + ByteArray(5000) { 0x20 } + "startxref\n9\n%%EOF\n".toByteArray())
            assertEquals(true, fileTailContains(file.path, "%%EOF"))
            file.writeBytes("%PDF-1.4\n".toByteArray() + ByteArray(5000) { 0x20 })
            assertEquals(false, fileTailContains(file.path, "%%EOF"), "truncated")
            file.writeBytes("%%EOF".toByteArray())
            assertEquals(true, fileTailContains(file.path, "%%EOF"), "file shorter than the window")
        } finally {
            file.delete()
        }
        assertEquals(null, fileTailContains(file.path, "%%EOF"), "missing file")
    }

    @Test
    fun fileIsReadUpToTheSample() {
        val file = File.createTempFile("content", ".pdf")
        try {
            file.writeBytes("%PDF-1.4\n".toByteArray() + ByteArray(10_000) { 0x20 })
            assertEquals(FileContent.PDF, detectFileContent(file.path))
            file.writeBytes(ByteArray(0))
            assertNull(detectFileContent(file.path))
        } finally {
            file.delete()
        }
        assertNull(detectFileContent(file.path), "missing file")
    }
}
