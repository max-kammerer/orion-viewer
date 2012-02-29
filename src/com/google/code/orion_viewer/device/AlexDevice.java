package com.google.code.orion_viewer.device;

/*
 * Orion Viewer is a pdf and djvu viewer for android devices
 *
 * Copyright (C) 2011-2012  Michael Bogdanov
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import android.content.Intent;
import android.os.Build;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.EpdRender;
import android.widget.TextView;
import com.google.code.orion_viewer.*;
import universe.constellation.orion.viewer.R;

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


    public void onSetContentView() {
        bindLayout((ViewGroup) activity.findViewById(R.id.epdLayout));
        pageTextView = (TextView) activity.findViewById(R.id.statusbar_page_number);
        titleTextView = (TextView) activity.findViewById(R.id.statusbar_title);
    }
}
