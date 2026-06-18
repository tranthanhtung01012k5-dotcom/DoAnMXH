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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doanmxh.CreatePage.AudioAdapter;
import com.example.doanmxh.ProfilePage.UserProfileActivity;
import com.example.doanmxh.R;
import com.example.doanmxh.Search.SearchResultActivity;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.widget.VideoView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private List<PostModel> postList;
    private OnPostActionListener listener;
    private OnItemClickListener itemClickListener;
    // PostAdapter.java — thêm constants
    private static final int VIEW_TYPE_SKELETON = 0;
    private static final int VIEW_TYPE_POST     = 1;

// Thêm flag vào PostModel
// post.setLoading(true) = skeleton, false = real content

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

            holder.tvContent.setText(
                    highlightText(holder.tvContent, content)
            );
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
                    holder.tvContent.setText(
                            highlightText(holder.tvContent, content)
                    );                    return;
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

        Log.e("POST_BIND", "Bind post = " + post.getDocumentId());

        List<String> audioList = post.getDanhSachAudio();

        Log.e("POST_BIND", "audioList = " + audioList);
        if (audioList != null && !audioList.isEmpty()) {
            holder.rvAudio.setVisibility(View.VISIBLE); // Phải hiển thị

            // Luôn reset layout manager hoặc adapter để tránh dữ liệu cũ
            if (holder.rvAudio.getAdapter() == null) {
                holder.rvAudio.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
                AudioAdapter audioAdapter = new AudioAdapter(new ArrayList<>(audioList), null);
                holder.rvAudio.setAdapter(audioAdapter);
            } else {
                AudioAdapter adapter = (AudioAdapter) holder.rvAudio.getAdapter();
                adapter.updateList(audioList);
            }
        } else {
            holder.rvAudio.setVisibility(View.GONE); // Ẩn hoàn toàn nếu rỗng
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
        bindMediaGrid(holder, post.getMediaList());


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
    private void bindMediaGrid(@NonNull PostViewHolder holder, List<MediaItem> mediaList) {
        Context ctx = holder.itemView.getContext();

        // ── Reset ──
        holder.layoutImages.setVisibility(View.GONE);
        holder.rightContainer.setVisibility(View.GONE);
        //
        holder.frameImg1.setVisibility(View.GONE);
        holder.img1.setVisibility(View.GONE);
        holder.videoView1.setVisibility(View.GONE);
        holder.ivPlay1.setVisibility(View.GONE);
        //
        holder.frameImg2.setVisibility(View.GONE);
        holder.img2.setVisibility(View.GONE);
        holder.videoView2.setVisibility(View.GONE);
        holder.ivPlay2.setVisibility(View.GONE);
        ///
        holder.frameImg3.setVisibility(View.GONE);
        holder.img3.setVisibility(View.GONE);
        holder.videoView3.setVisibility(View.GONE);
        holder.ivPlay3.setVisibility(View.GONE);
        //
        holder.viewOverlay.setVisibility(View.GONE);
        holder.tvMore.setVisibility(View.GONE);

        // Stop video đang chạy (tránh leak khi recycle)
        holder.videoView1.stopPlayback();
        holder.videoView2.stopPlayback();
        holder.videoView3.stopPlayback();
        Log.d("MEDIA_DEBUG", "mediaList = " + (mediaList == null ? "NULL" : "size=" + mediaList.size()));

        if (mediaList == null || mediaList.isEmpty()) return;
        if (mediaList != null) {
            for (int i = 0; i < mediaList.size(); i++) {
                MediaItem item = mediaList.get(i);
                Log.d("MEDIA_DEBUG", "  [" + i + "] type=" + item.getType() + " url=" + item.getUrl());
            }
        }
        holder.layoutImages.setVisibility(View.VISIBLE);
        int count = mediaList.size();
        ConstraintSet cs = new ConstraintSet();
        cs.clone(holder.layoutImages);

        if (count == 1) {
            cs.connect(R.id.frameImg1, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
            cs.constrainPercentWidth(R.id.frameImg1, 1.0f);
            cs.constrainHeight(R.id.frameImg1, dpToPx(ctx, 300)); // set height cứng

            // THÊM: disconnect bottom constraint để height cứng có hiệu lực
            cs.clear(R.id.frameImg1, ConstraintSet.BOTTOM);

            cs.applyTo(holder.layoutImages);

            holder.frameImg1.setVisibility(View.VISIBLE);
            holder.rightContainer.setVisibility(View.GONE); // đảm bảo gone
            bindSlot(ctx, holder, mediaList.get(0), 0, mediaList, 1);
        } else if (count == 2) {
            cs.connect(R.id.frameImg1, ConstraintSet.END, R.id.rightContainer, ConstraintSet.START);
            cs.constrainPercentWidth(R.id.frameImg1, 0.5f);
            cs.constrainHeight(R.id.frameImg1, dpToPx(ctx, 220));
            cs.applyTo(holder.layoutImages);

            holder.frameImg1.setVisibility(View.VISIBLE);
            holder.frameImg2.setVisibility(View.VISIBLE);
            holder.rightContainer.setVisibility(View.VISIBLE);
            holder.img2.setVisibility(View.VISIBLE);
            holder.videoView2.setVisibility(View.VISIBLE);

            holder.frameImg3.setVisibility(View.GONE);

            bindSlot(ctx, holder, mediaList.get(0), 0, mediaList, 2);
            bindSlot(ctx, holder, mediaList.get(1), 1, mediaList, 2);
            Log.e("CHECK",
                    "frame2=" + holder.frameImg2.getVisibility()
                            + " img2=" + holder.img2.getVisibility());

        } else {
            // 3+ media
            cs.connect(R.id.frameImg1, ConstraintSet.END, R.id.rightContainer, ConstraintSet.START);
            cs.constrainPercentWidth(R.id.frameImg1, 0.5f);
            cs.constrainHeight(R.id.frameImg1, dpToPx(ctx, 260));
            cs.applyTo(holder.layoutImages);

            holder.frameImg1.setVisibility(View.VISIBLE);
            holder.frameImg2.setVisibility(View.VISIBLE);
            holder.rightContainer.setVisibility(View.VISIBLE);
            holder.img2.setVisibility(View.VISIBLE);
            holder.videoView2.setVisibility(View.VISIBLE);
            holder.frameImg3.setVisibility(View.VISIBLE);
            holder.img3.setVisibility(View.VISIBLE);
            holder.videoView3.setVisibility(View.VISIBLE);

            bindSlot(ctx, holder, mediaList.get(0), 0, mediaList, count);
            bindSlot(ctx, holder, mediaList.get(1), 1, mediaList, count);
            bindSlot(ctx, holder, mediaList.get(2), 2, mediaList, count);

            if (count > 3) {
                holder.viewOverlay.setVisibility(View.VISIBLE);
                holder.tvMore.setVisibility(View.VISIBLE);
                holder.tvMore.setText("+" + (count - 3));
                View.OnClickListener openSheet = v ->
                        MediaListBottomSheet.show(ctx, mediaList); // cập nhật BottomSheet nhận MediaItem
                holder.frameImg3.setOnClickListener(openSheet);
                holder.viewOverlay.setOnClickListener(openSheet);
                holder.tvMore.setOnClickListener(openSheet);
            }
        }
    }

    // ── Helper: bind 1 slot (slot 0 = frameImg1, 1 = img2, 2 = img3) ──
    private void bindSlot(Context ctx, PostViewHolder holder,
                          MediaItem item, int slotIndex,
                          List<MediaItem> allMedia, int totalCount) {

        ImageView imgView   = slotIndex == 0 ? holder.img1
                : slotIndex == 1 ? holder.img2
                : holder.img3;
        VideoView videoView = slotIndex == 0 ? holder.videoView1
                : slotIndex == 1 ? holder.videoView2
                : holder.videoView3;
        ImageView playBtn   = slotIndex == 0 ? holder.ivPlay1
                : slotIndex == 1 ? holder.ivPlay2
                : holder.ivPlay3;

        // ← THÊM: reset slot trước
        imgView.setVisibility(View.GONE);
        videoView.setVisibility(View.GONE);
        playBtn.setVisibility(View.GONE);

        if (item.getType() == MediaItem.Type.IMAGE) {
            imgView.setVisibility(View.VISIBLE);
            videoView.setVisibility(View.GONE);
            playBtn.setVisibility(View.GONE);
            Glide.with(ctx).load(item.getUrl()).centerCrop().into(imgView);
            imgView.setOnClickListener(v ->
                    FullScreenImageActivity.open(ctx,
                            extractImageUrls(allMedia), slotIndex));

        } else {
            // VIDEO
            imgView.setVisibility(View.VISIBLE);   // thumbnail
            videoView.setVisibility(View.GONE);
            playBtn.setVisibility(View.VISIBLE);   // ← hiện nút play

            String thumb = item.getThumbnail();
            Glide.with(ctx)
                    .load(thumb != null && !thumb.isEmpty() ? thumb : item.getUrl())
                    .centerCrop()
                    .placeholder(R.drawable.ic_placeholder_avatar)
                    .into(imgView);

            View.OnClickListener playClick = v -> {
                // ĐỪNG hide imgView ngay — để videoView lấy kích thước trước
                playBtn.setVisibility(View.GONE);

                videoView.setVisibility(View.VISIBLE);
                videoView.setVideoPath(item.getUrl());
                videoView.requestFocus();

                videoView.setOnPreparedListener(mp -> {
                    // Chỉ hide imgView SAU KHI video sẵn sàng play
                    imgView.setVisibility(View.GONE);
                    mp.start();
                    mp.setLooping(true);
                });

                videoView.setOnErrorListener((mp, what, extra) -> {
                    Log.e("VIDEO", "Error: what=" + what + " extra=" + extra);
                    // Khôi phục lại thumbnail nếu lỗi
                    imgView.setVisibility(View.VISIBLE);
                    playBtn.setVisibility(View.VISIBLE);
                    videoView.setVisibility(View.GONE);
                    return true;
                });

                videoView.setOnClickListener(fv ->
                        FullScreenVideoActivity.open(ctx, item.getUrl()));
            };

            imgView.setOnClickListener(playClick);
            playBtn.setOnClickListener(playClick);
        }
        Log.d("MEDIA_BIND", "slot=" + slotIndex
                + " type=" + item.getType()
                + " url=" + item.getUrl());
    }
    // Helper lấy list URL ảnh (bỏ qua video) để truyền cho FullScreenImageActivity
    private List<String> extractImageUrls(List<MediaItem> mediaList) {
        List<String> urls = new ArrayList<>();
        for (MediaItem m : mediaList) {
            if (m.getType() == MediaItem.Type.IMAGE) urls.add(m.getUrl());
        }
        return urls;
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
    @Override
    public int getItemViewType(int position) {
        return postList.get(position).isLoading() ? VIEW_TYPE_SKELETON : VIEW_TYPE_POST;
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
    private SpannableString highlightText(TextView textView, String text) {

        SpannableString spannable = new SpannableString(text);

        // ── Mention @username ──
        Pattern mentionPattern = Pattern.compile("@\\w+");
        Matcher mentionMatcher = mentionPattern.matcher(text);

        while (mentionMatcher.find()) {
            String mention  = mentionMatcher.group();
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
                            .addOnSuccessListener(snap -> {
                                if (!snap.isEmpty()) {
                                    String uid = snap.getDocuments().get(0).getId();
                                    Intent intent = new Intent(ctx, UserProfileActivity.class);
                                    intent.putExtra("user_uid", uid);
                                    ctx.startActivity(intent);
                                }
                            });
                }

                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    ds.setColor(Color.parseColor("#1DA1F2"));
                    ds.setUnderlineText(false);
                }
            };

            spannable.setSpan(clickableSpan,
                    mentionMatcher.start(), mentionMatcher.end(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // ── Hashtag #topic ──
        Pattern hashtagPattern = Pattern.compile("#\\w+");
        Matcher hashtagMatcher = hashtagPattern.matcher(text);

        while (hashtagMatcher.find()) {
            String hashtag = hashtagMatcher.group();          // "#android"
            String topic   = hashtag.substring(1);            // "android"

            ClickableSpan hashSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    // Tuỳ bạn: mở màn search với keyword = hashtag
                    Context ctx = widget.getContext();
                    Intent intent = new Intent(ctx, SearchResultActivity.class);
                    intent.putExtra("keyword", hashtag);
                    ctx.startActivity(intent);
                }

                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    ds.setColor(Color.parseColor("#1DA1F2"));
                    ds.setUnderlineText(false);
                }
            };

            spannable.setSpan(hashSpan,
                    hashtagMatcher.start(), hashtagMatcher.end(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        textView.setMovementMethod(LinkMovementMethod.getInstance());
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
        ImageView img1, img2, img3, ivAddFriend, ivVerified,ivPlay1, ivPlay2, ivPlay3;
        FrameLayout frameImg3,frameImg2,frameImg1;
        LinearLayout rightContainer;
        TextView tvMore;
        VideoView videoView1, videoView2, videoView3;
        View viewOverlay;
        ConstraintLayout layoutImages;
        TextView tvAuthorName, tvPostTime, tvContent;
        TextView tvLikeCount, tvCommentCount, tvRepostCount,tvShareCount,tvSeeMore; // ← thay tvStats
        ImageButton btnLike, btnComment, btnRepost, btnShare, btnMoreOptions;
        RecyclerView rvAudio;
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
            frameImg2           = itemView.findViewById(R.id.frameImg2);
            frameImg1           = itemView.findViewById(R.id.frameImg1);
            videoView1 = itemView.findViewById(R.id.videoView1);
            videoView2 = itemView.findViewById(R.id.videoView2);
            videoView3 = itemView.findViewById(R.id.videoView3);
            rvAudio = itemView.findViewById(R.id.rvAudio);
            ivPlay1 = itemView.findViewById(R.id.ivPlay1);
            ivPlay2 = itemView.findViewById(R.id.ivPlay2);
            ivPlay3 = itemView.findViewById(R.id.ivPlay3);
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