/*
 * SPDX-FileCopyrightText: 2026 xz3-dev
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.xperiaparts;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

final class LedPrefs {

    static final String PREFS_NAME = "notification_light";
    static final String KEY_ENABLED = "led_enabled";
    static final String KEY_ONLY_NEW = "led_only_new";

    static final String CHANNEL_ID = "notification_light";
    static final int LED_NOTIFICATION_ID = 1;
    static final int TEST_NOTIFICATION_ID = 2;

    // Settings.System.NOTIFICATION_LIGHT_PULSE, the system-wide "Notification light" switch.
    private static final String NOTIFICATION_LIGHT_PULSE = "notification_light_pulse";

    private LedPrefs() {}

    /** Device-protected storage, so the listener works before the first unlock. */
    static SharedPreferences get(Context context) {
        Context storage = context.isDeviceProtectedStorage()
                ? context : context.createDeviceProtectedStorageContext();
        return storage.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    static boolean enabled(Context context) {
        return get(context).getBoolean(KEY_ENABLED, true);
    }

    static boolean onlyNew(Context context) {
        return get(context).getBoolean(KEY_ONLY_NEW, false);
    }

    static boolean systemLightEnabled(Context context) {
        return Settings.System.getInt(context.getContentResolver(), NOTIFICATION_LIGHT_PULSE, 1) != 0;
    }

    /**
     * The LED is only lit for notifications of importance DEFAULT or higher whose channel has
     * lights enabled. Sound, vibration and badge are off so the helper never alerts. Light
     * colour 0 lets lineage-sdk apply the default colour chosen in the notification light
     * settings.
     */
    static String ensureChannel(Context context) {
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    context.getString(R.string.led_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(context.getString(R.string.led_channel_description));
            channel.setSound(null, null);
            channel.enableVibration(false);
            channel.enableLights(true);
            channel.setLightColor(0);
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
            nm.createNotificationChannel(channel);
        }
        return CHANNEL_ID;
    }
}
