package universe.constellation.orion.viewer.test.framework

import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.idling.CountingIdlingResource
import universe.constellation.orion.viewer.test.IdlingResource

class OperationIdlingResource : IdlingResource() {
    val res = CountingIdlingResource("Book loading")

    override fun busy() {
        res.increment()
    }

    override fun free() {
        res.decrement()
    }

}