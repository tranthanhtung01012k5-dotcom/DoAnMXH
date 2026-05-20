package com.example.doanmxh.HomePage;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doanmxh.R;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private List<CommentModel> commentList;
    private String postId;
    private OnCommentActionListener listener;

    public interface OnCommentActionListener {
        void onReplyClick(CommentModel comment, int position);
        void onLikeClick(CommentModel comment, int position);
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
        boolean isReply = comment.getBinhLuanChaId() != null
                && !comment.getBinhLuanChaId().isEmpty();

        // ✅ Lùi vào nếu là reply
        float density = holder.itemView.getContext()
                .getResources().getDisplayMetrics().density;
        int indentPx  = (int) (52 * density); // lùi 52dp

        ViewGroup.MarginLayoutParams params =
                (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
        params.leftMargin = isReply ? indentPx : 0;
        holder.itemView.setLayoutParams(params);

        // ✅ Đường kẻ từ avatar cha xuống — hiện khi là reply
        holder.viewParentLine.setVisibility(isReply ? View.VISIBLE : View.GONE);

        // ✅ Đường kẻ từ avatar xuống — hiện khi comment tiếp theo là reply của comment này
        boolean nextIsReply = (position + 1 < commentList.size())
                && commentList.get(position + 1).getBinhLuanChaId() != null
                && commentList.get(position + 1).getBinhLuanChaId()
                .equals(comment.getDocumentId());
        holder.viewThreadLine.setVisibility(nextIsReply ? View.VISIBLE : View.INVISIBLE);

        // Tên
        holder.tvAuthorName.setText(
                comment.getHoVaTen() != null ? comment.getHoVaTen() : "Người dùng"
        );

        // Thời gian
        holder.tvTime.setText(formatTime(comment.getNgayTao().toDate()));

        // Nội dung
        holder.tvContent.setText(
                comment.getNoiDung() != null ? comment.getNoiDung() : ""
        );

        // Verified
        holder.ivVerified.setVisibility(
                comment.isVerified() ? View.VISIBLE : View.GONE
        );

        // Avatar
        if (comment.getAnhDaiDien() != null && !comment.getAnhDaiDien().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(comment.getAnhDaiDien())
                    .placeholder(R.drawable.ic_placeholder_avatar)
                    .circleCrop()
                    .into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(R.drawable.ic_placeholder_avatar);
        }

        // Số like
        int soLike = comment.getSoLike();
        holder.tvLikeCount.setText(soLike > 0 ? String.valueOf(soLike) : "");

        // Màu tim
        if (comment.isLikedByMe()) {
            holder.btnLike.setImageResource(R.drawable.ic_heart_filled_24);
            holder.btnLike.setColorFilter(Color.RED);
            holder.tvLikeCount.setTextColor(Color.RED);
        } else {
            holder.btnLike.setImageResource(R.drawable.ic_heart_outline_24);
            holder.btnLike.clearColorFilter();
            holder.tvLikeCount.setTextColor(Color.parseColor("#888888"));
        }

        // Like bình luận
        // Like bình luận
        holder.btnLike.setOnClickListener(v -> {

            int adapterPosition = holder.getBindingAdapterPosition();

            if (adapterPosition == RecyclerView.NO_POSITION) return;

            CommentModel currentComment = commentList.get(adapterPosition);

            String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                    : null;

            if (uid == null) return;

            FirebaseFirestore db = FirebaseFirestore.getInstance();

            String commentId = currentComment.getDocumentId();

            db.collection("bai_viet")
                    .document(postId)
                    .collection("binh_luan")
                    .document(commentId)
                    .collection("luot_thich")
                    .document(uid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {

                        boolean daLike = documentSnapshot.exists();

                        if (daLike) {

                            // unlike
                            documentSnapshot.getReference().delete();

                            db.collection("bai_viet")
                                    .document(postId)
                                    .collection("binh_luan")
                                    .document(commentId)
                                    .update("so_like", FieldValue.increment(-1));

                            currentComment.setLikedByMe(false);

                            int newLike = Math.max(0, currentComment.getSoLike() - 1);
                            currentComment.setSoLike(newLike);

                        } else {

                            Map<String, Object> likeData = new HashMap<>();
                            likeData.put("nguoi_dung_id", uid);
                            likeData.put("ngay_like", new Date());

                            db.collection("bai_viet")
                                    .document(postId)
                                    .collection("binh_luan")
                                    .document(commentId)
                                    .collection("luot_thich")
                                    .document(uid)
                                    .set(likeData);

                            db.collection("bai_viet")
                                    .document(postId)
                                    .collection("binh_luan")
                                    .document(commentId)
                                    .update("so_like", FieldValue.increment(1));

                            currentComment.setLikedByMe(true);
                            currentComment.setSoLike(currentComment.getSoLike() + 1);
                        }

                        // callback
                        if (listener != null) {
                            listener.onLikeClick(currentComment, adapterPosition);
                        }

                        notifyItemChanged(adapterPosition);
                    });
        });

        // Reply
        holder.tvReply.setOnClickListener(v -> {
            if (listener != null) listener.onReplyClick(comment, position);
        });
    }

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

    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivAvatar;
        ImageView ivVerified;
        TextView tvAuthorName, tvTime, tvContent, tvLikeCount, tvReply;
        ImageButton btnLike, btnMore;
        View viewThreadLine, viewParentLine;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar       = itemView.findViewById(R.id.ivAvatar);
            ivVerified     = itemView.findViewById(R.id.ivVerified);
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