package com.example.cs364_project;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {

            SharedPreferences prefs = context.getSharedPreferences("reminder_prefs", Context.MODE_PRIVATE);
            boolean isOn = prefs.getBoolean("reminder_on", false);
            long intervalMillis = prefs.getLong("interval_millis", 0L);

            if (isOn && intervalMillis > 0) {
                ReminderScheduler.scheduleRepeatingAlarm(context, intervalMillis);
            }
        }
    }
}
