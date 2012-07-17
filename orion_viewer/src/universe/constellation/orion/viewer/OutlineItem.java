package universe.constellation.orion.viewer;

/**
 * User: mike
 * Date: 14.07.12
 * Time: 21:15
 */
public class OutlineItem {
    public final int    level;
   	public final String title;
   	public final int    page;

   	public OutlineItem(int _level, String _title, int _page) {
   		level = _level;
   		title = _title;
   		page  = _page;
   	}

}
