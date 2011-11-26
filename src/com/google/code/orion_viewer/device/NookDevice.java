package com.google.code.orion_viewer.device;

/*Orion Viewer is a pdf viewer for Nook Classic based on mupdf

Copyright (C) 2011  Michael Bogdanov

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;
import android.view.KeyEvent;
import com.google.code.orion_viewer.Device;
import com.google.code.orion_viewer.OperationHolder;
import com.google.code.orion_viewer.OrionViewerActivity;
import com.google.code.orion_viewer.R;

/**
 * User: mike
 * Date: 18.10.11
 * Time: 11:14
 * Nook specific features obtained from nookCommon at http://code.google.com/p/nookdevs/
 */
public class NookDevice implements Device {

    public final static String UPDATE_TITLE = "com.bravo.intent.UPDATE_TITLE";

    public final static String UPDATE_STATUSBAR = "com.bravo.intent.UPDATE_STATUSBAR";

    public final static String STATUSBAR_ICON = "Statusbar.icon";

    public final static String STATUSBAR_ACTION = "Statusbar.action";

    protected static final int NOOK_PAGE_UP_KEY_RIGHT = 98;

    protected static final int NOOK_PAGE_DOWN_KEY_RIGHT = 97;

    protected static final int NOOK_PAGE_UP_KEY_LEFT = 96;

    protected static final int NOOK_PAGE_DOWN_KEY_LEFT = 95;

    protected static final int NOOK_PAGE_DOWN_SWIPE = 100;

    protected static final int NOOK_PAGE_UP_SWIPE = 101;

    private PowerManager.WakeLock screenLock;

    private OrionViewerActivity activity;

    private int DELAY = 600000;

    public NookDevice(OrionViewerActivity activity) {
        this.activity = activity;
    }

    public void updateTitle(String title) {
        try {
            Intent intent = new Intent(UPDATE_TITLE);
            String key = "apptitle";
            intent.putExtra(key, title);
            activity.sendBroadcast(intent);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void updatePageNumber(int current, int max) {
        try {
            Intent msg = new Intent(UPDATE_STATUSBAR);
            msg.putExtra(STATUSBAR_ICON, 7);
            msg.putExtra(STATUSBAR_ACTION, 1);
            msg.putExtra("current", current);
            msg.putExtra("max", max);
            activity.sendBroadcast(msg);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    public boolean onKeyDown(int keyCode, KeyEvent event, OperationHolder holder) {
        switch (keyCode) {
            case NOOK_PAGE_UP_KEY_LEFT:
            case NOOK_PAGE_UP_KEY_RIGHT:
                holder.value = PREV;
                break;
            case NOOK_PAGE_DOWN_KEY_LEFT:
            case NOOK_PAGE_DOWN_KEY_RIGHT:
                holder.value = NEXT;
                break;
            case NOOK_PAGE_UP_SWIPE:
                holder.value = PREV;
                break;
            case NOOK_PAGE_DOWN_SWIPE:
                holder.value = NEXT;
                break;
            default:
                return false;
        }
        return true;
    }

    public void onCreate(Activity activity) {
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
        onUserInteraction();
    }


    public void onUserInteraction() {
        if (screenLock != null) {
            screenLock.acquire(DELAY);
        }
    }

    public void flush() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getLayoutId() {
        return R.layout.main;
    }

    public String getDefaultDirectory() {
        return "/system/media/sdcard";
    }
}
