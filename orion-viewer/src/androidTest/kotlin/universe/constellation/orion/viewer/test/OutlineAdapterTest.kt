package universe.constellation.orion.viewer.test

import org.junit.Assert.assertEquals
import org.junit.Test
import universe.constellation.orion.viewer.document.OutlineItem
import universe.constellation.orion.viewer.outline.OutlineAdapter

/* Tree flattening/toggling logic of the outline RecyclerView adapter; no UI needed. */
class OutlineAdapterTest {

    /*  0 A            (page 0)
     *  1   B          (page 2)
     *  2     C        (page 3)
     *  3   D          (page 5)
     *  4 E            (page 8)
     *  5   F          (page 9)
     */
    private fun items() = arrayOf(
        OutlineItem(0, "A", 0),
        OutlineItem(1, "B", 2),
        OutlineItem(2, "C", 3),
        OutlineItem(1, "D", 5),
        OutlineItem(0, "E", 8),
        OutlineItem(1, "F", 9)
    )

    private fun OutlineAdapter.visibleTitles(items: Array<OutlineItem>): List<String> =
        (0 until itemCount).map { items[getItemId(it).toInt()].title }

    @Test
    fun pathToCurrentPageIsExpanded() {
        val items = items()
        /* current page 3 -> C is current, so A and B are expanded */
        val adapter = OutlineAdapter(items, 3) {}
        assertEquals(listOf("A", "B", "C", "D", "E"), adapter.visibleTitles(items))
        assertEquals(2, adapter.initialPosition)
    }

    @Test
    fun collapsedByDefaultWhenCurrentAtTopLevel() {
        val items = items()
        /* current page 0 -> A is current: A's children visible, B collapsed */
        val adapter = OutlineAdapter(items, 0) {}
        assertEquals(listOf("A", "B", "D", "E"), adapter.visibleTitles(items))
        assertEquals(0, adapter.initialPosition)
    }

    @Test
    fun toggleExpandsAndCollapsesSubtree() {
        val items = items()
        val adapter = OutlineAdapter(items, 0) {}
        adapter.toggle(1) /* expand B */
        assertEquals(listOf("A", "B", "C", "D", "E"), adapter.visibleTitles(items))
        adapter.toggle(0) /* collapse A: hides B, C, D */
        assertEquals(listOf("A", "E"), adapter.visibleTitles(items))
        adapter.toggle(0) /* expand A again: B keeps its expanded state */
        assertEquals(listOf("A", "B", "C", "D", "E"), adapter.visibleTitles(items))
    }

    @Test
    fun expandAndCollapseAll() {
        val items = items()
        val adapter = OutlineAdapter(items, -1) {}
        adapter.expandAll()
        assertEquals(listOf("A", "B", "C", "D", "E", "F"), adapter.visibleTitles(items))
        adapter.collapseAll()
        assertEquals(listOf("A", "E"), adapter.visibleTitles(items))
    }

    @Test
    fun leafToggleIsNoOp() {
        val items = items()
        val adapter = OutlineAdapter(items, -1) {}
        adapter.expandAll()
        val before = adapter.visibleTitles(items)
        adapter.toggle(2) /* C is a leaf */
        assertEquals(before, adapter.visibleTitles(items))
    }
}
