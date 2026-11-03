package com.hotspotkeeper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    static final String EXTRA_OPEN_HOTSPOT = "open_hotspot";

    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int pad = (int) (20 * getResources().getDisplayMetrics().density);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("Hotspot Keeper");
        title.setTextSize(26);
        root.addView(title);

        status = new TextView(this);
        status.setTextSize(16);
        status.setPadding(0, pad / 2, 0, pad / 2);
        root.addView(status);

        addButton(root, "Monitoring START", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setEnabled(true);
                startMonitor();
                refresh();
            }
        });
        addButton(root, "Monitoring STOP", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setEnabled(false);
                stopService(new Intent(MainActivity.this, MonitorService.class));
                refresh();
            }
        });
        addButton(root, "Hotspot Settings kholein", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HotspotHelper.openSettings(MainActivity.this);
            }
        });
        addButton(root, "Battery Optimization band karein", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openBatterySettings();
            }
        });

        TextView note = new TextView(this);
        note.setTextSize(13);
        note.setPadding(0, pad, 0, 0);
        note.setText("Dhyan dein: Android 10+ par normal app Hotspot chupchap ON nahi kar sakti. "
                + "Jahan Android allow karta hai wahan auto ON hoga, baaki devices par Hotspot OFF "
                + "hote hi notification aayega, usse tap karke Hotspot ON kar sakte hain.\n\n"
                + "Best result ke liye: Battery Optimization band karein aur phone ki App Settings me "
                + "Autostart / Background activity allow karein.");
        root.addView(note);

        ScrollView sv = new ScrollView(this);
        sv.addView(root);
        setContentView(sv);

        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
        }

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.getBooleanExtra(EXTRA_OPEN_HOTSPOT, false)) {
            intent.removeExtra(EXTRA_OPEN_HOTSPOT);
            HotspotHelper.openSettings(this);
        }
    }

    private void addButton(LinearLayout parent, String label, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setOnClickListener(l);
        parent.addView(b);
    }

    private SharedPreferences prefs() {
        return getSharedPreferences("hk", Context.MODE_PRIVATE);
    }

    private void setEnabled(boolean enabled) {
        prefs().edit().putBoolean("enabled", enabled).apply();
    }

    private void startMonitor() {
        Intent s = new Intent(this, MonitorService.class);
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                startForegroundService(s);
            } else {
                startService(s);
            }
        } catch (Throwable t) {
            Toast.makeText(this, "Service start nahi hui: " + t.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void openBatterySettings() {
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null && pm.isIgnoringBatteryOptimizations(getPackageName())) {
                Toast.makeText(this, "Battery optimization pehle se band hai",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            i.setData(Uri.parse("package:" + getPackageName()));
            startActivity(i);
        } catch (Throwable t) {
            try {
                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            } catch (Throwable ignored) {
            }
        }
    }

    private void refresh() {
        boolean enabled = prefs().getBoolean("enabled", false);
        boolean on = HotspotHelper.isHotspotOn(this);
        status.setText("Monitoring: " + (enabled ? "ON" : "OFF")
                + "\nHotspot abhi: " + (on ? "ON" : "OFF"));
    }
}
