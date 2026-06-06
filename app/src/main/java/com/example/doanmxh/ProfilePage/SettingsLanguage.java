package com.example.doanmxh.ProfilePage;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsLanguage {
    private static final String PREF_SETTINGS = "app_settings";
    private static final String KEY_LANGUAGE = "settings_language";

    public static final String VI = "vi";
    public static final String EN = "en";

    private SettingsLanguage() {
    }

    public static String get(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_SETTINGS, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LANGUAGE, VI);
    }

    public static void set(Context context, String language) {
        context.getSharedPreferences(PREF_SETTINGS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LANGUAGE, EN.equals(language) ? EN : VI)
                .apply();
    }

    public static boolean isEnglish(Context context) {
        return EN.equals(get(context));
    }

    public static String languageName(Context context) {
        return isEnglish(context) ? "English" : "Tiếng Việt";
    }
}
