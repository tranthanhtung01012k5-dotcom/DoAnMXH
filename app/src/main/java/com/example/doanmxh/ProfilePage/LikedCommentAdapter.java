package com.example.doanmxh.ProfilePage;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doanmxh.HomePage.CommentModel;
import com.example.doanmxh.R;
import com.google.firebase.Timestamp;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class LikedCommentAdapter extends RecyclerView.Adapter<LikedCommentAdapter.ViewHolder> {
    public interface OnCommentClickListener {
        void onCommentClick(String postId);
    }

    public static class LikedCommentItem {
        private final CommentModel comment;
        private final String postContent;

        public LikedCommentItem(CommentModel comment, String postContent) {
            this.comment = comment;
            this.postContent = postContent;
        }
    }

    private final List<LikedCommentItem> items;
    private final OnCommentClickListener listener;

    public LikedCommentAdapter(List<LikedCommentItem> items, OnCommentClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_liked_comment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LikedCommentItem item = items.get(position);
        CommentModel comment = item.comment;

        holder.txtAuthor.setText(nonEmpty(comment.getHoVaTen(), "Người dùng"));
        holder.txtTime.setText(formatTime(comment.getNgayTao()));
        holder.txtComment.setText(nonEmpty(comment.getNoiDung(), "(Không có nội dung)"));
        holder.txtPost.setText("Trong bài: " + nonEmpty(item.postContent, "(Không có nội dung bài viết)"));
        holder.txtStats.setText(comment.getSoLike() + " lượt thích");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null && comment.getPostId() != null) {
                listener.onCommentClick(comment.getPostId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String nonEmpty(String value, String fallback) {
        return value != null && !value.trim().isEmpty() ? value.trim() : fallback;
    }

    private String formatTime(Timestamp timestamp) {
        if (timestamp == null) return "";
        Date now = new Date();
        Date date = timestamp.toDate();
        long diff = Math.max(0, now.getTime() - date.getTime());
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
        if (minutes < 1) return "Vừa xong";
        if (minutes < 60) return minutes + " phút";
        long hours = TimeUnit.MILLISECONDS.toHours(diff);
        if (hours < 24) return hours + " giờ";
        long days = TimeUnit.MILLISECONDS.toDays(diff);
        if (days < 7) return days + " ngày";
        return android.text.format.DateFormat.format("dd/MM/yyyy", date).toString();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtAuthor, txtTime, txtComment, txtPost, txtStats;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtAuthor = itemView.findViewById(R.id.txtAuthor);
            txtTime = itemView.findViewById(R.id.txtTime);
            txtComment = itemView.findViewById(R.id.txtComment);
            txtPost = itemView.findViewById(R.id.txtPost);
            txtStats = itemView.findViewById(R.id.txtStats);
        }
    }
}
