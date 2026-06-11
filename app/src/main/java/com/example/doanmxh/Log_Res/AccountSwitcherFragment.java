package com.example.doanmxh.Log_Res;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doanmxh.Log_Res.AccountManager;
import com.example.doanmxh.MainActivity;
import com.example.doanmxh.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AccountSwitcherFragment extends BottomSheetDialogFragment {

    public static AccountSwitcherFragment newInstance() {
        return new AccountSwitcherFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_account_switcher, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Context ctx = requireContext().getApplicationContext();

        RecyclerView rv = view.findViewById(R.id.rvAccounts);
        rv.setLayoutManager(new LinearLayoutManager(ctx));

        // ── Lấy danh sách tài khoản đã lưu ──────────────────────────────────
        List<AccountManager.SavedAccount> saved = AccountManager.getAll(ctx);

        // ── Lấy tài khoản đang đăng nhập hiện tại ───────────────────────────
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String currentEmail = currentUser != null ? currentUser.getEmail() : null;

        // ── Ghép danh sách: current lên đầu, bỏ trùng email ─────────────────
        List<AccountManager.SavedAccount> displayList = new ArrayList<>();

        if (currentUser != null) {
            // Tìm trong saved xem có entry cho current user không
            AccountManager.SavedAccount currentEntry = null;
            for (AccountManager.SavedAccount a : saved) {
                if (a.email.equalsIgnoreCase(currentEmail)) {
                    currentEntry = a;
                    break;
                }
            }
            // Nếu có → dùng entry đó; nếu không → tạo entry tạm (password rỗng, không switch được)
            if (currentEntry != null) {
                displayList.add(currentEntry);
            } else {
                String name = currentUser.getDisplayName();
                displayList.add(new AccountManager.SavedAccount(
                        currentEmail,
                        "",   // không có password → không thể re-login
                        (name != null && !name.isEmpty()) ? name : currentEmail,
                        currentUser.getUid()
                ));
            }
        }

        // Thêm các tài khoản đã lưu khác (bỏ current)
        for (AccountManager.SavedAccount a : saved) {
            if (!a.email.equalsIgnoreCase(currentEmail)) {
                displayList.add(a);
            }
        }

        AccountAdapter adapter = new AccountAdapter(
                displayList,
                currentEmail,
                new AccountAdapter.Listener() {
                    @Override
                    public void onSelect(AccountManager.SavedAccount account) {
                        // Không làm gì nếu đã là tài khoản hiện tại
                        if (account.email.equalsIgnoreCase(currentEmail)) {
                            Toast.makeText(ctx, "Đây là tài khoản đang đăng nhập",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (account.password.isEmpty()) {
                            Toast.makeText(ctx,
                                    "Tài khoản này chưa được lưu mật khẩu.\nVui lòng đăng nhập lại và tick Ghi nhớ.",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        dismiss();
                        switchAccount(ctx, account);
                    }

                    @Override
                    public void onRemove(AccountManager.SavedAccount account) {
                        // Không cho xóa tài khoản đang đăng nhập
                        if (account.email.equalsIgnoreCase(currentEmail)) {
                            Toast.makeText(ctx, "Không thể xóa tài khoản đang đăng nhập",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        AccountManager.removeAccount(ctx, account.email);
                        displayList.remove(account);
                        rv.getAdapter().notifyDataSetChanged();
                        Toast.makeText(ctx, "Đã xóa " + account.displayName,
                                Toast.LENGTH_SHORT).show();
                    }
                });

        rv.setAdapter(adapter);

        // Nút thêm tài khoản
        view.findViewById(R.id.btnAddAccount).setOnClickListener(v -> {
            dismiss();
            markCurrentUserOffline();
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(ctx, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            ctx.startActivity(intent);
        });
    }

    // ── Offline ───────────────────────────────────────────────────────────────

    private void markCurrentUserOffline() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            FirebaseFirestore.getInstance()
                    .collection("nguoi_dung")
                    .document(auth.getCurrentUser().getUid())
                    .update("trang_thai_hoat_dong", false);
        }
    }

    // ── Switch ────────────────────────────────────────────────────────────────

    private void switchAccount(final Context ctx, AccountManager.SavedAccount account) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (auth.getCurrentUser() != null) {
            db.collection("nguoi_dung")
                    .document(auth.getCurrentUser().getUid())
                    .update("trang_thai_hoat_dong", false);
        }
        auth.signOut();

        auth.signInWithEmailAndPassword(account.email, account.password)
                .addOnSuccessListener(authResult -> {
                    String newUid = authResult.getUser().getUid();
                    db.collection("nguoi_dung")
                            .document(newUid)
                            .update("trang_thai_hoat_dong", true)
                            .addOnSuccessListener(unused -> {
                                ctx.getSharedPreferences("login_pref", Context.MODE_PRIVATE)
                                        .edit()
                                        .putBoolean("remember_login", true)
                                        .apply();
                                Toast.makeText(ctx,
                                        "Đã chuyển sang " + account.displayName,
                                        Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(ctx, MainActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                        | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                ctx.startActivity(intent);
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(ctx,
                                            "Không cập nhật được trạng thái: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(ctx,
                                "Chuyển tài khoản thất bại: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    private static class AccountAdapter
            extends RecyclerView.Adapter<AccountAdapter.VH> {

        interface Listener {
            void onSelect(AccountManager.SavedAccount account);
            void onRemove(AccountManager.SavedAccount account);
        }

        private final List<AccountManager.SavedAccount> data;
        private final String currentEmail;
        private final Listener listener;

        private static final int[] AVATAR_COLORS = {
                0xFF5B8DEF, 0xFF43C59E, 0xFFE4844A,
                0xFFB36AE2, 0xFFE05C7A, 0xFF4ECDC4
        };

        AccountAdapter(List<AccountManager.SavedAccount> data,
                       String currentEmail,
                       Listener listener) {
            this.data         = data;
            this.currentEmail = currentEmail;
            this.listener     = listener;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_saved_account, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            AccountManager.SavedAccount a = data.get(position);
            boolean isCurrent = a.email.equalsIgnoreCase(currentEmail);

            // Avatar
            String initial = a.displayName.substring(0, 1).toUpperCase();
            h.tvAvatar.setText(initial);
            h.tvAvatar.getBackground().setTint(AVATAR_COLORS[position % AVATAR_COLORS.length]);

            h.tvName.setText(a.displayName);

            // Email + badge "Đang đăng nhập" nếu là current
            if (isCurrent) {
                h.tvEmail.setText(a.email + "  ✓ Đang đăng nhập");
                h.tvEmail.setTextColor(0xFF43C59E);   // màu xanh nổi bật
                h.btnRemove.setVisibility(View.GONE); // ẩn nút xóa
            } else {
                h.tvEmail.setText(a.email);
                h.tvEmail.setTextColor(0xFF8E8E93);
                h.btnRemove.setVisibility(View.VISIBLE);
            }

            h.itemView.setOnClickListener(v -> listener.onSelect(a));
            h.btnRemove.setOnClickListener(v -> listener.onRemove(a));
        }

        @Override
        public int getItemCount() { return data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView    tvAvatar, tvName, tvEmail;
            ImageButton btnRemove;

            VH(View v) {
                super(v);
                tvAvatar  = v.findViewById(R.id.tvAvatar);
                tvName    = v.findViewById(R.id.tvName);
                tvEmail   = v.findViewById(R.id.tvEmail);
                btnRemove = v.findViewById(R.id.btnRemove);
            }
        }
    }
}