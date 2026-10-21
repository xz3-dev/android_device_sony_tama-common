/*
 * SPDX-FileCopyrightText: 2026 xz3-dev
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.xperiaparts;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ContentResolver;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;

import lineageos.providers.LineageSettings;

/**
 * Camera button options. The key itself is handled by the Lineage window manager policy
 * (config_deviceHardwareKeys and config_deviceHardwareWakeKeys include the camera key); this
 * page only edits its LineageSettings so it can live under Settings > System > Gestures.
 */
public class CameraButtonActivity extends Activity {

    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .replace(android.R.id.content, new CameraButtonFragment())
                    .commit();
        }
    }

    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }

    @SuppressWarnings("deprecation")
    public static class CameraButtonFragment extends PreferenceFragment
            implements Preference.OnPreferenceChangeListener {

        private static final String KEY_WAKE = "camera_wake_screen";
        private static final String KEY_LAUNCH = "camera_launch";

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.camera_button);
            bind(KEY_WAKE, LineageSettings.System.CAMERA_WAKE_SCREEN);
            bind(KEY_LAUNCH, LineageSettings.System.CAMERA_LAUNCH);
        }

        private void bind(String key, String setting) {
            SwitchPreference pref = (SwitchPreference) findPreference(key);
            ContentResolver resolver = getActivity().getContentResolver();
            pref.setChecked(LineageSettings.System.getInt(resolver, setting, 0) != 0);
            pref.setOnPreferenceChangeListener(this);
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String setting;
            switch (preference.getKey()) {
                case KEY_WAKE:
                    setting = LineageSettings.System.CAMERA_WAKE_SCREEN;
                    break;
                case KEY_LAUNCH:
                    setting = LineageSettings.System.CAMERA_LAUNCH;
                    break;
                default:
                    return false;
            }
            return LineageSettings.System.putInt(getActivity().getContentResolver(), setting,
                    (Boolean) newValue ? 1 : 0);
        }
    }
}
