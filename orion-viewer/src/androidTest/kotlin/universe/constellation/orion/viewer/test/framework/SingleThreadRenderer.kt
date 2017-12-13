package universe.constellation.orion.viewer.test.framework

import universe.constellation.orion.viewer.*
import universe.constellation.orion.viewer.document.Document
import universe.constellation.orion.viewer.layout.LayoutPosition
import universe.constellation.orion.viewer.layout.LayoutStrategy
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
        doc: Document
) : RenderThread(activity, layout, doc, false, scene) {

    override fun startRenderer() {

    }

    override fun render(lastInfo: LayoutPosition) {
        super.render(lastInfo)
        renderInCurrentThread(true, lastInfo.deepCopy(), layout.rotation)
    }
}