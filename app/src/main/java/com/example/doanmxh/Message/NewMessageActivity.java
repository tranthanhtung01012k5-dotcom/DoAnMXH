package com.example.doanmxh.Message;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doanmxh.BaseActivity;
import com.example.doanmxh.MainActivity;
import com.example.doanmxh.R;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class NewMessageActivity extends BaseActivity {
    private RecyclerView rvSuggestions;
    private TextView btnCancel;
    private EditText txtSearch;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private List<SugestionModel> suggestionList = new ArrayList<>();
    private SugestionAdapter sugestionAdapter;
    private boolean isLoadingSuggestions = false;
    private List<SugestionModel> fullList = new ArrayList<>(); // giữ bản gốc



    @Override
    public void onCreate(@NonNull Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_message);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        anhXa();
        loadSuggestions();
        btnCancel.setOnClickListener(v -> finish());
    }
    private void anhXa() {
        btnCancel = findViewById(R.id.btnCancel);
        rvSuggestions = findViewById(R.id.rvSuggestions);
        txtSearch = findViewById(R.id.txtSearch);
        rvSuggestions.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        );
    }

    private void loadSuggestions() {
        String uid = auth.getUid();
        if (uid == null) return;
        if (isLoadingSuggestions) return;
        isLoadingSuggestions = true;

        suggestionList.clear();
        fullList.clear();
        if (sugestionAdapter == null) {
            sugestionAdapter = new SugestionAdapter(this, suggestionList, user -> {
                Intent intent = new Intent(this, ChatActivity.class);
                intent.putExtra("target_uid", user.getUid());
                startActivity(intent);
            });
            rvSuggestions.setAdapter(sugestionAdapter);
        }

        Task<QuerySnapshot> followingTask = db.collection("nguoi_dung")
                .document(uid).collection("nguoi_dang_theo_doi").get();

        Task<QuerySnapshot> followerTask = db.collection("nguoi_dung")
                .document(uid).collection("nguoi_theo_doi").get();

        Tasks.whenAllSuccess(followingTask, followerTask)
                .addOnSuccessListener(results -> {
                    QuerySnapshot followingSnap = (QuerySnapshot) results.get(0);
                    QuerySnapshot followerSnap  = (QuerySnapshot) results.get(1);

                    Log.d("DEBUG_SUGGEST", "following count: " + followingSnap.size());
                    Log.d("DEBUG_SUGGEST", "follower count: " + followerSnap.size());

                    Set<String> allUids = new LinkedHashSet<>();
                    for (DocumentSnapshot doc : followingSnap.getDocuments()) {
                        Log.d("DEBUG_SUGGEST", "following uid: " + doc.getId());
                        allUids.add(doc.getId());
                    }
                    for (DocumentSnapshot doc : followerSnap.getDocuments()) {
                        Log.d("DEBUG_SUGGEST", "follower uid: " + doc.getId());
                        allUids.add(doc.getId());
                    }
                    allUids.remove(uid);

                    Log.d("DEBUG_SUGGEST", "total unique uids: " + allUids.size());

                    // Tạo list task fetch từng user
                    List<Task<DocumentSnapshot>> userTasks = new ArrayList<>();
                    for (String friendUid : allUids) {
                        userTasks.add(db.collection("nguoi_dung").document(friendUid).get());
                    }

                    // Dùng whenAllComplete để không bỏ sót user nào
                    Tasks.whenAllComplete(userTasks)
                            .addOnSuccessListener(taskResults -> {
                                suggestionList.clear();

                                for (Task<?> task : taskResults) {
                                    if (!task.isSuccessful()) continue;

                                    DocumentSnapshot userDoc = (DocumentSnapshot) task.getResult();
                                    if (userDoc == null || !userDoc.exists()) continue;

                                    SugestionModel user = new SugestionModel();
                                    user.setUid(userDoc.getId());
                                    user.setHoVaTen(userDoc.getString("ho_va_ten"));
                                    user.setTenDangNhap(userDoc.getString("ten_dang_nhap"));
                                    user.setAnhDaiDien(userDoc.getString("anh_dai_dien"));
                                    suggestionList.add(user);
                                    fullList.add(user);
                                    Log.d("DEBUG_SUGGEST", "added: " + user.getTenDangNhap());
                                }

                                Log.d("DEBUG_SUGGEST", "final list size: " + suggestionList.size());
                                sugestionAdapter.notifyDataSetChanged();
                                isLoadingSuggestions = false;
                                searchName();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("DEBUG_SUGGEST", "error: " + e.getMessage());
                    isLoadingSuggestions = false;

                });
    }
    private void searchName() {
        txtSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterList(s.toString().trim());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }
    private void filterList(String keyword) {
        if (keyword.isEmpty()) {
            suggestionList.clear();
            suggestionList.addAll(fullList);
            sugestionAdapter.notifyDataSetChanged();
            return;
        }

        String lower = keyword.toLowerCase();
        List<SugestionModel> filtered = new ArrayList<>();

        for (SugestionModel user : fullList) {
            boolean matchTen = user.getTenDangNhap() != null
                    && user.getTenDangNhap().toLowerCase().contains(lower);
            boolean matchHo  = user.getHoVaTen() != null
                    && user.getHoVaTen().toLowerCase().contains(lower);

            if (matchTen || matchHo) {
                filtered.add(user);
            }
        }

        suggestionList.clear();
        suggestionList.addAll(filtered);
        sugestionAdapter.notifyDataSetChanged();
    }
}
