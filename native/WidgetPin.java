package com.allinone.app;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.os.Build;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.provider.Settings;
import androidx.core.content.FileProvider;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@CapacitorPlugin(name = "WidgetPin")
public class WidgetPin extends Plugin {

    @PluginMethod
    public void info(PluginCall call) {
        try {
            PackageInfo pi = getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0);
            JSObject o = new JSObject();
            long code = Build.VERSION.SDK_INT >= 28 ? pi.getLongVersionCode() : pi.versionCode;
            o.put("code", code);
            o.put("name", pi.versionName);
            call.resolve(o);
        } catch (Exception e) {
            call.reject(String.valueOf(e.getMessage()));
        }
    }

    @PluginMethod
    public void pin(PluginCall call) {
        try {
            AppWidgetManager m = AppWidgetManager.getInstance(getContext());
            ComponentName cn = new ComponentName(getContext(), PrayerWidget.class);
            if (Build.VERSION.SDK_INT >= 26 && m.isRequestPinAppWidgetSupported()) {
                m.requestPinAppWidget(cn, null, null);
                call.resolve();
            } else {
                call.reject("unsupported");
            }
        } catch (Exception e) {
            call.reject(String.valueOf(e.getMessage()));
        }
    }

    @PluginMethod
    public void install(final PluginCall call) {
        final String url = call.getString("url");
        if (url == null || url.length() == 0) { call.reject("no url"); return; }
        final Context ctx = getContext();
        if (Build.VERSION.SDK_INT >= 26 && !ctx.getPackageManager().canRequestPackageInstalls()) {
            try {
                Intent s = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + ctx.getPackageName()));
                s.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(s);
            } catch (Exception e) { }
            call.reject("need_permission");
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File dir = new File(ctx.getCacheDir(), "apk");
                    dir.mkdirs();
                    File out = new File(dir, "update.apk");
                    if (out.exists()) out.delete();
                    HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
                    con.setInstanceFollowRedirects(true);
                    con.setConnectTimeout(20000);
                    con.setReadTimeout(30000);
                    con.connect();
                    int rc = con.getResponseCode();
                    if (rc != 200) throw new Exception("http " + rc);
                    long total = con.getContentLengthLong();
                    InputStream in = con.getInputStream();
                    FileOutputStream fo = new FileOutputStream(out);
                    byte[] buf = new byte[32768];
                    long got = 0;
                    int last = -1, n;
                    while ((n = in.read(buf)) > 0) {
                        fo.write(buf, 0, n);
                        got += n;
                        if (total > 0) {
                            int p = (int) (got * 100 / total);
                            if (p != last) {
                                last = p;
                                JSObject e = new JSObject();
                                e.put("pct", p);
                                notifyListeners("dl", e);
                            }
                        }
                    }
                    fo.close();
                    in.close();
                    Uri u = FileProvider.getUriForFile(ctx, ctx.getPackageName() + ".fileprovider", out);
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setDataAndType(u, "application/vnd.android.package-archive");
                    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                    ctx.startActivity(i);
                    call.resolve();
                } catch (Exception e) {
                    call.reject(String.valueOf(e.getMessage()));
                }
            }
        }).start();
    }
}
