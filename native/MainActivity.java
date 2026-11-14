package com.allinone.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(WidgetPin.class);
        super.onCreate(savedInstanceState);
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                Uri u = Uri.parse("android.resource://" + getPackageName() + "/raw/adhan");
                AudioAttributes aa = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build();
                NotificationChannel ch = new NotificationChannel("adhan_alarm", "الأذان (منبّه)", NotificationManager.IMPORTANCE_HIGH);
                ch.setDescription("صوت الأذان وقت الصلاة، بيرن حتى لو الموبايل صامت");
                ch.setSound(u, aa);
                ch.enableVibration(true);
                ch.setLockscreenVisibility(1);
                nm.createNotificationChannel(ch);
            }
        } catch (Exception e) { }
    }
}
