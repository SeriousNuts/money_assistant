package com.example.myapplication;

import android.content.Context;

public final class DemoSession {
    public static final String EMAIL = "demo@money.local";
    public static final String PASSWORD = "demo123";
    private static final String PREF = "demo_session";
    private DemoSession() { }

    public static boolean active(Context context) {
        return context.getSharedPreferences(PREF, 0).getBoolean("active", false);
    }

    public static void set(Context context, boolean demo) {
        context.getSharedPreferences(PREF, 0).edit().putBoolean("active", demo).apply();
    }
}
