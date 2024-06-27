package universe.constellation.orion.viewer.selection;

import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;

import androidx.annotation.NonNull;

import java.util.List;

import universe.constellation.orion.viewer.Action;
import universe.constellation.orion.viewer.Controller;
import universe.constellation.orion.viewer.OrionViewerActivity;
import universe.constellation.orion.viewer.R;
import universe.constellation.orion.viewer.dialog.DialogOverView;
import universe.constellation.orion.viewer.document.TextAndSelection;
import universe.constellation.orion.viewer.view.PageLayoutManager;

public class SelectionAutomata extends DialogOverView {

    private enum STATE {START, MOVING, END, CANCELED}

    private final static int SINGLE_WORD_AREA = 2;

    private STATE state = STATE.CANCELED;

    private float startX, startY, width, height;

    private final SelectionViewNew selectionView;

    private boolean isSingleWord = false;

    private boolean translate = false;

    public SelectionAutomata(final OrionViewerActivity activity) {
        super(activity, universe.constellation.orion.viewer.R.layout.text_selector, android.R.style.Theme_Translucent_NoTitleBar);

        selectionView = dialog.findViewById(R.id.text_selector);
        selectionView.setOnTouchListener((v, event) -> SelectionAutomata.this.onTouch(event));
    }

    public boolean onTouch(MotionEvent event) {
        int action = event.getAction();

        STATE oldState = state;
        boolean result = true;
        switch (state) {
            case START:
                if (action == MotionEvent.ACTION_DOWN) {
                    startX = event.getX();
                    startY = event.getY();
                    width = 0;
                    height = 0;
                    state = STATE.MOVING;
                    selectionView.reset();
                } else {
                    state = STATE.CANCELED;
                }
                break;

            case MOVING:
                float endX = event.getX();
                float endY = event.getY();
                width = endX - startX;
                height = endY - startY;
                if (action == MotionEvent.ACTION_UP) {
                    state = STATE.END;
                } else {
                    selectionView.updateView(new RectF(Math.min(startX, endX), Math.min(startY, endY), Math.max(startX, endX), Math.max(startY, endY)));
                }
                break;

            default: result = false;
        }

        if (oldState != state) {
            switch (state) {
                case CANCELED: dialog.dismiss(); break;

                case END:
                    selectText(isSingleWord, translate, getSelectionRectangle(), getScreenSelectionRect());
                    break;
            }
        }
        return result;
    }

    public void selectText(
            boolean isSingleWord, boolean translate, List<PageAndSelection> data, Rect originSelection
    ) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        Controller controller = activity.getController();
        if (controller == null) return;

        for (PageAndSelection selection: data) {
            Rect rect = selection.getAbsoluteRectWithoutCrop();
            TextAndSelection text = controller.selectRawText(selection.getPage(), rect.left, rect.top, rect.width(), rect.height(), isSingleWord);
            if (text != null) {
                if (!first) {
                    sb.append(" ");
                }
                sb.append(text.getValue());
                first = false;
            }
            if (isSingleWord && text != null) {
                RectF originRect = text.getRect();
                RectF sceneRect = selection.getPageView().getSceneRect(originRect);
                originSelection = new Rect((int) sceneRect.left, (int) sceneRect.top, (int) sceneRect.right, (int) sceneRect.bottom);
                selectionView.updateView(sceneRect);
            }
        }
        String text = sb.toString();
        if (!text.isEmpty()) {
            if (isSingleWord && translate) {
                dialog.dismiss();
                Action.DICTIONARY.doAction(controller, activity, text);
            } else {
                if (isSingleWord && !dialog.isShowing()) {
                    //TODO: refactor
                    final Rect origin = originSelection;
                    dialog.setOnShowListener(dialog2 -> {
                        new SelectedTextActions(activity, dialog).show(text, origin);
                        dialog.setOnShowListener(null);
                    });
                    startSelection(true, false, true);
                    state = STATE.END;
                } else {
                    new SelectedTextActions(activity, dialog).show(text, originSelection);
                }
            }
        } else {
            dialog.dismiss();
            activity.showFastMessage(R.string.warn_no_text_in_selection);
        }
    }

    public void startSelection(boolean isSingleWord, boolean translate) {
        startSelection(isSingleWord, translate, false);
    }
    public void startSelection(boolean isSingleWord, boolean translate, boolean quite) {
        selectionView.setColorFilter(activity.getFullScene().getColorStuff().getBackgroundPaint().getColorFilter());
        if (!quite) {
            selectionView.reset();
        }
        initDialogSize();
        dialog.show();
        if (!quite) {
            String msg = activity.getResources().getString(isSingleWord ? R.string.msg_select_word : R.string.msg_select_text);
            activity.showFastMessage(msg);
        }
        state = STATE.START;
        this.isSingleWord = isSingleWord;
        this.translate = translate;
    }

    private List<PageAndSelection> getSelectionRectangle() {
        Rect screenRect = getScreenSelectionRect();
        return getSelectionRectangle(screenRect.left, screenRect.top, screenRect.width(), screenRect.height(), isSingleWord, activity.getController().getPageLayoutManager());
    }

    private Rect getScreenSelectionRect() {
        float startX = this.startX;
        float startY = this.startY;
        float width = this.width;
        float height = this.height;

        if (width < 0) {
            startX += width;
            width = -width;
        }
        if (height < 0) {
            startY += height;
            height = -height;
        }

        return new Rect((int) startX, (int) startY, (int) (startX + width), (int) (startY + height));
    }

    public static List<PageAndSelection> getSelectionRectangle(int startX, int startY, int width, int height, boolean isSingleWord, PageLayoutManager pageLayoutManager) {
        Rect rect = getSelectionRect(startX, startY, width, height, isSingleWord);
        return pageLayoutManager.findPageAndPageRect(rect);
    }

    @NonNull
    public static Rect getSelectionRect(int startX, int startY, int width, int height, boolean isSingleWord) {
        int singleWordDelta = isSingleWord ? SINGLE_WORD_AREA : 0;
        int x = startX - singleWordDelta;
        int y = startY - singleWordDelta;
        return new Rect(x, y, x + width + singleWordDelta, y + height + singleWordDelta);
    }
}
