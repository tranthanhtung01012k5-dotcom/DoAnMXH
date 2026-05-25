package com.example.doanmxh.HomePage;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.recyclerview.widget.DiffUtil;
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
    private OnItemClickListener itemClickListener;

    public interface OnItemClickListener {
        void onItemClick(String postId);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    public interface OnPostActionListener {
        void onLikeClick(PostModel post, int position);
        void onCommentClick(PostModel post, int position);
        void onRepostClick(PostModel post, int position);
        void onShareClick(PostModel post, int position);
        void onMoreOptionsClick(PostModel post, int position);
        void onAvatarClick(PostModel post, int position);
        void onCommentAvataClick(CommentModel comment, int position);
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

        // Click toàn bộ item → mở PostDetail
        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) {
                itemClickListener.onItemClick(post.getDocumentId());
            }
        });

        // Tên
        String hoVaTen = post.getHoVaTen();
        holder.tvAuthorName.setText(
                hoVaTen != null && !hoVaTen.isEmpty() ? hoVaTen : "Người dùng"
        );

        // Thời gian
        if (post.getNgayTao() != null) {
            holder.tvPostTime.setText(formatTime(post.getNgayTao().toDate()));
        } else {
            holder.tvPostTime.setText("");
        }

        // Nội dung
        String noiDung = post.getNoiDung();
        if (post.isDaXoa()) {
            holder.tvContent.setText("Bài viết đã bị xóa");
            holder.tvContent.setAlpha(0.4f);
        } else {
            holder.tvContent.setText(noiDung != null ? noiDung : "");
            holder.tvContent.setAlpha(post.isRepost() ? 0.7f : 1f);
        }

        // Stats
        int soBinhLuan  = post.getSoBinhLuan();
        int soLuotThich = post.getSoLuotThich();
        if (soBinhLuan == 0 && soLuotThich == 0) {
            holder.tvStats.setVisibility(View.GONE);
        } else {
            holder.tvStats.setVisibility(View.VISIBLE);
            holder.tvStats.setText(
                    soBinhLuan + " lượt trả lời · " + soLuotThich + " lượt thích"
            );
        }

        // Top Comment
        CommentModel topComment = post.getTopComment();
        if (topComment != null) {
            holder.layoutTopComment.setVisibility(View.VISIBLE);
            holder.tvTopCommentUser.setText(topComment.getHoVaTen());
            holder.tvTopComment.setText(topComment.getNoiDung());
            Glide.with(holder.itemView.getContext())
                    .load(topComment.getAnhDaiDien())
                    .placeholder(R.drawable.ic_person_outline_24)
                    .into(holder.imgTopCommentAvatar);
            holder.imgTopCommentAvatar.setOnClickListener(v -> {
                if (listener != null)
                    listener.onCommentAvataClick(topComment, holder.getAdapterPosition());
            });
            holder.tvTopCommentUser.setOnClickListener(v -> {
                if (listener != null)
                    listener.onCommentAvataClick(topComment, holder.getAdapterPosition());
            });
        } else {
            holder.layoutTopComment.setVisibility(View.GONE);
        }

        // Replies
        List<CommentModel> replies = post.getTopReplies();
        if (replies != null && !replies.isEmpty()) {
            holder.tvReplies.setVisibility(View.VISIBLE);
            StringBuilder builder = new StringBuilder();
            int maxReply = Math.min(replies.size(), 2);
            for (int i = 0; i < maxReply; i++) {
                CommentModel c = replies.get(i);
                String ten = c.getHoVaTen() != null ? c.getHoVaTen() : "Người dùng";
                String nd  = c.getNoiDung() != null ? c.getNoiDung() : "";
                builder.append("↳ ").append(ten).append(": ").append(nd);
                if (i < maxReply - 1) builder.append("\n");
            }
            holder.tvReplies.setText(builder.toString());
        } else {
            holder.tvReplies.setVisibility(View.GONE);
        }

        // Verified
        holder.ivVerified.setVisibility(post.isVerified() ? View.VISIBLE : View.GONE);

        // Avatar
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

        // Nút Follow
        String myUid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null
                ? com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
        boolean laBanThan = myUid != null && myUid.equals(post.getNguoiDungId());
        boolean daFollow = post.isFollowing();
        if (laBanThan || daFollow) {
            holder.ivAddFriend.setVisibility(View.GONE);
            holder.ivAddFriend.setOnClickListener(null);
        } else {
            holder.ivAddFriend.setVisibility(View.VISIBLE);
            holder.ivAddFriend.setOnClickListener(v -> {
                if (listener != null && holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onAddFriendClick(post, holder.getAdapterPosition());
                }
            });
        }

        // ────────────────────────────────────────────────────
        //  ẢNH BÀI VIẾT — logic hiển thị động
        // ────────────────────────────────────────────────────
        bindImages(holder, post.getDanhSachAnh());

        // Like UI
        updateLikeUI(holder, post);

        // Click Profile
        holder.ivAvatar.setOnClickListener(v -> {
            if (listener != null) listener.onAvatarClick(post, holder.getAdapterPosition());
        });
        holder.tvAuthorName.setOnClickListener(v -> {
            if (listener != null) listener.onAvatarClick(post, holder.getAdapterPosition());
        });

        // Click Actions
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
    }

    // ════════════════════════════════════════════════════════
    //  BIND IMAGES — logic chính
    //
    //  1 ảnh  → img1 full width, height 300dp
    //  2 ảnh  → img1 50% trái | img2 50% phải (cùng hàng, rightContainer 1 slot)
    //  3 ảnh  → img1 50% trái | img2 trên 50% + img3 dưới 50% trong rightContainer
    //  >3 ảnh → giống 3 ảnh, img3 có overlay "+N"
    //           click "+N" → ImageListBottomSheet (list dọc, click → full screen)
    // ════════════════════════════════════════════════════════
    private void bindImages(@NonNull PostViewHolder holder, List<String> images) {
        Context ctx = holder.itemView.getContext();

        // Reset tất cả về hidden trước
        holder.layoutImages.setVisibility(View.GONE);
        holder.rightContainer.setVisibility(View.GONE);
        holder.img1.setVisibility(View.GONE);
        holder.img2.setVisibility(View.GONE);
        holder.frameImg3.setVisibility(View.GONE);
        holder.img3.setVisibility(View.GONE);
        holder.viewOverlay.setVisibility(View.GONE);
        holder.tvMore.setVisibility(View.GONE);

        // Xóa click listener cũ tránh reuse
        holder.img1.setOnClickListener(null);
        holder.img2.setOnClickListener(null);
        holder.img3.setOnClickListener(null);
        holder.viewOverlay.setOnClickListener(null);
        holder.tvMore.setOnClickListener(null);

        if (images == null || images.isEmpty()) return;

        holder.layoutImages.setVisibility(View.VISIBLE);
        int count = images.size();

        // Lấy ConstraintLayout cha để điều chỉnh constraint của img1
        ConstraintLayout layoutImages = holder.layoutImages;
        ConstraintSet cs = new ConstraintSet();
        cs.clone(layoutImages);

        if (count == 1) {
            // ── 1 ảnh: img1 chiếm toàn bộ, height cố định 300dp ──
            // img1 end → parent end (không bị chặn bởi rightContainer)
            cs.connect(R.id.img1, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
            cs.constrainPercentWidth(R.id.img1, 1.0f); // full width
            cs.constrainHeight(R.id.img1, dpToPx(ctx, 300));
            cs.applyTo(layoutImages);

            holder.img1.setVisibility(View.VISIBLE);
            Glide.with(ctx).load(images.get(0)).centerCrop().into(holder.img1);
            holder.img1.setOnClickListener(v ->
                    FullScreenImageActivity.open(ctx, images, 0));

        } else if (count == 2) {
            // ── 2 ảnh: img1 50% | img2 50%, height 220dp ──
            // img1 end → start của rightContainer (50%)
            cs.connect(R.id.img1, ConstraintSet.END, R.id.rightContainer, ConstraintSet.START);
            cs.constrainPercentWidth(R.id.img1, 0.5f);
            cs.constrainHeight(R.id.img1, dpToPx(ctx, 220));
            cs.applyTo(layoutImages);

            // rightContainer: chỉ hiện img2 (không hiện img3)
            holder.rightContainer.setVisibility(View.VISIBLE);
            holder.img2.setVisibility(View.VISIBLE);
            holder.frameImg3.setVisibility(View.GONE); // ẩn slot img3

            Glide.with(ctx).load(images.get(0)).centerCrop().into(holder.img1);
            Glide.with(ctx).load(images.get(1)).centerCrop().into(holder.img2);

            holder.img1.setVisibility(View.VISIBLE);
            holder.img1.setOnClickListener(v ->
                    FullScreenImageActivity.open(ctx, images, 0));
            holder.img2.setOnClickListener(v ->
                    FullScreenImageActivity.open(ctx, images, 1));

        } else {
            // ── 3+ ảnh: img1 50% | (img2 trên + img3 dưới) 50% ──
            cs.connect(R.id.img1, ConstraintSet.END, R.id.rightContainer, ConstraintSet.START);
            cs.constrainPercentWidth(R.id.img1, 0.5f);
            cs.constrainHeight(R.id.img1, dpToPx(ctx, 260));
            cs.applyTo(layoutImages);

            holder.rightContainer.setVisibility(View.VISIBLE);
            holder.img2.setVisibility(View.VISIBLE);
            holder.frameImg3.setVisibility(View.VISIBLE);
            holder.img3.setVisibility(View.VISIBLE);

            holder.img1.setVisibility(View.VISIBLE);

            Glide.with(ctx).load(images.get(0)).centerCrop().into(holder.img1);
            Glide.with(ctx).load(images.get(1)).centerCrop().into(holder.img2);
            Glide.with(ctx).load(images.get(2)).centerCrop().into(holder.img3);

            holder.img1.setOnClickListener(v ->
                    FullScreenImageActivity.open(ctx, images, 0));
            holder.img2.setOnClickListener(v ->
                    FullScreenImageActivity.open(ctx, images, 1));

            if (count > 3) {
                // Có thêm ảnh → hiện overlay "+N", click → bottom sheet
                holder.viewOverlay.setVisibility(View.VISIBLE);
                holder.tvMore.setVisibility(View.VISIBLE);
                holder.tvMore.setText("+" + (count - 3));

                View.OnClickListener openSheet = v ->
                        ImageListBottomSheet.show(ctx, images);
                holder.img3.setOnClickListener(openSheet);
                holder.viewOverlay.setOnClickListener(openSheet);
                holder.tvMore.setOnClickListener(openSheet);
            } else {
                // Đúng 3 ảnh → click xem full
                holder.img3.setOnClickListener(v ->
                        FullScreenImageActivity.open(ctx, images, 2));
            }
        }
    }

    private int dpToPx(Context ctx, int dp) {
        float density = ctx.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void updateLikeUI(@NonNull PostViewHolder holder, PostModel post) {
        if (post.isLikedByMe()) {
            holder.btnLike.setImageResource(R.drawable.ic_heart_filled_24);
            holder.btnLike.setColorFilter(
                    android.graphics.Color.RED,
                    android.graphics.PorterDuff.Mode.SRC_IN
            );
        } else {
            holder.btnLike.setImageResource(R.drawable.ic_heart_outline_24);
            holder.btnLike.clearColorFilter();
        }
    }
    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position,
                                 @NonNull List<Object> payloads) {

        Log.d("ADAPTER", "payloads size: " + payloads.size() + " | content: " + payloads.toString());

        PostModel post = postList.get(position);

        if (!payloads.isEmpty() && payloads.contains("LIKE_UPDATE")) {

            updateLikeUI(holder, post);

            int soLuotThich = post.getSoLuotThich();
            int soBinhLuan  = post.getSoBinhLuan();

            if (soBinhLuan == 0 && soLuotThich == 0) {

                holder.tvStats.setVisibility(View.GONE);

            } else {

                holder.tvStats.setVisibility(View.VISIBLE);

                holder.tvStats.setText(
                        soBinhLuan + " lượt trả lời · "
                                + soLuotThich + " lượt thích"
                );
            }

        } else {

            super.onBindViewHolder(holder, position, payloads);
        }

        // ===================================================
        // REPOST
        // ===================================================

        View layoutRepost = holder.itemView.findViewById(R.id.layoutRepost);

        TextView tvRepostAuthor =
                holder.itemView.findViewById(R.id.tvRepostAuthor);

        TextView tvRepostContent =
                holder.itemView.findViewById(R.id.tvRepostContent);

        ShapeableImageView ivRepostAvatar =
                holder.itemView.findViewById(R.id.ivRepostAvatar);

        // ✅ THÊM IMAGEVIEW NÀY
        ImageView ivRepostImage =
                holder.itemView.findViewById(R.id.ivRepostImage);

        PostModel postCha = post.getPostCha();

        if (postCha != null) {

            layoutRepost.setVisibility(View.VISIBLE);

            tvRepostAuthor.setText(
                    postCha.getTenDangNhap() != null
                            ? "@" + postCha.getTenDangNhap()
                            : "Người dùng"
            );

            tvRepostContent.setText(
                    postCha.getNoiDung() != null
                            ? postCha.getNoiDung()
                            : ""
            );

            // =========================
            // AVATAR
            // =========================

            if (postCha.getAnhDaiDien() != null
                    && !postCha.getAnhDaiDien().isEmpty()) {

                Glide.with(holder.itemView.getContext())
                        .load(postCha.getAnhDaiDien())
                        .circleCrop()
                        .into(ivRepostAvatar);
            }

            // =========================
            // ẢNH REPOST
            // =========================

            if (postCha.getDanhSachAnh() != null
                    && !postCha.getDanhSachAnh().isEmpty()) {

                ivRepostImage.setVisibility(View.VISIBLE);

                Glide.with(holder.itemView.getContext())
                        .load(postCha.getDanhSachAnh().get(0))
                        .placeholder(R.drawable.ic_placeholder_avatar)
                        .into(ivRepostImage);

            } else {

                ivRepostImage.setVisibility(View.GONE);
            }

            layoutRepost.setOnClickListener(v -> {

                Intent intent = new Intent(
                        holder.itemView.getContext(),
                        PostDetailActivity.class
                );

                intent.putExtra(
                        PostDetailActivity.EXTRA_POST_ID,
                        postCha.getDocumentId()
                );

                holder.itemView.getContext()
                        .startActivity(intent);
            });

        } else {

            layoutRepost.setVisibility(View.GONE);
        }
    }

    public void updateListSmooth(List<PostModel> newList) {
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return postList.size(); }
            @Override public int getNewListSize() { return newList.size(); }
            @Override public boolean areItemsTheSame(int oldPos, int newPos) {
                return postList.get(oldPos).getDocumentId()
                        .equals(newList.get(newPos).getDocumentId());
            }
            @Override public boolean areContentsTheSame(int oldPos, int newPos) {
                PostModel o = postList.get(oldPos);
                PostModel n = newList.get(newPos);
                return o.getSoLuotThich() == n.getSoLuotThich()
                        && o.getSoBinhLuan() == n.getSoBinhLuan()
                        && o.isLikedByMe() == n.isLikedByMe();
            }
        });
        this.postList = newList;
        result.dispatchUpdatesTo(this);
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

    @Override public int getItemCount() { return postList != null ? postList.size() : 0; }

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

    // ════════════════════════════════════════════════════════
    //  VIEW HOLDER
    // ════════════════════════════════════════════════════════
    public static class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView ivRepostImage;
        TextView tvTopCommentUser, tvTopComment, tvReplies;
        LinearLayout layoutTopComment;
        ImageView imgTopCommentAvatar;
        ShapeableImageView ivAvatar;
        ImageView img1, img2, img3, ivAddFriend, ivVerified;
        FrameLayout frameImg3;
        LinearLayout rightContainer;
        TextView tvMore;
        View viewOverlay;
        ConstraintLayout layoutImages;
        TextView tvAuthorName, tvPostTime, tvContent, tvStats;
        ImageButton btnLike, btnComment, btnRepost, btnShare, btnMoreOptions;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            ivRepostImage = itemView.findViewById(R.id.ivRepostImage);
            tvTopCommentUser    = itemView.findViewById(R.id.tvTopCommentUser);
            layoutTopComment    = itemView.findViewById(R.id.layoutTopComment);
            imgTopCommentAvatar = itemView.findViewById(R.id.imgTopCommentAvatar);
            tvTopComment        = itemView.findViewById(R.id.tvTopComment);
            tvReplies           = itemView.findViewById(R.id.tvReplies);
            layoutImages        = itemView.findViewById(R.id.layoutImages);
            img1                = itemView.findViewById(R.id.img1);
            img2                = itemView.findViewById(R.id.img2);
            img3                = itemView.findViewById(R.id.img3);
            frameImg3           = itemView.findViewById(R.id.frameImg3);
            rightContainer      = itemView.findViewById(R.id.rightContainer);
            ivAddFriend         = itemView.findViewById(R.id.ivAddFriend);
            viewOverlay         = itemView.findViewById(R.id.viewOverlay);
            tvMore              = itemView.findViewById(R.id.tvMore);
            ivAvatar            = itemView.findViewById(R.id.ivAvatar);
            ivVerified          = itemView.findViewById(R.id.ivVerified);
            tvAuthorName        = itemView.findViewById(R.id.tvAuthorName);
            tvPostTime          = itemView.findViewById(R.id.tvPostTime);
            tvContent           = itemView.findViewById(R.id.tvContent);
            tvStats             = itemView.findViewById(R.id.tvStats);
            btnLike             = itemView.findViewById(R.id.btnLike);
            btnComment          = itemView.findViewById(R.id.btnComment);
            btnRepost           = itemView.findViewById(R.id.btnRepost);
            btnShare            = itemView.findViewById(R.id.btnShare);
            btnMoreOptions      = itemView.findViewById(R.id.btnMoreOptions);
        }
    }
}