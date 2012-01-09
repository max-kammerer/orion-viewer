package com.google.code.orion_viewer;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.ImageButton;
import pl.polidea.customwidget.TheMissingTabHost;

/**
 * User: mike
 * Date: 24.12.11
 * Time: 17:00
 */
public class OrionBaseActivity extends Activity {

    private int screenOrientation;

    protected Device device ;

    protected SharedPreferences.OnSharedPreferenceChangeListener listener;

    public OrionBaseActivity() {
        if (supportDevice()) {
            device = Common.createDevice();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String orientation = PreferenceManager.getDefaultSharedPreferences(this).getString(GlobalOptions.SCREEN_ORIENTATION, "DEFAULT");
        screenOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
        if ("LANDSCAPE".equals(orientation)) {
            screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        } else if ("PORTRAIT".equals(orientation)) {
            screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        }


//        Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        System.out.println("display "+ getRequestedOrientation() + " screenOrientation " + getWindow().getAttributes().screenOrientation);
        if (getRequestedOrientation() != screenOrientation) {
            System.out.println("display on create " + screenOrientation);
            setRequestedOrientation(screenOrientation);
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.screenOrientation = screenOrientation;
            getWindow().setAttributes(params);
        }

        super.onCreate(savedInstanceState);


        if (device != null) {
            device.onCreate(this);
            listener = createPreferenceListener();
            if (listener != null) {
                registerPreferenceListener(listener);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        String orientation = PreferenceManager.getDefaultSharedPreferences(this).getString(GlobalOptions.SCREEN_ORIENTATION, "DEFAULT");
        int newScreenOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;

        if ("LANDSCAPE".equals(orientation)) {
            newScreenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        } else if ("PORTRAIT".equals(orientation)) {
            newScreenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        }
        System.out.println("onrdisplay "+ getRequestedOrientation() + " screenOrientation " + getWindow().getAttributes().screenOrientation);
        System.out.println("onrdisplay "+ newScreenOrientation);

        if (screenOrientation != newScreenOrientation) {
            System.out.println("Changing display in onresume " + newScreenOrientation);
            this.screenOrientation = newScreenOrientation;
            setRequestedOrientation(newScreenOrientation);
        }

        if (device != null) {
            device.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (device != null) {
            device.onPause();
        }
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        if (device != null) {
            device.onUserInteraction();
        }
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

    public SharedPreferences.OnSharedPreferenceChangeListener createPreferenceListener() {
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();    //To change body of overridden methods use File | Settings | File Templates.
        if (listener != null) {
            unregisterPreferenceListener(listener);
        }
    }

    protected void initHelpScreen() {
        TheMissingTabHost host = (TheMissingTabHost) findMyViewById(R.id.helptab);

        host.setup();

        TheMissingTabHost.TheMissingTabSpec spec = host.newTabSpec("general_help");
        spec.setContent(R.id.general_help);
        spec.setIndicator("", getResources().getDrawable(R.drawable.help));
        host.addTab(spec);
        TheMissingTabHost.TheMissingTabSpec recent = host.newTabSpec("app_info");
        recent.setContent(R.id.app_info);
        recent.setIndicator("", getResources().getDrawable(R.drawable.info));
        host.addTab(recent);
        host.setCurrentTab(0);

        ImageButton btn = (ImageButton) findMyViewById(R.id.help_close);
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //animator.setDisplayedChild(MAIN_SCREEN);
                onAnimatorCancel();
            }
        });

        btn = (ImageButton) findMyViewById(R.id.info_close);
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onAnimatorCancel();
                //animator.setDisplayedChild(MAIN_SCREEN);
            }
        });
    }

    protected View findMyViewById(int id) {
        return findViewById(id);
    }

    protected void onAnimatorCancel() {

    }

    protected void onApplyAction() {

    }

    protected void registerPreferenceListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(listener);
    }

    protected  void unregisterPreferenceListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(listener);
    }

    public boolean supportDevice() {
        return true;
    }

}
