package universe.constellation.orion.viewer.test

import universe.constellation.orion.viewer.RenderThread
import universe.constellation.orion.viewer.OrionViewerActivity
import universe.constellation.orion.viewer.OrionView
import universe.constellation.orion.viewer.LayoutStrategy
import universe.constellation.orion.viewer.DocumentWrapper
import universe.constellation.orion.viewer.LayoutPosition
import android.graphics.Bitmap
import universe.constellation.orion.viewer.ImageView

/**
 * User: mike
 * Date: 19.10.13
 * Time: 21:34
 */

class SingleThreadRenderer(actvity: OrionViewerActivity, view: ImageView, layout: LayoutStrategy, doc: DocumentWrapper, config: Bitmap.Config) :
                    RenderThread(actvity, view, layout, doc, config, false) {


    override fun startRenreder() {

    }


    override fun render(lastInfo: LayoutPosition?) {
        super<RenderThread>.render(lastInfo)
        renderInCurrentThread(true, lastInfo!!.clone(), layout!!.getRotation());
    }
}