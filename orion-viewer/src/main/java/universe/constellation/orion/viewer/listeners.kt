package universe.constellation.orion.viewer

import android.graphics.Point
import universe.constellation.orion.viewer.geometry.Rect

typealias Position = Point
typealias ORect = Rect

interface OrionBookListener {
    fun onNewBook(title: String?, pageCount: Int)
}
