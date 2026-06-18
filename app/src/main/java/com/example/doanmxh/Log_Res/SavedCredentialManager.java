package com.example.doanmxh.Log_Res;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SavedCredentialManager {
    private static final String PREF = "saved_credentials";

    public static void save(Context ctx, String email, String password) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .putString(email, password)
                .apply();
    }

    public static List<SavedCredential> getAll(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        List<SavedCredential> list = new ArrayList<>();
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            list.add(new SavedCredential(entry.getKey(), (String) entry.getValue()));
        }
        return list;
    }

    public static void remove(Context ctx, String email) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit().remove(email).apply();
    }

    public static class SavedCredential {
        public String email, password;
        public SavedCredential(String email, String password) {
            this.email    = email;
            this.password = password;
        }
    }
}