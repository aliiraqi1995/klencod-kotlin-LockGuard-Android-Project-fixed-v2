package com.example.lockguard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.content.ContextCompat;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)) {
            if (GuardPrefs.isGuardEnabled(context)) {
                Intent serviceIntent = new Intent(context, GuardForegroundService.class);
                ContextCompat.startForegroundService(context, serviceIntent);
            }
        }
    }
}
