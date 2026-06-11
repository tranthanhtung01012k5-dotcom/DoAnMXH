package com.example.doanmxh.ProfilePage;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doanmxh.Message.ChatActivity;
import com.example.doanmxh.Message.ChatListAdapter;
import com.example.doanmxh.Message.ChatUser;
import com.example.doanmxh.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ArchivedPostsActivity extends AppCompatActivity {

    private RecyclerView rvPosts;
    private TextView txtEmpty;
    private ProgressBar progressBar;

    private FirebaseFirestore db;

    private final List<ChatUser> chatList = new ArrayList<>();
    private ChatListAdapter chatListAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_activity_posts);

        db = FirebaseFirestore.getInstance();

        rvPosts = findViewById(R.id.rvPosts);
        txtEmpty = findViewById(R.id.txtEmpty);
        progressBar = findViewById(R.id.progressBar);
        ImageView btnBack = findViewById(R.id.btnBack);
        TextView txtTitle = findViewById(R.id.txtTitle);

        txtTitle.setText("Kho lưu trữ");

        btnBack.setOnClickListener(v -> finish());

        // ✔ SET ADAPTER (QUAN TRỌNG NHẤT)
        rvPosts.setLayoutManager(new LinearLayoutManager(this));
        chatListAdapter = new ChatListAdapter(chatList, ChatListAdapter.Mode.ARCHIVE_LIST);
        rvPosts.setAdapter(chatListAdapter);
        chatListAdapter.setOnArchiveLongClickListener(chatUser -> {

            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            String convId = uid.compareTo(chatUser.getUid()) < 0
                    ? uid + "_" + chatUser.getUid()
                    : chatUser.getUid() + "_" + uid;

            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Xóa khỏi lưu trữ")
                    .setMessage("Bạn muốn bỏ cuộc trò chuyện này khỏi kho lưu trữ?")
                    .setPositiveButton("Xóa", (dialog, which) -> {

                        db.collection("nguoi_dung")
                                .document(uid)
                                .collection("cuoc_tro_luu_tru")
                                .document(convId)
                                .delete()
                                .addOnSuccessListener(v -> {

                                    chatList.remove(chatUser);
                                    chatListAdapter.notifyDataSetChanged();

                                    if (chatList.isEmpty()) {
                                        showEmpty();
                                    }

                                    android.widget.Toast.makeText(
                                            this,
                                            "Đã xóa khỏi lưu trữ",
                                            android.widget.Toast.LENGTH_SHORT
                                    ).show();
                                });

                    })
                    .setNegativeButton("Hủy", null)
                    .show();
//            finish();
        });
        chatListAdapter.setOnItemClickListener( chatUser -> {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            String convId = uid.compareTo(chatUser.getUid()) < 0
                    ? uid + "_" + chatUser.getUid()
                    : chatUser.getUid() + "_" + uid;
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("target_uid", chatUser.getUid());
            intent.putExtra("conversation_id", convId);
            startActivity(intent);
//            finish();
        });
        loadArchivedPosts();
    }

    private void loadArchivedPosts() {

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (uid == null) {
            showEmpty();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        db.collection("nguoi_dung")
                .document(uid)
                .collection("cuoc_tro_luu_tru")
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    List<String> archivedIds = new ArrayList<>();

                    for (var doc : querySnapshot.getDocuments()) {
                        String convId = doc.getString("tin_nhan_id");
                        if (convId != null) {
                            archivedIds.add(convId);
                        }
                    }

                    Log.d("ARCHIVE", "size = " + archivedIds.size());

                    if (archivedIds.isEmpty()) {
                        showEmpty();
                        return;
                    }

                    loadArchivedChatDetails(archivedIds);

                })
                .addOnFailureListener(e -> {
                    Log.e("ARCHIVE", "ERROR", e);
                    showEmpty();
                });
    }

    private void loadArchivedChatDetails(List<String> ids) {

        List<ChatUser> result = new ArrayList<>();
        int total = ids.size();
        int[] loaded = {0};

        String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        for (String id : ids) {

            db.collection("cuoc_tro_chuyen")
                    .document(id)
                    .get()
                    .addOnSuccessListener(doc -> {

                        if (doc.exists()) {

                            List<String> members = (List<String>) doc.get("thanh_vien");

                            String targetUid = null;
                            for (String m : members) {
                                if (!m.equals(myUid)) {
                                    targetUid = m;
                                    break;
                                }
                            }

                            String finalTargetUid = targetUid;

                            db.collection("nguoi_dung")
                                    .document(finalTargetUid)
                                    .get()
                                    .addOnSuccessListener(userDoc -> {

                                        ChatUser user = new ChatUser();

                                        user.setUid(finalTargetUid);
                                        user.setUsername(userDoc.getString("ho_va_ten"));
                                        user.setAvatarResId(userDoc.getString("anh_dai_dien"));

                                        user.setLastMessage(doc.getString("tin_nhan_cuoi"));
                                        user.setChatTime(doc.getTimestamp("thoi_gian_cuoi"));

                                        result.add(user);

                                        loaded[0]++;
                                        checkDone(loaded[0], total, result);
                                    });

                        } else {
                            loaded[0]++;
                            checkDone(loaded[0], total, result);
                        }
                    })
                    .addOnFailureListener(e -> {
                        loaded[0]++;
                        checkDone(loaded[0], total, result);
                    });
        }
    }

    private void checkDone(int loaded, int total, List<ChatUser> result) {
        if (loaded == total) {
            updateUI(result);
        }
    }

    private void updateUI(List<ChatUser> list) {

        progressBar.setVisibility(View.GONE);

        if (list.isEmpty()) {
            showEmpty();
            return;
        }

        txtEmpty.setVisibility(View.GONE);

        chatList.clear();
        chatList.addAll(list);

        chatListAdapter.notifyDataSetChanged();
    }

    private void showEmpty() {
        progressBar.setVisibility(View.GONE);
        txtEmpty.setVisibility(View.VISIBLE);
    }
}