package com.example.doanmxh.Notifications;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.example.doanmxh.R;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    // ─── Callback interface ───────────────────────────────────────────────────
    public interface OnNotificationActionListener {
        void onFollowToggle(NotificationModel item, int position);
        void onItemClick(NotificationModel item);
        void onMoreClick(NotificationModel item, View anchor);
    }

    private final List<NotificationModel> list;
    private OnNotificationActionListener  listener;

    public NotificationAdapter(List<NotificationModel> list) {
        this.list = list;
    }

    public void setOnNotificationActionListener(OnNotificationActionListener l) {
        this.listener = l;
    }

    // ─── ViewHolder ───────────────────────────────────────────────────────────
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        ImageView ivPostThumbnail;
        ImageView ivTypeIcon;
        TextView  tvUsername;
        TextView  tvContent;
        TextView  tvTime;
        TextView  tvPostSnippet;
        Button    btnFollow;
        ImageView btnMore;
        View      unreadDot;
        View      rootView;

        ViewHolder(View v) {
            super(v);
            rootView       = v;
            ivAvatar       = v.findViewById(R.id.ivAvatar);
            ivTypeIcon     = v.findViewById(R.id.ivTypeIcon);
            tvUsername     = v.findViewById(R.id.tvUsername);
            tvContent      = v.findViewById(R.id.tvContent);
            tvTime         = v.findViewById(R.id.tvTime);
            tvPostSnippet  = v.findViewById(R.id.tvPostSnippet);
            btnFollow      = v.findViewById(R.id.btnFollow);
            btnMore        = v.findViewById(R.id.btnMore);
            unreadDot      = v.findViewById(R.id.unreadDot);
        }
    }

    // ─── Inflate ──────────────────────────────────────────────────────────────
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(v);
    }

    // ─── Bind ─────────────────────────────────────────────────────────────────
    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        NotificationModel item = list.get(position);
        Context ctx = h.itemView.getContext();

        // Avatar
        if (item.getAvatar() != null && !item.getAvatar().isEmpty()) {
            Glide.with(ctx)
                    .load(item.getAvatar())
                    .transform(new CircleCrop())
                    .placeholder(R.drawable.ic_placeholder_avatar)
                    .error(R.drawable.ic_placeholder_avatar)
                    .into(h.ivAvatar);
        } else {
            h.ivAvatar.setImageResource(R.drawable.ic_placeholder_avatar);
        }

        // Tên & nội dung & thời gian
        h.tvUsername.setText(item.getName() != null ? item.getName() : "");
        h.tvContent.setText(item.getDisplayContent());
        h.tvTime.setText(item.getTime() != null ? item.getTime() : "");

        // Icon loại thông báo
        if (h.ivTypeIcon != null) {
            switch (item.getType() == null ? "" : item.getType()) {
                case "LIKE":
                    h.ivTypeIcon.setVisibility(View.VISIBLE);
                    h.ivTypeIcon.setImageResource(R.drawable.ic_heart_filled_24);
                    h.ivTypeIcon.setColorFilter(Color.parseColor("#FF3040"));
                    break;
                case "FOLLOW":
                    h.ivTypeIcon.setVisibility(View.VISIBLE);
                    h.ivTypeIcon.setImageResource(R.drawable.ic_follow);
                    h.ivTypeIcon.clearColorFilter();
                    break;
                case "COMMENT":
                    h.ivTypeIcon.setVisibility(View.VISIBLE);
                    h.ivTypeIcon.setImageResource(R.drawable.ic_comment);
                    h.ivTypeIcon.clearColorFilter();
                    break;
                case "MENTION":
                    h.ivTypeIcon.setVisibility(View.VISIBLE);
                    h.ivTypeIcon.setImageResource(R.drawable.ic_mention);
                    h.ivTypeIcon.clearColorFilter();
                    break;

                case "LIKE_COMMENT":
                    h.ivTypeIcon.setVisibility(View.VISIBLE);
                    h.ivTypeIcon.setImageResource(
                            R.drawable.ic_heart_filled_24);
                    h.ivTypeIcon.setColorFilter(Color.RED);
                    break;
                case "REPOST":
                    h.ivTypeIcon.setVisibility(View.VISIBLE);
                    h.ivTypeIcon.setImageResource(R.drawable.ic_repost);
                    h.ivTypeIcon.clearColorFilter();
                    break;
                case "FOLLOWING":
                    h.ivTypeIcon.setVisibility(View.VISIBLE);
                    h.ivTypeIcon.setImageResource(R.drawable.ic_follow);
                    h.ivTypeIcon.clearColorFilter();
                    break;
                default:
                    h.ivTypeIcon.setVisibility(View.GONE);
                    break;
            }
        }

        // Thumbnail bài viết
        if (h.ivPostThumbnail != null) {
            String imgUrl = item.getPostImageUrl();
            boolean isFollowType = "FOLLOW".equals(item.getType()) || "FOLLOWING".equals(item.getType());
            if (imgUrl != null && !imgUrl.isEmpty() && !isFollowType) {
                h.ivPostThumbnail.setVisibility(View.VISIBLE);
                Glide.with(ctx)
                        .load(imgUrl)
                        .placeholder(R.drawable.ic_placeholder_avatar)
                        .into(h.ivPostThumbnail);
            } else {
                h.ivPostThumbnail.setVisibility(View.GONE);
            }
        }

        // Preview text bài viết
        if (h.tvPostSnippet != null) {
            String snippet = item.getPostSnippet();
            boolean isFollowType = "FOLLOW".equals(item.getType());
            if (snippet != null && !snippet.isEmpty() && !isFollowType) {
                h.tvPostSnippet.setVisibility(View.VISIBLE);
                h.tvPostSnippet.setText(snippet);
            } else {
                h.tvPostSnippet.setVisibility(View.GONE);
            }
        }

        // Nút Theo dõi (chỉ với FOLLOW)
        if (h.btnFollow != null) {
            if ("FOLLOW".equals(item.getType())) {
                h.btnFollow.setVisibility(View.VISIBLE);
                updateFollowButton(h.btnFollow, item.isFollowing());
                h.btnFollow.setOnClickListener(v -> {
                    if (listener != null) {
                        int pos = h.getAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION)
                            listener.onFollowToggle(item, pos);
                    }
                });
            } else {
                h.btnFollow.setVisibility(View.GONE);
            }
        }

        // Chấm chưa đọc
        if (h.unreadDot != null) {
            h.unreadDot.setVisibility(item.isRead() ? View.GONE : View.VISIBLE);
        }

        // Background highlight nếu chưa đọc
        h.rootView.setBackgroundColor(
                item.isRead() ? Color.TRANSPARENT : Color.parseColor("#9E9E9E"));

        // Nút 3 chấm
        if (h.btnMore != null) {
            h.btnMore.setOnClickListener(v -> {
                if (listener != null) listener.onMoreClick(item, v);
            });
        }

        // Click toàn item → đánh dấu đã đọc
        h.rootView.setOnClickListener(v -> {
//            if (!item.isRead()) {
//                item.setRead(true);
//                notifyItemChanged(h.getAdapterPosition());
//            }
            if (listener != null) listener.onItemClick(item);
        });
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────
    private void updateFollowButton(Button btn, boolean isFollowing) {
        if (isFollowing) {
            btn.setText("Đang theo dõi");
            btn.setBackgroundResource(R.drawable.bg_follow_outline);
            btn.setTextColor(Color.BLACK);
        } else {
            btn.setText("Theo dõi");
            btn.setBackgroundResource(R.drawable.bg_follow_filled);
            btn.setTextColor(Color.WHITE);
        }
    }

    @Override
    public int getItemCount() { return list.size(); }
}