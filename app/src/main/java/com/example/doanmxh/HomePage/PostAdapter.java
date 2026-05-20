package com.example.doanmxh.HomePage;

import android.util.Log;
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

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private List<PostModel> postList;
    private OnPostActionListener listener;

    public interface OnPostActionListener {
        void onLikeClick(PostModel post, int position);
        void onCommentClick(PostModel post, int position);
        void onRepostClick(PostModel post, int position);
        void onShareClick(PostModel post, int position);
        void onMoreOptionsClick(PostModel post, int position);
        void onAvatarClick(PostModel post, int position);
        void onAddFriendClick(PostModel post, int position);
    }

    public PostAdapter(List<PostModel> postList, OnPostActionListener listener) {
        this.postList = postList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        PostModel post = postList.get(position);

        // ── Tên tác giả ──
        String hoVaTen = post.getHoVaTen();
        holder.tvAuthorName.setText(
                hoVaTen != null && !hoVaTen.isEmpty() ? hoVaTen : "Người dùng"
        );

        // ── Thời gian ──
        holder.tvPostTime.setText(formatTime(post.getNgayTao().toDate()));

        // ── Nội dung ──
        String noiDung = post.getNoiDung();
        if (post.isDaXoa()) {
            holder.tvContent.setText("Bài viết đã bị xóa");
            holder.tvContent.setAlpha(0.4f);
        } else {
            holder.tvContent.setText(noiDung != null ? noiDung : "");
            holder.tvContent.setAlpha(post.isRepost() ? 0.7f : 1.0f);
        }

        // ── Stats ──
        int soBinhLuan  = post.getSoBinhLuan();
        int soLuotThich = post.getSoLuotThich();
        if (soBinhLuan == 0 && soLuotThich == 0) {
            holder.tvStats.setVisibility(View.GONE);
        } else {
            holder.tvStats.setVisibility(View.VISIBLE);
            holder.tvStats.setText(soBinhLuan + " lượt trả lời · " + soLuotThich + " lượt thích");
        }

        // ── Badge verified ──
        holder.ivVerified.setVisibility(post.isVerified() ? View.VISIBLE : View.GONE);

        // ── Avatar ──
        String anhDaiDien = post.getAnhDaiDien();
        if (anhDaiDien != null && !anhDaiDien.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(anhDaiDien)
                    .placeholder(R.drawable.ic_placeholder_avatar)
                    .error(R.drawable.ic_placeholder_avatar)
                    .circleCrop()
                    .into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(R.drawable.ic_placeholder_avatar);
        }

        // ── Nút + theo dõi (dùng cache isFollowing từ PostModel) ──
        String myUid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null
                ? com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        boolean laBanThan = myUid != null && myUid.equals(post.getNguoiDungId());
        boolean daFollow  = post.isFollowing();

        if (laBanThan || daFollow) {
            // Bài của chính mình hoặc đã follow → ẩn nút +
            holder.ivAddFriend.setVisibility(View.GONE);
        } else {
            holder.ivAddFriend.setVisibility(View.VISIBLE);
            holder.ivAddFriend.setOnClickListener(v -> {
                if (listener != null)
                    listener.onAddFriendClick(post, holder.getAdapterPosition());
            });
        }

        // ── Ảnh bài đăng ──
        List<String> images = post.getDanhSachAnh();
        if (images != null && !images.isEmpty()) {
            holder.layoutImages.setVisibility(View.VISIBLE);

            holder.img1.setVisibility(View.GONE);
            holder.img2.setVisibility(View.GONE);
            holder.img3.setVisibility(View.GONE);
            holder.viewOverlay.setVisibility(View.GONE);
            holder.tvMore.setVisibility(View.GONE);

            if (images.size() >= 1) {
                holder.img1.setVisibility(View.VISIBLE);
                Glide.with(holder.itemView.getContext()).load(images.get(0)).into(holder.img1);
            }
            if (images.size() >= 2) {
                holder.img2.setVisibility(View.VISIBLE);
                Glide.with(holder.itemView.getContext()).load(images.get(1)).into(holder.img2);
            }
            if (images.size() >= 3) {
                holder.img3.setVisibility(View.VISIBLE);
                Glide.with(holder.itemView.getContext()).load(images.get(2)).into(holder.img3);
            }
            if (images.size() > 3) {
                holder.viewOverlay.setVisibility(View.VISIBLE);
                holder.tvMore.setVisibility(View.VISIBLE);
                holder.tvMore.setText("+" + (images.size() - 3));
            }
        } else {
            holder.layoutImages.setVisibility(View.GONE);
        }

        // ── Like ──
        if (post.isLikedByMe()) {
            holder.btnLike.setImageResource(R.drawable.ic_heart_filled_24);
            holder.btnLike.setColorFilter(
                    android.graphics.Color.RED,
                    android.graphics.PorterDuff.Mode.SRC_IN);
        } else {
            holder.btnLike.setImageResource(R.drawable.ic_heart_outline_24);
            holder.btnLike.clearColorFilter();
        }

        // ── Click listeners ──
        holder.ivAvatar.setOnClickListener(v -> {

            if (listener != null) {

                listener.onAvatarClick(
                        post,
                        holder.getAdapterPosition()
                );
            }
        });
        holder.btnLike.setOnClickListener(v -> {
            if (listener != null) listener.onLikeClick(post, holder.getAdapterPosition());
        });
        holder.btnComment.setOnClickListener(v -> {
            if (listener != null) listener.onCommentClick(post, holder.getAdapterPosition());
        });
        holder.btnRepost.setOnClickListener(v -> {
            if (listener != null) listener.onRepostClick(post, holder.getAdapterPosition());
        });
        holder.btnShare.setOnClickListener(v -> {
            if (listener != null) listener.onShareClick(post, holder.getAdapterPosition());
        });
        holder.btnMoreOptions.setOnClickListener(v -> {
            if (listener != null) listener.onMoreOptionsClick(post, holder.getAdapterPosition());
        });
        holder.ivAvatar.setOnClickListener(v -> {
            if (listener != null) listener.onAvatarClick(post, holder.getAdapterPosition());
        });
    }

    private String formatTime(Date date) {
        if (date == null) return "";
        long diff    = new Date().getTime() - date.getTime();
        long seconds = TimeUnit.MILLISECONDS.toSeconds(diff);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
        long hours   = TimeUnit.MILLISECONDS.toHours(diff);
        long days    = TimeUnit.MILLISECONDS.toDays(diff);
        long weeks   = days / 7;

        if (seconds < 60)      return "vừa xong";
        else if (minutes < 60) return minutes + " phút";
        else if (hours < 24)   return hours + " giờ";
        else if (days < 7)     return days + " ngày";
        else if (weeks < 4)    return weeks + " tuần";
        else                   return (weeks / 4) + " tháng";
    }

    @Override
    public int getItemCount() {
        return postList != null ? postList.size() : 0;
    }

    public void updateList(List<PostModel> newList) {
        this.postList = newList;
        notifyDataSetChanged();
    }

    public void addPost(PostModel post) {
        postList.add(0, post);
        notifyItemInserted(0);
    }

    public void removePost(int position) {
        postList.remove(position);
        notifyItemRemoved(position);
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {

        ShapeableImageView ivAvatar;
        ImageView img1, img2, img3;
        ImageView ivAddFriend;
        TextView tvMore;
        View viewOverlay;
        View layoutImages;
        ImageView ivVerified;
        TextView tvAuthorName;
        TextView tvPostTime;
        TextView tvContent;
        TextView tvStats;
        ImageButton btnLike;
        ImageButton btnComment;
        ImageButton btnRepost;
        ImageButton btnShare;
        ImageButton btnMoreOptions;
        RecyclerView rvImages;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutImages   = itemView.findViewById(R.id.layoutImages);
            img1           = itemView.findViewById(R.id.img1);
            img2           = itemView.findViewById(R.id.img2);
            img3           = itemView.findViewById(R.id.img3);
            ivAddFriend    = itemView.findViewById(R.id.ivAddFriend);
            viewOverlay    = itemView.findViewById(R.id.viewOverlay);
            tvMore         = itemView.findViewById(R.id.tvMore);
            ivAvatar       = itemView.findViewById(R.id.ivAvatar);
            ivVerified     = itemView.findViewById(R.id.ivVerified);
            tvAuthorName   = itemView.findViewById(R.id.tvAuthorName);
            tvPostTime     = itemView.findViewById(R.id.tvPostTime);
            tvContent      = itemView.findViewById(R.id.tvContent);
            tvStats        = itemView.findViewById(R.id.tvStats);
            btnLike        = itemView.findViewById(R.id.btnLike);
            btnComment     = itemView.findViewById(R.id.btnComment);
            btnRepost      = itemView.findViewById(R.id.btnRepost);
            btnShare       = itemView.findViewById(R.id.btnShare);
            btnMoreOptions = itemView.findViewById(R.id.btnMoreOptions);
        }
    }
}