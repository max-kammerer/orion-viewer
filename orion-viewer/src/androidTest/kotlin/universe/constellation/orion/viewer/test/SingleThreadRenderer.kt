package universe.constellation.orion.viewer.test

import android.graphics.Bitmap
import android.view.ViewGroup
import universe.constellation.orion.viewer.*
import universe.constellation.orion.viewer.view.OrionStatusBarHelper

/**
 * User: mike
 * Date: 19.10.13
 * Time: 21:34
 */

class SingleThreadRenderer(actvity: OrionViewerActivity, view: OrionImageView, layout: LayoutStrategy, doc: DocumentWrapper, config: Bitmap.Config) :
                    RenderThread(actvity, view, layout, doc, config, false, OrionStatusBarHelper(actvity.findViewById(R.id.toolbar) as ViewGroup)) {


    override fun startRenreder() {

    }


    override fun render(lastInfo: LayoutPosition?) {
        super.render(lastInfo)
        renderInCurrentThread(true, lastInfo!!.clone(), layout!!.getRotation());
    }
}