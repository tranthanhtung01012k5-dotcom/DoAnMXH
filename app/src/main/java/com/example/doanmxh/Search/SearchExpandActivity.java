package com.example.doanmxh.Search;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doanmxh.BaseActivity;
import com.example.doanmxh.R;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class SearchExpandActivity extends BaseActivity {

    private static final String PREF_NAME   = "search_history";
    private static final String KEY_HISTORY = "history";
    private static final int    MAX_HISTORY = 8;

    private RecyclerView       rvTrending;
    private TrendingAdapter    trendingAdapter;
    private ArrayList<String>  trendingList = new ArrayList<>();

    private ImageView          btnBack, btnFilter;
    private TextInputEditText  edtSearch;
    private RecyclerView       rvUsers, rvPosts;
    private LinearLayout       layoutRecent, layoutTopics, layoutEmpty, layoutTrending;
    private ShimmerFrameLayout shimmerLayout;
    private ChipGroup          chipGroupRecent, chipGroupHashtags;
    private TextView           btnEdit, tvPostsHeader;

    private UserAdapter        adapter;
    private ArrayList<Post>    postList = new ArrayList<>();

    private FirebaseFirestore  db;
    private SharedPreferences  prefs;

    private Handler            searchHandler  = new Handler(Looper.getMainLooper());
    private Runnable           searchRunnable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_expand);

        db    = FirebaseFirestore.getInstance();
        prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        initViews();
        setupRecycler();
        setupBackButton();
        setupFilterButton();
        setupSearch();
        setupEditButton();
        loadTrending();
        loadHistory();
        loadTopHashtags();

        edtSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || actionId == EditorInfo.IME_ACTION_DONE
                    || actionId == EditorInfo.IME_NULL) {
                String keyword = edtSearch.getText().toString().trim();
                if (!keyword.isEmpty()) {
                    saveHistory(keyword);
                    Intent intent = new Intent(this, SearchResultActivity.class);
                    intent.putExtra("keyword", keyword);
                    startActivity(intent);
                }
                return true;
            }
            return false;
        });
    }

    private void initViews() {
        btnBack           = findViewById(R.id.btnBack);
        btnFilter         = findViewById(R.id.btnFilter);
        edtSearch         = findViewById(R.id.edtSearch);
        rvUsers           = findViewById(R.id.rvUsers);
        rvPosts           = findViewById(R.id.rvPosts);
        layoutRecent      = findViewById(R.id.layoutRecent);
        layoutTopics      = findViewById(R.id.layoutTopics);
        layoutTrending    = findViewById(R.id.layoutTrending);
        layoutEmpty       = findViewById(R.id.layoutEmpty);
        shimmerLayout     = findViewById(R.id.shimmerLayout);
        chipGroupRecent   = findViewById(R.id.chipGroupRecent);
        chipGroupHashtags = findViewById(R.id.chipGroupHashtags);
        btnEdit           = findViewById(R.id.btnEdit);
        tvPostsHeader     = findViewById(R.id.tvPostsHeader);
        rvTrending        = findViewById(R.id.rvTrending);
    }

    private void setupRecycler() {
        adapter = new UserAdapter();
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        rvUsers.setAdapter(adapter);

        rvPosts.setLayoutManager(new LinearLayoutManager(this));

        trendingAdapter = new TrendingAdapter(trendingList, keyword -> {
            edtSearch.setText(keyword);
            edtSearch.setSelection(keyword.length());
        });
        rvTrending.setLayoutManager(new LinearLayoutManager(this));
        rvTrending.setAdapter(trendingAdapter);
    }

    private void setupBackButton() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupFilterButton() {
        btnFilter.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(this, btnFilter);
            popupMenu.getMenuInflater().inflate(R.menu.filter_menu, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(this::handleFilterClick);
            popupMenu.show();
        });
    }

    private boolean handleFilterClick(MenuItem item) {
        if (item.getItemId() == R.id.filter_from_profile) {
            Toast.makeText(this, "Lọc người dùng", Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    private void setupEditButton() {
        btnEdit.setOnClickListener(v -> {
            prefs.edit().remove(KEY_HISTORY).apply();
            chipGroupRecent.removeAllViews();
            layoutRecent.setVisibility(View.GONE);
        });
    }

    private void saveHistory(String keyword) {
        List<String> history = getHistory();
        history.remove(keyword);
        history.add(0, keyword);
        if (history.size() > MAX_HISTORY)
            history = history.subList(0, MAX_HISTORY);
        JSONArray arr = new JSONArray();
        for (String s : history) arr.put(s);
        prefs.edit().putString(KEY_HISTORY, arr.toString()).apply();
        renderHistory(history);

        db.collection("tim_kiem")
                .document(keyword)
                .get()
                .addOnSuccessListener(doc -> {
                    long soLuot = doc.exists() ? doc.getLong("so_luot") : 0L;
                    db.collection("tim_kiem")
                            .document(keyword)
                            .set(new java.util.HashMap<String, Object>() {{
                                put("tu_khoa", keyword);
                                put("so_luot", soLuot + 1);
                            }});
                });
    }

    private void loadTrending() {
        db.collection("tim_kiem")
                .orderBy("so_luot", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(query -> {
                    trendingList.clear();
                    for (var doc : query.getDocuments()) {
                        String tuKhoa = doc.getString("tu_khoa");
                        if (tuKhoa != null) trendingList.add(tuKhoa);
                    }
                    trendingAdapter.notifyDataSetChanged();
                    layoutTrending.setVisibility(trendingList.isEmpty() ? View.GONE : View.VISIBLE);
                });
    }

    private void loadTopHashtags() {
        db.collection("hashtag")
                .orderBy("so_bai_viet", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(query -> {
                    chipGroupHashtags.removeAllViews();

                    if (query.isEmpty()) {
                        layoutTopics.setVisibility(View.GONE);
                        return;
                    }

                    for (var doc : query.getDocuments()) {
                        String ten = doc.getString("ten");
                        if (ten == null) continue;

                        long soBaiViet = doc.getLong("so_bai_viet") != null
                                ? doc.getLong("so_bai_viet") : 0;

                        Chip chip = new Chip(this);
                        chip.setText("#" + ten );
                        chip.setTextColor(this.getResources().getColor(R.color.bg_primary));
                        chip.setChipBackgroundColorResource(R.color.text_primary);
                        chip.setCloseIconVisible(false);
                        chip.setOnClickListener(v -> {
                            edtSearch.setText("#" + ten);
                            edtSearch.setSelection(edtSearch.getText().length());
                        });

                        chipGroupHashtags.addView(chip);
                    }

                    layoutTopics.setVisibility(View.VISIBLE);
                })
                .addOnFailureListener(e ->
                        Log.e("HASHTAG", "loadTopHashtags failed: " + e.getMessage()));
    }

    private void searchHashtags(String tag) {
        showLoading(true);

        db.collection("hashtag")
                .get()
                .addOnSuccessListener(query -> {
                    showLoading(false);
                    chipGroupHashtags.removeAllViews();

                    int matched = 0;
                    for (var doc : query.getDocuments()) {
                        String ten = doc.getString("ten");
                        if (ten == null) continue;
                        if (!ten.toLowerCase().contains(tag)) continue;

                        long soBaiViet = doc.getLong("so_bai_viet") != null
                                ? doc.getLong("so_bai_viet") : 0;

                        Chip chip = new Chip(this);
                        chip.setText("#" + ten + "  " + soBaiViet);
                        chip.setTextColor(0xFFFFFFFF);
                        chip.setChipBackgroundColorResource(R.color.bg_chip);
                        chip.setCloseIconVisible(false);
                        chip.setOnClickListener(v -> {
                            Intent intent = new Intent(this, SearchResultActivity.class);
                            intent.putExtra("keyword", "#" + ten);
                            startActivity(intent);
                        });

                        chipGroupHashtags.addView(chip);
                        matched++;
                    }

                    if (matched > 0) {
                        showHashtagResults();
                    } else {
                        showEmptyState();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e("HASHTAG", "searchHashtags failed: " + e.getMessage());
                    showEmptyState();
                });
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

    private void loadHistory() {
        List<String> history = getHistory();
        if (history.isEmpty()) {
            layoutRecent.setVisibility(View.GONE);
            return;
        }
        renderHistory(history);
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

            chip.setOnClickListener(v -> {
                edtSearch.setText(keyword);
                edtSearch.setSelection(keyword.length());
            });

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

    private void setupSearch() {
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String keyword = s.toString().trim();
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);

                if (keyword.isEmpty()) {
                    showDefaultUI();
                    return;
                }

                searchRunnable = () -> {
                    if (keyword.startsWith("#")) {
                        String tag = keyword.substring(1).toLowerCase();
                        if (!tag.isEmpty()) searchHashtags(tag);
                        else showDefaultUI();
                    } else {
                        searchUsers(keyword);
                    }
                };
                searchHandler.postDelayed(searchRunnable, 300);
            }
        });

        edtSearch.setOnEditorActionListener((v, actionId, event) -> {
            String keyword = edtSearch.getText().toString().trim();
            if (!keyword.isEmpty()) saveHistory(keyword);
            return false;
        });
    }

    private void searchUsers(String keyword) {
        showLoading(true);

        db.collection("nguoi_dung")
                .get()
                .addOnSuccessListener(query -> {
                    showLoading(false);
                    List<User> results = new ArrayList<>();
                    for (User user : query.toObjects(User.class)) {
                        if (user.getUsername() != null &&
                                user.getUsername().toLowerCase().contains(keyword.toLowerCase())) {
                            results.add(user);
                        }
                    }
                    if (!results.isEmpty()) {
                        saveHistory(keyword);
                        showResults(results);
                    } else {
                        showEmptyState();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                });
    }

    // ── Visibility helpers ───────────────────────────────────────────────

    private void showDefaultUI() {
        shimmerLayout.setVisibility(View.GONE);
        shimmerLayout.stopShimmer();
        rvUsers.setVisibility(View.GONE);
        rvPosts.setVisibility(View.GONE);
        tvPostsHeader.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
        loadHistory();
        loadTrending();
        loadTopHashtags();
    }

    private void showLoading(boolean isLoading) {
        shimmerLayout.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (isLoading) {
            shimmerLayout.startShimmer();
            rvUsers.setVisibility(View.GONE);
            layoutRecent.setVisibility(View.GONE);
            layoutTopics.setVisibility(View.GONE);
            layoutTrending.setVisibility(View.GONE);
        } else {
            shimmerLayout.stopShimmer();
        }
    }

    private void showHashtagResults() {
        layoutRecent.setVisibility(View.GONE);
        layoutTrending.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
        rvUsers.setVisibility(View.GONE);
        layoutTopics.setVisibility(View.VISIBLE);
    }

    private void showResults(List<User> list) {
        layoutRecent.setVisibility(View.GONE);
        layoutTopics.setVisibility(View.GONE);
        layoutTrending.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
        rvUsers.setVisibility(View.VISIBLE);
        adapter.setList(list);
    }

    private void showEmptyState() {
        rvUsers.setVisibility(View.GONE);
        layoutRecent.setVisibility(View.GONE);
        layoutTopics.setVisibility(View.GONE);
        layoutTrending.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
    }
}