package com.example.doanmxh.Log_Res;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Lưu danh sách tài khoản đã đăng nhập vào EncryptedSharedPreferences.
 * Mỗi entry: { "email": "...", "password": "...", "displayName": "...", "uid": "..." }
 */
public class AccountManager {

    private static final String PREF_FILE  = "saved_accounts";
    private static final String KEY_LIST   = "accounts_json";

    // ── Model ────────────────────────────────────────────────────────────────

    public static class SavedAccount {
        public final String email;
        public final String password;
        public final String displayName;   // tên hiển thị (hoặc email nếu chưa có)
        public final String uid;

        public SavedAccount(String email, String password,
                            String displayName, String uid) {
            this.email       = email;
            this.password    = password;
            this.displayName = (displayName != null && !displayName.isEmpty())
                    ? displayName : email;
            this.uid         = uid;
        }
    }

    // ── SharedPreferences (encrypted) ────────────────────────────────────────

    private static SharedPreferences getPrefs(Context ctx) {
        try {
            MasterKey masterKey = new MasterKey.Builder(ctx)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            return EncryptedSharedPreferences.create(
                    ctx,
                    PREF_FILE,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            // fallback (không nên xảy ra trên thiết bị bình thường)
            return ctx.getSharedPreferences(PREF_FILE + "_fb", Context.MODE_PRIVATE);
        }
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────

    /** Lưu / cập nhật tài khoản (upsert theo email). */
    public static void saveAccount(Context ctx, SavedAccount account) {
        List<SavedAccount> list = getAll(ctx);

        // Xóa entry cũ nếu email đã tồn tại
        list.removeIf(a -> a.email.equalsIgnoreCase(account.email));
        // Thêm vào đầu danh sách (mới nhất lên trên)
        list.add(0, account);

        persist(ctx, list);
    }

    /** Xóa tài khoản khỏi danh sách đã lưu. */
    public static void removeAccount(Context ctx, String email) {
        List<SavedAccount> list = getAll(ctx);
        list.removeIf(a -> a.email.equalsIgnoreCase(email));
        persist(ctx, list);
    }

    /** Lấy toàn bộ danh sách tài khoản đã lưu. */
    public static List<SavedAccount> getAll(Context ctx) {
        List<SavedAccount> result = new ArrayList<>();
        String json = getPrefs(ctx).getString(KEY_LIST, null);
        if (json == null) return result;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                result.add(new SavedAccount(
                        o.getString("email"),
                        o.getString("password"),
                        o.optString("displayName", ""),
                        o.optString("uid", "")
                ));
            }
        } catch (Exception ignored) {}
        return result;
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private static void persist(Context ctx, List<SavedAccount> list) {
        try {
            JSONArray arr = new JSONArray();
            for (SavedAccount a : list) {
                JSONObject o = new JSONObject();
                o.put("email",       a.email);
                o.put("password",    a.password);
                o.put("displayName", a.displayName);
                o.put("uid",         a.uid);
                arr.put(o);
            }
            getPrefs(ctx).edit().putString(KEY_LIST, arr.toString()).apply();
        } catch (Exception ignored) {}
    }
}