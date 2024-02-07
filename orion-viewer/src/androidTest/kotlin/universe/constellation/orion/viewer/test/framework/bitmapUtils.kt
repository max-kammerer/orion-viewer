package universe.constellation.orion.viewer.test.framework

import android.graphics.Bitmap
import android.os.Environment
import org.junit.Assert
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

internal fun dumpBitmap(prefix: String = "test", suffix: String, bitmap: Bitmap) {
    val file = Environment.getExternalStorageDirectory().path + "/orion/$prefix$suffix.png"
    println("saving dump into $file")
    val file1 = File(file)
    file1.parentFile?.mkdirs()
    file1.createNewFile()
    FileOutputStream(file).use { stream ->
        bitmap.compress(
            Bitmap.CompressFormat.PNG,
            100,
            stream
        )
        stream.close()
    }
}

internal const val DEFAULT_COLOR_DELTA = 3

//TODO check color difference reasons
internal fun compareBitmaps(partData: IntArray, fullData: IntArray, bitmapWidth: Int, message: String = "Fail", colorDelta: Int = DEFAULT_COLOR_DELTA, additionalDebugActions: () -> Unit) {
    if (!partData.contentEquals(fullData)) {
        if (MANUAL_DEBUG) {
            additionalDebugActions()
        }
        for ( i in partData.indices) {
            if (partData[i] != fullData[i]) {
                val colorDiff = colorDiff(partData[i], fullData[i])
                if (colorDiff > colorDelta) {
                    Assert.fail("$message: different pixels at line " + i / bitmapWidth + " and position " + i % bitmapWidth + ": " + partData[i] + " vs "  + fullData[i] + " diff=" + colorDiff)
                }
            }
        }
    }
}

internal fun colorDiff(a: Int, b: Int): Int {
    val a1 = a and 0xFF000000u.toInt() ushr 24
    val a2 = a and 0xFF0000 ushr 16
    val a3 = a and 0xFF00 ushr 8
    val a4 = a and 0xFF

    val b1 = b and 0xFF000000u.toInt() ushr 24
    val b2 = b and 0xFF0000 ushr 16
    val b3 = b and 0xFF00 ushr 8
    val b4 = b and 0xFF
    println(abs(a1-b1) * 1  + abs(a2-b2) * 10 + abs(a3-b3) * 100 + abs(a4-b4) * 1000)
    return abs(a1-b1)   + abs(a2-b2)  + abs(a3-b3) + abs(a4-b4)
}