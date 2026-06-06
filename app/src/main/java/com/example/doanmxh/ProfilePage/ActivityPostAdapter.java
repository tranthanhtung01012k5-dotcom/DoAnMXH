package com.example.doanmxh.ProfilePage;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doanmxh.HomePage.PostModel;
import com.example.doanmxh.R;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ActivityPostAdapter
        extends RecyclerView.Adapter<ActivityPostAdapter.ActivityPostViewHolder> {

    public interface OnPostClickListener {
        void onPostClick(String postId);
    }

    public static class ActivityPostItem {
        private final PostModel post;
        private final List<String> myComments;

        public ActivityPostItem(PostModel post, List<String> myComments) {
            this.post = post;
            this.myComments = myComments != null ? myComments : new ArrayList<>();
        }
    }

    private final List<ActivityPostItem> items;

    private final boolean showMyComments;
    private final OnPostClickListener listener;

    public ActivityPostAdapter(
            List<ActivityPostItem> items,
            boolean showMyComments,
            OnPostClickListener listener
    ) {
        this.items = items;
        this.showMyComments = showMyComments;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ActivityPostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_activity_post, parent, false);
        return new ActivityPostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActivityPostViewHolder holder, int position) {
        ActivityPostItem item = items.get(position);
        PostModel post = item.post;

        holder.txtAuthor.setText(
                post.getHoVaTen() != null && !post.getHoVaTen().isEmpty()
                        ? post.getHoVaTen()
                        : "Người dùng"
        );
        holder.txtTime.setText(formatTime(post.getNgayTao()));
        holder.txtContent.setText(
                post.getNoiDung() != null && !post.getNoiDung().isEmpty()
                        ? post.getNoiDung()
                        : "(Không có nội dung)"
        );
        holder.txtStats.setText(
                post.getSoLuotThich() + " lượt thích · "
                        + post.getSoBinhLuan() + " bình luận"
        );

        String avatar = post.getAnhDaiDien();
        Glide.with(holder.itemView.getContext())
                .load(avatar)
                .placeholder(R.drawable.ic_placeholder_avatar)
                .error(R.drawable.ic_placeholder_avatar)
                .circleCrop()
                .into(holder.imgAvatar);

        List<String> images = post.getDanhSachAnh();
        if (images != null && !images.isEmpty()) {
            holder.imgPost.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext())
                    .load(images.get(0))
                    .centerCrop()
                    .into(holder.imgPost);
        } else {
            holder.imgPost.setVisibility(View.GONE);
        }

        bindMyComments(holder, item.myComments);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null && post.getDocumentId() != null) {
                listener.onPostClick(post.getDocumentId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private void bindMyComments(ActivityPostViewHolder holder, List<String> comments) {
        holder.layoutMyComments.removeAllViews();
        if (!showMyComments || comments == null || comments.isEmpty()) {
            holder.layoutMyComments.setVisibility(View.GONE);
            return;
        }

        holder.layoutMyComments.setVisibility(View.VISIBLE);
        TextView title = buildCommentText(holder, "Bình luận của bạn", true);
        holder.layoutMyComments.addView(title);

        for (String comment : comments) {
            holder.layoutMyComments.addView(
                    buildCommentText(holder, "• " + comment, false)
            );
        }
    }

    private TextView buildCommentText(
            ActivityPostViewHolder holder,
            String text,
            boolean title
    ) {
        TextView textView = new TextView(holder.itemView.getContext());
        textView.setText(text);
        textView.setTextColor(
                holder.itemView.getContext().getColor(
                        title ? R.color.text_primary : R.color.text_secondary
                )
        );
        textView.setTextSize(title ? 13 : 14);
        if (title) textView.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        if (!title) params.topMargin = dp(holder, 6);
        textView.setLayoutParams(params);
        return textView;
    }

    private int dp(ActivityPostViewHolder holder, int value) {
        float density = holder.itemView.getResources().getDisplayMetrics().density;
        return Math.round(value * density);
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

    static class ActivityPostViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar, imgPost;
        LinearLayout layoutMyComments;
        TextView txtAuthor, txtTime, txtContent, txtStats;

        ActivityPostViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            imgPost = itemView.findViewById(R.id.imgPost);
            layoutMyComments = itemView.findViewById(R.id.layoutMyComments);
            txtAuthor = itemView.findViewById(R.id.txtAuthor);
            txtTime = itemView.findViewById(R.id.txtTime);
            txtContent = itemView.findViewById(R.id.txtContent);
            txtStats = itemView.findViewById(R.id.txtStats);
        }
    }
}
