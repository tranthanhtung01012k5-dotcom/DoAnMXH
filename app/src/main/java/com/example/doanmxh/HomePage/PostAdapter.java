package com.example.doanmxh.HomePage;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import android.widget.LinearLayout;
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

    public PostAdapter(List<PostModel> postList, OnPostActionListener onPostActionListener) {
    }

    public interface OnPostActionListener {

        void onLikeClick(PostModel post, int position);
        void onCommentClick(PostModel post, int position);
        void onRepostClick(PostModel post, int position);
        void onShareClick(PostModel post, int position);
        void onMoreOptionsClick(PostModel post, int position);

        // POST PROFILE
        void onAvatarClick(PostModel post, int position);

        // COMMENT PROFILE
        void onCommentAvatarClick(CommentModel comment, int position);

        void onAddFriendClick(PostModel post, int position);
    }

    public PostAdapter(List<PostModel> postList,
                       OnPostActionListener listener) {

        void onAvatarClick(PostModel post, int position);
        void onAddFriendClick(PostModel post, int position);
    }

    public PostAdapter(List<PostModel> postList, OnPostActionListener listener) {
        this.postList = postList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                             int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_post, parent, false);

    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
<<<<<<< HEAD
    public void onBindViewHolder(@NonNull PostViewHolder holder,
                                 int position) {

        PostModel post = postList.get(position);

        // =========================
        // TÊN
        // =========================

        String hoVaTen = post.getHoVaTen();

        holder.tvAuthorName.setText(
                hoVaTen != null && !hoVaTen.isEmpty()
                        ? hoVaTen
                        : "Người dùng"
        );

        // =========================
        // THỜI GIAN
        // =========================

        if (post.getNgayTao() != null) {

            holder.tvPostTime.setText(
                    formatTime(post.getNgayTao().toDate())
            );

        } else {

            holder.tvPostTime.setText("");
        }

        // =========================
        // NỘI DUNG
        // =========================

        String noiDung = post.getNoiDung();

        if (post.isDaXoa()) {

            holder.tvContent.setText("Bài viết đã bị xóa");
            holder.tvContent.setAlpha(0.4f);

        } else {

            holder.tvContent.setText(
                    noiDung != null ? noiDung : ""
            );

            holder.tvContent.setAlpha(
                    post.isRepost() ? 0.7f : 1f
            );
        }

        // =========================
        // STATS
        // =========================

        int soBinhLuan  = post.getSoBinhLuan();
        int soLuotThich = post.getSoLuotThich();

        if (soBinhLuan == 0 && soLuotThich == 0) {

            holder.tvStats.setVisibility(View.GONE);

        } else {

            holder.tvStats.setVisibility(View.VISIBLE);

            holder.tvStats.setText(
                    soBinhLuan
                            + " lượt trả lời · "
                            + soLuotThich
                            + " lượt thích"
            );
        }
//        holder.layoutTopComment
//        holder.imgTopCommentAvatar
// =========================
// TOP COMMENT
// =========================

        CommentModel topComment = post.getTopComment();

        if (topComment != null) {

            holder.layoutTopComment.setVisibility(View.VISIBLE);
            holder.tvTopCommentUser.setText(topComment.getHoVaTen());
            holder.tvTopComment.setText(
                    topComment.getNoiDung()
            );

            Glide.with(holder.itemView.getContext())
                    .load(topComment.getAnhDaiDien())
                    .placeholder(R.drawable.ic_person_outline_24)
                    .into(holder.imgTopCommentAvatar);

            holder.imgTopCommentAvatar.setOnClickListener(v -> {

                if (listener != null) {

                    listener.onCommentAvatarClick(
                            topComment,
                            holder.getAdapterPosition()
                    );
                }
            });

            holder.tvTopCommentUser.setOnClickListener(v -> {

                if (listener != null) {

                    listener.onCommentAvatarClick(
                            topComment,
                            holder.getAdapterPosition()
                    );
                }
            });
        } else {

            holder.layoutTopComment.setVisibility(View.GONE);
        }

// =========================
// REPLIES
// =========================

        List<CommentModel> replies =
                post.getTopReplies();

        if (replies != null && !replies.isEmpty()) {

            holder.tvReplies.setVisibility(View.VISIBLE);

            StringBuilder builder =
                    new StringBuilder();

            int maxReply =
                    Math.min(replies.size(), 2);

            for (int i = 0; i < maxReply; i++) {

                CommentModel c = replies.get(i);

                String ten =
                        c.getHoVaTen() != null
                                ? c.getHoVaTen()
                                : "Người dùng";

                String nd =
                        c.getNoiDung() != null
                                ? c.getNoiDung()
                                : "";

                builder.append("↳ ")
                        .append(ten)
                        .append(": ")
                        .append(nd);

                if (i < maxReply - 1) {
                    builder.append("\n");
                }
            }

            holder.tvReplies.setText(
                    builder.toString()
            );

        } else {

            holder.tvReplies.setVisibility(View.GONE);
        }
        // =========================
        // VERIFIED
        // =========================

        holder.ivVerified.setVisibility(
                post.isVerified()
                        ? View.VISIBLE
                        : View.GONE
        );

        // =========================
        // AVATAR
        // =========================

        String anhDaiDien = post.getAnhDaiDien();

        if (anhDaiDien != null && !anhDaiDien.isEmpty()) {

=======
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
>>>>>>> 8ef7ad65cdddf626cdcdb3b97ef342fec36f9900
            Glide.with(holder.itemView.getContext())
                    .load(anhDaiDien)
                    .placeholder(R.drawable.ic_placeholder_avatar)
                    .error(R.drawable.ic_placeholder_avatar)
                    .circleCrop()
                    .into(holder.ivAvatar);
<<<<<<< HEAD

        } else {

            holder.ivAvatar.setImageResource(
                    R.drawable.ic_placeholder_avatar
            );
        }

        // =========================
        // NÚT FOLLOW
        // =========================

        String myUid =
                com.google.firebase.auth.FirebaseAuth
                        .getInstance()
                        .getCurrentUser() != null

                        ? com.google.firebase.auth.FirebaseAuth
                        .getInstance()
                        .getCurrentUser()
                        .getUid()

                        : null;

        boolean laBanThan =
                myUid != null
                        && myUid.equals(post.getNguoiDungId());

        boolean daFollow = post.isFollowing();

        if (laBanThan || daFollow) {

            holder.ivAddFriend.setVisibility(View.GONE);

        } else {

            holder.ivAddFriend.setVisibility(View.VISIBLE);

            holder.ivAddFriend.setOnClickListener(v -> {

                if (listener != null) {

                    listener.onAddFriendClick(
                            post,
                            holder.getAdapterPosition()
                    );
                }
            });
        }

        // =========================
        // ẢNH BÀI VIẾT
        // =========================

        List<String> images = post.getDanhSachAnh();

        if (images != null && !images.isEmpty()) {

=======
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
>>>>>>> 8ef7ad65cdddf626cdcdb3b97ef342fec36f9900
            holder.layoutImages.setVisibility(View.VISIBLE);

            holder.img1.setVisibility(View.GONE);
            holder.img2.setVisibility(View.GONE);
            holder.img3.setVisibility(View.GONE);
<<<<<<< HEAD

=======
>>>>>>> 8ef7ad65cdddf626cdcdb3b97ef342fec36f9900
            holder.viewOverlay.setVisibility(View.GONE);
            holder.tvMore.setVisibility(View.GONE);

            if (images.size() >= 1) {
<<<<<<< HEAD

                holder.img1.setVisibility(View.VISIBLE);

                Glide.with(holder.itemView.getContext())
                        .load(images.get(0))
                        .into(holder.img1);
            }

            if (images.size() >= 2) {

                holder.img2.setVisibility(View.VISIBLE);

                Glide.with(holder.itemView.getContext())
                        .load(images.get(1))
                        .into(holder.img2);
            }

            if (images.size() >= 3) {

                holder.img3.setVisibility(View.VISIBLE);

                Glide.with(holder.itemView.getContext())
                        .load(images.get(2))
                        .into(holder.img3);
            }

            if (images.size() > 3) {

                holder.viewOverlay.setVisibility(View.VISIBLE);

                holder.tvMore.setVisibility(View.VISIBLE);

                holder.tvMore.setText(
                        "+" + (images.size() - 3)
                );
            }

        } else {

            holder.layoutImages.setVisibility(View.GONE);
        }

        // =========================
        // LIKE UI
        // =========================

        if (post.isLikedByMe()) {

            holder.btnLike.setImageResource(
                    R.drawable.ic_heart_filled_24
            );

            holder.btnLike.setColorFilter(
                    android.graphics.Color.RED,
                    android.graphics.PorterDuff.Mode.SRC_IN
            );

        } else {

            holder.btnLike.setImageResource(
                    R.drawable.ic_heart_outline_24
            );

            holder.btnLike.clearColorFilter();
        }

        // =========================
        // CLICK PROFILE
        // =========================

=======
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
>>>>>>> 8ef7ad65cdddf626cdcdb3b97ef342fec36f9900
        holder.ivAvatar.setOnClickListener(v -> {

            if (listener != null) {

                listener.onAvatarClick(
                        post,
                        holder.getAdapterPosition()
                );
            }
        });
<<<<<<< HEAD

        holder.tvAuthorName.setOnClickListener(v -> {

            if (listener != null) {

                listener.onAvatarClick(
                        post,
                        holder.getAdapterPosition()
                );
            }
        });

        // =========================
        // CLICK ACTION
        // =========================

        holder.btnLike.setOnClickListener(v -> {

            if (listener != null) {

                listener.onLikeClick(
                        post,
                        holder.getAdapterPosition()
                );
            }
        });

        holder.btnComment.setOnClickListener(v -> {

            if (listener != null) {

                listener.onCommentClick(
                        post,
                        holder.getAdapterPosition()
                );
            }
        });

        holder.btnRepost.setOnClickListener(v -> {

            if (listener != null) {

                listener.onRepostClick(
                        post,
                        holder.getAdapterPosition()
                );
            }
        });

        holder.btnShare.setOnClickListener(v -> {

            if (listener != null) {

                listener.onShareClick(
                        post,
                        holder.getAdapterPosition()
                );
            }
        });

        holder.btnMoreOptions.setOnClickListener(v -> {

            if (listener != null) {

                listener.onMoreOptionsClick(
                        post,
                        holder.getAdapterPosition()
                );
            }
=======
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
>>>>>>> 8ef7ad65cdddf626cdcdb3b97ef342fec36f9900
        });
    }

    private String formatTime(Date date) {
<<<<<<< HEAD

        if (date == null) return "";

        long diff =
                new Date().getTime() - date.getTime();

        long seconds =
                TimeUnit.MILLISECONDS.toSeconds(diff);

        long minutes =
                TimeUnit.MILLISECONDS.toMinutes(diff);

        long hours =
                TimeUnit.MILLISECONDS.toHours(diff);

        long days =
                TimeUnit.MILLISECONDS.toDays(diff);

        long weeks = days / 7;

        if (seconds < 60) {

            return "vừa xong";

        } else if (minutes < 60) {

            return minutes + " phút";

        } else if (hours < 24) {

            return hours + " giờ";

        } else if (days < 7) {

            return days + " ngày";

        } else if (weeks < 4) {

            return weeks + " tuần";

        } else {

            return (weeks / 4) + " tháng";
        }
=======
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
>>>>>>> 8ef7ad65cdddf626cdcdb3b97ef342fec36f9900
    }

    @Override
    public int getItemCount() {
<<<<<<< HEAD

        return postList != null
                ? postList.size()
                : 0;
    }

    public void updateList(List<PostModel> newList) {

        this.postList = newList;

=======
        return postList != null ? postList.size() : 0;
    }

    public void updateList(List<PostModel> newList) {
        this.postList = newList;
>>>>>>> 8ef7ad65cdddf626cdcdb3b97ef342fec36f9900
        notifyDataSetChanged();
    }

    public void addPost(PostModel post) {
<<<<<<< HEAD

        postList.add(0, post);

=======
        postList.add(0, post);
>>>>>>> 8ef7ad65cdddf626cdcdb3b97ef342fec36f9900
        notifyItemInserted(0);
    }

    public void removePost(int position) {
<<<<<<< HEAD

        postList.remove(position);

        notifyItemRemoved(position);
    }

    public static class PostViewHolder
            extends RecyclerView.ViewHolder {
        TextView tvTopCommentUser;
        TextView tvTopComment;
        TextView tvReplies;

        LinearLayout layoutTopComment;

        ImageView imgTopCommentAvatar;

        ShapeableImageView ivAvatar;

        ImageView img1, img2, img3;

        ImageView ivAddFriend;

        TextView tvMore;

        View viewOverlay;
        View layoutImages;

        ImageView ivVerified;

=======
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
>>>>>>> 8ef7ad65cdddf626cdcdb3b97ef342fec36f9900
        TextView tvAuthorName;
        TextView tvPostTime;
        TextView tvContent;
        TextView tvStats;
<<<<<<< HEAD

=======
>>>>>>> 8ef7ad65cdddf626cdcdb3b97ef342fec36f9900
        ImageButton btnLike;
        ImageButton btnComment;
        ImageButton btnRepost;
        ImageButton btnShare;
        ImageButton btnMoreOptions;
<<<<<<< HEAD

=======
>>>>>>> 8ef7ad65cdddf626cdcdb3b97ef342fec36f9900
        RecyclerView rvImages;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
<<<<<<< HEAD
            tvTopCommentUser =
                    itemView.findViewById(R.id.tvTopCommentUser);
            // TOP COMMENT
            layoutTopComment =
                    itemView.findViewById(R.id.layoutTopComment);

            imgTopCommentAvatar =
                    itemView.findViewById(R.id.imgTopCommentAvatar);

            tvTopComment =
                    itemView.findViewById(R.id.tvTopComment);

            tvReplies =
                    itemView.findViewById(R.id.tvReplies);

            // IMAGES
            layoutImages =
                    itemView.findViewById(R.id.layoutImages);

            img1 = itemView.findViewById(R.id.img1);
            img2 = itemView.findViewById(R.id.img2);
            img3 = itemView.findViewById(R.id.img3);

            ivAddFriend =
                    itemView.findViewById(R.id.ivAddFriend);

            viewOverlay =
                    itemView.findViewById(R.id.viewOverlay);

            tvMore =
                    itemView.findViewById(R.id.tvMore);

            ivAvatar =
                    itemView.findViewById(R.id.ivAvatar);

            ivVerified =
                    itemView.findViewById(R.id.ivVerified);

            tvAuthorName =
                    itemView.findViewById(R.id.tvAuthorName);

            tvPostTime =
                    itemView.findViewById(R.id.tvPostTime);

            tvContent =
                    itemView.findViewById(R.id.tvContent);

            tvStats =
                    itemView.findViewById(R.id.tvStats);

            btnLike =
                    itemView.findViewById(R.id.btnLike);

            btnComment =
                    itemView.findViewById(R.id.btnComment);

            btnRepost =
                    itemView.findViewById(R.id.btnRepost);

            btnShare =
                    itemView.findViewById(R.id.btnShare);

            btnMoreOptions =
                    itemView.findViewById(R.id.btnMoreOptions);
=======
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
>>>>>>> 8ef7ad65cdddf626cdcdb3b97ef342fec36f9900
        }
    }
}