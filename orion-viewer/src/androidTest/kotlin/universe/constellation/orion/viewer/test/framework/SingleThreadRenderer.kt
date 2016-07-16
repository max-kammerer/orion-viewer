package universe.constellation.orion.viewer.test.framework

import universe.constellation.orion.viewer.*
import universe.constellation.orion.viewer.view.Scene

/**
 * User: mike
 * Date: 19.10.13
 * Time: 21:34
 */

class SingleThreadRenderer(
        activity: OrionViewerActivity,
        scene: Scene,
        layout: LayoutStrategy,
        doc: DocumentWrapper
) : RenderThread(activity, layout, doc, false, scene) {

    override fun startRenreder() {

    }

    override fun render(lastInfo: LayoutPosition?) {
        super.render(lastInfo)
        renderInCurrentThread(true, lastInfo!!.clone(), layout!!.rotation);
    }
}