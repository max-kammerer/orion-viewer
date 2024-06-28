package universe.constellation.orion.viewer.selection;

import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;

import androidx.annotation.NonNull;

import java.util.List;

import universe.constellation.orion.viewer.Action;
import universe.constellation.orion.viewer.Controller;
import universe.constellation.orion.viewer.OrionBaseActivityKt;
import universe.constellation.orion.viewer.OrionViewerActivity;
import universe.constellation.orion.viewer.R;
import universe.constellation.orion.viewer.dialog.DialogOverView;
import universe.constellation.orion.viewer.document.TextAndSelection;
import universe.constellation.orion.viewer.document.TextInfoBuilder;
import universe.constellation.orion.viewer.view.PageLayoutManager;

public class SelectionAutomata extends DialogOverView {

    private enum STATE {START, MOVING, ACTIVE_SELECTION, MOVING_HANDLER, CANCELED}

    private final static int SINGLE_WORD_AREA = 2;

    private STATE state = STATE.CANCELED;

    private float startX, startY, width, height;

    private final SelectionViewNew selectionView;

    private boolean isSingleWord = false;

    private boolean translate = false;

    private final float radius;

    private Handler activeHandler = null;

    private SelectedTextActions actions = null;

    private boolean isMovingHandlers = false;

    private TextInfoBuilder builder = null;

    public SelectionAutomata(final OrionViewerActivity activity) {
        super(activity, universe.constellation.orion.viewer.R.layout.text_selector, android.R.style.Theme_Translucent_NoTitleBar);

        selectionView = dialog.findViewById(R.id.text_selector);
        selectionView.setOnTouchListener((v, event) -> SelectionAutomata.this.onTouch(event));
        radius = OrionBaseActivityKt.dpToPixels(activity, 10);
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
                    state = STATE.ACTIVE_SELECTION;
                } else {
                    selectionView.updateView(new RectF(Math.min(startX, endX), Math.min(startY, endY), Math.max(startX, endX), Math.max(startY, endY)));
                }
                break;

            case ACTIVE_SELECTION:
                System.out.println("XXX" + action);
                if (action == MotionEvent.ACTION_DOWN) {
                    activeHandler = SelectionViewNewKt.findClosestHandler(selectionView, event.getX(), event.getY(), radius * 1.2f);
                    System.out.println(activeHandler);
                    if (activeHandler == null) {
                        state = STATE.CANCELED;
                        result = false;
                    } else {
                        state = STATE.MOVING_HANDLER;
                        isMovingHandlers = true;
                        isSingleWord = false;
                        if (actions != null)
                            actions.dismissOnlyDialog();
                    }
                }
                break;
            case MOVING_HANDLER:
                 if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_MOVE) {
                    activeHandler.setX(event.getX());
                    activeHandler.setY(event.getY());
                    selectionView.invalidate();

                    if (action == MotionEvent.ACTION_UP) {
                        state = STATE.ACTIVE_SELECTION;
                        startX = selectionView.getStartHandler().getX();
                        width = selectionView.getEndHandler().getX() - startX;

                        startY = selectionView.getStartHandler().getY();
                        height = selectionView.getEndHandler().getY() - startY;
                    }
                } else {
                    result = false;
                }
            break;

            default: result = false;
        }

        if (oldState != state) {
            switch (state) {
                case CANCELED:
                    actions.dismissOnlyDialog();

                    actions = null;
                    dialog.dismiss();
                    break;

                case ACTIVE_SELECTION:
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
            System.out.println(selection.getAbsoluteRectWithoutCrop());
            Rect rect = selection.getAbsoluteRectWithoutCrop();
            TextAndSelection text = controller.selectRawText(selection.getPage(), rect.left, rect.top, rect.width(), rect.height(), isSingleWord);

            if (text != null) {
                if (!first) {
                    sb.append(" ");
                }
                sb.append(text.getValue());
                first = false;
            }
            if ((isSingleWord && text != null)  || isMovingHandlers) {
                RectF originRect = text.getRect();
                RectF sceneRect = selection.getPageView().getSceneRect(originRect);
                originSelection = new Rect((int) sceneRect.left, (int) sceneRect.top, (int) sceneRect.right, (int) sceneRect.bottom);
                selectionView.updateView(sceneRect);
                builder = text.getTextInfo();
                if (!isMovingHandlers) {
                    selectionView.setHandlers(new Handler(originSelection.left - radius / 2, originSelection.top - radius / 2, radius), new Handler(originSelection.right + radius / 2, originSelection.bottom + radius / 2, radius));
                }
            }
        }
        String text = sb.toString();
        if (!text.isEmpty()) {
            if (isSingleWord && translate) {
                dialog.dismiss();
                Action.DICTIONARY.doAction(controller, activity, text);
            } else {
                SelectedTextActions selectedTextActions = new SelectedTextActions(activity, dialog);
                actions = selectedTextActions;
                if (isSingleWord && !dialog.isShowing()) {
                    //TODO: refactor
                    final Rect origin = originSelection;
                    dialog.setOnShowListener(dialog2 -> {
                        selectedTextActions.show(text, origin);
                        dialog.setOnShowListener(null);
                    });
                    startSelection(true, false, true);
                    state = STATE.ACTIVE_SELECTION;
                } else {
                    selectedTextActions.show(text, originSelection);
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
