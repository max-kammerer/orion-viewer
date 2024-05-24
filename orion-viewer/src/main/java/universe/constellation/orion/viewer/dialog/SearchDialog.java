package universe.constellation.orion.viewer.dialog;

import static universe.constellation.orion.viewer.LoggerKt.log;
import static universe.constellation.orion.viewer.UtilKt.toAbsoluteRect;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.artifex.mupdfdemo.SearchTaskResult;

import java.util.ArrayList;
import java.util.List;

import universe.constellation.orion.viewer.Controller;
import universe.constellation.orion.viewer.OrionViewerActivity;
import universe.constellation.orion.viewer.PageWalker;
import universe.constellation.orion.viewer.R;
import universe.constellation.orion.viewer.document.Page;
import universe.constellation.orion.viewer.layout.LayoutPosition;
import universe.constellation.orion.viewer.layout.LayoutStrategy;
import universe.constellation.orion.viewer.layout.SimpleLayoutStrategy;
import universe.constellation.orion.viewer.search.SearchTask;
import universe.constellation.orion.viewer.util.Util;
import universe.constellation.orion.viewer.view.ColorStuff;
import universe.constellation.orion.viewer.view.DrawContext;
import universe.constellation.orion.viewer.view.DrawTask;
import universe.constellation.orion.viewer.view.OrionDrawScene;

public class SearchDialog extends DialogFragment {

    private SearchTask myTask;

    private List<SubBatch> screens;

    private String lastSearch = null;

    private int lastPosition;

    private volatile int lastDirectionOnSearch = 0;

    private volatile Page lastPage;

    private EditText searchField;

    private final SearchResultRenderer lastSearchResultRenderer = new SearchResultRenderer();

    private static final int ALPHA = 150;

    public static SearchDialog newInstance() {
        SearchDialog fragment = new SearchDialog();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        fragment.setStyle(STYLE_NO_FRAME, R.style.Theme_AppCompat_Light_Dialog);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Dialog dialog = requireDialog();
        dialog.setCanceledOnTouchOutside(true);


        Window window = dialog.getWindow();
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager.LayoutParams wlp = window.getAttributes();
        wlp.gravity = Gravity.TOP;
        OrionViewerActivity orionViewerActivity = requireOrionActivity();
        int toolbarHeight = orionViewerActivity.isNewUI() ? 0 : orionViewerActivity.getToolbar().getHeight();
        wlp.y = toolbarHeight + 5;

        window.setAttributes(wlp);
        window.setBackgroundDrawable(new ColorDrawable(ALPHA));

        dialog.setContentView(R.layout.search_dialog);
        View resultView = super.onCreateView(inflater, container, savedInstanceState);

        final Controller controller = orionViewerActivity.getController();

        searchField = dialog.findViewById(R.id.searchText);
        searchField.getBackground().setAlpha(ALPHA);

        android.widget.ImageButton searchNext = dialog.findViewById(R.id.searchNext);
        searchNext.getBackground().setAlpha(ALPHA);
        searchNext.setOnClickListener(v -> doSearch(searchField, controller.getCurrentPage(), +1, controller));

        android.widget.ImageButton searchPrev = dialog.findViewById(R.id.searchPrev);
        searchPrev.getBackground().setAlpha(ALPHA);
        searchPrev.setOnClickListener(v -> doSearch(searchField, controller.getCurrentPage(), -1, controller));

        OrionDrawScene view = orionViewerActivity.getView();
        view.addTask(lastSearchResultRenderer);

        myTask = new SearchTask(getActivity(), controller) {
            @Override
            protected void onResult(boolean isSuccessful, SearchTaskResult result) {
                boolean forward = lastDirectionOnSearch == +1;
                LayoutPosition position = new LayoutPosition();
                LayoutStrategy layoutStrategy = controller.getLayoutStrategy();
                PageWalker walker = layoutStrategy.getWalker();
                layoutStrategy.reset(position, result.info, forward);

                List<SubBatch> subBatches = prepareResults(result, forward, position, walker, layoutStrategy);
                lastPosition = forward ? 0 : subBatches.size() - 1;
                screens = subBatches;
                lastPage = result.page;

                SubBatch toShow = subBatches.get(lastPosition);
                toShow.active += lastDirectionOnSearch;
                drawBatch(toShow, controller);
            }

            @NonNull
            private List<SubBatch> prepareResults(SearchTaskResult result, boolean forward, LayoutPosition position, PageWalker walker, LayoutStrategy layoutStrategy) {
                List<SubBatch> screens = new ArrayList<>();
                RectF[] searchBoxes = result.searchBoxes;

                for (RectF searchBox : searchBoxes) {
                    log("Scaling rect " + searchBox);
                    Util.scale(searchBox, position.getDocZoom());
                }

                RectF temp = new RectF();
                do {
                    SubBatch sb = new SubBatch();
                    sb.lp = position.deepCopy();
                    RectF screenArea = toAbsoluteRect(position);
                    log("Area " + screenArea);
                    for (RectF searchBox : searchBoxes) {
                        float square1 = searchBox.width() * searchBox.height();
                        if (temp.setIntersect(searchBox, screenArea)) {
                            log("Rect " + searchBox);
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
                    log("Adding fake batch");
                    SubBatch sb = new SubBatch();
                    sb.lp = position.deepCopy();
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
            log("Stopping search task");
            myTask.stop();
        }
    }


    private void destroyLastPage() {
        if (lastPage != null) {
            log("Search: destroying page " + lastPage.getPageNum());
            lastPage.destroy();
            lastPage = null;
        }
    }

    private void doSearch(EditText searchField, int page, int direction, Controller controller) {
        InputMethodManager inputManager = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(searchField.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

        boolean performRealSearch = false;
        String newSearch = searchField.getText().toString();
        lastDirectionOnSearch = direction;
        if (newSearch.isEmpty()) {
            requireOrionActivity().showAlert(R.string.msg_error, R.string.msg_specify_keyword_for_search);
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
            log("Real search for " + page);
            destroyLastPage();
            myTask.go(newSearch, direction, page, -1, (SimpleLayoutStrategy) controller.getLayoutStrategy());
        } else {
            SubBatch subBatch = screens.get(lastPosition);
            drawBatch(subBatch, controller);
        }

        lastSearch = newSearch;
    }

    @NonNull
    private OrionViewerActivity requireOrionActivity() {
        return (OrionViewerActivity) requireActivity();
    }

    private void drawBatch(SubBatch subBatch, Controller controller) {
        log("lastDirectionOnSearch = " + lastDirectionOnSearch);
        log("lastPosition = " + lastPosition);
        log("lastIndex = " + subBatch.active);
        log("page = " + subBatch.lp.getPageNumber());
        log("Rect " + toAbsoluteRect(subBatch.lp));

        lastSearchResultRenderer.setBatch(subBatch);
        controller.drawPage(subBatch.lp);
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        lastSearchResultRenderer.setBatch(null);
        destroyLastPage();
        OrionDrawScene view = requireOrionActivity().getView();
        view.removeTask(lastSearchResultRenderer);
    }

    private static class SubBatch {
        List<RectF> rects = new ArrayList<>();

        int active = -1;

        LayoutPosition lp;
    }

    static class SearchResultRenderer implements DrawTask {

        private SubBatch batch;

        private static final int activeAlpha = 128;
        private static final int generalAlpha = 64;

        public SearchResultRenderer() {

        }

        @Override
        public boolean accept(int page) {
            return batch != null && batch.lp.getPageNumber() == page;
        }

        public void setBatch(SubBatch batch) {
            this.batch = batch;
        }

        @Override
        public void drawOnCanvas(@NonNull Canvas canvas, @NonNull ColorStuff stuff, DrawContext drawContext) {
            if (batch != null) {
                System.out.println("Draw search results");
                Paint paint = stuff.getBorderPaint();
                List<RectF> rects = batch.rects;
                int index = 0;
                int prevAlpha = paint.getAlpha();
                Paint.Style style = paint.getStyle();
                paint.setStyle(Paint.Style.FILL);
                for (RectF rect : rects) {
                    paint.setAlpha(index++ == batch.active ? activeAlpha : generalAlpha);
                    int left = batch.lp.getX().getMarginLeft();
                    int top = batch.lp.getY().getMarginLeft();
                    canvas.drawRect(rect.left - left, rect.top - top, rect.right - left, rect.bottom - top, paint);
                }
                paint.setAlpha(prevAlpha);
                paint.setStyle(style);
            }
        }
    }

}
