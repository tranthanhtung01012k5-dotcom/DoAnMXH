package com.example.doanmxh.ProfilePage;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.example.doanmxh.Log_Res.AccountManager;
import com.example.doanmxh.Log_Res.AccountSwitcherFragment;
import com.example.doanmxh.Log_Res.LoginActivity;
import com.example.doanmxh.MainActivity;
import com.example.doanmxh.R;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SettingsDialog {
    private static final String PREF_SETTINGS    = "app_settings";
    private static final String KEY_DARK_MODE    = "dark_mode";
    private static final String KEY_NOTIFICATIONS = "notifications_enabled";

    private final Fragment       host;
    private final FirebaseAuth   auth;
    private final FirebaseFirestore db;

    public SettingsDialog(Fragment host, FirebaseAuth auth, FirebaseFirestore db) {
        this.host = host;
        this.auth = auth;
        this.db   = db;
    }

    public void show() {
        if (host.getContext() == null) return;

        Context context = host.requireContext();
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setContentView(R.layout.dialog_settings);

        // ── Views ──────────────────────────────────────────────────────────────
        View      settingsRoot          = dialog.findViewById(R.id.settingsRoot);
        ImageView btnClose              = dialog.findViewById(R.id.btnCloseSettings);
        TextView  txtSettingsTitle      = dialog.findViewById(R.id.txtSettingsTitle);
        TextView  rowChangePassword     = dialog.findViewById(R.id.rowChangePassword);
        TextView  rowLikedPosts         = dialog.findViewById(R.id.rowLikedPosts);
        TextView  rowLikedComments      = dialog.findViewById(R.id.rowLikedComments);
        TextView  rowSavedPosts         = dialog.findViewById(R.id.rowSavedPosts);
        TextView  rowArchive            = dialog.findViewById(R.id.rowArchive);
        TextView  rowBlockedUsers       = dialog.findViewById(R.id.rowBlockedUsers);
        TextView  rowAccountStatus      = dialog.findViewById(R.id.rowAccountStatus);
        TextView  rowTerms              = dialog.findViewById(R.id.rowTerms);
        TextView  rowPrivacyPolicy      = dialog.findViewById(R.id.rowPrivacyPolicy);
//        TextView  rowLanguage           = dialog.findViewById(R.id.rowLanguage);
        TextView  rowAbout              = dialog.findViewById(R.id.rowAbout);
        TextView  rowLogout             = dialog.findViewById(R.id.rowLogout);
        TextView  txtLightModeLabel     = dialog.findViewById(R.id.txtLightModeLabel);
        TextView  txtPrivateAccountLabel= dialog.findViewById(R.id.txtPrivateAccountLabel);
        TextView  txtNotificationsLabel = dialog.findViewById(R.id.txtNotificationsLabel);

        // ✅ Row chuyển tài khoản
        TextView rowSwitchAccount = dialog.findViewById(R.id.rowSwitchAccount);

        TextView[] sectionTitles = new TextView[]{
                dialog.findViewById(R.id.txtSectionAccountInfo),
                dialog.findViewById(R.id.txtSectionMyActivity),
                dialog.findViewById(R.id.txtSectionContent),
                dialog.findViewById(R.id.txtSectionPrivacy),
                dialog.findViewById(R.id.txtSectionAccountStatus),
                dialog.findViewById(R.id.txtSectionAppearance),
                dialog.findViewById(R.id.txtSectionNotifications),
                dialog.findViewById(R.id.txtSectionSupport),
                dialog.findViewById(R.id.txtSectionOther),
                dialog.findViewById(R.id.txtSectionLogout)
        };

        MaterialSwitch switchPrivateAccount = dialog.findViewById(R.id.switchPrivateAccount);
        MaterialSwitch switchDarkMode       = dialog.findViewById(R.id.switchDarkMode);
        MaterialSwitch switchNotifications  = dialog.findViewById(R.id.switchNotifications);

        View[] sections = new View[]{
                dialog.findViewById(R.id.sectionAccountInfo),
                dialog.findViewById(R.id.sectionMyActivity),
                dialog.findViewById(R.id.sectionContent),
                dialog.findViewById(R.id.sectionPrivacy),
                dialog.findViewById(R.id.sectionAccountStatus),
                dialog.findViewById(R.id.sectionAppearance),
                dialog.findViewById(R.id.sectionNotifications),
                dialog.findViewById(R.id.sectionSupport),
                dialog.findViewById(R.id.sectionOther),
                dialog.findViewById(R.id.sectionLogout)
        };
        View[] switchRows = new View[]{
                dialog.findViewById(R.id.rowPrivateAccount),
                dialog.findViewById(R.id.rowDarkMode),
                dialog.findViewById(R.id.rowNotifications)
        };

        // ── Theme ──────────────────────────────────────────────────────────────
        boolean darkMode = isDarkModeEnabled(context);
        applyTheme(context, settingsRoot, btnClose, txtSettingsTitle,
                new TextView[]{
                        rowChangePassword, rowLikedPosts, rowLikedComments,
                        rowSavedPosts, rowArchive, rowBlockedUsers, rowAccountStatus,
                        rowTerms, rowPrivacyPolicy, rowAbout, rowSwitchAccount
                },
                rowLogout, sections, switchRows, darkMode);

        // ── Language texts ─────────────────────────────────────────────────────
//        applyLanguageTexts(context, txtSettingsTitle, sectionTitles,
//                rowChangePassword, rowLikedPosts, rowLikedComments,
//                rowSavedPosts, rowArchive, rowBlockedUsers, rowAccountStatus,
//                rowTerms, rowPrivacyPolicy, rowLanguage, rowAbout, rowLogout,
//                txtLightModeLabel, txtPrivateAccountLabel, txtNotificationsLabel,
//                rowSwitchAccount);

        // ── Listeners ──────────────────────────────────────────────────────────
        btnClose.setOnClickListener(v -> dialog.dismiss());
        loadPrivateAccountSwitch(switchPrivateAccount);

        switchDarkMode.setChecked(darkMode);
        switchDarkMode.setOnCheckedChangeListener((btn, isChecked) -> {
            setDarkModeEnabled(context, isChecked);
            AppCompatDelegate.setDefaultNightMode(isChecked
                    ? AppCompatDelegate.MODE_NIGHT_YES
                    : AppCompatDelegate.MODE_NIGHT_NO);
        });

        switchNotifications.setChecked(isNotificationsEnabled(context));
        switchNotifications.setOnCheckedChangeListener((btn, isChecked) ->
                setNotificationsEnabled(context, isChecked));

        rowChangePassword.setOnClickListener(v ->
                host.startActivity(new Intent(context, ChangePasswordActivity.class)));
        rowLikedPosts.setOnClickListener(v ->
                host.startActivity(new Intent(context, LikedPostsActivity.class)));
        rowLikedComments.setOnClickListener(v ->
                host.startActivity(new Intent(context, LikedCommentsActivity.class)));
        rowSavedPosts.setOnClickListener(v ->
                host.startActivity(new Intent(context, SavedPostsActivity.class)));
        rowArchive.setOnClickListener(v ->
                host.startActivity(new Intent(context, ArchivedPostsActivity.class)));
        rowBlockedUsers.setOnClickListener(v ->
                host.startActivity(new Intent(context, BlockedUsersActivity.class)));
        rowAccountStatus.setOnClickListener(v ->
                new AccountStatusDialog(host, auth, db).show());
        rowTerms.setOnClickListener(v -> showTermsDialog());
        rowPrivacyPolicy.setOnClickListener(v -> showPrivacyPolicyDialog());
//        rowLanguage.setOnClickListener(v -> showLanguageDialog(context, dialog));
        rowAbout.setOnClickListener(v -> showAboutDialog());

        // ✅ Chuyển tài khoản
        rowSwitchAccount.setOnClickListener(v -> {
            dialog.dismiss();
            List<AccountManager.SavedAccount> saved =
                    AccountManager.getAll(context);
            if (saved.isEmpty()) {
                // Chưa lưu tài khoản nào → thông báo nhỏ
                boolean english = SettingsLanguage.isEnglish(context);
                Toast.makeText(context,
                        english
                                ? "No saved accounts. Log in and tick Remember."
                                : "Chưa có tài khoản nào được lưu. Hãy đăng nhập và tick Ghi nhớ.",
                        Toast.LENGTH_LONG).show();
            } else {
                AccountSwitcherFragment
                        .newInstance()
                        .show(host.getChildFragmentManager(), "account_switcher");
            }
        });

        rowLogout.setOnClickListener(v -> {
            dialog.dismiss();
            logout();
        });

        // ── Window ─────────────────────────────────────────────────────────────
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.getDecorView().setPadding(0, 0, 0, 0);
            window.setGravity(Gravity.CENTER);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params = window.getAttributes();
            params.dimAmount = 0.68f;
            window.setAttributes(params);
            int width  = (int) (context.getResources().getDisplayMetrics().widthPixels  * 0.86f);
            int height = (int) (context.getResources().getDisplayMetrics().heightPixels * 0.68f);
            window.setLayout(width, height);
        }
    }

    // ── Dark mode helpers ──────────────────────────────────────────────────────

    public static boolean isDarkModeEnabled(Context context) {
        return context.getSharedPreferences(PREF_SETTINGS, Context.MODE_PRIVATE)
                .getBoolean(KEY_DARK_MODE, true);
    }

    private static void setDarkModeEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREF_SETTINGS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_DARK_MODE, enabled).apply();
    }

    public static boolean isNotificationsEnabled(Context context) {
        return context.getSharedPreferences(PREF_SETTINGS, Context.MODE_PRIVATE)
                .getBoolean(KEY_NOTIFICATIONS, true);
    }

    public static void setNotificationsEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREF_SETTINGS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply();
        if (context instanceof MainActivity) {
            ((MainActivity) context).refreshNotificationBadge();
        }
    }


    // ── Private account ────────────────────────────────────────────────────────

    private void loadPrivateAccountSwitch(MaterialSwitch sw) {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();
        db.collection("nguoi_dung").document(uid).get()
                .addOnSuccessListener(doc -> {
                    sw.setOnCheckedChangeListener(null);
                    sw.setChecked(Boolean.TRUE.equals(doc.getBoolean("private")));
                    sw.setOnCheckedChangeListener((btn, checked) ->
                            updatePrivateAccount(uid, checked));
                })
                .addOnFailureListener(e ->
                        sw.setOnCheckedChangeListener((btn, checked) ->
                                updatePrivateAccount(uid, checked)));
    }

    private void updatePrivateAccount(String uid, boolean isPrivate) {
        db.collection("nguoi_dung").document(uid)
                .update("private", isPrivate)
                .addOnFailureListener(e ->
                        Toast.makeText(host.getContext(),
                                "Không thể cập nhật quyền riêng tư",
                                Toast.LENGTH_SHORT).show());
    }

    // ── Language & text ────────────────────────────────────────────────────────

    private void applyLanguageTexts(
            Context context,
            TextView txtSettingsTitle,
            TextView[] sectionTitles,
            TextView rowChangePassword,
            TextView rowLikedPosts,
            TextView rowLikedComments,
            TextView rowSavedPosts,
            TextView rowArchive,
            TextView rowBlockedUsers,
            TextView rowAccountStatus,
            TextView rowTerms,
            TextView rowPrivacyPolicy,
            TextView rowLanguage,
            TextView rowAbout,
            TextView rowLogout,
            TextView txtLightModeLabel,
            TextView txtPrivateAccountLabel,
            TextView txtNotificationsLabel,
            TextView rowSwitchAccount          // ✅ thêm tham số mới
    ) {
        boolean english = SettingsLanguage.isEnglish(context);

        txtSettingsTitle.setText(english ? "Settings" : "Cài đặt");

        String[] sectionText = english
                ? new String[]{"Account", "My activity", "Your content", "Privacy",
                "Account status", "Appearance", "Notifications",
                "Support", "Other settings", "Account"}
                : new String[]{"Thông tin tài khoản", "Hoạt động của tôi",
                "Nội dung của bạn", "Quyền riêng tư",
                "Trạng thái tài khoản", "Giao diện", "Thông báo",
                "Hỗ trợ", "Cài đặt khác", "Tài khoản"};
        for (int i = 0; i < sectionTitles.length && i < sectionText.length; i++) {
            sectionTitles[i].setText(sectionText[i]);
        }

        rowChangePassword.setText(english ? "Change password"             : "Đổi mật khẩu");
        rowLikedPosts.setText(english     ? "Liked posts"                 : "Bài viết đã thích");
        rowLikedComments.setText(english  ? "Liked comments"              : "Bình luận đã thích");
        rowSavedPosts.setText(english     ? "Saved"                       : "Đã lưu");
        rowArchive.setText(english        ? "Archive"                     : "Kho lưu trữ");
        rowBlockedUsers.setText(english   ? "Blocked users"               : "Chặn người dùng");
        rowAccountStatus.setText(english  ? "Login and profile information": "Thông tin đăng nhập và hồ sơ");
        rowTerms.setText(english          ? "Terms of Use"                : "Điều khoản sử dụng");
        rowPrivacyPolicy.setText(english  ? "Privacy Policy"              : "Chính sách bảo mật");
        rowLanguage.setText((english      ? "Language" : "Ngôn ngữ") + ": " + SettingsLanguage.languageName(context));
        rowAbout.setText(english          ? "About"                       : "Giới thiệu");
        rowLogout.setText(english         ? "Log out"                     : "Đăng xuất");
        txtLightModeLabel.setText(english         ? "Light mode"              : "Chế độ sáng");
        txtPrivateAccountLabel.setText(english    ? "Private account"         : "Tài khoản riêng tư");
        txtNotificationsLabel.setText(english     ? "Enable notifications"    : "Bật thông báo");

        // ✅ Text cho row chuyển tài khoản
        rowSwitchAccount.setText(english ? "Switch account" : "Chuyển tài khoản");
    }

    // ── Dialogs ────────────────────────────────────────────────────────────────

    private void showAboutDialog() {
        if (host.getContext() == null) return;
        boolean english = SettingsLanguage.isEnglish(host.requireContext());
        new AlertDialog.Builder(host.requireContext())
                .setTitle(english ? "About" : "Giới thiệu")
                .setMessage(english
                        ? "Threads Clone\nAndroid social networking app using Firebase Authentication and Firestore."
                        : "Threads Clone\nỨng dụng mạng xã hội Android dùng Firebase Authentication và Firestore.")
                .setPositiveButton(english ? "Close" : "Đóng", null)
                .show();
    }

    private void showTermsDialog() {
        if (host.getContext() == null) return;
        boolean english = SettingsLanguage.isEnglish(host.requireContext());
        String message = english
                ? "1. Account usage\nYou are responsible for posts, comments, photos and actions from your account.\n\n"
                + "2. Prohibited content\nDo not post harassment, impersonation, spam, scams, privacy violations or illegal content.\n\n"
                + "3. Community interaction\nRespect other users. Abuse of follows, comments or reposts may be restricted.\n\n"
                + "4. Post management\nYou can edit, delete, save or archive posts using app features.\n\n"
                + "5. Terms updates\nThese terms may be updated when the app changes."
                : "1. Sử dụng tài khoản\nBạn chịu trách nhiệm với nội dung đăng tải, bình luận, ảnh và các hoạt động từ tài khoản của mình.\n\n"
                + "2. Nội dung không được phép\nKhông đăng nội dung xúc phạm, giả mạo, spam, lừa đảo, vi phạm quyền riêng tư hoặc nội dung trái pháp luật.\n\n"
                + "3. Tương tác trong cộng đồng\nHãy tôn trọng người dùng khác. Các hành vi quấy rối, đe dọa hoặc lợi dụng tính năng theo dõi, bình luận, đăng lại có thể bị hạn chế.\n\n"
                + "4. Quản lý bài viết\nBạn có thể chỉnh sửa, xóa, lưu hoặc lưu trữ bài viết theo các chức năng mà ứng dụng cung cấp.\n\n"
                + "5. Thay đổi điều khoản\nĐiều khoản có thể được cập nhật để phù hợp với thay đổi của ứng dụng.";
        new AlertDialog.Builder(host.requireContext())
                .setTitle(english ? "Terms of Use" : "Điều khoản sử dụng")
                .setMessage(message)
                .setPositiveButton(english ? "Close" : "Đóng", null)
                .show();
    }

    private void showPrivacyPolicyDialog() {
        if (host.getContext() == null) return;
        boolean english = SettingsLanguage.isEnglish(host.requireContext());
        String message = english
                ? "1. Account data\nThe app stores profile information such as name, username, email, avatar, bio and privacy status.\n\n"
                + "2. Activity data\nPosts, comments, likes, follows, saved posts, archived posts and blocked users are stored for app features.\n\n"
                + "3. Password\nPasswords are handled by Firebase Authentication.\n\n"
                + "4. Privacy\nYou can switch public/private account status and block users in Settings.\n\n"
                + "5. Data protection\nData is used for app experience and should not be shared with third parties outside app operation."
                : "1. Dữ liệu tài khoản\nỨng dụng lưu thông tin hồ sơ như tên, username, email, ảnh đại diện, tiểu sử và trạng thái riêng tư.\n\n"
                + "2. Dữ liệu hoạt động\nBài viết, bình luận, lượt thích, danh sách theo dõi, bài đã lưu, bài lưu trữ và danh sách người bị chặn được lưu để phục vụ các chức năng trong app.\n\n"
                + "3. Mật khẩu\nMật khẩu được xử lý qua Firebase Authentication.\n\n"
                + "4. Quyền riêng tư\nBạn có thể chuyển tài khoản công khai/riêng tư và chặn người dùng trong phần Cài đặt.\n\n"
                + "5. Bảo vệ dữ liệu\nDữ liệu được dùng cho trải nghiệm trong ứng dụng và không nên chia sẻ cho bên thứ ba ngoài mục đích vận hành hệ thống.";
        new AlertDialog.Builder(host.requireContext())
                .setTitle(english ? "Privacy Policy" : "Chính sách bảo mật")
                .setMessage(message)
                .setPositiveButton(english ? "Close" : "Đóng", null)
                .show();
    }

    private void showLanguageDialog(Context context, Dialog settingsDialog) {
        boolean english  = SettingsLanguage.isEnglish(context);
        boolean darkMode = isDarkModeEnabled(context);
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setContentView(R.layout.dialog_language);

        View     languageRoot       = dialog.findViewById(R.id.languageRoot);
        View     rowVietnamese      = dialog.findViewById(R.id.rowVietnamese);
        View     rowEnglish         = dialog.findViewById(R.id.rowEnglish);
        TextView txtLanguageTitle   = dialog.findViewById(R.id.txtLanguageTitle);
        TextView txtVietnameseCheck = dialog.findViewById(R.id.txtVietnameseCheck);
        TextView txtEnglishCheck    = dialog.findViewById(R.id.txtEnglishCheck);
        TextView txtVietnamese      = dialog.findViewById(R.id.txtVietnamese);
        TextView txtEnglish         = dialog.findViewById(R.id.txtEnglish);

        int backgroundColor  = Color.parseColor(darkMode ? "#101010" : "#FFFFFF");
        int primaryTextColor = Color.parseColor(darkMode ? "#FFFFFF" : "#111111");
        int secondaryColor   = Color.parseColor(darkMode ? "#8E8E93" : "#6E6E73");

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(backgroundColor);
        bg.setCornerRadius(24f * context.getResources().getDisplayMetrics().density);
        languageRoot.setBackground(bg);

        txtLanguageTitle.setText(english ? "Language" : "Ngôn ngữ");
        txtLanguageTitle.setTextColor(primaryTextColor);
        txtVietnamese.setTextColor(primaryTextColor);
        txtEnglish.setTextColor(primaryTextColor);
        txtVietnameseCheck.setTextColor(english ? secondaryColor : primaryTextColor);
        txtEnglishCheck.setTextColor(english ? primaryTextColor : secondaryColor);
        txtVietnameseCheck.setText(english ? "○" : "●");
        txtEnglishCheck.setText(english ? "●" : "○");

        rowVietnamese.setOnClickListener(v -> changeLanguage(context, settingsDialog, dialog, SettingsLanguage.VI));
        rowEnglish.setOnClickListener(v -> changeLanguage(context, settingsDialog, dialog, SettingsLanguage.EN));

        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.getDecorView().setPadding(0, 0, 0, 0);
            window.setGravity(Gravity.CENTER);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params = window.getAttributes();
            params.dimAmount = 0.48f;
            window.setAttributes(params);
            int width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.78f);
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void changeLanguage(Context context, Dialog settingsDialog,
                                Dialog languageDialog, String language) {
        SettingsLanguage.set(context, language);
        languageDialog.dismiss();
        settingsDialog.dismiss();
        show();
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    private void logout() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            Map<String, Object> offlineData = new HashMap<>();
            offlineData.put("status", "offline");
            offlineData.put("lastSeen", ServerValue.TIMESTAMP);
            FirebaseDatabase.getInstance()
                    .getReference("presence/" + user.getUid())
                    .setValue(offlineData);

            FirebaseFirestore.getInstance()
                    .collection("nguoi_dung")
                    .document(user.getUid())
                    .update("trang_thai_hoat_dong", false,
                            "lan_cuoi_hoat_dong", FieldValue.serverTimestamp())
                    .addOnSuccessListener(unused -> {
                        FirebaseAuth.getInstance().signOut();
                        Toast.makeText(host.requireContext(),
                                "Đăng xuất thành công", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(host.requireContext(), LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        host.startActivity(intent);
                        if (host.getActivity() != null) host.requireActivity().finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(host.requireContext(),
                                    "Không thể cập nhật trạng thái người dùng",
                                    Toast.LENGTH_SHORT).show());
        } else {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(host.requireContext(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            host.startActivity(intent);
            if (host.getActivity() != null) host.requireActivity().finish();
        }
    }

    // ── Theme ─────────────────────────────────────────────────────────────────

    private void applyTheme(Context context, View settingsRoot, ImageView btnClose,
                            TextView txtSettingsTitle, TextView[] rows,
                            TextView rowLogout, View[] sections,
                            View[] switchRows, boolean darkMode) {
        int backgroundColor = Color.parseColor(darkMode ? "#000000" : "#FFFFFF");
        int sectionColor    = Color.parseColor(darkMode ? "#101010" : "#F2F2F7");
        int primaryColor    = Color.parseColor(darkMode ? "#FFFFFF" : "#111111");

        GradientDrawable popupBg = new GradientDrawable();
        popupBg.setColor(backgroundColor);
        popupBg.setCornerRadius(28f * context.getResources().getDisplayMetrics().density);
        settingsRoot.setBackground(popupBg);
        btnClose.setColorFilter(primaryColor);
        txtSettingsTitle.setTextColor(primaryColor);

        for (TextView row : rows) {
            row.setTextColor(primaryColor);
            row.setBackgroundColor(sectionColor);
        }
        for (View section : sections) section.setBackgroundColor(sectionColor);
        for (View row : switchRows)     row.setBackgroundColor(sectionColor);

        updateTextColorRecursive(settingsRoot, primaryColor);
        rowLogout.setTextColor(Color.parseColor("#FF453A"));
    }

    private void updateTextColorRecursive(View view, int textColor) {
        if (view instanceof TextView && view.getId() != R.id.rowLogout) {
            ((TextView) view).setTextColor(textColor);
        }
        if (!(view instanceof ViewGroup)) return;
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            updateTextColorRecursive(group.getChildAt(i), textColor);
        }
    }
}