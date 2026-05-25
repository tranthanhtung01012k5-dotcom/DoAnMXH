package com.example.doanmxh.ProfilePage;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doanmxh.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class FollowBottomSheet extends BottomSheetDialogFragment {

    private RecyclerView rvFollow;
    private TabLayout tabLayout;
    private EditText edtSearch;
    private ImageView btnClose;

    private final List<FollowModel> list = new ArrayList<>();
    private final List<FollowModel> originalList = new ArrayList<>();

    private FollowAdapter adapter;
    private FirebaseFirestore db;

    private final String userId;

    // CALLBACK
    public interface OnDismissListener {
        void onDismissed();
    }

    private OnDismissListener listener;

    public FollowBottomSheet(String userId, OnDismissListener listener) {
        this.userId = userId;
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {

        View view = inflater.inflate(R.layout.dialog_follow, container, false);

        initViews(view);

        db = FirebaseFirestore.getInstance();

        adapter = new FollowAdapter(list);

        rvFollow.setLayoutManager(new LinearLayoutManager(getContext()));
        rvFollow.setAdapter(adapter);

        // TAB
        tabLayout.addTab(tabLayout.newTab().setText("Người theo dõi (0)"));
        tabLayout.addTab(tabLayout.newTab().setText("Đang theo dõi (0)"));

        loadTabCounts();

        loadFollowing();

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {

            @Override
            public void onTabSelected(TabLayout.Tab tab) {

                if (tab.getPosition() == 0) {

                    loadFollowing();

                } else {

                    loadFollowers();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        btnClose.setOnClickListener(v -> dismiss());

        edtSearch.addTextChangedListener(
                new android.text.TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after
                    ) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count
                    ) {

                        filterUsers(s.toString());
                    }

                    @Override
                    public void afterTextChanged(
                            android.text.Editable s
                    ) {
                    }
                });

        return view;
    }

    // DISMISS CALLBACK
    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);

        if (listener != null) {
            listener.onDismissed();
        }
    }

    private void initViews(View view) {

        rvFollow = view.findViewById(R.id.rvFollow);
        tabLayout = view.findViewById(R.id.tabLayout);
        edtSearch = view.findViewById(R.id.edtSearch);
        btnClose = view.findViewById(R.id.btnClose);
    }

    private void loadTabCounts() {

        db.collection("nguoi_dung")
                .document(userId)
                .collection("nguoi_dang_theo_doi")
                .get()
                .addOnSuccessListener(snap -> {

                    int count = snap.size();

                    TabLayout.Tab tab = tabLayout.getTabAt(0);

                    if (tab != null) {
                        tab.setText("Người theo dõi (" + count + ")");
                    }
                });

        db.collection("nguoi_dung")
                .document(userId)
                .collection("nguoi_theo_doi")
                .get()
                .addOnSuccessListener(snap -> {

                    int count = snap.size();

                    TabLayout.Tab tab = tabLayout.getTabAt(1);

                    if (tab != null) {
                        tab.setText("Đang theo dõi (" + count + ")");
                    }
                });
    }

    private void loadFollowers() {

        list.clear();
        originalList.clear();

        adapter.notifyDataSetChanged();

        db.collection("nguoi_dung")
                .document(userId)
                .collection("nguoi_theo_doi")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    for (var doc : queryDocumentSnapshots) {
                        loadUser(doc.getId());
                    }
                });
    }

    private void loadFollowing() {

        list.clear();
        originalList.clear();

        adapter.notifyDataSetChanged();

        db.collection("nguoi_dung")
                .document(userId)
                .collection("nguoi_dang_theo_doi")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    for (var doc : queryDocumentSnapshots) {
                        loadUser(doc.getId());
                    }
                });
    }

    private void loadUser(String uid) {

        db.collection("nguoi_dung")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (!documentSnapshot.exists()) return;

                    String avatar =
                            documentSnapshot.getString("anh_dai_dien");

                    FollowModel model = new FollowModel(
                            uid,
                            documentSnapshot.getString("ten_dang_nhap"),
                            documentSnapshot.getString("ho_va_ten"),

                            avatar != null && !avatar.isEmpty()
                                    ? avatar
                                    : String.valueOf(
                                    R.drawable.ic_person_outline_24
                            )
                    );

                    originalList.add(model);

                    list.clear();
                    list.addAll(originalList);

                    adapter.notifyDataSetChanged();
                });
    }

    private void filterUsers(String keyword) {

        list.clear();

        if (keyword == null || keyword.trim().isEmpty()) {

            list.addAll(originalList);

        } else {

            String lower = keyword.toLowerCase().trim();

            for (FollowModel model : originalList) {

                String username =
                        model.getUsername() != null
                                ? model.getUsername().toLowerCase()
                                : "";

                String name =
                        model.getName() != null
                                ? model.getName().toLowerCase()
                                : "";

                if (username.contains(lower)
                        || name.contains(lower)) {

                    list.add(model);
                }
            }
        }

        adapter.notifyDataSetChanged();
    }
}