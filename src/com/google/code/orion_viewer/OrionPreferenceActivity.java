package com.google.code.orion_viewer;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * User: mike
 * Date: 02.01.12
 * Time: 17:35
 */
public class OrionPreferenceActivity extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.userpreferences);
    }
}
