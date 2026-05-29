package com.example.doanmxh.Mention;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.text.*;
import android.text.method.ArrowKeyMovementMethod;
import android.text.style.ClickableSpan;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.ArrayList;
import java.util.List;
import com.example.doanmxh.R;

public class MentionHelper {

    public interface OnMentionInserted {
        /** Gọi sau khi người dùng chọn xong 1 mention */
        void onInserted(MentionUser user);
    }

    private final Context context;
    private final EditText editText;
    private final FirebaseFirestore db;
    private final String myUid;
    private final OnMentionInserted callback; // nullable

    private PopupWindow mentionPopup;
    private MentionAdapter mentionAdapter;
    private final List<MentionUser> mentionList     = new ArrayList<>();
    private final List<MentionUser> mentionFiltered = new ArrayList<>();
    private boolean isShowing = false;

    // ── Constructor ──────────────────────────────────────────────────────────
    public MentionHelper(Context context,
                         EditText editText,
                         @NonNull FirebaseFirestore db,
                         OnMentionInserted callback) {
        this.context  = context;
        this.editText = editText;
        this.db       = db;
        this.callback = callback;
        this.myUid    = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        setupPopup();
        loadFriends();
        attachWatcher();
    }

    // ── Setup popup ──────────────────────────────────────────────────────────
    private void setupPopup() {
        View popupView = LayoutInflater.from(context)
                .inflate(R.layout.layout_mention_popup, null);

        RecyclerView rv = popupView.findViewById(R.id.rvMentionSuggestions);
        rv.setLayoutManager(new LinearLayoutManager(context));

        mentionAdapter = new MentionAdapter(mentionFiltered, user -> {
            insertMention(user);
            dismiss();
            if (callback != null) callback.onInserted(user);
        });
        rv.setAdapter(mentionAdapter);

        mentionPopup = new PopupWindow(popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, false);
        mentionPopup.setElevation(16f);
        mentionPopup.setOutsideTouchable(true);
    }

    // ── TextWatcher theo dõi ký tự @ ─────────────────────────────────────────
    private void attachWatcher() {
        editText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}

            @Override
            public void afterTextChanged(Editable s) {
                int cursor = editText.getSelectionStart();
                if (cursor <= 0) { dismiss(); return; }

                String before = s.subSequence(0, cursor).toString();
                int atIdx = before.lastIndexOf('@');
                if (atIdx < 0) { dismiss(); return; }

                String query = before.substring(atIdx + 1);
                if (query.contains(" ")) { dismiss(); return; }

                filter(query);
                if (mentionFiltered.isEmpty()) { dismiss(); return; }
                show();
            }
        });
    }

    // ── Filter ───────────────────────────────────────────────────────────────
    private void filter(String query) {
        mentionFiltered.clear();
        String lq = query.toLowerCase();
        for (MentionUser u : mentionList) {
            if (u.tenDangNhap.toLowerCase().contains(lq)
                    || u.hoVaTen.toLowerCase().contains(lq)) {
                mentionFiltered.add(u);
            }
        }
        if (mentionAdapter != null) mentionAdapter.notifyDataSetChanged();
    }

    // ── Hiện popup ───────────────────────────────────────────────────────────
    private void show() {
        if (mentionPopup == null || !editText.isAttachedToWindow()) return;

        mentionPopup.getContentView().measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

        int[] loc = new int[2];
        editText.getLocationInWindow(loc);
        int popupH = mentionPopup.getContentView().getMeasuredHeight();

        if (!isShowing) {
            mentionPopup.showAtLocation(editText, Gravity.NO_GRAVITY,
                    loc[0] + 16, loc[1] - popupH - 16);
            isShowing = true;
        } else {
            mentionPopup.update();
        }
    }

    // ── Ẩn popup ─────────────────────────────────────────────────────────────
    public void dismiss() {
        if (isShowing && mentionPopup != null) {
            mentionPopup.dismiss();
            isShowing = false;
        }
    }

    // ── Chèn mention màu xanh vào EditText ───────────────────────────────────
    private void insertMention(MentionUser user) {
        Editable text   = editText.getText();
        int cursor       = editText.getSelectionStart();
        String before    = text.subSequence(0, cursor).toString();
        int atIdx        = before.lastIndexOf('@');
        if (atIdx < 0) return;

        text.delete(atIdx, cursor);

        String mention = "@" + user.tenDangNhap;
        SpannableString sp = new SpannableString(mention + " ");

        sp.setSpan(new ClickableSpan() {
            @Override public void onClick(@NonNull android.view.View w) {
                Intent intent = new Intent(context,
                        com.example.doanmxh.ProfilePage.UserProfileActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("user_uid", user.uid);
                context.startActivity(intent);
            }
            @Override public void updateDrawState(@NonNull android.text.TextPaint ds) {
                ds.setColor(Color.parseColor("#4A90E2"));
                ds.setFakeBoldText(true);
                ds.setUnderlineText(false);
            }
        }, 0, mention.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        text.insert(atIdx, sp);
        editText.setSelection(atIdx + sp.length());
        editText.setMovementMethod(new ArrowKeyMovementMethod());
    }

    // ── Load bạn bè từ Firestore ──────────────────────────────────────────────
    private void loadFriends() {
        if (myUid == null) return;
        db.collection("nguoi_dung").document(myUid)
                .collection("nguoi_dang_theo_doi")
                .limit(100)
                .get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String uid = doc.getString("nguoi_dung_id");
                        if (uid == null) continue;
                        db.collection("nguoi_dung").document(uid).get()
                                .addOnSuccessListener(uDoc -> {
                                    if (!uDoc.exists()) return;
                                    String ten = uDoc.getString("ten_dang_nhap");
                                    if (ten == null) return;
                                    mentionList.add(new MentionUser(
                                            uid,
                                            uDoc.getString("ho_va_ten") != null
                                                    ? uDoc.getString("ho_va_ten") : "",
                                            ten,
                                            uDoc.getString("anh_dai_dien") != null
                                                    ? uDoc.getString("anh_dai_dien") : ""
                                    ));
                                });
                    }
                });
    }

    /**
     * Thêm user vào danh sách gợi ý thủ công
     * (dùng khi trang cha đã có sẵn danh sách user, không cần load lại)
     */
    public void addUsers(List<MentionUser> users) {
        mentionList.addAll(users);
    }
}