/*
 * SPDX-FileCopyrightText: 2026 xz3-dev
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.xperiaparts;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.Set;

/**
 * Most apps post on channels without lights, so the LED stays dark for them. While the screen
 * is off and another app has an unread, user-visible notification, this listener posts one
 * silent notification of its own on a channel with lights, and NotificationManagerService
 * blinks the LED for it. Priority against the battery light, DND and calls stays with the
 * system. The helper is cancelled when the screen turns on or the notifications are cleared.
 */
public class LedService extends NotificationListenerService {

    private static final Set<String> IGNORED_PACKAGES = Set.of(
            "android", "com.android.systemui", "com.android.providers.downloads");

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean mConnected;
    private boolean mScreenOn = true;
    private long mScreenOffTime;
    private boolean mPosted;

    private final BroadcastReceiver mScreenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                mScreenOn = false;
                mScreenOffTime = System.currentTimeMillis();
            } else {
                mScreenOn = true;
            }
            update();
        }
    };

    private final ContentObserver mSettingObserver = new ContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange) {
            update();
        }
    };

    private final SharedPreferences.OnSharedPreferenceChangeListener mPrefsListener =
            (prefs, key) -> update();

    @Override
    public void onListenerConnected() {
        mConnected = true;
        // A helper left over from a previous process would otherwise blink forever.
        getSystemService(NotificationManager.class).cancel(LedPrefs.LED_NOTIFICATION_ID);
        mPosted = false;
        mScreenOn = getSystemService(PowerManager.class).isInteractive();
        mScreenOffTime = mScreenOn ? System.currentTimeMillis() : 0;

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(mScreenReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        getContentResolver().registerContentObserver(
                Settings.System.getUriFor("notification_light_pulse"), false, mSettingObserver);
        LedPrefs.get(this).registerOnSharedPreferenceChangeListener(mPrefsListener);
        update();
    }

    @Override
    public void onListenerDisconnected() {
        mConnected = false;
        unregisterReceiver(mScreenReceiver);
        getContentResolver().unregisterContentObserver(mSettingObserver);
        LedPrefs.get(this).unregisterOnSharedPreferenceChangeListener(mPrefsListener);
        cancelHelper();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (!getPackageName().equals(sbn.getPackageName())) {
            update();
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (!getPackageName().equals(sbn.getPackageName())) {
            update();
        }
    }

    private void update() {
        if (!mConnected) {
            return;
        }
        if (mScreenOn || !LedPrefs.enabled(this) || !LedPrefs.systemLightEnabled(this)
                || !hasUnread()) {
            cancelHelper();
            return;
        }
        if (mPosted) {
            // Posting again would alert again.
            return;
        }
        Notification notification = new Notification.Builder(this, LedPrefs.ensureChannel(this))
                .setSmallIcon(R.drawable.ic_stat_led)
                .setContentTitle(getString(R.string.led_notification_title))
                .setContentText(getString(R.string.led_notification_text))
                .setOnlyAlertOnce(true)
                .setLocalOnly(true)
                .setShowWhen(false)
                .setCategory(Notification.CATEGORY_STATUS)
                .setVisibility(Notification.VISIBILITY_SECRET)
                .build();
        getSystemService(NotificationManager.class).notify(LedPrefs.LED_NOTIFICATION_ID,
                notification);
        mPosted = true;
    }

    private boolean hasUnread() {
        StatusBarNotification[] active = getActiveNotifications();
        if (active == null) {
            return false;
        }
        boolean onlyNew = LedPrefs.onlyNew(this);
        RankingMap rankingMap = getCurrentRanking();
        Ranking ranking = new Ranking();
        for (StatusBarNotification sbn : active) {
            String pkg = sbn.getPackageName();
            if (pkg.equals(getPackageName()) || IGNORED_PACKAGES.contains(pkg)) {
                continue;
            }
            if (sbn.isOngoing() || !sbn.isClearable()) {
                continue;
            }
            if ((sbn.getNotification().flags & Notification.FLAG_GROUP_SUMMARY) != 0) {
                // The children are counted instead.
                continue;
            }
            if (rankingMap != null && rankingMap.getRanking(sbn.getKey(), ranking)) {
                if (ranking.getImportance() < NotificationManager.IMPORTANCE_DEFAULT
                        || ranking.isSuspended()) {
                    continue;
                }
            }
            if (onlyNew && sbn.getPostTime() < mScreenOffTime) {
                continue;
            }
            return true;
        }
        return false;
    }

    private void cancelHelper() {
        if (mPosted) {
            getSystemService(NotificationManager.class).cancel(LedPrefs.LED_NOTIFICATION_ID);
            mPosted = false;
        }
    }
}
