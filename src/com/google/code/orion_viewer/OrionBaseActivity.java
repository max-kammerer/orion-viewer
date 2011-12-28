package com.google.code.orion_viewer;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

/**
 * User: mike
 * Date: 24.12.11
 * Time: 17:00
 */
public class OrionBaseActivity extends Activity {

    protected Device device = Common.createDevice();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        device.onCreate(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        device.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        device.onPause();
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        device.onUserInteraction();
    }

    public Device getDevice() {
        return device;
    }

    public View getView() {
        return null;
    }

    public int getViewerType() {
        return Device.DEFAULT_ACTIVITY;
    }
}
