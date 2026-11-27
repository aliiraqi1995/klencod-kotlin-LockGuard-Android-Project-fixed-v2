package com.example.lockguard;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

public class IntruderDeviceAdminReceiver extends DeviceAdminReceiver {

    @Override
    public void onPasswordFailed(@NonNull Context context, @NonNull Intent intent, @NonNull UserHandle user) {
        super.onPasswordFailed(context, intent, user);
        triggerIntruderCapture(context);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onPasswordFailed(@NonNull Context context, @NonNull Intent intent) {
        super.onPasswordFailed(context, intent);
        triggerIntruderCapture(context);
    }

    private void triggerIntruderCapture(Context context) {
        if (!GuardPrefs.isGuardEnabled(context)) {
            return;
        }
        Intent serviceIntent = new Intent(context, GuardForegroundService.class);
        serviceIntent.setAction(GuardForegroundService.ACTION_TRIGGER_CAPTURE);
        ContextCompat.startForegroundService(context, serviceIntent);
    }
}
