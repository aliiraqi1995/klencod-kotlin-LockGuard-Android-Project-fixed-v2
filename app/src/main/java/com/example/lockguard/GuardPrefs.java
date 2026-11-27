package com.example.lockguard;

import android.content.Context;
import android.content.SharedPreferences;

public class GuardPrefs {
    private static final String PREF_NAME = "lockguard_prefs";
    private static final String KEY_ENABLED = "guard_enabled";

    public static boolean isGuardEnabled(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean(KEY_ENABLED, false);
    }

    public static void setGuardEnabled(Context context, boolean enabled) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putBoolean(KEY_ENABLED, enabled).apply();
    }
}
