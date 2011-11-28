package com.google.code.orion_viewer.device;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.widget.EpdRender;
import com.google.code.orion_viewer.Common;
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

    public AlexDevice(Activity activity) {
        this.activity = activity;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event, OperationHolder operation) {
        Common.d("On key down " + Thread.currentThread().getName() + Thread.currentThread().getId());
        if (keyCode == KeyEvent.KEYCODE_SOFT_LEFT || keyCode == KeyEvent.KEYCODE_SOFT_RIGHT) {
            operation.value = keyCode == KeyEvent.KEYCODE_SOFT_LEFT ? PREV : NEXT;
            return true;
        }
        return false;
    }

    public void onCreate(Activity activity) {
        bindLayout((ViewGroup) activity.findViewById(R.id.epdLayout));
    }

    public void onPause() {
        Common.d("on Pause");
        setVdsActive(false);
    }

    public void onResume() {
        Common.d("on Resume");
        setVdsActive(true);
        onUserInteraction();
    }

    public void onUserInteraction() {

    }

    public void updatePageNumber(int current, int max) {

    }

    public void updateTitle(String title) {

    }

    @Override
    public void executeKeyEvent(int what, int arg1, int arg2) {
        Common.d("Execute key event " + Thread.currentThread().getName() + Thread.currentThread().getId());
        super.executeKeyEvent(what, arg1, arg2);
    }

    public boolean onPageUp(int arg1, int arg2) {
        Common.d("On page up " + Thread.currentThread().getName() + Thread.currentThread().getId());
        //uiThread
        //handler.sendEmptyMessage(PREV);
        return activity.onKeyDown(KeyEvent.KEYCODE_SOFT_LEFT, null);
        //return true;
    }

    public boolean onPageDown(int arg1, int arg2) {
        Common.d("On page down " + Thread.currentThread().getName() + Thread.currentThread().getId());
        //uiThread
        //handler.sendEmptyMessage(NEXT);
        return activity.onKeyDown(KeyEvent.KEYCODE_SOFT_RIGHT, null);
       // return true;
    }

    public void flush() {
        updateEpdView();
        //handler.sendEmptyMessage(0);
    }

    public int getLayoutId() {
        return R.layout.alex_main;
    }

    public String getDefaultDirectory() {
        return "/sdcard/ebooks";
    }


}
