package com.google.code.orion_viewer.device;

import android.content.Intent;
import android.os.Build;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.EpdRender;
import android.widget.TextView;
import com.google.code.orion_viewer.*;

/**
 * User: mike
 * Date: 19.11.11
 * Time: 10:24
 */
public class AlexDevice extends EpdRender implements Device {

    private OrionBaseActivity activity;

    private TextView pageTextView;

    private TextView titleTextView;

    private String title;

    public AlexDevice() {

    }

    public boolean onKeyDown(int keyCode, KeyEvent event, OperationHolder operation) {
        Common.d("On key down " + Thread.currentThread().getName() + Thread.currentThread().getId());
        if (keyCode == KeyEvent.KEYCODE_SOFT_LEFT || keyCode == KeyEvent.KEYCODE_SOFT_RIGHT) {
            operation.value = keyCode == KeyEvent.KEYCODE_SOFT_LEFT ? PREV : NEXT;
            return true;
        }
        return false;
    }

    public void onCreate(OrionBaseActivity activity) {
        this.activity = activity;
        bindLayout((ViewGroup) activity.findViewById(R.id.epdLayout));
        pageTextView = (TextView) activity.findViewById(R.id.statusbar_page_number);
        titleTextView = (TextView) activity.findViewById(R.id.statusbar_title);
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
        pageTextView.setText(current + "/" + max);
        //flushed on page draw
    }

    public void updateTitle(String title) {
        this.title = title;
        if (this.title == null) {
            this.title = "";
        }
        titleTextView.setText(title);
    }

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

    //called from ui thread
    public void flushBitmap(int delay) {
        if (delay == 0) {
            updateEpdView();
        } else {
            updateEpdViewDelay(delay);
        }
    }

    public int getLayoutId() {
        return R.layout.alex_main;
    }

    public String getDefaultDirectory() {
        return "ebooks";
    }

    public int getFileManagerLayoutId() {
        return R.layout.alex_file_manager;
    }

    public boolean optionViaDialog() {
        return false;
    }

    public int getHelpLayoutId() {
        return R.layout.android_help;
    }

    public void updateOptions(GlobalOptions options) {

    }
}
