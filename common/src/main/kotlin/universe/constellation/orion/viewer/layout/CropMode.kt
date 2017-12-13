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

package universe.constellation.orion.viewer.layout

enum class CropMode(@JvmField val cropMode: Int) {
    NO_MODE(-1),
    MANUAL(0),
    AUTO(1),
    MANUAL_AUTO(2),
    AUTO_MANUAL(3)
}


val Int.toMode: CropMode
    get() = when (this) {
        -1 -> CropMode.NO_MODE
        0 -> CropMode.MANUAL
        1 -> CropMode.AUTO
        2 -> CropMode.MANUAL_AUTO
        3 -> CropMode.AUTO_MANUAL
        else -> throw RuntimeException("Unknown mode $this")
    }

fun CropMode.isManualFirst() = this == CropMode.MANUAL || this == CropMode.MANUAL_AUTO
fun CropMode.hasManual() = this == CropMode.MANUAL || this == CropMode.MANUAL_AUTO || this == CropMode.AUTO_MANUAL
fun CropMode.isAutoFirst() = this == CropMode.AUTO || this == CropMode.AUTO_MANUAL
fun CropMode.hasAuto() = this == CropMode.AUTO || this == CropMode.AUTO_MANUAL || this == CropMode.MANUAL_AUTO
