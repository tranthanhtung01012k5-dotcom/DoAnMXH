package com.example.doanmxh.HomePage;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.VideoView;
import android.media.MediaPlayer;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doanmxh.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Bottom Sheet hiển thị danh sách ảnh + video dạng dọc (1 item/dòng).
 * - Ảnh: click → FullScreenImageActivity
 * - Video: play/pause inline
 *
 * Mở bằng:
 *   ImageListBottomSheet.show(context, imageUrls, videoUrls);
 */
public class ImageListBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_IMAGES = "arg_images";
    private static final String ARG_VIDEOS = "arg_videos";

    // ── Mở bottom sheet với cả ảnh lẫn video ──────────────────
    public static void show(Context ctx, List<String> images, List<String> videos) {
        if (!(ctx instanceof androidx.fragment.app.FragmentActivity)) return;

        ImageListBottomSheet sheet = new ImageListBottomSheet();
        Bundle args = new Bundle();
        args.putStringArrayList(ARG_IMAGES, new ArrayList<>(images != null ? images : new ArrayList<>()));
        args.putStringArrayList(ARG_VIDEOS, new ArrayList<>(videos != null ? videos : new ArrayList<>()));
        sheet.setArguments(args);
        sheet.show(
                ((androidx.fragment.app.FragmentActivity) ctx).getSupportFragmentManager(),
                "ImageListBottomSheet"
        );
    }

    // Giữ backward-compatible với code cũ chỉ truyền ảnh
    public static void show(Context ctx, List<String> images) {
        show(ctx, images, new ArrayList<>());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_image_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        List<String> images = getArguments() != null
                ? getArguments().getStringArrayList(ARG_IMAGES) : new ArrayList<>();
        List<String> videos = getArguments() != null
                ? getArguments().getStringArrayList(ARG_VIDEOS) : new ArrayList<>();

        // Gộp tất cả thành một danh sách MediaItem
        List<MediaItem> items = new ArrayList<>();
        if (images != null) {
            for (String url : images) items.add(new MediaItem(url, MediaItem.TYPE_IMAGE));
        }
        if (videos != null) {
            for (String url : videos) items.add(new MediaItem(url, MediaItem.TYPE_VIDEO));
        }

        // Lọc chỉ ảnh để truyền vào FullScreenImageActivity
        List<String> imageUrlsOnly = new ArrayList<>();
        if (images != null) imageUrlsOnly.addAll(images);

        RecyclerView rv = view.findViewById(R.id.rvImageList);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(new MediaListAdapter(items, (item, index) -> {
            if (item.type == MediaItem.TYPE_IMAGE) {
                // Tính index thực trong danh sách ảnh
                int imageIndex = 0;
                for (int i = 0; i < index; i++) {
                    if (items.get(i).type == MediaItem.TYPE_IMAGE) imageIndex++;
                }
                dismiss();
                FullScreenImageActivity.open(requireContext(), imageUrlsOnly, imageIndex);
            }
            // Video xử lý play/pause trực tiếp trong ViewHolder
        }));
    }

    // ── Model ──────────────────────────────────────────────────
    static class MediaItem {
        static final int TYPE_IMAGE = 0;
        static final int TYPE_VIDEO = 1;

        final String url;
        final int type;

        MediaItem(String url, int type) {
            this.url  = url;
            this.type = type;
        }
    }

    // ── Adapter ────────────────────────────────────────────────
    static class MediaListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        interface OnItemClick {
            void onClick(MediaItem item, int index);
        }

        private static final int VIEW_IMAGE = 0;
        private static final int VIEW_VIDEO = 1;

        private final List<MediaItem> items;
        private final OnItemClick listener;

        MediaListAdapter(List<MediaItem> items, OnItemClick listener) {
            this.items    = items;
            this.listener = listener;
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position).type == MediaItem.TYPE_VIDEO ? VIEW_VIDEO : VIEW_IMAGE;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == VIEW_VIDEO) {
                View v = inflater.inflate(R.layout.item_video_list, parent, false);
                return new VideoVH(v);
            } else {
                View v = inflater.inflate(R.layout.item_image_list, parent, false);
                return new ImageVH(v);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            MediaItem item = items.get(position);
            if (holder instanceof ImageVH) {
                ((ImageVH) holder).bind(item, position, listener);
            } else if (holder instanceof VideoVH) {
                ((VideoVH) holder).bind(item);
            }
        }

        @Override
        public int getItemCount() { return items != null ? items.size() : 0; }

        // ── Image ViewHolder ──
        static class ImageVH extends RecyclerView.ViewHolder {
            ImageView imageView;

            ImageVH(@NonNull View v) {
                super(v);
                imageView = v.findViewById(R.id.ivListImage);
            }

            void bind(MediaItem item, int position, OnItemClick listener) {
                Glide.with(imageView.getContext())
                        .load(item.url)
                        .centerCrop()
                        .placeholder(R.drawable.ic_placeholder_avatar)
                        .into(imageView);

                imageView.setOnClickListener(v -> listener.onClick(item, position));
            }
        }

        // ── Video ViewHolder ──
        static class VideoVH extends RecyclerView.ViewHolder {
            VideoView videoView;
            ImageButton btnPlayPause;
            ImageView imgThumbnail;
            boolean isPrepared = false;

            VideoVH(@NonNull View v) {
                super(v);
                videoView    = v.findViewById(R.id.videoView);
                btnPlayPause = v.findViewById(R.id.btnPlayPause);
                imgThumbnail = v.findViewById(R.id.imgVideoThumbnail);
            }

            void bind(MediaItem item) {
                // Load thumbnail bằng Glide (frame đầu của video)
                Glide.with(imgThumbnail.getContext())
                        .load(item.url)
                        .centerCrop()
                        .placeholder(R.drawable.ic_placeholder_avatar)
                        .into(imgThumbnail);

                videoView.setVideoPath(item.url);

                videoView.setOnPreparedListener(mp -> {
                    isPrepared = true;
                    mp.setLooping(true);
                });

                btnPlayPause.setOnClickListener(v -> {
                    if (!isPrepared) {
                        videoView.setVideoPath(item.url);
                        videoView.requestFocus();
                    }

                    if (videoView.isPlaying()) {
                        videoView.pause();
                        imgThumbnail.setVisibility(View.GONE);
                        btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
                    } else {
                        videoView.start();
                        imgThumbnail.setVisibility(View.GONE);
                        btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                    }
                });

                videoView.setOnCompletionListener(mp -> {
                    btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
                });
            }
        }
    }
}