package com.hotspotkeeper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

public class MonitorService extends Service {

    static final String CH_MON = "hk_monitor";
    static final String CH_ALERT = "hk_alert";
    static final int ID_MON = 1;
    static final int ID_ALERT = 2;
    static final long INTERVAL_MS = 10_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable loop = new Runnable() {
        @Override
        public void run() {
            check();
            handler.postDelayed(this, INTERVAL_MS);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createChannels();
        Notification n = buildMonitorNotification("Monitoring shuru ho rahi hai...");
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(ID_MON, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(ID_MON, n);
        }
        handler.removeCallbacks(loop);
        handler.post(loop);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(loop);
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.cancel(ID_ALERT);
        }
        super.onDestroy();
    }

    private void check() {
        boolean on = HotspotHelper.isHotspotOn(this);
        if (!on) {
            HotspotHelper.tryEnable(this);
            on = HotspotHelper.isHotspotOn(this);
        }
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) {
            return;
        }
        nm.notify(ID_MON, buildMonitorNotification(on ? "Hotspot ON hai" : "Hotspot OFF hai"));
        if (on) {
            nm.cancel(ID_ALERT);
        } else {
            nm.notify(ID_ALERT, buildAlertNotification());
        }
    }

    private PendingIntent openAppIntent(boolean openHotspotSettings) {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (openHotspotSettings) {
            i.putExtra(MainActivity.EXTRA_OPEN_HOTSPOT, true);
        }
        return PendingIntent.getActivity(this, openHotspotSettings ? 1 : 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private Notification buildMonitorNotification(String text) {
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CH_MON)
                : new Notification.Builder(this);
        b.setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Hotspot Keeper")
                .setContentText(text)
                .setOngoing(true)
                .setContentIntent(openAppIntent(false));
        return b.build();
    }

    private Notification buildAlertNotification() {
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CH_ALERT)
                : new Notification.Builder(this);
        b.setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Hotspot OFF ho gaya hai")
                .setContentText("Hotspot ON karne ke liye yahan tap karein")
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(openAppIntent(true));
        if (Build.VERSION.SDK_INT < 26) {
            b.setPriority(Notification.PRIORITY_HIGH);
        }
        return b.build();
    }

    private void createChannels() {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) {
            return;
        }
        nm.createNotificationChannel(new NotificationChannel(
                CH_MON, "Hotspot monitoring", NotificationManager.IMPORTANCE_LOW));
        nm.createNotificationChannel(new NotificationChannel(
                CH_ALERT, "Hotspot OFF alert", NotificationManager.IMPORTANCE_HIGH));
    }
}
