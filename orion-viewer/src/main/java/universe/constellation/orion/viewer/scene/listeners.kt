package universe.constellation.orion.viewer.scene

import universe.constellation.orion.viewer.PageInfo

/**
 * Created by mike on 1/2/17.
 */

interface PageInfoConsumer : Consumer<PageInfo>

interface Consumer<in T> {
    fun onNewEvent(p: T)
}

interface Observable<T> {

    val listeners: MutableList<Consumer<T>>

    fun register(listener: Consumer<T>) {
        listeners.add(listener)
    }

    fun unregister(listener: Consumer<T>) {
        listeners.remove(listener)
    }

    fun onNewEvent(event: T) {
        listeners.map { it.onNewEvent(event) }
    }

}