package com.hotspotkeeper;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.provider.Settings;

import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;

public class HotspotHelper {

    /** Hotspot ON hai ya nahi, ye pata lagane ki koshish. */
    public static boolean isHotspotOn(Context ctx) {
        try {
            WifiManager wm = (WifiManager) ctx.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
            Method m = wm.getClass().getDeclaredMethod("isWifiApEnabled");
            m.setAccessible(true);
            Object r = m.invoke(wm);
            if (r instanceof Boolean && (Boolean) r) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        return hasApInterface();
    }

    /** Hotspot ke network interface (ap0, swlan0, wlan1...) ko IP ke saath dhundhna. */
    private static boolean hasApInterface() {
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                String n = ni.getName().toLowerCase();
                boolean ap = n.startsWith("ap") || n.startsWith("swlan")
                        || n.startsWith("softap") || n.equals("wlan1") || n.startsWith("wigig");
                if (!ap || !ni.isUp()) {
                    continue;
                }
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (a instanceof Inet4Address) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    /** Hotspot ON karne ki koshish. Sirf kuch purane/allowed devices par chalega. */
    public static boolean tryEnable(Context ctx) {
        try {
            WifiManager wm = (WifiManager) ctx.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
            Method m = wm.getClass().getMethod("setWifiApEnabled",
                    WifiConfiguration.class, boolean.class);
            Object r = m.invoke(wm, null, true);
            return r instanceof Boolean && (Boolean) r;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Hotspot/Tethering settings screen kholna. */
    public static void openSettings(Context ctx) {
        try {
            Intent i = new Intent();
            i.setComponent(new ComponentName("com.android.settings",
                    "com.android.settings.TetherSettings"));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
        } catch (Throwable t) {
            try {
                Intent i = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(i);
            } catch (Throwable ignored) {
            }
        }
    }
}
