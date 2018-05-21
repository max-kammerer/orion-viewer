package universe.constellation.orion.viewer.view

import universe.constellation.orion.viewer.layout.LayoutPosition

/**
 * User: mike
 * Date: 19.10.13
 * Time: 20:38
 */
interface Renderer {

    fun invalidateCache()

    fun stopRenderer()

    fun onPause()

    fun onResume()

    fun render(lastInfo: LayoutPosition)

    fun startRenderer()

    companion object {
        val EMPTY = object : Renderer {
            override fun invalidateCache() {
            }

            override fun stopRenderer() {
            }

            override fun onPause() {
            }

            override fun onResume() {
            }

            override fun render(lastInfo: LayoutPosition) {
            }

            override fun startRenderer() {

            }
        }
    }
}
