package universe.constellation.orion.viewer.view

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import universe.constellation.orion.viewer.Controller
import universe.constellation.orion.viewer.OrionViewerActivity
import universe.constellation.orion.viewer.State
import universe.constellation.orion.viewer.log


class PageViewHolder(val view: PageScene) : RecyclerView.ViewHolder(view) {


}


class PageAdapter(val recyclerView: RecyclerView, val context: OrionViewerActivity, val controller: Controller, val colorStuff: ColorStuff, val statusBarHelper: OrionStatusBarHelper) : RecyclerView.Adapter<PageViewHolder>() {

    init {
        setHasStableIds(false)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }



    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PageViewHolder {
        log("Create view holder: ${controller.layoutStrategy.viewWidth}  ${controller.layoutStrategy.viewHeight}")
        val view = PageScene(context)
        view.layoutParams = ViewGroup.LayoutParams(parent.width, parent.height)
        return PageViewHolder(view)
    }



    override fun onBindViewHolder(
        holder: PageViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        println("payloads for pos $position, ${payloads.size}: ${payloads.joinToString()}")
        super.onBindViewHolder(holder, position, payloads)
    }

    override fun onBindViewHolder(
        holder: PageViewHolder,
        position: Int
    ) {
        log("Create bind view for page $position")
        val pageScene = holder.view
        val pageView = controller.createCachePageView(position)
        pageView.pageAdapter = this
        pageView.recyclerView = recyclerView
        pageView.scene = pageScene
        if (pageView.state == State.SIZE_AND_BITMAP_CREATED) {
            holder.view.layoutParams = ViewGroup.LayoutParams(pageView.layoutInfo.x.pageDimension, pageView.layoutInfo.y.pageDimension)
            println("rebind: ${pageView.layoutInfo.x.pageDimension}  ${pageView.layoutInfo.y.pageDimension}" )
            holder.view.requestLayout()
        }
        pageScene.pageView = pageView
        pageScene.init(colorStuff, statusBarHelper)
    }

    override fun onViewRecycled(holder: PageViewHolder) {
//        (holder.itemView as? PageScene)?.pageView?.let {
//            controller.markForDelete(it)
//        }
//        (holder.itemView as? PageScene)?.pageView = null
    }



    override fun getItemCount(): Int {
        return controller.document.pageCount
    }
}