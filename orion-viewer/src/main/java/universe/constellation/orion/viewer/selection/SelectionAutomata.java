package universe.constellation.orion.viewer.selection;

import android.app.Dialog;
import android.graphics.Rect;
import android.view.*;

import androidx.annotation.NonNull;

import java.util.List;

import universe.constellation.orion.viewer.Action;
import universe.constellation.orion.viewer.Controller;
import universe.constellation.orion.viewer.OrionViewerActivity;
import universe.constellation.orion.viewer.R;
import universe.constellation.orion.viewer.dialog.DialogOverView;
import universe.constellation.orion.viewer.view.PageLayoutManager;

public class SelectionAutomata extends DialogOverView {

    private enum STATE {START, MOVING, END, CANCELED}

    private final static int SINGLE_WORD_AREA = 2;

    private STATE state = STATE.CANCELED;

    private int startX, startY, width, height;

    private final SelectionView selectionView;

    private boolean isSingleWord = false;

    private boolean translate = false;

    public SelectionAutomata(final OrionViewerActivity activity) {
        super(activity, universe.constellation.orion.viewer.R.layout.text_selector, android.R.style.Theme_Translucent_NoTitleBar);

        selectionView = dialog.findViewById(R.id.text_selector);
        selectionView.setOnTouchListener((v, event) -> SelectionAutomata.this.onTouch(event));
    }



    public boolean onTouch(MotionEvent event) {
        int action = event.getAction();

        //System.out.println("aaaction " + action + " " + event.getX() + ", " + event.getY());
        STATE oldState = state;
        boolean result = true;
        switch (state) {
            case START:
                if (action == MotionEvent.ACTION_DOWN) {
                    startX = (int) event.getX();
                    startY = (int) event.getY();
                    width = 0;
                    height = 0;
                    state = STATE.MOVING;
                    selectionView.reset();
                } else {
                    state = STATE.CANCELED;
                }
                break;

            case MOVING:
                int endX = (int) event.getX();
                int endY = (int) event.getY();
                width = endX - startX;
                height = endY - startY;
                if (action == MotionEvent.ACTION_UP) {
                    state = STATE.END;
                } else {
                    selectionView.updateView(Math.min(startX, endX), Math.min(startY, endY), Math.max(startX, endX), Math.max(startY, endY));
                }
                break;

            default: result = false;
        }

        if (oldState != state) {
            switch (state) {
                case CANCELED: dialog.dismiss(); break;

                case END:
                    selectText(activity, isSingleWord, translate, dialog, getSelectionRectangle());
                    break;
            }
        }
        return result;
    }

    public static void selectText(
            OrionViewerActivity activity, boolean isSingleWord, boolean translate, Dialog dialog,
            List<PageAndSelection> data
    ) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        Controller controller = activity.getController();
        if (controller == null) return;

        for (PageAndSelection selection: data) {
            Rect rect = selection.getAbsoluteRectWithoutCrop();
            String text = controller.selectRawText(selection.getPageNum(), rect.left, rect.top, rect.width(), rect.height(), isSingleWord);
            if (text != null) {
                if (!first) {
                    sb.append(" ");
                }
                sb.append(text);
                first = false;
            }
        }
        String text = sb.toString();
        if (!"".equals(text)) {
            if (isSingleWord && translate) {
                dialog.dismiss();
                Action.DICTIONARY.doAction(controller, activity, text);
            } else {
                new SelectedTextActions(activity, dialog).show(text);
            }
        } else {
            dialog.dismiss();
            activity.showFastMessage(R.string.warn_no_text_in_selection);
        }
    }

    public void startSelection(boolean isSingleWord, boolean translate) {
        selectionView.reset();
        initDialogSize();
        dialog.show();
        String msg = activity.getResources().getString(isSingleWord ? R.string.msg_select_word : R.string.msg_select_text);
        activity.showFastMessage(msg);
        state = STATE.START;
        this.isSingleWord = isSingleWord;
        this.translate = translate;
    }

    public boolean inSelection() {
        return state == STATE.START || state == STATE.MOVING;
    }

    public boolean isSuccessful() {
        return state == STATE.END;
    }

    private List<PageAndSelection> getSelectionRectangle() {
        int startX = this.startX;
        int startY = this.startY;
        int width = this.width;
        int height = this.height;

        if (width < 0) {
            startX += width;
            width = -width;
        }
        if (height < 0) {
            startY += height;
            height = -height;
        }

        return getSelectionRectangle(startX, startY, width, height, isSingleWord, activity.getController().getPageLayoutManager());
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
