package com.example.doanmxh.HomePage;

import android.content.Context;
import android.content.Intent;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
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
import com.example.doanmxh.ProfilePage.UserProfileActivity;
import com.example.doanmxh.R;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

        String content = post.getNoiDung() != null ? post.getNoiDung() : "";

// nếu đã expand → show full
        if (post.isExpanded()) {

            holder.tvContent.setText(content);
            holder.tvContent.setMaxLines(Integer.MAX_VALUE);
            holder.tvContent.setEllipsize(null);
            holder.tvContent.setMovementMethod(null);

//            return;
        } else {
            holder.tvContent.post(() -> {

                int width = holder.tvContent.getWidth();
                if (width == 0) return;

                StaticLayout layout = StaticLayout.Builder.obtain(
                        content,
                        0,
                        content.length(),
                        holder.tvContent.getPaint(),
                        width
                ).build();

                if (layout.getLineCount() <= 3) {
                    holder.tvContent.setText(content);
                    return;
                }

                CharSequence visibleText = TextUtils.ellipsize(
                        content,
                        holder.tvContent.getPaint(),
                        width * 3,
                        TextUtils.TruncateAt.END
                );

                String finalText = visibleText + "  Xem thêm";

                SpannableString spannable = new SpannableString(finalText);

                int start = finalText.indexOf("Xem thêm");
                int end = finalText.length();

                ClickableSpan clickSpan = new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View widget) {

                        post.setExpanded(true);
                        notifyItemChanged(holder.getAdapterPosition());
                    }

                    @Override
                    public void updateDrawState(@NonNull TextPaint ds) {
                        ds.setColor(Color.parseColor("#3b82f6"));
                        ds.setUnderlineText(false);
                    }
                };

                spannable.setSpan(clickSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                holder.tvContent.setText(spannable);
                holder.tvContent.setMovementMethod(LinkMovementMethod.getInstance());
            });
        }

        bindStats(holder, post);

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
    private void bindStats(@NonNull PostViewHolder holder, PostModel post) {
        int likes    = post.getSoLuotThich();
        int comments = post.getSoBinhLuan();
        int reposts  = post.getSoRepost();
        int shares  = post.getSoShare();// nếu có, không thì bỏ dòng này

        holder.tvLikeCount.setText(likes > 0 ? String.valueOf(likes) : "");
        holder.tvCommentCount.setText(comments > 0 ? String.valueOf(comments) : "");
        holder.tvRepostCount.setText(reposts > 0 ? String.valueOf(reposts) : "");
        holder.tvShareCount.setText(shares > 0 ? String.valueOf(shares) : "");
        Log.d("LIKE_DEBUG", "so_like = " + post.getSoLuotThich());
    }
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

        if (!payloads.isEmpty()) {
            if (payloads.contains("LIKE_UPDATE") || payloads.contains("REPOST_UPDATE")) {
                updateLikeUI(holder, post);
                bindStats(holder, post);
            }
        } else {
            super.onBindViewHolder(holder, position, payloads);
        }
        // =========================
// REPOST FIXED VERSION
// =========================

        View layoutRepost = holder.itemView.findViewById(R.id.layoutRepost);

        TextView tvRepostAuthor = holder.itemView.findViewById(R.id.tvRepostAuthor);
        TextView tvRepostContent = holder.itemView.findViewById(R.id.tvRepostContent);
        ShapeableImageView ivRepostAvatar = holder.itemView.findViewById(R.id.ivRepostAvatar);
        ImageView ivRepostImage = holder.itemView.findViewById(R.id.ivRepostImage);

        PostModel postCha = post.getPostCha();

// RESET trước (QUAN TRỌNG)
        layoutRepost.setVisibility(View.GONE);

        if (postCha != null) {

            layoutRepost.setVisibility(View.VISIBLE);

            // AUTHOR
            tvRepostAuthor.setText(
                    postCha.getTenDangNhap() != null
                            ? "@" + postCha.getTenDangNhap()
                            : "Người dùng"
            );

            // CONTENT
            tvRepostContent.setText(
                    postCha.getNoiDung() != null
                            ? postCha.getNoiDung()
                            : ""
            );

            // AVATAR (SAFE CHECK)
            if (postCha.getAnhDaiDien() != null && !postCha.getAnhDaiDien().isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(postCha.getAnhDaiDien())
                        .circleCrop()
                        .into(ivRepostAvatar);
            } else {
                ivRepostAvatar.setImageResource(R.drawable.ic_placeholder_avatar);
            }

            // IMAGE (SAFE CHECK)
            if (postCha.getDanhSachAnh() != null && !postCha.getDanhSachAnh().isEmpty()) {

                ivRepostImage.setVisibility(View.VISIBLE);

                Glide.with(holder.itemView.getContext())
                        .load(postCha.getDanhSachAnh().get(0))
                        .placeholder(R.drawable.ic_placeholder_avatar)
                        .into(ivRepostImage);

            } else {
                ivRepostImage.setVisibility(View.GONE);
            }

            // CLICK
            layoutRepost.setOnClickListener(v -> {
                Intent intent = new Intent(holder.itemView.getContext(), PostDetailActivity.class);
                intent.putExtra(PostDetailActivity.EXTRA_POST_ID, postCha.getDocumentId());
                holder.itemView.getContext().startActivity(intent);
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
    private SpannableString highlightMentions(TextView textView, String text) {

        SpannableString spannable = new SpannableString(text);

        Pattern pattern = Pattern.compile("@\\w+");
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {

            String mention = matcher.group(); // @tung
            String username = mention.substring(1);

            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {

                    Context ctx = widget.getContext();

                    FirebaseFirestore.getInstance()
                            .collection("nguoi_dung")
                            .whereEqualTo("ten_dang_nhap", username)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {

                                if (!queryDocumentSnapshots.isEmpty()) {

                                    String uid =
                                            queryDocumentSnapshots
                                                    .getDocuments()
                                                    .get(0)
                                                    .getId();

                                    Log.d("DEBUG", "uid = " + uid);

                                    Intent intent =
                                            new Intent(ctx, UserProfileActivity.class);

                                    intent.putExtra("user_uid", uid);

                                    ctx.startActivity(intent);
                                }
                            });
                }

                @Override
                public void updateDrawState(
                        @NonNull android.text.TextPaint ds
                ) {
                    super.updateDrawState(ds);

                    ds.setColor(Color.parseColor("#1DA1F2"));
                    ds.setUnderlineText(false);
                }
            };

            spannable.setSpan(
                    clickableSpan,
                    matcher.start(),
                    matcher.end(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }

        textView.setMovementMethod(
                android.text.method.LinkMovementMethod.getInstance()
        );

        textView.setHighlightColor(Color.TRANSPARENT);

        return spannable;
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
        TextView tvAuthorName, tvPostTime, tvContent;
        TextView tvLikeCount, tvCommentCount, tvRepostCount,tvShareCount,tvSeeMore; // ← thay tvStats
        ImageButton btnLike, btnComment, btnRepost, btnShare, btnMoreOptions;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            ivRepostImage       = itemView.findViewById(R.id.ivRepostImage);
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
            tvLikeCount         = itemView.findViewById(R.id.tvLikeCount);    // ← mới
            tvCommentCount      = itemView.findViewById(R.id.tvCommentCount); // ← mới
            tvRepostCount       = itemView.findViewById(R.id.tvRepostCount);
            tvShareCount        = itemView.findViewById(R.id.tvShareCount); // ← mới
            btnLike             = itemView.findViewById(R.id.btnLike);
            btnComment          = itemView.findViewById(R.id.btnComment);
            btnRepost           = itemView.findViewById(R.id.btnRepost);
//            tvSeeMore           = itemView.findViewById(R.id.tvSeeMore);
            btnShare            = itemView.findViewById(R.id.btnShare);
            btnMoreOptions      = itemView.findViewById(R.id.btnMoreOptions);
        }
    }

}