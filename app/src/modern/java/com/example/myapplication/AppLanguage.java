package com.example.myapplication;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;

import java.util.Locale;

/** Explicit in-app language choice; Russian remains the fallback for existing users. */
final class AppLanguage {
    private static final String PREFS = "app-language";

    static String selected(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("language", "ru");
    }

    static Context wrap(Context context) {
        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        configuration.setLocale(new Locale(selected(context)));
        return context.createConfigurationContext(configuration);
    }

    static void change(Activity activity, String language) {
        if (!"ru".equals(language) && !"en".equals(language)) throw new IllegalArgumentException("Unsupported language");
        if (selected(activity).equals(language)) return;
        if (!activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("language", language).commit()) throw new IllegalStateException("Could not save language");
        activity.startActivity(new Intent(activity, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        activity.finish();
    }

    private AppLanguage() { }
}
