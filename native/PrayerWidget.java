package com.allinone.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.SystemClock;
import android.view.View;
import android.widget.RemoteViews;
import org.json.JSONObject;
import java.util.Calendar;

public class PrayerWidget extends AppWidgetProvider {
    static final String TICK = "com.allinone.app.WIDGET_TICK";
    static final String[] KEYS = {"Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha"};
    static final String[] NAMES = {"الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء"};
    static final int[] NID = {R.id.n0, R.id.n1, R.id.n2, R.id.n3, R.id.n4, R.id.n5};
    static final int[] TID = {R.id.t0, R.id.t1, R.id.t2, R.id.t3, R.id.t4, R.id.t5};

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int id : ids) update(c, m, id);
    }

    @Override
    public void onReceive(Context c, Intent i) {
        super.onReceive(c, i);
        String a = i.getAction();
        if (TICK.equals(a) || Intent.ACTION_BOOT_COMPLETED.equals(a) || Intent.ACTION_TIME_CHANGED.equals(a) || Intent.ACTION_TIMEZONE_CHANGED.equals(a)) {
            AppWidgetManager m = AppWidgetManager.getInstance(c);
            int[] ids = m.getAppWidgetIds(new ComponentName(c, PrayerWidget.class));
            for (int id : ids) update(c, m, id);
        }
    }

    static String fmt(int mins) {
        int h = mins / 60, mm = mins % 60;
        int h12 = h % 12 == 0 ? 12 : h % 12;
        return h12 + ":" + (mm < 10 ? "0" : "") + mm + (h >= 12 ? " م" : " ص");
    }

    static String keyFor(Calendar n, int add) {
        Calendar t = (Calendar) n.clone();
        t.add(Calendar.DAY_OF_MONTH, add);
        return t.get(Calendar.YEAR) + "-" + (t.get(Calendar.MONTH) + 1) + "-" + t.get(Calendar.DAY_OF_MONTH);
    }

    static void update(Context c, AppWidgetManager m, int id) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.prayer_widget);
        int[] mins = new int[6];
        int[] tm = new int[6];
        boolean ok = false, okT = false;
        try {
            String s = c.getSharedPreferences("CapacitorStorage", Context.MODE_PRIVATE).getString("prayer_widget", null);
            if (s != null) {
                JSONObject j = new JSONObject(s);
                Calendar n = Calendar.getInstance();
                JSONObject o = j.optJSONObject(keyFor(n, 0));
                if (o != null) {
                    for (int i = 0; i < 6; i++) mins[i] = o.optInt(KEYS[i], -1);
                    ok = true;
                }
                JSONObject o2 = j.optJSONObject(keyFor(n, 1));
                if (o2 != null) {
                    for (int i = 0; i < 6; i++) tm[i] = o2.optInt(KEYS[i], -1);
                    okT = true;
                }
            }
        } catch (Exception e) { ok = false; }
        Calendar now = Calendar.getInstance();
        long nowMs = now.getTimeInMillis();
        int nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        int next = -1, nextMin = -1;
        boolean tomorrow = false;
        if (ok) {
            for (int i = 0; i < 6; i++) { if (i == 1) continue; if (mins[i] > nowMin) { next = i; nextMin = mins[i]; break; } }
            if (next < 0 && okT && tm[0] >= 0) { next = 0; nextMin = tm[0]; tomorrow = true; }
        }
        long nextMs = 0;
        if (next >= 0) {
            Calendar t = Calendar.getInstance();
            t.set(Calendar.HOUR_OF_DAY, 0); t.set(Calendar.MINUTE, 0); t.set(Calendar.SECOND, 0); t.set(Calendar.MILLISECOND, 0);
            if (tomorrow) t.add(Calendar.DAY_OF_MONTH, 1);
            t.add(Calendar.MINUTE, nextMin);
            nextMs = t.getTimeInMillis();
        }
        if (ok) {
            for (int i = 0; i < 6; i++) {
                v.setTextViewText(NID[i], NAMES[i]);
                v.setTextViewText(TID[i], mins[i] >= 0 ? fmt(mins[i]) : "--");
                int col = (i == next) ? Color.parseColor("#F5B942") : Color.WHITE;
                v.setTextColor(NID[i], col);
                v.setTextColor(TID[i], col);
            }
            if (next >= 0) {
                String title = "الصلاة الجاية: " + NAMES[next] + (tomorrow ? " (بكرة) " : " ") + fmt(nextMin);
                long left = nextMs - nowMs;
                if (Build.VERSION.SDK_INT >= 24 && left > 0) {
                    v.setTextViewText(R.id.title, title);
                    v.setViewVisibility(R.id.cd, View.VISIBLE);
                    v.setChronometerCountDown(R.id.cd, true);
                    v.setChronometer(R.id.cd, SystemClock.elapsedRealtime() + left, "متبقي %s", true);
                } else {
                    long mn = Math.max(0, left / 60000);
                    v.setViewVisibility(R.id.cd, View.GONE);
                    v.setTextViewText(R.id.title, title + " — بعد " + (mn / 60) + " س " + (mn % 60) + " د");
                }
            } else {
                v.setViewVisibility(R.id.cd, View.GONE);
                v.setTextViewText(R.id.title, "خلصت صلوات النهاردة");
            }
        } else {
            for (int i = 0; i < 6; i++) { v.setTextViewText(NID[i], NAMES[i]); v.setTextViewText(TID[i], "--"); }
            v.setViewVisibility(R.id.cd, View.GONE);
            v.setTextViewText(R.id.title, "افتح التطبيق مرة عشان تظهر المواقيت");
        }
        Intent launch = c.getPackageManager().getLaunchIntentForPackage(c.getPackageName());
        if (launch != null) {
            v.setOnClickPendingIntent(R.id.root, PendingIntent.getActivity(c, 0, launch, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
        }
        m.updateAppWidget(id, v);
        try {
            long fire;
            if (next >= 0) {
                fire = nextMs + 2000;
            } else {
                Calendar t = Calendar.getInstance();
                t.set(Calendar.HOUR_OF_DAY, 0); t.set(Calendar.MINUTE, 1); t.set(Calendar.SECOND, 0); t.set(Calendar.MILLISECOND, 0);
                t.add(Calendar.DAY_OF_MONTH, 1);
                fire = t.getTimeInMillis();
            }
            AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            Intent ti = new Intent(c, PrayerWidget.class);
            ti.setAction(TICK);
            PendingIntent pi = PendingIntent.getBroadcast(c, 77, ti, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) am.set(AlarmManager.RTC_WAKEUP, fire, pi);
            else if (Build.VERSION.SDK_INT >= 23) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fire, pi);
            else am.setExact(AlarmManager.RTC_WAKEUP, fire, pi);
        } catch (Exception e) { }
    }
}
