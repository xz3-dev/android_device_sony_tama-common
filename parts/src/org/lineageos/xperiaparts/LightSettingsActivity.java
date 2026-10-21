/*
 * SPDX-FileCopyrightText: 2026 xz3-dev
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.xperiaparts;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.provider.Settings;
import android.widget.Toast;

public class LightSettingsActivity extends Activity {

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
                    .replace(android.R.id.content, new LightSettingsFragment())
                    .commit();
        }
    }

    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }

    @SuppressWarnings("deprecation")
    public static class LightSettingsFragment extends PreferenceFragment
            implements Preference.OnPreferenceClickListener {

        private static final String KEY_ACCESS = "led_access";
        private static final String KEY_TEST = "led_test";
        private static final long TEST_DURATION_MS = 10_000;

        private Preference mAccess;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // Same file the listener reads (LedPrefs.get()).
            getPreferenceManager().setStorageDeviceProtected();
            getPreferenceManager().setSharedPreferencesName(LedPrefs.PREFS_NAME);
            addPreferencesFromResource(R.xml.notification_light);
            mAccess = findPreference(KEY_ACCESS);
            mAccess.setOnPreferenceClickListener(this);
            findPreference(KEY_TEST).setOnPreferenceClickListener(this);
        }

        @Override
        public void onResume() {
            super.onResume();
            mAccess.setSummary(hasListenerAccess()
                    ? R.string.light_access_granted : R.string.light_access_missing);
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            if (KEY_ACCESS.equals(preference.getKey())) {
                Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS);
                intent.putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                        listenerComponent().flattenToString());
                startActivity(intent);
                return true;
            }
            if (KEY_TEST.equals(preference.getKey())) {
                postTest();
                return true;
            }
            return false;
        }

        private ComponentName listenerComponent() {
            return new ComponentName(getActivity(), LedService.class);
        }

        private boolean hasListenerAccess() {
            return getActivity().getSystemService(NotificationManager.class)
                    .isNotificationListenerAccessGranted(listenerComponent());
        }

        private void postTest() {
            Context context = getActivity();
            Notification notification = new Notification.Builder(context,
                    LedPrefs.ensureChannel(context))
                    .setSmallIcon(R.drawable.ic_stat_led)
                    .setContentTitle(context.getString(R.string.led_test_title))
                    .setContentText(context.getString(R.string.led_test_text))
                    .setTimeoutAfter(TEST_DURATION_MS)
                    .setLocalOnly(true)
                    .build();
            context.getSystemService(NotificationManager.class)
                    .notify(LedPrefs.TEST_NOTIFICATION_ID, notification);
            Toast.makeText(context, R.string.light_test_toast, Toast.LENGTH_SHORT).show();
        }
    }
}
