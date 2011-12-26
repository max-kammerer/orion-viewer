package com.google.code.orion_viewer.device;

import android.content.Context;
import android.os.Build;
import android.os.PowerManager;
import android.view.KeyEvent;
import com.google.code.orion_viewer.*;

/**
 * User: mike
 * Date: 17.12.11
 * Time: 17:02
 */
public class AndroidDevice implements Device {

    private PowerManager.WakeLock screenLock;

    private OrionBaseActivity activity;

    private int DELAY = 600000;

    public AndroidDevice() {

    }

    public void updateTitle(String title) {

    }

    public void updatePageNumber(int current, int max) {

    }

    public boolean onKeyDown(int keyCode, KeyEvent event, OperationHolder holder) {
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
            screenLock.acquire(DELAY);
        }
    }


    public void onUserInteraction() {
        if (screenLock != null) {
            screenLock.acquire(DELAY);
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

    public int getViewWidth() {
        return activity.getView().getLayoutParams().width;
    }

    public int getViewHeight() {
        return activity.getView().getLayoutParams().height;
    }

    public int getFileManagerLayoutId() {
        return R.layout.android_file_manager;
    }

    public boolean optionViaDialog() {
        return true;
    }
}
