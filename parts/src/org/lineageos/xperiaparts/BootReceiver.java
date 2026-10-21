/*
 * SPDX-FileCopyrightText: 2026 xz3-dev
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.xperiaparts;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;

import lineageos.providers.LineageSettings;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }
        ContentResolver resolver = context.getContentResolver();
        // LineageSettingsProvider has no defaults for the camera key, so the device defaults
        // (wake and long press to launch, as on the stock firmware) are written once here.
        // A value the user has set is never touched.
        applyDefault(resolver, LineageSettings.System.CAMERA_WAKE_SCREEN, 1);
        applyDefault(resolver, LineageSettings.System.CAMERA_LAUNCH, 1);
    }

    private static void applyDefault(ContentResolver resolver, String key, int value) {
        if (LineageSettings.System.getString(resolver, key) == null) {
            LineageSettings.System.putInt(resolver, key, value);
        }
    }
}
