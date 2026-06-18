package com.example.doanmxh.HomePage;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doanmxh.Notifications.NotificationsFragment;
import com.example.doanmxh.ProfilePage.UserProfileActivity;
import com.example.doanmxh.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import android.content.Intent;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommentAdapter
        extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private List<CommentModel> commentList;
    private String postId;
    private OnCommentActionListener listener;

    public interface OnCommentActionListener {
        void onReplyClick(CommentModel comment, int position);
        void onLikeClick(CommentModel comment, int position);
        void onAvatarClick(CommentModel comment, int position);
        void onAddFriendClick(CommentModel comment, int position);
        void onEditComment(CommentModel comment, int position, String newContent);
        void onDeleteComment(CommentModel comment, int position);
        void onCommentClick(CommentModel comment, int position);
    }

    public CommentAdapter(List<CommentModel> commentList,
                          String postId,
                          OnCommentActionListener listener) {
        this.commentList = commentList;
        this.postId      = postId;
        this.listener    = listener;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        if (position >= commentList.size()) return;

        CommentModel comment = commentList.get(position);

        // =========================
        // INDENT REPLY
        // =========================
        boolean isReply = comment.getBinhLuanChaId() != null
                && !comment.getBinhLuanChaId().isEmpty();

        float density = holder.itemView.getContext()
                .getResources().getDisplayMetrics().density;
        int indentPx = (int) (52 * density);

        ViewGroup.MarginLayoutParams params =
                (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
        params.leftMargin = isReply ? indentPx : 0;
        holder.itemView.setLayoutParams(params);

        // =========================
        // THREAD LINE
        // =========================
        holder.viewParentLine.setVisibility(isReply ? View.VISIBLE : View.GONE);

        boolean nextIsReply = (position + 1 < commentList.size())
                && commentList.get(position + 1).getBinhLuanChaId() != null
                && commentList.get(position + 1).getBinhLuanChaId()
                .equals(comment.getDocumentId());
        holder.viewThreadLine.setVisibility(nextIsReply ? View.VISIBLE : View.INVISIBLE);

        // =========================
        // AUTHOR
        // =========================
        holder.tvAuthorName.setText(
                comment.getHoVaTen() != null ? comment.getHoVaTen() : "Người dùng");

        // =========================
        // TIME
        // =========================
        if (comment.getNgayTao() != null) {
            holder.tvTime.setText(formatTime(comment.getNgayTao().toDate()));
        } else {
            holder.tvTime.setText("");
        }

        // =========================
        // CONTENT + MENTION SPAN
        // =========================
        String content = comment.getNoiDung() != null ? comment.getNoiDung() : "";

        SpannableString spannable = new SpannableString(content);

        Pattern pattern = Pattern.compile("@[\\p{L}\\p{N}_]+");

        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            int start = matcher.start();
            int end   = matcher.end();
            String mentionText = content.substring(start, end);

            ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.parseColor("#4DA6FF"));
            StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);

            // ✅ Query Firestore theo ten_dang_nhap để lấy đúng uid người được @
            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    Context context = holder.itemView.getContext();
                    String ten = mentionText.substring(1); // bỏ ký tự @
//                    String ten = context;

                    FirebaseFirestore.getInstance()
                            .collection("nguoi_dung")
                            .whereEqualTo("ten_dang_nhap", ten)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(query -> {
                                if (!query.isEmpty()) {
                                    String uid = query.getDocuments().get(0).getId();
                                    Intent intent = new Intent(context, UserProfileActivity.class);
                                    intent.putExtra("user_uid", uid);
                                    context.startActivity(intent);
                                } else {
                                    Log.e("MENTION_CLICK", "Không tìm thấy user: " + ten);
                                }
                            })
                            .addOnFailureListener(e ->
                                    Log.e("MENTION_CLICK", "Lỗi query: " + e.getMessage()));
                }

                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setColor(Color.parseColor("#4DA6FF"));
                    ds.setFakeBoldText(true);
                    ds.setUnderlineText(false);
                    ds.setShadowLayer(12, 0, 0, Color.parseColor("#4DA6FF"));
                }
            };

            spannable.setSpan(colorSpan,      start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(boldSpan,        start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(clickableSpan,   start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        holder.tvContent.setText(spannable);
        holder.tvContent.setMovementMethod(LinkMovementMethod.getInstance());
        holder.tvContent.setHighlightColor(Color.TRANSPARENT);

        // =========================
        // VERIFIED
        // =========================
        holder.ivVerified.setVisibility(comment.isVerified() ? View.VISIBLE : View.GONE);

        // =========================
        // AVATAR
        // =========================
        if (comment.getAnhDaiDien() != null && !comment.getAnhDaiDien().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(comment.getAnhDaiDien())
                    .placeholder(R.drawable.ic_placeholder_avatar)
                    .error(R.drawable.ic_placeholder_avatar)
                    .circleCrop()
                    .into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(R.drawable.ic_placeholder_avatar);
        }

        // =========================
        // FOLLOW BUTTON
        // =========================
        String myUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        boolean laBanThan = myUid != null && myUid.equals(comment.getNguoiDungId());
        boolean daFollow  = comment.isFollowing();

        if (laBanThan || daFollow) {
            holder.ivAddFriend.setVisibility(View.GONE);
            holder.ivAddFriend.setOnClickListener(null);
        } else {
            holder.ivAddFriend.setVisibility(View.VISIBLE);
            holder.ivAddFriend.setOnClickListener(v -> {
                if (listener != null)
                    listener.onAddFriendClick(comment, holder.getBindingAdapterPosition());
            });
        }

        // =========================
        // LIKE COUNT
        // =========================
        int soLike = comment.getSoLike();
        holder.tvLikeCount.setText(soLike > 0 ? String.valueOf(soLike) : "");

        // =========================
        // LIKE UI
        // =========================
        if (comment.isLikedByMe()) {
            holder.btnLike.setImageResource(R.drawable.ic_heart_filled_24);
            holder.btnLike.setColorFilter(Color.RED);
            holder.tvLikeCount.setTextColor(Color.RED);
        } else {
            holder.btnLike.setImageResource(R.drawable.ic_heart_outline_24);
            holder.btnLike.clearColorFilter();
            holder.tvLikeCount.setTextColor(Color.parseColor("#888888"));
        }

        // =========================
        // LIKE CLICK
        // =========================
        holder.btnLike.setOnClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) return;

            CommentModel currentComment = commentList.get(adapterPosition);
            String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                    : null;
            if (uid == null) return;

            // ← Thêm dòng này
            String effectivePostId = (currentComment.getPostId() != null && !currentComment.getPostId().isEmpty())
                    ? currentComment.getPostId()
                    : postId;
            if (effectivePostId == null || effectivePostId.isEmpty()) return; // ← chặn crash

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            String commentId = currentComment.getDocumentId();

            db.collection("bai_viet").document(effectivePostId) // ← đổi postId → effectivePostId
                    .collection("binh_luan").document(commentId)
                    .collection("luot_thich").document(uid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        boolean daLike = documentSnapshot.exists();
                        if (daLike) {
                            documentSnapshot.getReference().delete();
                            db.collection("bai_viet").document(effectivePostId) // ← đổi
                                    .collection("binh_luan").document(commentId)
                                    .update("so_like", FieldValue.increment(-1));
                            currentComment.setLikedByMe(false);
                            currentComment.setSoLike(Math.max(0, currentComment.getSoLike() - 1));
                        } else {
                            Map<String, Object> likeData = new HashMap<>();
                            likeData.put("nguoi_dung_id", uid);
                            likeData.put("ngay_like", new Date());
                            db.collection("bai_viet").document(effectivePostId) // ← đổi
                                    .collection("binh_luan").document(commentId)
                                    .collection("luot_thich").document(uid)
                                    .set(likeData);
                            db.collection("bai_viet").document(effectivePostId) // ← đổi
                                    .collection("binh_luan").document(commentId)
                                    .update("so_like", FieldValue.increment(1));
                            currentComment.setLikedByMe(true);
                            currentComment.setSoLike(currentComment.getSoLike() + 1);
                            String commentOwnerId = currentComment.getNguoiDungId();
                            String commentPreview = currentComment.getNoiDung(); // preview nội dung BL

                            if (commentOwnerId != null && !commentOwnerId.equals(uid)) {
                                NotificationsFragment.sendLikeCommentNotification(
                                        commentOwnerId,  // receiverId = chủ bình luận
                                        uid,             // senderId   = người đang like
                                        commentId,       // commentId  để click mở đúng BL
                                        effectivePostId, // postId     để mở bài viết
                                        commentPreview   // preview hiển thị trong thông báo
                                );
                            }
                        }
                        if (listener != null)
                            listener.onLikeClick(currentComment, adapterPosition);
                        notifyItemChanged(adapterPosition);
                    });
        });

        // =========================
        // REPLY
        // =========================
        holder.tvReply.setOnClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) return;
            if (listener != null)
                listener.onReplyClick(commentList.get(adapterPosition), adapterPosition);
        });

        // =========================
        // OPEN PROFILE (AVATAR + TÊN)
        // =========================
        holder.ivAvatar.setOnClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) return;
            if (listener != null)
                listener.onAvatarClick(commentList.get(adapterPosition), adapterPosition);
        });

        holder.tvAuthorName.setOnClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) return;
            if (listener != null)
                listener.onAvatarClick(commentList.get(adapterPosition), adapterPosition);
        });
        holder.itemView.setOnClickListener(v -> {

            if (listener != null) {

                listener.onCommentClick(comment,
                        holder.getAdapterPosition());
            }
        });
        // =========================
        // MORE (⋯) — POPUP XÓA / SỬA
        // =========================
        if (laBanThan) {
            holder.btnMore.setVisibility(View.VISIBLE);
            holder.btnMore.setOnClickListener(v ->
                    showMorePopup(v, comment, holder.getBindingAdapterPosition()));
        } else {
            holder.btnMore.setVisibility(View.INVISIBLE);
            holder.btnMore.setOnClickListener(null);
        }
    }

    // ════════════════════════════════════════════════════════
    //  POPUP MENU — SỬA / XÓA
    // ════════════════════════════════════════════════════════
    private void showMorePopup(View anchor, CommentModel comment, int position) {
        Context ctx = anchor.getContext();
        PopupMenu popup = new PopupMenu(ctx, anchor);
        popup.getMenu().add(0, 1, 0, "✏️  Sửa bình luận");
        popup.getMenu().add(0, 2, 1, "🗑️  Xóa bình luận");
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                showEditDialog(ctx, comment, position);
                return true;
            } else if (item.getItemId() == 2) {
                showDeleteConfirm(ctx, comment, position);
                return true;
            }
            return false;
        });
        popup.show();
    }

    // ────────────────────────────────────────────────────────
    //  DIALOG SỬA BÌNH LUẬN
    // ────────────────────────────────────────────────────────
    private void showEditDialog(Context ctx, CommentModel comment, int position) {
        String effectivePostId = (comment.getPostId() != null && !comment.getPostId().isEmpty())
                ? comment.getPostId()
                : postId;

        if (effectivePostId == null || effectivePostId.isEmpty()) return;

        View dialogView = LayoutInflater.from(ctx)
                .inflate(R.layout.dialog_edit_comment, null);

        TextInputEditText etContent = dialogView.findViewById(R.id.etEditComment);
        MaterialButton btnCancel    = dialogView.findViewById(R.id.btnCancel);
        MaterialButton btnSave      = dialogView.findViewById(R.id.btnSave);

        etContent.setText(comment.getNoiDung());
        if (etContent.getText() != null)
            etContent.setSelection(etContent.getText().length());

        // Dialog không viền, nền trong suốt để MaterialCardView tự bo góc
        android.app.Dialog dialog = new android.app.Dialog(ctx);
        dialog.setContentView(dialogView);
        dialog.getWindow().setBackgroundDrawable(
                new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.getWindow().setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCancelable(true);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String newContent = etContent.getText() != null
                    ? etContent.getText().toString().trim() : "";
            if (newContent.isEmpty()) return;

            FirebaseFirestore.getInstance()
                    .collection("bai_viet").document(effectivePostId)
                    .collection("binh_luan").document(comment.getDocumentId())
                    .update("noi_dung", newContent)
                    .addOnSuccessListener(unused -> {
                        comment.setNoiDung(newContent);
                        notifyItemChanged(position);
                        if (listener != null)
                            listener.onEditComment(comment, position, newContent);
                        dialog.dismiss();
                    });
        });

        dialog.show();
    }

    // ────────────────────────────────────────────────────────
    //  DIALOG XÁC NHẬN XÓA
    // ────────────────────────────────────────────────────────
    private void showDeleteConfirm(Context ctx,
                                   CommentModel comment,
                                   int position) {

        String effectivePostId =
                (comment.getPostId() != null
                        && !comment.getPostId().isEmpty())
                        ? comment.getPostId()
                        : postId;

        if (effectivePostId == null
                || effectivePostId.isEmpty()) {
            return;
        }

        new AlertDialog.Builder(ctx, R.style.DarkAlertDialog)
                .setTitle("Xóa bình luận")
                .setMessage("Bạn có chắc muốn xóa bình luận này không?")
                .setPositiveButton("Xóa", (dialog, which) -> {

                    FirebaseFirestore db =
                            FirebaseFirestore.getInstance();

                    String commentId =
                            comment.getDocumentId();

                    // ====================================
                    // Tìm tất cả comment con
                    // ====================================

                    db.collection("bai_viet")
                            .document(effectivePostId)
                            .collection("binh_luan")
                            .whereEqualTo(
                                    "binh_luan_cha_id",
                                    commentId
                            )
                            .get()
                            .addOnSuccessListener(query -> {

                                // ====================================
                                // Chuyển comment con thành comment thường
                                // ====================================

                                for (DocumentSnapshot doc
                                        : query.getDocuments()) {

                                    String childId = doc.getId();

                                    // Update Firestore
                                    doc.getReference()
                                            .update(
                                                    "binh_luan_cha_id",
                                                    ""
                                            );

                                    // Update local list
                                    for (CommentModel item : commentList) {

                                        if (item.getDocumentId()
                                                .equals(childId)) {

                                            item.setBinhLuanChaId("");

                                            break;
                                        }
                                    }
                                }

                                // ====================================
                                // Xóa comment cha
                                // ====================================

                                db.collection("bai_viet")
                                        .document(effectivePostId)
                                        .collection("binh_luan")
                                        .document(commentId)
                                        .delete()
                                        .addOnSuccessListener(unused -> {

                                            // ====================================
                                            // Xóa local object theo object
                                            // Không xóa theo position
                                            // ====================================

                                            commentList.remove(comment);

                                            // ====================================
                                            // Render lại toàn bộ
                                            // ====================================

                                            notifyDataSetChanged();

                                            // ====================================
                                            // Callback
                                            // ====================================

                                            if (listener != null) {
                                                listener.onDeleteComment(
                                                        comment,
                                                        position
                                                );
                                            }
                                        })
                                        .addOnFailureListener(e ->
                                                Log.e(
                                                        "DELETE_COMMENT",
                                                        "Xóa comment thất bại: "
                                                                + e.getMessage()
                                                ));
                            })
                            .addOnFailureListener(e ->
                                    Log.e(
                                            "DELETE_COMMENT",
                                            "Lỗi xử lý reply: "
                                                    + e.getMessage()
                                    ));
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // ════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════
    private String formatTime(Date date) {
        if (date == null) return "";
        long diff    = new Date().getTime() - date.getTime();
        long seconds = TimeUnit.MILLISECONDS.toSeconds(diff);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
        long hours   = TimeUnit.MILLISECONDS.toHours(diff);
        long days    = TimeUnit.MILLISECONDS.toDays(diff);
        if (seconds < 60)      return "vừa xong";
        else if (minutes < 60) return minutes + " phút";
        else if (hours < 24)   return hours + " giờ";
        else if (days < 7)     return days + " ngày";
        else                   return (days / 7) + " tuần";
    }

    @Override
    public int getItemCount() {
        return commentList != null ? commentList.size() : 0;
    }

    public void addComment(CommentModel comment) {
        commentList.add(comment);
        notifyItemInserted(commentList.size() - 1);
    }

    // ════════════════════════════════════════════════════════
    //  VIEW HOLDER
    // ════════════════════════════════════════════════════════
    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivAvatar;
        ImageView ivVerified, ivAddFriend;
        TextView tvAuthorName, tvTime, tvContent, tvLikeCount, tvReply;
        ImageButton btnLike, btnMore;
        View viewThreadLine, viewParentLine;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar       = itemView.findViewById(R.id.ivAvatar);
            ivVerified     = itemView.findViewById(R.id.ivVerified);
            ivAddFriend    = itemView.findViewById(R.id.ivAddFriend);
            tvAuthorName   = itemView.findViewById(R.id.tvAuthorName);
            tvTime         = itemView.findViewById(R.id.tvTime);
            tvContent      = itemView.findViewById(R.id.tvContent);
            tvLikeCount    = itemView.findViewById(R.id.tvLikeCount);
            tvReply        = itemView.findViewById(R.id.tvReply);
            btnLike        = itemView.findViewById(R.id.btnLike);
            btnMore        = itemView.findViewById(R.id.btnMore);
            viewThreadLine = itemView.findViewById(R.id.viewThreadLine);
            viewParentLine = itemView.findViewById(R.id.viewParentLine);
        }
    }
}