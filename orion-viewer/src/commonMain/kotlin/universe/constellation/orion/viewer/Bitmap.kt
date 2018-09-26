/*
 * Orion Viewer - pdf, djvu, xps and cbz file viewer for android devices
 *
 * Copyright (C) 2011-2017 Michael Bogdanov & Co
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package universe.constellation.orion.viewer

expect class Bitmap {
    fun getWidth(): Int
    fun getHeight(): Int
    open fun getPixels(bitmapArray: IntArray, i: Int, width: Int, i1: Int, i2: Int, width1: Int, height: Int)
}

expect fun createBitmap(width: Int, height: Int): Bitmap

expect class LruCache<K, V> {
    fun evictAll()
    operator fun get(k: K): V?
    fun put(k: K, v: V): V
}

expect fun <K, V> createCache(size: Int): LruCache<K, V>