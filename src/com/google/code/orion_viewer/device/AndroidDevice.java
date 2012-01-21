package com.google.code.orion_viewer.device;

import android.content.Context;
import android.os.PowerManager;
import android.view.KeyEvent;
import com.google.code.orion_viewer.*;

/**
 * User: mike
 * Date: 17.12.11
 * Time: 17:02
 */
public class AndroidDevice implements Device {

    protected static final int NOOK_PAGE_UP_KEY_RIGHT = 95;

    protected static final int NOOK_PAGE_DOWN_KEY_RIGHT = 94;

    protected static final int NOOK_PAGE_UP_KEY_LEFT = 93;

    protected static final int NOOK_PAGE_DOWN_KEY_LEFT = 92;

    private PowerManager.WakeLock screenLock;

    protected OrionBaseActivity activity;

    private static int DELAY = 60000;

    private static int VIEWER_DELAY = 600000;

    private static int delay = DELAY;

    public GlobalOptions options;

    public AndroidDevice() {

    }

    public void updateTitle(String title) {

    }

    public void updatePageNumber(int current, int max) {

    }

    public boolean onKeyDown(int keyCode, KeyEvent event, OperationHolder holder) {
        //check mapped keys
        if (options != null) {
            if (keyCode == options.getNextKey()) {
                holder.value = NEXT;
                return true;
            }

            if (keyCode == options.getPrevKey()) {
                holder.value = PREV;
                return true;
            }

        }

        if (Info.NOOK2) {
            switch (keyCode) {
                case NOOK_PAGE_UP_KEY_LEFT:
                case NOOK_PAGE_UP_KEY_RIGHT:
                    holder.value = PREV;
                    return true;
                case NOOK_PAGE_DOWN_KEY_LEFT:
                case NOOK_PAGE_DOWN_KEY_RIGHT:
                    holder.value = NEXT;
                    return true;
                }
        }

        if (Info.SONY_PRS_T1) {
            if (keyCode == 0) {
                 switch (event.getScanCode()) {
                    case 105:
                        holder.value = PREV;
                        return true;
                    case 106:
                        holder.value = NEXT;
                        return true;
                    }
            }

        }

        if (keyCode == KeyEvent.KEYCODE_SOFT_LEFT || keyCode == KeyEvent.KEYCODE_SOFT_RIGHT) {
            holder.value = keyCode == KeyEvent.KEYCODE_SOFT_LEFT ? PREV : NEXT;
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            holder.value = PREV ;
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            holder.value = NEXT;
            return true;
        }
        return false;
    }

    public void onCreate(OrionBaseActivity activity) {
        if (activity.getViewerType() == VIEWER_ACTIVITY) {
            delay = VIEWER_DELAY;
        }
        this.activity = activity;
        PowerManager power = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
        screenLock = power.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "OrionViewer" + hashCode());
        screenLock.setReferenceCounted(false);
    }

    public void onPause() {
        if (screenLock != null) {
            screenLock.release();
        }
    }

    public void onResume() {
        if (screenLock != null) {
            screenLock.acquire(delay);
        }
    }


    public void onUserInteraction() {
        if (screenLock != null) {
            screenLock.acquire(delay);
        }
    }

    public void flushBitmap(int delay) {
        if (activity.getView() != null) {
            activity.getView().invalidate();
        }
    }

    public int getLayoutId() {
        return R.layout.android_main;
    }

    public String getDefaultDirectory() {
        return "";
    }

    public int getFileManagerLayoutId() {
        return R.layout.android_file_manager;
    }

    public int getHelpLayoutId() {
        return R.layout.android_help;
    }

    public boolean optionViaDialog() {
        return true;
    }


    public void updateOptions(GlobalOptions options) {
        this.options = options;
    }

}
