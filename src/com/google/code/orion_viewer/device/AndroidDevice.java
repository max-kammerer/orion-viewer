package com.google.code.orion_viewer.device;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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

    private OrionViewerActivity activity;

    private int DELAY = 600000;

    public AndroidDevice(OrionViewerActivity activity) {
        this.activity = activity;
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
        return false;
    }

    public void onCreate(Activity activity) {
        PowerManager power = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
        screenLock = power.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "OrionViewer" + hashCode());
        screenLock.setReferenceCounted(false);
        Common.d("View size " + this.activity.getView().getLayoutParams().width + " " + this.activity.getView().getHeight());
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
        activity.getView().invalidate();
    }

    public int getLayoutId() {
        return R.layout.main;
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
}
