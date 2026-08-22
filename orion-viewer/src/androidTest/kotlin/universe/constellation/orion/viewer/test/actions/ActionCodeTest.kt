package universe.constellation.orion.viewer.test.actions

import org.junit.Assert.assertEquals
import org.junit.Test
import universe.constellation.orion.viewer.Action
import universe.constellation.orion.viewer.test.framework.BaseTest

class ActionCodeTest : BaseTest() {

    /**
     * Actions are stored by code, so two actions sharing one code make the later one shadow
     * the earlier: the shadowed action becomes unreachable from tap zones and key bindings.
     */
    @Test
    fun actionCodesAreUnique() {
        val byCode = Action.entries.groupBy { it.code }.filterValues { it.size > 1 }
        assertEquals("Actions with shared codes", emptyMap<Int, List<Action>>(), byCode)
    }

    @Test
    fun everyActionIsResolvableByItsCode() {
        Action.entries.forEach {
            assertEquals("Wrong action for code ${it.code}", it, Action.getAction(it.code))
        }
    }
}
