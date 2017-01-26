package universe.constellation.orion.viewer.scene

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import org.jetbrains.anko.displayMetrics
import universe.constellation.orion.viewer.Common
import universe.constellation.orion.viewer.LayoutPosition
import universe.constellation.orion.viewer.OrionScene
import universe.constellation.orion.viewer.view.ColorStuff
import universe.constellation.orion.viewer.view.DrawTask
import universe.constellation.orion.viewer.view.ViewDimensionAware
import java.util.*
import java.util.concurrent.CountDownLatch


class Screen(val canvasProvider: CanvasProvider): OnDraw, OrionScene, ViewDimensionAware {

    val pages: ArrayList<LazyPage> = arrayListOf<LazyPage>()

    val paints = Paints(Paint())

    val displaySize: Dimension

    private var inTranslation: Boolean = false
    private var translation: Position = Position(0, 0)

    init {
        val displayMetrics = toView().context.applicationContext.displayMetrics
        displaySize = Dimension(displayMetrics.widthPixels, displayMetrics.heightPixels)
        canvasProvider.addDimensionAwareListener(this)
    }

    /*ui thread*/
    private val screenRectImp = Rect()
    val screenRect : Rect
        get() = screenRectImp.apply { top =0; left =0; right = toView().width; bottom = toView().height }

    init {
        canvasProvider.register(this)
    }

    fun onMove(p: Position) {
        pages.map { it.translate(p) }
    }

    fun redraw() {
        canvasProvider.invalidate()
    }

    /*UI thread*/
    fun getOrCreatePage(page: LazyPage) {
        val existing = pages.binarySearch { it.number0.compareTo(page.number0) }
        if (existing >= 0) {
            d("add existing page " + page)
            pages[existing] = page
        }
        else {
            d("add page " + page)
            pages.add(page)
        }
    }

    override fun draw(canvas: Canvas) {
        d("screen.draw")
        if (inTranslation) {
            canvas.save()
            canvas.translate(translation)
        }

        pages.map { it.draw(canvas, paints) }

        if (inTranslation) {
            canvas.restore()
        }
    }

    override fun init(colorStuff: ColorStuff) {

    }

    override fun onNewImage(bitmap: Bitmap?, info: LayoutPosition?, latch: CountDownLatch?) {
        pages.firstOrNull {
            it.number0 == info?.pageNumber
        }
    }

    /*SCALING*/
    override fun doScale(scale: Float, startFocus: Point, endFocus: Point, enableMoveOnPinchZoom: Boolean) {
        d("screen.doScale")
        with(translation) {
            x = -(endFocus.x - startFocus.x)
            y = -(endFocus.y - startFocus.y)
        }
    }

    override fun beforeScaling() {
        inTranslation = true
    }

    override fun afterScaling() {
        inTranslation = false
    }


    override fun postInvalidate() {
        canvasProvider.postInvalidate()
    }

    override fun invalidate() {
        d("screen.invalidate")
        canvasProvider.invalidate()
    }

    override fun onDimensionChanged(newWidth: Int, newHeight: Int) {
        d("Screen.onDimensionChanged ${pages.size}")
        pages.forEach { it.redraw() }
        invalidate()
    }

    override val width: Int
        get() = toView().width
    override val height: Int
        get() = toView().height
    override val info: LayoutPosition?
        get() = throw UnsupportedOperationException()

    override fun setDimensionAware(dimensionAware: ViewDimensionAware) {
        canvasProvider.addDimensionAwareListener(dimensionAware)
    }

    override fun setOnTouchListener(listener: View.OnTouchListener) = canvasProvider.setOnTouchListener(listener)

    override fun toView(): View = canvasProvider.toView()

    override fun addTask(drawTask: DrawTask) = Unit

    override fun removeTask(drawTask: DrawTask) = Unit

    override fun isDefaultColorMatrix(): Boolean = true

    /*object BitmapCache {*/

    val bitmaps = arrayListOf<Bitmap>()

    private var index: Int = 0

    fun clearBitmaps() {
        bitmaps.forEach(Bitmap::recycle)
    }

    fun createOrGet(): Bitmap {
        if (bitmaps.size < 4) {
            val bitmap = Bitmap.createBitmap(displaySize.x, displaySize.y, Bitmap.Config.ARGB_8888)
            bitmaps.add(bitmap)
        }

        return bitmaps[index++ / 4]
    }
    /*}*/


    fun clear() {
        d("clear screen")
        pages.forEach {  }
        pages.clear()
        clearBitmaps()
    }
}

class ViewCanvasProvider: View, CanvasProvider  {

    val listeners = arrayListOf<OnDraw>()

    var dimensionAwareListeners = arrayListOf<ViewDimensionAware>()

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

    override fun addDimensionAwareListener(dimensionAware: ViewDimensionAware) {
        dimensionAwareListeners.add(dimensionAware)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        Common.d("ViewCanvasProvider: onSizeChanged " + w + "x" + h)
        super.onSizeChanged(w, h, oldw, oldh)
        if (w != oldw || h != oldh) {
            dimensionAwareListeners.forEach { it.onDimensionChanged(width, height) }
        }
    }
}
