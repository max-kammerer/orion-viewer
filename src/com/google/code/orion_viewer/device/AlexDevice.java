package com.google.code.orion_viewer.device;

import android.app.Activity;
import android.content.Context;
import android.os.PowerManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.widget.EpdRender;
import com.google.code.orion_viewer.Device;
import com.google.code.orion_viewer.OperationHolder;
import com.google.code.orion_viewer.R;

import java.security.Key;

/**
 * User: mike
 * Date: 19.11.11
 * Time: 10:24
 */
public class AlexDevice extends EpdRender implements Device {

    private Activity activity;

    private PowerManager.WakeLock screenLock;

    private int DELAY = 600000;

    public AlexDevice(Activity activity) {
        this.activity = activity;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event, OperationHolder operation) {
        System.out.println("On key down " + Thread.currentThread().getName() + Thread.currentThread().getId());
        if (keyCode == NEXT || keyCode == PREV) {
            operation.value = keyCode;
            return true;
        }
        return false;
    }

    public void onCreate(Activity activity) {
        bindLayout((ViewGroup) activity.findViewById(R.id.epdLayout));

        PowerManager power = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
        screenLock = power.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "OrionViewer" + hashCode());
        screenLock.setReferenceCounted(false);
    }

    public void onPause() {
        setVdsActive(false);
        if (screenLock != null) {
            screenLock.release();
        }
    }

    public void onResume() {
        setVdsActive(true);
        onUserInteraction();
    }

    public void onUserInteraction() {
        if (screenLock != null) {
            screenLock.acquire(DELAY);
        }
    }

    public void updatePageNumber(int current, int max) {

    }

    public void updateTitle(String title) {

    }

    @Override
    public void executeKeyEvent(int what, int arg1, int arg2) {
        super.executeKeyEvent(what, arg1, arg2);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public boolean onPageUp(int arg1, int arg2) {
        //uiThread
        return activity.onKeyDown(PREV, null);
    }

    public boolean onPageDown(int arg1, int arg2) {
        //uiThread
        return activity.onKeyDown(NEXT, null);
    }

    public void flush() {
        updateEpdView();
    }

    public int getLayoutId() {
        return R.layout.alex_main;
    }

    public String getDefaultDirectory() {
        return "/sdcard/ebooks";
    }


}
