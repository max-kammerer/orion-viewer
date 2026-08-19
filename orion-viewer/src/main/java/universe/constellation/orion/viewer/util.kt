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

import android.graphics.RectF
import universe.constellation.orion.viewer.layout.LayoutPosition


fun getPrefKey(keyCode: Int, isLong: Boolean): String {
    return keyCode.toString() + if (isLong) "long" else ""
}

fun LayoutPosition.toAbsoluteRect(): RectF {
    val left = x.offset + x.marginLeft
    val top = y.offset + y.marginLeft
    return RectF(left.toFloat(), top.toFloat(), (left + x.screenDimension).toFloat(), (top + y.screenDimension).toFloat())///
}

private const val NATIVE_MEM_LOG_INTERVAL_MS = 1000L

private var lastNativeMemLogTime = 0L

/* Diagnostics for native heap growth: alloc ~= size means real leak,
 * small alloc with huge size means allocator retention/fragmentation */
fun logNativeMemory(tag: String, force: Boolean = false) {
    val now = android.os.SystemClock.uptimeMillis()
    if (!force && now - lastNativeMemLogTime < NATIVE_MEM_LOG_INTERVAL_MS) return
    lastNativeMemLogTime = now
    val size = android.os.Debug.getNativeHeapSize() shr 20
    val alloc = android.os.Debug.getNativeHeapAllocatedSize() shr 20
    val free = android.os.Debug.getNativeHeapFreeSize() shr 20
    log("NATIVE_MEM [$tag]: size=${size}M alloc=${alloc}M free=${free}M")
}

private const val NATIVE_MEM_PROBE_INTERVAL_MS = 5000L

private var lastNativeMemProbeTime = 0L

/* Splits the native heap into three parts:
 * 1 -> 2 is memory held by unreachable but not finalized mupdf wrappers,
 * 2 -> 3 is the mupdf resource store,
 * whatever is left after 3 is leaked by still reachable objects */
fun logNativeMemoryBreakdown(tag: String, force: Boolean = false) {
    val now = android.os.SystemClock.uptimeMillis()
    if (!force && now - lastNativeMemProbeTime < NATIVE_MEM_PROBE_INTERVAL_MS) return
    lastNativeMemProbeTime = now

    logNativeMemory("$tag/1-current", force = true)

    System.gc()
    System.runFinalization()
    System.gc()
    logNativeMemory("$tag/2-afterGc", force = true)

    try {
        com.artifex.mupdf.fitz.Context.emptyStore()
        logNativeMemory("$tag/3-afterEmptyStore", force = true)
    } catch (e: Throwable) {
        log("NATIVE_MEM [$tag]: no mupdf context: ${e.message}")
    }
}

fun logNativeMemoryAfterGc(tag: String) = logNativeMemoryBreakdown(tag, force = true)

/* fz_store_item stores an item even when it fails to evict enough space for it,
 * and ensure_space is all or nothing: once the store overshoots its limit by more
 * than the currently evictable amount, the automatic eviction gives up forever and
 * the store ratchets up without bound. fz_shrink_store evicts greedily instead,
 * so it is the only way back under the limit */
fun shrinkMupdfStore(percent: Int, tag: String) {
    try {
        if (com.artifex.mupdf.fitz.Context.shrinkStore(percent)) {
            log("MUPDF_STORE [$tag]: shrunk to $percent%")
        } else {
            /* scavenge could not reach the target: the store is over its limit and
             * automatic eviction will never bring it back, so drop everything */
            com.artifex.mupdf.fitz.Context.emptyStore()
            log("MUPDF_STORE [$tag]: shrink to $percent% failed, store emptied")
        }
    } catch (e: Throwable) {
        log("MUPDF_STORE [$tag]: shrink failed: ${e.message}")
    }
}

fun emptyMupdfStore(tag: String) {
    try {
        com.artifex.mupdf.fitz.Context.emptyStore()
        log("MUPDF_STORE [$tag]: emptied")
    } catch (e: Throwable) {
        log("MUPDF_STORE [$tag]: empty failed: ${e.message}")
    }
}
