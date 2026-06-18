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
 *
 * Mỗi entry: { "email": "...", "password": "...", "displayName": "...", "uid": "..." }
 *
 * - password = ""   → tài khoản đã đăng nhập nhưng không tick Remember
 *                     (chỉ dùng cho đổi tài khoản nhanh, không autofill)
 * - password = "xx" → tài khoản đã tick Remember
 *                     (dùng cho cả đổi tài khoản nhanh lẫn autofill email+password)
 */
public class AccountManager {

    private static final String PREF_FILE = "saved_accounts";
    private static final String KEY_LIST  = "accounts_json";

    // ── Model ────────────────────────────────────────────────────────────────

    public static class SavedAccount {
        public final String email;
        public final String password;      // "" nếu không tick Remember
        public final String displayName;
        public final String uid;

        public SavedAccount(String email, String password,
                            String displayName, String uid) {
            this.email       = email != null ? email : "";
            this.password    = password != null ? password : "";   // không bao giờ null
            this.displayName = (displayName != null && !displayName.isEmpty())
                    ? displayName : (email != null ? email : "");
            this.uid         = uid != null ? uid : "";
        }

        /** Tài khoản này có lưu password không (đã từng tick Remember). */
        public boolean hasPassword() {
            return password != null && !password.isEmpty();
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
            return ctx.getSharedPreferences(PREF_FILE + "_fb", Context.MODE_PRIVATE);
        }
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────

    /**
     * Lưu / cập nhật tài khoản (upsert theo email).
     *
     * Nếu email đã tồn tại:
     *   - Nếu account mới có password → ghi đè toàn bộ (cập nhật password mới)
     *   - Nếu account mới KHÔNG có password → giữ lại password cũ (nếu có)
     *     để không mất password đã lưu từ lần tick Remember trước
     */
    public static void saveAccount(Context ctx, SavedAccount account) {
        List<SavedAccount> list = getAll(ctx);

        // Tìm entry cũ theo email
        SavedAccount existing = null;
        for (SavedAccount a : list) {
            if (a.email.equalsIgnoreCase(account.email)) {
                existing = a;
                break;
            }
        }

        // Giữ lại password cũ nếu lần này không truyền password
        String passwordToSave = account.password;
        if (!account.hasPassword() && existing != null && existing.hasPassword()) {
            passwordToSave = existing.password;
        }

        SavedAccount merged = new SavedAccount(
                account.email,
                passwordToSave,
                account.displayName,
                account.uid
        );

        // Xóa entry cũ, thêm vào đầu danh sách (mới nhất lên trên)
        list.removeIf(a -> a.email.equalsIgnoreCase(account.email));
        list.add(0, merged);

        persist(ctx, list);
    }

    /**
     * Cập nhật password cho một email cụ thể.
     * Dùng khi người dùng đổi mật khẩu thành công.
     */
    public static void updatePassword(Context ctx, String email, String newPassword) {
        List<SavedAccount> list = getAll(ctx);
        for (int i = 0; i < list.size(); i++) {
            SavedAccount a = list.get(i);
            if (a.email.equalsIgnoreCase(email)) {
                list.set(i, new SavedAccount(a.email, newPassword, a.displayName, a.uid));
                break;
            }
        }
        persist(ctx, list);
    }

    /**
     * Xóa password của một tài khoản (khi người dùng bỏ tick Remember).
     * Vẫn giữ tài khoản trong danh sách để đổi nhanh, chỉ xóa password.
     */
    public static void clearPassword(Context ctx, String email) {
        updatePassword(ctx, email, "");
    }

    /** Xóa hoàn toàn tài khoản khỏi danh sách. */
    public static void removeAccount(Context ctx, String email) {
        List<SavedAccount> list = getAll(ctx);
        list.removeIf(a -> a.email.equalsIgnoreCase(email));
        persist(ctx, list);
    }

    /** Lấy toàn bộ danh sách tài khoản đã đăng nhập. */
    public static List<SavedAccount> getAll(Context ctx) {
        List<SavedAccount> result = new ArrayList<>();
        String json = getPrefs(ctx).getString(KEY_LIST, null);
        if (json == null) return result;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                result.add(new SavedAccount(
                        o.optString("email", ""),
                        o.optString("password", ""),       // optString thay vì getString → không crash khi null
                        o.optString("displayName", ""),
                        o.optString("uid", "")
                ));
            }
        } catch (Exception ignored) {}
        return result;
    }

    /**
     * Lấy danh sách tài khoản có lưu password (đã tick Remember).
     * Dùng cho autofill dropdown email + password kiểu Facebook.
     */
    public static List<SavedAccount> getAllWithPassword(Context ctx) {
        List<SavedAccount> result = new ArrayList<>();
        for (SavedAccount a : getAll(ctx)) {
            if (a.hasPassword()) result.add(a);
        }
        return result;
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private static void persist(Context ctx, List<SavedAccount> list) {
        try {
            JSONArray arr = new JSONArray();
            for (SavedAccount a : list) {
                JSONObject o = new JSONObject();
                o.put("email",       a.email);
                o.put("password",    a.password != null ? a.password : "");  // không bao giờ lưu null
                o.put("displayName", a.displayName);
                o.put("uid",         a.uid);
                arr.put(o);
            }
            getPrefs(ctx).edit().putString(KEY_LIST, arr.toString()).apply();
        } catch (Exception ignored) {}
    }
}