package com.example.doanmxh.Search;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doanmxh.BaseActivity;
import com.example.doanmxh.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SearchActivity extends BaseActivity {

    private static final String PREF_NAME   = "search_history";
    private static final String KEY_HISTORY = "history";

    private TextInputEditText edtSearch;
    private RecyclerView      rvUsers, rvTrending;
    private LinearLayout      layoutTrending, layoutRecent;
    private ChipGroup         chipGroupRecent;
    private TextView          btnEdit;
    private ImageView         btnBack;

    private FirebaseFirestore db;
    private FirebaseAuth      auth;
    private SharedPreferences prefs;

    private List<User>        userList     = new ArrayList<>();
    private ArrayList<String> trendingList = new ArrayList<>();

    private UserAdapter     adapter;
    private TrendingAdapter trendingAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        db    = FirebaseFirestore.getInstance();
        auth  = FirebaseAuth.getInstance();
        prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        initViews();
        setupRecyclers();
        setupClickListeners();
        loadUsers();
        loadTrending();
        loadHistory();
    }

    private void initViews() {
        edtSearch       = findViewById(R.id.edtSearch);
        rvUsers         = findViewById(R.id.rvUsers);
        rvTrending      = findViewById(R.id.rvTrending);
        layoutTrending  = findViewById(R.id.layoutTrending);
        layoutRecent    = findViewById(R.id.layoutRecent);
        chipGroupRecent = findViewById(R.id.chipGroupRecent);
        btnEdit         = findViewById(R.id.btnEdit);
        btnBack         = findViewById(R.id.btnBack);
    }

    private void setupRecyclers() {
        adapter = new UserAdapter();
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        rvUsers.setAdapter(adapter);
        adapter.setList(userList);

        trendingAdapter = new TrendingAdapter(trendingList, keyword -> {
            Intent intent = new Intent(this, SearchExpandActivity.class);
            intent.putExtra("keyword", keyword);
            startActivity(intent);
        });
        rvTrending.setLayoutManager(new LinearLayoutManager(this));
        rvTrending.setAdapter(trendingAdapter);
    }

    private void setupClickListeners() {
        edtSearch.setFocusable(false);
        edtSearch.setClickable(true);
        edtSearch.setOnClickListener(v -> startActivity(
                new Intent(this, SearchExpandActivity.class)));

        btnBack.setOnClickListener(v -> finish());

        // Xoá toàn bộ lịch sử
        btnEdit.setOnClickListener(v -> {
            prefs.edit().remove(KEY_HISTORY).apply();
            chipGroupRecent.removeAllViews();
            layoutRecent.setVisibility(View.GONE);
        });
    }

    // ── Lịch sử tìm kiếm ─────────────────────────────────────────────────
    private void loadHistory() {
        List<String> history = getHistory();
        if (history.isEmpty()) {
            layoutRecent.setVisibility(View.GONE);
            return;
        }
        renderHistory(history);
    }

    private List<String> getHistory() {
        List<String> list = new ArrayList<>();
        try {
            String raw = prefs.getString(KEY_HISTORY, "[]");
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++)
                list.add(arr.getString(i));
        } catch (Exception ignored) {}
        return list;
    }

    private void renderHistory(List<String> history) {
        chipGroupRecent.removeAllViews();
        if (history.isEmpty()) {
            layoutRecent.setVisibility(View.GONE);
            return;
        }
        layoutRecent.setVisibility(View.VISIBLE);
        for (String keyword : history) {
            Chip chip = new Chip(this);
            chip.setText(keyword);
            chip.setTextColor(0xFFFFFFFF);
            chip.setChipBackgroundColorResource(R.color.bg_chip);
            chip.setCloseIconVisible(true);
            chip.setCloseIconTint(android.content.res.ColorStateList.valueOf(0xFF8E8E93));

            // Bấm chip → mở SearchExpandActivity với từ khoá đó
            chip.setOnClickListener(v -> {
                Intent intent = new Intent(this, SearchExpandActivity.class);
                intent.putExtra("keyword", keyword);
                startActivity(intent);
            });

            // Bấm X → xoá chip đó
            chip.setOnCloseIconClickListener(v -> {
                List<String> current = getHistory();
                current.remove(keyword);
                JSONArray arr = new JSONArray();
                for (String s : current) arr.put(s);
                prefs.edit().putString(KEY_HISTORY, arr.toString()).apply();
                renderHistory(current);
            });

            chipGroupRecent.addView(chip);
        }
    }

    // ── Trending ──────────────────────────────────────────────────────────
    private void loadTrending() {
        db.collection("tim_kiem")
                .orderBy("so_luot", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(query -> {
                    trendingList.clear();
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        String tuKhoa = doc.getString("tu_khoa");
                        if (tuKhoa != null) trendingList.add(tuKhoa);
                    }
                    trendingAdapter.notifyDataSetChanged();
                    layoutTrending.setVisibility(
                            trendingList.isEmpty() ? View.GONE : View.VISIBLE);
                });
    }

    // ── Load users ────────────────────────────────────────────────────────
    private void loadUsers() {
        String myUid = auth.getCurrentUser().getUid();

        db.collection("nguoi_dung")
                .document(myUid)
                .collection("nguoi_dang_theo_doi")
                .get()
                .addOnSuccessListener(followSnapshot -> {
                    Set<String> followedIds = new HashSet<>();
                    for (DocumentSnapshot doc : followSnapshot.getDocuments())
                        followedIds.add(doc.getId());

                    db.collection("nguoi_dung")
                            .get()
                            .addOnSuccessListener(userSnapshot -> {
                                userList.clear();
                                for (DocumentSnapshot doc : userSnapshot.getDocuments()) {
                                    User user = doc.toObject(User.class);
                                    if (user == null) continue;
                                    String uid = doc.getId();
                                    user.setUid(uid);
                                    user.setAvatar(doc.getString("anh_dai_dien"));
                                    user.setFullname(doc.getString("ho_va_ten"));
                                    user.setUsername(doc.getString("ten_dang_nhap"));
                                    user.setFollowed(followedIds.contains(uid));
                                    userList.add(user);
                                    Log.d("User", uid + " followed=" + user.isFollowed());
                                }
                                adapter.notifyDataSetChanged();
                            });
                });
    }
    @Override
    protected void onResume() {
        super.onResume();
        loadHistory();
        loadTrending();
    }
}