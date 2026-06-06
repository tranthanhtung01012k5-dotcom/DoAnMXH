package com.example.doanmxh.ProfilePage;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doanmxh.BaseActivity;
import com.example.doanmxh.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockedUsersActivity extends BaseActivity {
    private final List<BlockedUser> blockedUsers = new ArrayList<>();
    private BlockedUsersAdapter adapter;
    private FirebaseFirestore db;
    private String myUid;
    private EditText edtUsername;
    private TextView txtEmpty;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blocked_users);

        myUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
        db = FirebaseFirestore.getInstance();

        ImageView btnBack = findViewById(R.id.btnBack);
        TextView btnBlock = findViewById(R.id.btnBlock);
        RecyclerView rvBlockedUsers = findViewById(R.id.rvBlockedUsers);
        edtUsername = findViewById(R.id.edtUsername);
        txtEmpty = findViewById(R.id.txtEmpty);

        btnBack.setOnClickListener(v -> finish());
        btnBlock.setOnClickListener(v -> blockByUsername());

        adapter = new BlockedUsersAdapter(blockedUsers, this::unblockUser);
        rvBlockedUsers.setLayoutManager(new LinearLayoutManager(this));
        rvBlockedUsers.setAdapter(adapter);

        loadBlockedUsers();
    }

    private void blockByUsername() {
        if (myUid == null) return;

        String username = edtUsername.getText() != null
                ? edtUsername.getText().toString().trim()
                : "";
        if (username.startsWith("@")) username = username.substring(1);
        if (TextUtils.isEmpty(username)) {
            Toast.makeText(this, "Nhập username cần chặn", Toast.LENGTH_SHORT).show();
            return;
        }
        final String targetUsername = username;

        db.collection("nguoi_dung")
                .whereEqualTo("ten_dang_nhap", targetUsername)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        Toast.makeText(this, "Không tìm thấy người dùng", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    var userDoc = snapshot.getDocuments().get(0);
                    String targetUid = userDoc.getId();
                    if (myUid.equals(targetUid)) {
                        Toast.makeText(this, "Bạn không thể chặn chính mình", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Map<String, Object> data = new HashMap<>();
                    data.put("nguoi_dung_id", targetUid);
                    data.put("ten_dang_nhap", userDoc.getString("ten_dang_nhap"));
                    data.put("ho_va_ten", userDoc.getString("ho_va_ten"));
                    data.put("anh_dai_dien", userDoc.getString("anh_dai_dien"));
                    data.put("ngay_chan", FieldValue.serverTimestamp());

                    db.collection("nguoi_dung")
                            .document(myUid)
                            .collection("nguoi_bi_chan")
                            .document(targetUid)
                            .set(data)
                            .addOnSuccessListener(unused -> {
                                edtUsername.setText("");
                                Toast.makeText(this, "Đã chặn @" + targetUsername, Toast.LENGTH_SHORT).show();
                                loadBlockedUsers();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Không thể chặn người dùng", Toast.LENGTH_SHORT).show());
                });
    }

    private void loadBlockedUsers() {
        if (myUid == null) {
            showEmpty();
            return;
        }

        db.collection("nguoi_dung")
                .document(myUid)
                .collection("nguoi_bi_chan")
                .get()
                .addOnSuccessListener(snapshot -> {
                    blockedUsers.clear();
                    for (var doc : snapshot.getDocuments()) {
                        blockedUsers.add(new BlockedUser(
                                doc.getId(),
                                doc.getString("ho_va_ten"),
                                doc.getString("ten_dang_nhap"),
                                doc.getString("anh_dai_dien")
                        ));
                    }
                    txtEmpty.setVisibility(blockedUsers.isEmpty() ? View.VISIBLE : View.GONE);
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> showEmpty());
    }

    private void unblockUser(BlockedUser user) {
        if (myUid == null) return;

        db.collection("nguoi_dung")
                .document(myUid)
                .collection("nguoi_bi_chan")
                .document(user.uid)
                .delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Đã bỏ chặn @" + user.username, Toast.LENGTH_SHORT).show();
                    loadBlockedUsers();
                });
    }

    private void showEmpty() {
        txtEmpty.setVisibility(View.VISIBLE);
    }

    private static class BlockedUser {
        final String uid;
        final String name;
        final String username;
        final String avatar;

        BlockedUser(String uid, String name, String username, String avatar) {
            this.uid = uid;
            this.name = name != null ? name : "Người dùng";
            this.username = username != null ? username : "user";
            this.avatar = avatar;
        }
    }

    private static class BlockedUsersAdapter extends RecyclerView.Adapter<BlockedUsersAdapter.ViewHolder> {
        interface OnUnblockClick {
            void onUnblock(BlockedUser user);
        }

        private final List<BlockedUser> users;
        private final OnUnblockClick listener;

        BlockedUsersAdapter(List<BlockedUser> users, OnUnblockClick listener) {
            this.users = users;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_blocked_user, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            BlockedUser user = users.get(position);
            holder.txtName.setText(user.name);
            holder.txtUsername.setText("@" + user.username);
            Glide.with(holder.itemView.getContext())
                    .load(user.avatar)
                    .placeholder(R.drawable.ic_placeholder_avatar)
                    .error(R.drawable.ic_placeholder_avatar)
                    .circleCrop()
                    .into(holder.imgAvatar);
            holder.btnUnblock.setOnClickListener(v -> listener.onUnblock(user));
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imgAvatar;
            TextView txtName, txtUsername, btnUnblock;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                imgAvatar = itemView.findViewById(R.id.imgAvatar);
                txtName = itemView.findViewById(R.id.txtName);
                txtUsername = itemView.findViewById(R.id.txtUsername);
                btnUnblock = itemView.findViewById(R.id.btnUnblock);
            }
        }
    }
}
