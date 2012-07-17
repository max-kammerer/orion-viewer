package universe.constellation.orion.viewer.outline;

import android.app.Activity;
import android.app.Dialog;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import pl.polidea.treeview.AbstractTreeViewAdapter;
import pl.polidea.treeview.InMemoryTreeStateManager;
import pl.polidea.treeview.TreeBuilder;
import pl.polidea.treeview.TreeNodeInfo;
import universe.constellation.orion.viewer.Common;
import universe.constellation.orion.viewer.Controller;
import universe.constellation.orion.viewer.OutlineItem;
import universe.constellation.orion.viewer.R;

/**
 * User: mike
 * Date: 14.07.12
 * Time: 21:18
 */
public class OutlineAdapter extends AbstractTreeViewAdapter<Integer> {
    private OutlineItem[] items = null;
    private int maxLevel = 0;
    private Controller controller = null;
    private Dialog dialog = null;

    public OutlineAdapter(final Controller controller,
                          final Activity activity,
                          final Dialog dialog,
                          InMemoryTreeStateManager<Integer> manager,
                          OutlineItem[] items) {
        super(activity, manager, 20);
        this.items = items;
        this.dialog = dialog;
        this.controller = controller;
    }

    static public void SetManagerFromOutlineItems(InMemoryTreeStateManager<Integer> manager, OutlineItem[] items) {
        OutlineItem item_cur, item_last;
        TreeBuilder<Integer> builder = new TreeBuilder<Integer>(manager);
		builder.sequentiallyAddNextNode(0, items[0].level);

		Common.d("OutlineAdapter:: SetManagerFromOutlineItems");
        for (int i=1; i<items.length; i++) {
            item_last = items[i-1];
            item_cur = items[i];
            int last = i - 1;
            int cur = i;
            if (item_cur.level > item_last.level) {
                builder.addRelation(last, cur);
            } else {
                builder.sequentiallyAddNextNode(cur, item_cur.level);
            }
        }
		Common.d("OutlineAdapter:: SetManagerFromOutlineItems -- END");
    }


    @Override
    public View getNewChildView(final TreeNodeInfo<Integer> treeNodeInfo) {
        final LinearLayout viewLayout = (LinearLayout) getActivity()
                .getLayoutInflater().inflate(pl.polidea.treeview.R.layout.demo_list_item, null);
		Common.d("OutlineAdapter:: GetChildView");
        return updateView(viewLayout, treeNodeInfo);
    }

    private String getDescription(final int id) {
		Common.d("OutlineAdapter:: GetDescription");
        return this.items[id].title;
    }

    @Override
    public LinearLayout updateView(final View view,
                                   final TreeNodeInfo<Integer> treeNodeInfo) {
		Common.d("OutlineAdapter:: updateView");
        final LinearLayout viewLayout = (LinearLayout) view;
        final TextView descriptionView = (TextView) viewLayout
                .findViewById(pl.polidea.treeview.R.id.list_item_description);
        descriptionView.setText(getDescription(treeNodeInfo.getId()));
        return viewLayout;
    }

    @Override
    public long getItemId(final int position) {
		Common.d("OutlineAdapter:: getItemID");
        return getTreeId(position);
    }

    @Override
    public Object getItem(final int position) {
		Common.d("OutlineAdapter:: getItem");
        int id = (int)getItemId(position);
        return this.items[id];
    }

    @Override
    public void handleItemClick(final View view, final Object id) {
		Common.d("OutlineAdapter:: handleItemClick");
        final Integer longId = (Integer) id;
        final TreeNodeInfo<Integer> info = getManager().getNodeInfo(longId);
        if (info.isWithChildren()) {
            super.handleItemClick(view, id);
        } else {
            this.controller.drawPage(this.items[longId].page);
            this.dialog.dismiss();
        }
		Common.d("OutlineAdapter:: handleItemClickEnd");
    }
}


