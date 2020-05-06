package com.artifex.mupdfdemo

import android.graphics.RectF
import com.artifex.mupdf.fitz.Quad
import com.artifex.mupdf.fitz.StructuredText.TextChar
import kotlin.math.max
import kotlin.math.min

class TextWord {
    private val chars = arrayListOf<Int>()
    val rect = RectF()

    fun add(tc: TextChar) {
        this.rect.union(tc.quad.toRectF())
        chars.add(tc.c)
    }

    private fun Quad.toRectF(): RectF {
        val x0: Float = min(Math.min(ul_x, ur_x), Math.min(ll_x, lr_x))
        val y0: Float = min(Math.min(ul_y, ur_y), Math.min(ll_y, lr_y))
        val x1: Float = max(Math.max(ul_x, ur_x), Math.max(ll_x, lr_x))
        val y1: Float = max(Math.max(ul_y, ur_y), Math.max(ll_y, lr_y))
        return RectF(x0, y0, x1, y1)
    }

    fun isNotEmpty(): Boolean {
        return chars.isNotEmpty()
    }

    fun width(): Float {
        return rect.width()
    }

    fun height(): Float {
        return rect.height()
    }

    override fun toString(): String {
        return String(IntArray(chars.size) { chars[it] }, 0, chars.size)
    }
}