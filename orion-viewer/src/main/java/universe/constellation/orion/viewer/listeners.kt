package universe.constellation.orion.viewer

import android.graphics.Bitmap

import java.util.concurrent.CountDownLatch

/**
 * User: mike
 * Date: 20.10.13
 * Time: 9:21
 */
interface OrionBookListener {
    fun onNewBook(title: String?, pageCount: Int)
}

interface OrionImageListener {
    fun onNewImage(bitmap: Bitmap?, info: LayoutPosition?, latch: CountDownLatch?)
}
