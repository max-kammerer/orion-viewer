package universe.constellation.orion.viewer.scene

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import org.jetbrains.anko.uiThread
import universe.constellation.orion.viewer.Common
import universe.constellation.orion.viewer.Common.d
import universe.constellation.orion.viewer.PageInfo
import universe.constellation.orion.viewer.view.ViewDimensionAware

/**
 * Created by mike on 12/29/16.
 */

class Paints(val default: Paint)

interface Layouter {
    fun layoutPage()
}

class BitmapInfo(val screenArea: Rect, val bitmap: Bitmap)

class LazyPage(val number0/*zero based*/: Int, val positionOnScreen/*absolute*/: Position, val zoomedDimension: Dimension, val pageProvider: PageProvider, val screen: Screen): PageInfoConsumer {

    private val pageRectImpl = Rect(0, 0, 100, 100)
    private val drawRect = Rect(0, 0, 100, 100)
    private var bitmapInfo: BitmapInfo? = null

    private val pageRectangle: Rect
        get() {
            checkUIThread()
            pageRectImpl.left = positionOnScreen.x
            pageRectImpl.top = positionOnScreen.y
            pageRectImpl.right = pageRectImpl.left + zoomedDimension.x
            pageRectImpl.bottom = pageRectImpl.top + zoomedDimension.y
            return pageRectImpl
        }

    init {
        pageProvider.getPageInfo(number0, this)
    }

    /*ui thread*/
    fun draw(canvas: Canvas, paints: Paints) {
        val visibleRect = getVisibleRect(/*pageRectangle*/)
        val isVisible = isVisible()
        d("drawPage $number0 $visibleRect $isVisible")
        if (isVisible) {
            with(canvas) {
                drawRect(pageRectangle, paints.default)

                save()
                translate(positionOnScreen)
                paints.default.color = Color.BLACK

                if (bitmapInfo != null ) {
                    val bitmap = bitmapInfo!!
                    d("drawRect processing bitmap")
                    if (!bitmap.bitmap.isRecycled) {
                        d("drawRect $number0 ")
                        //save()
                        //translate(-bitmap.screenArea.left.toFloat(), -bitmap.screenArea.top.toFloat())
                        with(drawRect) {
                            left = 0
                            top = 0
                            right = bitmap.screenArea.width()
                            bottom = bitmap.screenArea.height()
                        }
                        canvas.drawBitmap(bitmap.bitmap, drawRect, bitmap.screenArea, paints.default)
                    }
                }
                restore()
            }
        }
        else {
            bitmapInfo = null
        }
    }

    override fun onNewEvent(p: PageInfo) {
        d("on new PageInfo: " + p)
        /*TODO: update zoomed dimension*/
        redraw()
    }

    fun translate(p: Position) {
        d("translate page $positionOnScreen on $p")
        positionOnScreen.translate(p)
        d("new position $positionOnScreen")
        redraw()
    }

    fun redraw() {
        orionAsync {
            val visibleRect = uiThreadAndWait { getVisibleRect() }
            if (visibleRect != null) {
                val bitmap: Bitmap = screen.createOrGet()
                d("rendering $number0 $visibleRect")
                pageProvider.render(number0, bitmap, 1.0, visibleRect)
                uiThread {
                    bitmapInfo = BitmapInfo(visibleRect, bitmap)
                    screen.invalidate()
                    /*TODO: invalidate area*/
                }

            }
            else {
                bitmapInfo = null
            }
        }
    }

    fun isVisible(): Boolean {
        checkUIThread()
        return Rect.intersects(pageRectangle, screen.screenRect)
    }

    fun getVisibleRect(): Rect? {
        checkUIThread()
        return getVisibleRect(Rect(pageRectangle))
    }

    fun getVisibleRect(rect: Rect): Rect? {
        checkUIThread()
        val left = -rect.left
        val top = -rect.top

        return if (rect.intersect(screen.screenRect)) {
            rect.offset(left, top)
            rect
        }
        else null
    }


    fun relayoutPage() {

    }
}

interface PageProvider {
    fun getPageInfo(page: Int, pageInfoConsumer: PageInfoConsumer)

    fun render(page: Int, bitmap: Bitmap, zoom: Double, rect: Rect)
}

interface CanvasProvider {
    fun invalidate()

    fun postInvalidate()

    fun register(onDraw: OnDraw)

    fun setOnTouchListener(listener: View.OnTouchListener)

    fun toView(): View

    fun setDimensionAware(dimensionAware: ViewDimensionAware)
}

class ViewCanvasProvider: View, CanvasProvider {

    val listeners = arrayListOf<OnDraw>()

    var dimensionAwareListener: ViewDimensionAware? = null

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    override fun onDraw(canvas: Canvas) {
        listeners.forEach { it.draw(canvas) }
    }

    override fun register(onDraw: OnDraw) {
        listeners.add(onDraw)
    }


    override fun toView(): View  = this

    override fun setDimensionAware(dimensionAware: ViewDimensionAware) {
        this.dimensionAwareListener = dimensionAware
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        Common.d("OrionView: onSizeChanged " + w + "x" + h)
        super.onSizeChanged(w, h, oldw, oldh)
        if (w != oldw || h != oldh) {
            dimensionAwareListener?.onDimensionChanged(width, height)
        }
    }
}

fun Canvas.translate(p: Position) {
    translate(-p.x.toFloat(), -p.y.toFloat())
}


interface OnDraw {
    fun draw(canvas: Canvas)
}