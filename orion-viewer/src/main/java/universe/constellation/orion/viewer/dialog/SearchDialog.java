package universe.constellation.orion.viewer.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.artifex.mupdfdemo.SearchTaskResult;

import java.util.ArrayList;
import java.util.List;

import universe.constellation.orion.viewer.Common;
import universe.constellation.orion.viewer.Controller;
import universe.constellation.orion.viewer.LayoutPosition;
import universe.constellation.orion.viewer.LayoutStrategy;
import universe.constellation.orion.viewer.OrionBaseActivity;
import universe.constellation.orion.viewer.OrionScene;
import universe.constellation.orion.viewer.OrionViewerActivity;
import universe.constellation.orion.viewer.PageWalker;
import universe.constellation.orion.viewer.R;
import universe.constellation.orion.viewer.search.SearchTask;
import universe.constellation.orion.viewer.util.Util;
import universe.constellation.orion.viewer.view.ColorStuff;
import universe.constellation.orion.viewer.view.DrawContext;
import universe.constellation.orion.viewer.view.DrawTask;

/**
 * User: mike
 * Date: 23.11.13
 * Time: 11:39
 */
public class SearchDialog extends DialogFragment {

    private SearchTask myTask;

    private List<SubBatch> screens;

    private String lastSearch = null;

    private int lastPosition;

    private volatile int lastDirectionOnSearch = 0;

    private EditText searchField;

    private SearchDrawler lastSearchDrawler = new SearchDrawler();

    private static final int ALPHA = 150;

    public static SearchDialog newInstance() {
        SearchDialog fragment = new SearchDialog();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        fragment.setStyle(STYLE_NO_FRAME, R.style.Theme_AppCompat_Light_Dialog);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Dialog dialog = getDialog();
        dialog.setCanceledOnTouchOutside(true);


        Window window = dialog.getWindow();
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager.LayoutParams wlp = window.getAttributes();
        wlp.gravity = Gravity.TOP;
        android.support.v7.widget.Toolbar toolbar = ((OrionBaseActivity) getActivity()).getToolbar();
        wlp.y = toolbar.getHeight() + 5;

        window.setAttributes(wlp);
        window.setBackgroundDrawable(new ColorDrawable(ALPHA));

        dialog.setContentView(R.layout.search_dialog);
        View resultView = super.onCreateView(inflater, container, savedInstanceState);

        final Controller controller = ((OrionViewerActivity) getActivity()).getController();

        searchField = (EditText) dialog.findViewById(R.id.searchText);
        searchField.getBackground().setAlpha(ALPHA);

        android.widget.ImageButton button = (android.widget.ImageButton) dialog.findViewById(R.id.searchNext);
        button.getBackground().setAlpha(ALPHA);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //getDialog().hide();
                doSearch(controller.getCurrentPage(), +1, controller);
            }
        });

        button = (android.widget.ImageButton) dialog.findViewById(R.id.searchPrev);
        button.getBackground().setAlpha(ALPHA);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //getDialog().hide();
                doSearch(controller.getCurrentPage(), -1, controller);
            }
        });

        OrionScene view = controller.getActivity().getView();
        view.addTask(lastSearchDrawler);

        myTask = new SearchTask(getActivity(), controller.getDocument()) {
            @Override
            protected void onResult(boolean isSuccessful, SearchTaskResult result) {
                boolean forward = lastDirectionOnSearch == +1;
                LayoutPosition position = new LayoutPosition();
                LayoutStrategy layoutStrategy = controller.getLayoutStrategy();
                PageWalker walker = layoutStrategy.getWalker();
                layoutStrategy.reset(position, result.pageNumber, forward);

                List<SubBatch> subBatches = prepareResults(result, forward, position, walker, layoutStrategy);
                lastPosition = forward ? 0 : subBatches.size() -1;
                screens = subBatches;

                SubBatch toShow = subBatches.get(lastPosition);
                toShow.active += lastDirectionOnSearch;
                drawBatch(toShow, controller);
            }

            private List<SubBatch> prepareResults(SearchTaskResult result, boolean forward, LayoutPosition position, PageWalker walker, LayoutStrategy layoutStrategy) {
                List<SubBatch> screens = new ArrayList<SubBatch>();
                RectF[] searchBoxes = result.searchBoxes;

                for (RectF searchBox : searchBoxes) {
                    System.out.println("Scaling rect " + searchBox);
                    Util.scale(searchBox, position.docZoom);
                }

                RectF temp = new RectF();
                do {
                    SubBatch sb = new SubBatch();
                    sb.lp = position.clone();
                    RectF screenArea = position.toAbsoluteRect();
                    System.out.println("Area " + screenArea);
                    for (RectF searchBox : searchBoxes) {
                        float square1 = searchBox.width() * searchBox.height();
                        if (temp.setIntersect(searchBox, screenArea)) {
                            System.out.println("Rect " + searchBox);
                            float square2 = temp.width() * temp.height();
                            if (square2 >= square1 / 9) { /*33%*/
                                sb.rects.add(searchBox);
                            }
                        }
                    }
                    sb.active = forward ? -1 : sb.rects.size();
                    if (!sb.rects.isEmpty()) {
                        screens.add(sb);
                    }
                } while (forward ? !walker.next(position, layoutStrategy.getLayout()) : !walker.prev(position, layoutStrategy.getLayout()));

                if (screens.isEmpty()) {
                    Common.d("Adding fake batch");
                    SubBatch sb = new SubBatch();
                    sb.lp = position.clone();
                    screens.add(sb);
                }
                return screens;
            }
        };
        return resultView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        searchField.requestFocus();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (myTask != null) {
            Common.d("Stopping search task");
            myTask.stop();
        }

        OrionScene view = ((OrionViewerActivity)getActivity()).getView();
        view.removeTask(lastSearchDrawler);
    }

    private void doSearch(int page, int direction, Controller controller) {
        InputMethodManager inputManager = (InputMethodManager)
        getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(getDialog().getCurrentFocus().getWindowToken(),
        InputMethodManager.HIDE_NOT_ALWAYS);

        boolean performRealSearch = false;
        String newSearch = searchField.getText().toString();
        lastDirectionOnSearch = direction;
        if ("".equals(newSearch)) {
            ((OrionBaseActivity)getActivity()).showAlert(R.string.msg_error, R.string.msg_specify_keyword_for_search);
            return;
        }

        if (newSearch.equals(lastSearch) && screens != null) {
            //iterate
            if (!Util.inRange(screens, lastPosition)) {
                performRealSearch = true;
            }
            if (!performRealSearch) {
                SubBatch subBatch = screens.get(lastPosition);
                subBatch.active += lastDirectionOnSearch;

                if (!Util.inRange(subBatch.rects, subBatch.active)) {
                    lastPosition += lastDirectionOnSearch;
                    if (!Util.inRange(screens, lastPosition)) {
                        performRealSearch = true;
                        page += lastDirectionOnSearch;
                    } else {
                        subBatch = screens.get(lastPosition);
                        subBatch.active += lastDirectionOnSearch;
                    }
                }
            }
        } else {
            performRealSearch = true;
            lastSearch = null;
            screens = null;
        }

        if (performRealSearch) {
            Common.d("Real search for " + page);
            myTask.go(newSearch, direction, page, -1);
        } else {
            SubBatch subBatch = screens.get(lastPosition);
            drawBatch(subBatch, controller);
        }

        lastSearch = newSearch;
    }

    private void drawBatch(SubBatch subBatch, Controller controller) {
        System.out.println("lastDirectionOnSearch = " + lastDirectionOnSearch);
        System.out.println("lastPosition = " + lastPosition);
        System.out.println("lastIndex = " + subBatch.active);
        System.out.println("page = " + subBatch.lp.pageNumber);
        Common.d("Rect " + subBatch.lp.toAbsoluteRect());

        lastSearchDrawler.setBatch(subBatch);
        controller.drawPage(subBatch.lp);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        lastSearchDrawler.setBatch(null);
    }

    private class SubBatch {
        List<RectF> rects = new ArrayList<RectF>();

        int active = -1;

        LayoutPosition lp;
    }

    static class SearchDrawler implements DrawTask {

        private SubBatch batch;

        private static int activeAlpha = 128;
        private static int generalAlpha = 64;

        public SearchDrawler() {

        }

        public void setBatch(SubBatch batch) {
            this.batch = batch;
        }

        @Override
        public void drawOnCanvas(Canvas canvas, ColorStuff stuff, DrawContext drawContext) {
            if (batch != null) {
                Paint paint = stuff.getBorderPaint();
                List<RectF> rects = batch.rects;
                int index = 0;
                int prevAlpgha = paint.getAlpha();
                Paint.Style style = paint.getStyle();
                paint.setStyle(Paint.Style.FILL);
                for (RectF rect : rects) {
                    paint.setAlpha(index++ == batch.active ? activeAlpha : generalAlpha);
                    int left = batch.lp.x.marginLess + batch.lp.x.offset;
                    int top = batch.lp.y.marginLess + batch.lp.y.offset;
                    canvas.drawRect(rect.left - left, rect.top - top, rect.right - left, rect.bottom - top, paint);
                }
                paint.setAlpha(prevAlpgha);
                paint.setStyle(style);
            }
        }
    }

}
