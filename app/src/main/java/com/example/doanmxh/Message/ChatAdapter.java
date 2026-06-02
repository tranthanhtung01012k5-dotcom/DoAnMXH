package com.example.doanmxh.Message;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doanmxh.R;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {
    private String highlightMessageId;
    private String selectedMessageId = null;
    private static final int TYPE_ME             = 1;
    private static final int TYPE_OTHER          = 2;
    private static final int TYPE_SHARE_ME       = 3;
    private static final int TYPE_SHARE_OTHER    = 4;

    public interface OnReplySelectedListener {
        void onReplySelected(ChatMessage message);
    }
    public interface OnMessageClickListener {
        void onScrollToMessage(String messageId);
    }

    private final List<ChatMessage> messageList;
    private final String myUid;
    private final String conversationId;
    private final FirebaseFirestore db;
    private final OnReplySelectedListener replyListener;
    private final OnMessageClickListener messageClickListener;
    private final SimpleDateFormat timeFormat =
            new SimpleDateFormat("HH:mm", Locale.getDefault());

    public ChatAdapter(List<ChatMessage> messageList,
                       String myUid,
                       String conversationId,
                       OnReplySelectedListener replyListener,
                       OnMessageClickListener messageClickListener) {
        this.messageList        = messageList;
        this.myUid              = myUid;
        this.conversationId     = conversationId;
        this.db                 = FirebaseFirestore.getInstance();
        this.replyListener      = replyListener;
        this.messageClickListener = messageClickListener;
    }

    // ════════════════════════════════════════════════════
    //  VIEW TYPE
    // ════════════════════════════════════════════════════
    @Override
    public int getItemViewType(int position) {
        ChatMessage msg = messageList.get(position);
        boolean isMe = myUid.equals(msg.getNguoiGuiId());
        if ("share_bai_viet".equals(msg.getLoai())) {
            return isMe ? TYPE_SHARE_ME : TYPE_SHARE_OTHER;
        }
        return isMe ? TYPE_ME : TYPE_OTHER;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout;
        switch (viewType) {
            case TYPE_SHARE_ME:    layout = R.layout.item_message_share_me;    break;
            case TYPE_SHARE_OTHER: layout = R.layout.item_message_share_other; break;
            case TYPE_ME:          layout = R.layout.item_message_me;          break;
            default:               layout = R.layout.item_message_other;       break;
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new MessageViewHolder(view);
    }

    // ════════════════════════════════════════════════════
    //  BIND
    // ════════════════════════════════════════════════════
    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage msg = messageList.get(position);
        int viewType = getItemViewType(position);

        // ── HIGHLIGHT ──
        if (highlightMessageId != null && highlightMessageId.equals(msg.getMessageId())) {
            holder.itemView.setBackgroundResource(R.drawable.bg_highlight);
        } else {
            holder.itemView.setBackgroundResource(0);
        }

        // ── ALPHA STATE ──
        boolean isSelected     = selectedMessageId != null;
        boolean isThisSelected = msg.getMessageId() != null && msg.getMessageId().equals(selectedMessageId);
        holder.itemView.setAlpha(isSelected && !isThisSelected ? 0.3f : 1f);

        // ── LONG CLICK ──
        holder.itemView.setOnLongClickListener(v -> {
            selectedMessageId = msg.getMessageId();
            notifyDataSetChanged();
            showMessageOptions(holder.itemView, msg);
            return true;
        });

        // ════════════════════════════════════════════════
        //  SHARE BAI VIET — layout riêng, bind riêng
        // ════════════════════════════════════════════════
        if (viewType == TYPE_SHARE_ME || viewType == TYPE_SHARE_OTHER) {
            bindSharePost(holder, msg);
            // TIME
            if (holder.txtTime != null && msg.getThoiGian() != null) {
                holder.txtTime.setText(timeFormat.format(msg.getThoiGian()));
            }
            return;
        }

        // ════════════════════════════════════════════════
        //  TIN NHẮN THƯỜNG
        // ════════════════════════════════════════════════
        TextView tvMessage    = holder.itemView.findViewById(R.id.txtMessageContent);
        ShapeableImageView imgMessage = holder.itemView.findViewById(R.id.imgMessageContent);

        // ── REPLY PREVIEW ──
        boolean hasReply = msg.getReplyToId() != null && !msg.getReplyToId().isEmpty();
        if (hasReply && holder.layoutReplyPreview != null) {
            holder.layoutReplyPreview.setVisibility(View.VISIBLE);
            if (holder.txtContent != null) holder.txtContent.setTranslationY(-20f);
            if (holder.txtReplyPreviewName != null)
                holder.txtReplyPreviewName.setText(msg.getReplyToSenderName());
            if (holder.txtReplyPreviewContent != null) {
                String replyContent = msg.getReplyToContent();
                holder.txtReplyPreviewContent.setText(
                        (replyContent != null && !replyContent.isEmpty())
                                ? replyContent : "[Hình ảnh]");
            }
        } else {
            if (holder.layoutReplyPreview != null)
                holder.layoutReplyPreview.setVisibility(View.GONE);
            if (holder.txtContent != null) holder.txtContent.setTranslationY(0f);
        }

        // ── REACTION ──
        if (holder.txtReaction != null) {
            String reaction = msg.getReaction();
            if (reaction != null && !reaction.trim().isEmpty()) {
                holder.txtReaction.setVisibility(View.VISIBLE);
                holder.txtReaction.setText(reaction);
            } else {
                holder.txtReaction.setVisibility(View.GONE);
            }
        }
    //KIỂM TRA XEM TIN NHẮN ĐƯỢC XÓA CHUA
        if (msg.isDa_xoa()) {

            tvMessage.setVisibility(View.VISIBLE);
            imgMessage.setVisibility(View.GONE);

            tvMessage.setText("Tin nhắn đã bị xóa");
            tvMessage.setTextColor(Color.DKGRAY);
            tvMessage.setTypeface(null, android.graphics.Typeface.ITALIC);

            if (holder.txtReaction != null) {
                holder.txtReaction.setVisibility(View.GONE);
            }

            return;
        }
        // ── CONTENT ──
        if ("image".equals(msg.getLoai())) {
            tvMessage.setVisibility(View.GONE);
            imgMessage.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext())
                    .load(msg.getNoiDung())
                    .placeholder(R.drawable.ic_placeholder_avatar)
                    .centerCrop()
                    .into(imgMessage);
        } else {
            tvMessage.setVisibility(View.VISIBLE);
            imgMessage.setVisibility(View.GONE);
            tvMessage.setTextColor(Color.WHITE);
            tvMessage.setPaintFlags(tvMessage.getPaintFlags()
                    & ~android.graphics.Paint.UNDERLINE_TEXT_FLAG);
            tvMessage.setText(msg.getNoiDung());
            tvMessage.setOnClickListener(null);
        }

        // ── TIME ──
        if (holder.txtTime != null) {
            if (msg.getThoiGian() != null)
                holder.txtTime.setText(timeFormat.format(msg.getThoiGian()));
            else
                holder.txtTime.setText("");
        }
    }

    // ════════════════════════════════════════════════════
    //  BIND SHARE POST CARD
    // ════════════════════════════════════════════════════
    private void bindSharePost(@NonNull MessageViewHolder holder, ChatMessage msg) {
        String postId = msg.getPostId();
        if (postId == null || postId.isEmpty()) return;

        ShapeableImageView ivAvatar = holder.itemView.findViewById(R.id.ivPostAvatar);
        TextView tvAuthor           = holder.itemView.findViewById(R.id.tvPostAuthor);
        TextView tvContent          = holder.itemView.findViewById(R.id.tvPostContent);
        ImageView ivImage           = holder.itemView.findViewById(R.id.ivPostImage);

        // Reset trước khi load để tránh ViewHolder tái sử dụng hiện dữ liệu cũ
        if (tvAuthor  != null) tvAuthor.setText("");
        if (tvContent != null) tvContent.setText("");
        if (ivImage   != null) ivImage.setVisibility(View.GONE);
        if (ivAvatar  != null) ivAvatar.setImageResource(R.drawable.ic_placeholder_avatar);

        db.collection("bai_viet").document(postId).get()
                .addOnSuccessListener(postDoc -> {
                    if (!postDoc.exists()) return;

                    String noiDung   = postDoc.getString("noi_dung");
                    String authorUid = postDoc.getString("nguoi_dung_id");
                    List<String> imgs = (List<String>) postDoc.get("hinh_anh");

                    if (tvContent != null)
                        tvContent.setText(noiDung != null ? noiDung : "");

                    if (ivImage != null) {
                        if (imgs != null && !imgs.isEmpty()) {
                            ivImage.setVisibility(View.VISIBLE);
                            Glide.with(holder.itemView.getContext())
                                    .load(imgs.get(0))
                                    .centerCrop()
                                    .into(ivImage);
                        } else {
                            ivImage.setVisibility(View.GONE);
                        }
                    }

                    if (authorUid != null) {
                        db.collection("nguoi_dung").document(authorUid).get()
                                .addOnSuccessListener(userDoc -> {
                                    if (tvAuthor != null)
                                        tvAuthor.setText(userDoc.getString("ten_dang_nhap"));
                                    String anh = userDoc.getString("anh_dai_dien");
                                    if (ivAvatar != null && anh != null && !anh.isEmpty()) {
                                        Glide.with(holder.itemView.getContext())
                                                .load(anh).circleCrop().into(ivAvatar);
                                    }
                                });
                    }
                });

        // Click → mở PostDetailActivity
        holder.itemView.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(
                    v.getContext(),
                    com.example.doanmxh.HomePage.PostDetailActivity.class
            );
            intent.putExtra(
                    com.example.doanmxh.HomePage.PostDetailActivity.EXTRA_POST_ID,
                    postId
            );
            v.getContext().startActivity(intent);
        });
    }

    private void clearSelection() {
        selectedMessageId = null;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() { return messageList.size(); }

    // ════════════════════════════════════════════════════
    //  DIALOG TÙY CHỌN
    // ════════════════════════════════════════════════════
    private void showMessageOptions(View anchor, ChatMessage msg) {
        View popupView = LayoutInflater.from(anchor.getContext())
                .inflate(R.layout.layout_message_actions, null);

        PopupWindow popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );
        popupWindow.setElevation(20f);
        popupWindow.setOnDismissListener(this::clearSelection);

        TextView emojiLove = popupView.findViewById(R.id.reactLove);
        TextView emojiHaha = popupView.findViewById(R.id.reactHaha);
        TextView emojiFire = popupView.findViewById(R.id.reactFire);
        TextView emojiLike = popupView.findViewById(R.id.reactLike);
        TextView emojiWow  = popupView.findViewById(R.id.reactEye);

        popupView.findViewById(R.id.btnReply).setOnClickListener(v -> {
            popupWindow.dismiss();
            if (replyListener != null) replyListener.onReplySelected(msg);
        });
        emojiLove.setOnClickListener(v -> { setReaction(msg, "❤️"); popupWindow.dismiss(); });
        emojiHaha.setOnClickListener(v -> { setReaction(msg, "😂"); popupWindow.dismiss(); });
        emojiFire.setOnClickListener(v -> { setReaction(msg, "🔥"); popupWindow.dismiss(); });
        emojiLike.setOnClickListener(v -> { setReaction(msg, "👍"); popupWindow.dismiss(); });
        emojiWow .setOnClickListener(v -> { setReaction(msg, "😮"); popupWindow.dismiss(); });

        popupView.findViewById(R.id.btnCopy).setOnClickListener(v -> {
            ClipboardManager clipboard =
                    (ClipboardManager) anchor.getContext()
                            .getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(
                    ClipData.newPlainText("message", msg.getNoiDung()));
            popupWindow.dismiss();
        });

        boolean isMyMessage = myUid.equals(msg.getNguoiGuiId());
        popupView.findViewById(R.id.btnEdit)
                .setVisibility(isMyMessage ? View.VISIBLE : View.GONE);
        popupView.findViewById(R.id.btnDelete)
                .setVisibility(isMyMessage ? View.VISIBLE : View.GONE);

        popupView.findViewById(R.id.btnEdit).setOnClickListener(v -> {
            popupWindow.dismiss();
            showEditDialog(anchor, msg);
        });
        popupView.findViewById(R.id.btnDelete).setOnClickListener(v -> {
            popupWindow.dismiss();
            confirmDelete(anchor, msg);
        });
        if (msg.isDa_xoa()) {

            popupView.findViewById(R.id.btnReply).setVisibility(View.GONE);
            popupView.findViewById(R.id.btnCopy).setVisibility(View.GONE);
            popupView.findViewById(R.id.btnEdit).setVisibility(View.GONE);
            popupView.findViewById(R.id.btnDelete).setVisibility(View.GONE);
            emojiLike.findViewById(R.id.reactLike).setVisibility(View.GONE);
            emojiHaha.findViewById(R.id.reactHaha).setVisibility(View.GONE);
            emojiFire.findViewById(R.id.reactFire).setVisibility(View.GONE);
            emojiWow.findViewById(R.id.reactEye).setVisibility(View.GONE);
            emojiLove.findViewById(R.id.reactLove).setVisibility(View.GONE);
        }
        int[] location = new int[2];
        anchor.getLocationOnScreen(location);
        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int height = popupView.getMeasuredHeight();
        popupWindow.showAtLocation(anchor, android.view.Gravity.NO_GRAVITY,
                location[0], location[1] - height);
    }

    private void setReaction(ChatMessage msg, String emoji) {
        String current = msg.getReaction();
        db.collection("cuoc_tro_chuyen")
                .document(conversationId)
                .collection("tin_nhan")
                .document(msg.getMessageId())
                .update("reaction", (current != null && current.equals(emoji)) ? null : emoji);
    }

    public void highlightMessage(String messageId) {
        highlightMessageId = messageId;
        notifyDataSetChanged();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            highlightMessageId = null;
            notifyDataSetChanged();
        }, 2000);
    }

    // ════════════════════════════════════════════════════
    //  CHỈNH SỬA
    // ════════════════════════════════════════════════════
    private void showEditDialog(View anchor, ChatMessage msg) {
        android.app.Dialog popup = new android.app.Dialog(anchor.getContext(),
                android.R.style.Theme_Material_Light_Dialog_NoActionBar);
        popup.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);

        android.widget.LinearLayout root = new android.widget.LinearLayout(anchor.getContext());
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setPadding(56, 48, 56, 40);

        android.graphics.drawable.GradientDrawable rootBg = new android.graphics.drawable.GradientDrawable();
        rootBg.setColor(android.graphics.Color.parseColor("#1E1E1E"));
        rootBg.setCornerRadius(32f);
        root.setBackground(rootBg);

        TextView title = new TextView(anchor.getContext());
        title.setText("Chỉnh sửa tin nhắn");
        title.setTextSize(17);
        title.setTextColor(android.graphics.Color.WHITE);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, 0, 0, 32);
        root.addView(title);

        TextView label = new TextView(anchor.getContext());
        label.setText("Nội dung mới");
        label.setTextSize(12);
        label.setTextColor(android.graphics.Color.parseColor("#888888"));
        label.setPadding(4, 0, 0, 8);
        root.addView(label);

        EditText input = new EditText(anchor.getContext());
        input.setText(msg.getNoiDung());
        input.setSelection(input.getText().length());
        input.setTextSize(15);
        input.setTextColor(android.graphics.Color.WHITE);
        input.setPadding(28, 24, 28, 24);
        input.setMinLines(2);
        input.setMaxLines(5);
        input.setGravity(android.view.Gravity.TOP);

        android.graphics.drawable.GradientDrawable inputBg = new android.graphics.drawable.GradientDrawable();
        inputBg.setColor(android.graphics.Color.parseColor("#2A2A2A"));
        inputBg.setCornerRadius(16f);
        inputBg.setStroke(1, android.graphics.Color.parseColor("#444444"));
        input.setBackground(inputBg);
        root.addView(input, new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView charCount = new TextView(anchor.getContext());
        charCount.setText(msg.getNoiDung().length() + " ký tự");
        charCount.setTextSize(11);
        charCount.setTextColor(android.graphics.Color.parseColor("#666666"));
        charCount.setGravity(android.view.Gravity.END);
        charCount.setPadding(0, 6, 4, 0);
        root.addView(charCount);

        input.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                charCount.setText(s.length() + " ký tự");
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        android.widget.LinearLayout btnRow = new android.widget.LinearLayout(anchor.getContext());
        btnRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        btnRow.setGravity(android.view.Gravity.END);
        btnRow.setPadding(0, 32, 0, 0);

        TextView btnCancel = new TextView(anchor.getContext());
        btnCancel.setText("Hủy");
        btnCancel.setTextSize(15);
        btnCancel.setTextColor(android.graphics.Color.parseColor("#888888"));
        btnCancel.setPadding(32, 20, 32, 20);
        btnCancel.setOnClickListener(v -> popup.dismiss());

        TextView btnSave = new TextView(anchor.getContext());
        btnSave.setText("Lưu");
        btnSave.setTextSize(15);
        btnSave.setTextColor(android.graphics.Color.WHITE);
        btnSave.setPadding(40, 20, 40, 20);

        android.graphics.drawable.GradientDrawable saveBg = new android.graphics.drawable.GradientDrawable();
        saveBg.setColor(android.graphics.Color.parseColor("#6C63FF"));
        saveBg.setCornerRadius(24f);
        btnSave.setBackground(saveBg);

        btnSave.setOnClickListener(v -> {
            String newContent = input.getText().toString().trim();
            if (newContent.isEmpty()) { input.setError("Không được để trống"); return; }
            if (newContent.equals(msg.getNoiDung())) { popup.dismiss(); return; }
            btnSave.setEnabled(false);
            btnSave.setText("Đang lưu...");
            db.collection("cuoc_tro_chuyen").document(conversationId)
                    .collection("tin_nhan").document(msg.getMessageId())
                    .update("noi_dung", newContent, "da_chinh_sua", true)
                    .addOnSuccessListener(unused -> {
                        popup.dismiss();
                        Toast.makeText(anchor.getContext(), "Đã chỉnh sửa", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(err -> {
                        btnSave.setEnabled(true);
                        btnSave.setText("Lưu");
                        Toast.makeText(anchor.getContext(), "Lỗi: " + err.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        btnRow.addView(btnCancel);
        btnRow.addView(btnSave);
        root.addView(btnRow);

        popup.setContentView(root);
        if (popup.getWindow() != null) {
            popup.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            popup.getWindow().setGravity(android.view.Gravity.CENTER);
            android.view.WindowManager.LayoutParams params = popup.getWindow().getAttributes();
            params.width = (int)(anchor.getContext().getResources()
                    .getDisplayMetrics().widthPixels * 0.88f);
            popup.getWindow().setAttributes(params);
            popup.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            popup.getWindow().getAttributes().dimAmount = 0.6f;
        }
        popup.show();
    }

    // ════════════════════════════════════════════════════
    //  XÓA
    // ════════════════════════════════════════════════════
    private void confirmDelete(View anchor, ChatMessage msg) {
        android.app.Dialog popup = new android.app.Dialog(anchor.getContext(),
                android.R.style.Theme_Material_Light_Dialog_NoActionBar);
        popup.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);

        android.widget.LinearLayout root = new android.widget.LinearLayout(anchor.getContext());
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setPadding(56, 48, 56, 40);

        android.graphics.drawable.GradientDrawable rootBg = new android.graphics.drawable.GradientDrawable();
        rootBg.setColor(android.graphics.Color.parseColor("#1E1E1E"));
        rootBg.setCornerRadius(32f);
        root.setBackground(rootBg);

        TextView title = new TextView(anchor.getContext());
        title.setText("🗑️  Xóa tin nhắn");
        title.setTextSize(17);
        title.setTextColor(android.graphics.Color.WHITE);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, 0, 0, 16);
        root.addView(title);

        TextView desc = new TextView(anchor.getContext());
        desc.setText("Tin nhắn sẽ bị xóa vĩnh viễn với cả hai phía. Bạn có chắc không?");
        desc.setTextSize(14);
        desc.setTextColor(android.graphics.Color.parseColor("#AAAAAA"));
        desc.setLineSpacing(6f, 1f);
        desc.setPadding(0, 0, 0, 32);
        root.addView(desc);

        android.widget.LinearLayout btnRow = new android.widget.LinearLayout(anchor.getContext());
        btnRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        btnRow.setGravity(android.view.Gravity.END);

        TextView btnCancel = new TextView(anchor.getContext());
        btnCancel.setText("Hủy");
        btnCancel.setTextSize(15);
        btnCancel.setTextColor(android.graphics.Color.parseColor("#888888"));
        btnCancel.setPadding(32, 20, 32, 20);
        btnCancel.setOnClickListener(v -> popup.dismiss());

        TextView btnDelete = new TextView(anchor.getContext());
        btnDelete.setText("Xóa");
        btnDelete.setTextSize(15);
        btnDelete.setTextColor(android.graphics.Color.WHITE);
        btnDelete.setPadding(40, 20, 40, 20);

        android.graphics.drawable.GradientDrawable deleteBg = new android.graphics.drawable.GradientDrawable();
        deleteBg.setColor(android.graphics.Color.parseColor("#CC3333"));
        deleteBg.setCornerRadius(24f);
        btnDelete.setBackground(deleteBg);

        btnDelete.setOnClickListener(v -> {
            btnDelete.setEnabled(false);
            btnDelete.setText("Đang xóa...");
            db.collection("cuoc_tro_chuyen").document(conversationId)
                    .collection("tin_nhan").document(msg.getMessageId())
                    .update(
                            "noi_dung", "Tin nhắn đã bị xóa",
                            "da_xoa", true,
                            "reaction", null,
                            "loai","text"
                    )
                    .addOnSuccessListener(unused -> {
                        popup.dismiss();
                        Toast.makeText(anchor.getContext(), "Đã xóa tin nhắn", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(err -> {
                        btnDelete.setEnabled(true);
                        btnDelete.setText("Xóa");
                        Toast.makeText(anchor.getContext(), "Lỗi: " + err.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        btnRow.addView(btnCancel);
        btnRow.addView(btnDelete);
        root.addView(btnRow);

        popup.setContentView(root);
        if (popup.getWindow() != null) {
            popup.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            popup.getWindow().setGravity(android.view.Gravity.CENTER);
            android.view.WindowManager.LayoutParams params = popup.getWindow().getAttributes();
            params.width = (int)(anchor.getContext().getResources()
                    .getDisplayMetrics().widthPixels * 0.85f);
            popup.getWindow().setAttributes(params);
            popup.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            popup.getWindow().getAttributes().dimAmount = 0.6f;
        }
        popup.show();
    }

    // ════════════════════════════════════════════════════
    //  VIEW HOLDER
    // ════════════════════════════════════════════════════
    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView txtContent, txtTime, txtReadReceipt;
        LinearLayout layoutReplyPreview;
        TextView txtReplyPreviewName, txtReplyPreviewContent, txtReaction;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            txtContent             = itemView.findViewById(R.id.txtMessageContent);
            txtTime                = itemView.findViewById(R.id.txtMessageTime);
            txtReadReceipt         = itemView.findViewById(R.id.txtReadReceipt);
            txtReaction            = itemView.findViewById(R.id.txtReaction);
            layoutReplyPreview     = itemView.findViewById(R.id.layoutReplyPreview);
            txtReplyPreviewName    = itemView.findViewById(R.id.txtReplyPreviewName);
            txtReplyPreviewContent = itemView.findViewById(R.id.txtReplyPreviewContent);
        }
    }
}
