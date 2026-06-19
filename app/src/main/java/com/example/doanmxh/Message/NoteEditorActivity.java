package com.example.doanmxh.Message;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.doanmxh.R;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Màn hình tạo / chỉnh ghi chú kiểu Messenger.
 *
 * Dữ liệu ghi chú được lưu trực tiếp trên document người dùng (collection "nguoi_dung"):
 *   - ghi_chu            : String  — nội dung ghi chú
 *   - ngay_tao_ghi_chu   : Timestamp — thời điểm tạo, dùng để tự hết hạn sau 24h
 *
 * Ghi chú tự hết hạn sau {@link #NOTE_TTL_MS} (24 giờ) — các màn hình hiển thị
 * (HomeFragment / fragment_messages) cần tự kiểm tra thời gian này khi load.
 */
public class NoteEditorActivity extends AppCompatActivity {

    public static final String EXTRA_CURRENT_NOTE = "current_note";

    private static final int MAX_NOTE_LENGTH = 60;
    /** Ghi chú tự hết hạn sau 24 giờ, giống Messenger */
    public static final long NOTE_TTL_MS = 24 * 60 * 60 * 1000L;

    private static final String[] SUGGESTIONS = {
            "Đang bận 🙏", "Rảnh đi chơi 🎉", "Đang ngủ 😴",
            "Đang học 📚", "Đói bụng 🍜", "Cafe không? ☕"
    };

    private EditText edtNoteContent;
    private TextView tvCharCount;
    private TextView tvPreviewNote;
    private TextView btnShareNote;
    private TextView btnRemoveNote;
    private View flPreviewBubble;
    private ShapeableImageView imgPreviewAvatar;
    private LinearLayout layoutSuggestions;
    private ListenerRegistration noteListener;
    private FirebaseFirestore db;
    private String myUid;
    private String existingNote;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);

        db    = FirebaseFirestore.getInstance();
        myUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        existingNote = getIntent().getStringExtra(EXTRA_CURRENT_NOTE);

        bindViews();
        loadMyAvatar();
        setupSuggestionChips();

        if (!TextUtils.isEmpty(existingNote)) {
            edtNoteContent.setText(existingNote);
            edtNoteContent.setSelection(existingNote.length());
            btnRemoveNote.setVisibility(View.VISIBLE);
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        edtNoteContent.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePreview(s.toString());
            }

            @Override public void afterTextChanged(Editable s) {}
        });

        btnShareNote.setOnClickListener(v -> saveNote());
        btnRemoveNote.setOnClickListener(v -> removeNote());

        // Khởi tạo preview ban đầu
        updatePreview(edtNoteContent.getText().toString());
    }

    private void bindViews() {
        edtNoteContent   = findViewById(R.id.edtNoteContent);
        tvCharCount      = findViewById(R.id.tvCharCount);
        tvPreviewNote    = findViewById(R.id.tvPreviewNote);
        btnShareNote     = findViewById(R.id.btnShareNote);
        btnRemoveNote    = findViewById(R.id.btnRemoveNote);
        flPreviewBubble  = findViewById(R.id.flPreviewBubble);
        imgPreviewAvatar = findViewById(R.id.imgPreviewAvatar);
        layoutSuggestions = findViewById(R.id.layoutSuggestions);
    }

    private void loadMyAvatar() {
        if (myUid == null || isFinishing()) return;
        db.collection("nguoi_dung").document(myUid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists() || isFinishing()) return;
                    String avatar = doc.getString("anh_dai_dien");
                    if (!TextUtils.isEmpty(avatar)) {
                        Glide.with(this)
                                .load(avatar)
                                .placeholder(R.drawable.ic_placeholder_avatar)
                                .into(imgPreviewAvatar);
                    }
                });
    }

    /** Tạo các chip gợi ý nhanh, bấm vào sẽ điền thẳng vào ô nhập */
    private void setupSuggestionChips() {
        List<String> suggestions = Arrays.asList(SUGGESTIONS);
        LayoutInflater inflater = LayoutInflater.from(this);

        for (String text : suggestions) {
            TextView chip = (TextView) inflater.inflate(
                    R.layout.item_note_suggestion_chip, layoutSuggestions, false);
            chip.setText(text);
            chip.setOnClickListener(v -> {
                edtNoteContent.setText(text);
                edtNoteContent.setSelection(text.length());
            });
            layoutSuggestions.addView(chip);
        }
    }

    private void updatePreview(String text) {
        int length = text.length();
        tvCharCount.setText(length + "/" + MAX_NOTE_LENGTH);

        if (TextUtils.isEmpty(text.trim())) {
            flPreviewBubble.setVisibility(View.GONE);
            setShareEnabled(false);
        } else {
            flPreviewBubble.setVisibility(View.VISIBLE);
            tvPreviewNote.setText(text);
            setShareEnabled(true);
        }
    }

    private void setShareEnabled(boolean enabled) {
        btnShareNote.setEnabled(enabled);
        btnShareNote.setAlpha(enabled ? 1f : 0.5f);
    }

    private void saveNote() {
        if (myUid == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        String content = edtNoteContent.getText().toString().trim();
        if (content.isEmpty()) return;

        if (content.length() > MAX_NOTE_LENGTH) {
            content = content.substring(0, MAX_NOTE_LENGTH);
        }

        btnShareNote.setEnabled(false);

        Map<String, Object> data = new HashMap<>();
        data.put("ghi_chu", content);
        data.put("ngay_tao_ghi_chu", Timestamp.now());

        db.collection("nguoi_dung").document(myUid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Đã chia sẻ ghi chú", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnShareNote.setEnabled(true);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void removeNote() {
        if (myUid == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put("ghi_chu", "");
        data.put("ngay_tao_ghi_chu", null);

        db.collection("nguoi_dung").document(myUid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Đã xóa ghi chú", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /**
     * Helper dùng ở nơi khác (HomeFragment, fragment_messages...) để kiểm tra
     * ghi chú còn hiệu lực hay đã quá 24h.
     */
    public static boolean isNoteExpired(@Nullable Timestamp ngayTaoGhiChu) {
        if (ngayTaoGhiChu == null) return true;
        long createdAtMs = ngayTaoGhiChu.toDate().getTime();
        return (System.currentTimeMillis() - createdAtMs) > NOTE_TTL_MS;
    }
}