package universe.constellation.orion.viewer.test.framework

import android.graphics.Bitmap
import universe.constellation.orion.viewer.*

/**
 * User: mike
 * Date: 19.10.13
 * Time: 21:34
 */

class SingleThreadRenderer(
        actvity: OrionViewerActivity,
        view: OrionImageView,
        layout: LayoutStrategy,
        doc: DocumentWrapper,
        config: Bitmap.Config
) : RenderThread(actvity, layout, doc, false, actvity.fullScene) {

    override fun startRenreder() {

    }

    override fun render(lastInfo: LayoutPosition?) {
        super.render(lastInfo)
        renderInCurrentThread(true, lastInfo!!.clone(), layout!!.rotation);
    }
}