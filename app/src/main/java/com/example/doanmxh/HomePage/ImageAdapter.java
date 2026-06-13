package com.example.doanmxh.HomePage;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doanmxh.R;

import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int    TYPE_IMAGE   = 0;
    private static final int    TYPE_VIDEO   = 1;
    public  static final String VIDEO_PREFIX = "video:";

    private final List<String>          mediaList;
    private final OnImageRemoveListener removeListener;

    public interface OnImageRemoveListener {
        void onRemove(int position);
    }

    public ImageAdapter(List<String> mediaList) {
        this.mediaList      = mediaList;
        this.removeListener = null;
    }

    public ImageAdapter(List<String> mediaList, OnImageRemoveListener removeListener) {
        this.mediaList      = mediaList;
        this.removeListener = removeListener;
    }

    @Override
    public int getItemViewType(int position) {
        return mediaList.get(position).startsWith(VIDEO_PREFIX) ? TYPE_VIDEO : TYPE_IMAGE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_VIDEO) {
            return new VideoViewHolder(inf.inflate(R.layout.item_video_preview, parent, false));
        } else {
            return new ImageViewHolder(inf.inflate(R.layout.item_image, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        String raw = mediaList.get(position);

        if (holder instanceof VideoViewHolder) {
            VideoViewHolder vh = (VideoViewHolder) holder;
            String uriStr = raw.substring(VIDEO_PREFIX.length());

            Glide.with(vh.imgThumbnail.getContext())
                    .load(Uri.parse(uriStr))
                    .centerCrop()
                    .placeholder(R.drawable.ic_placeholder_avatar)
                    .into(vh.imgThumbnail);

            setupRemoveButton(vh.btnRemove, vh);

        } else if (holder instanceof ImageViewHolder) {
            ImageViewHolder vh = (ImageViewHolder) holder;

            Glide.with(vh.imageView.getContext())
                    .load(raw)
                    .centerCrop()
                    .into(vh.imageView);

            setupRemoveButton(vh.btnRemove, vh);
        }
    }

    /** Gắn nút X — dùng getAdapterPosition() từ ViewHolder để tránh stale index */
    // Trong ImageAdapter.java — sửa setupRemoveButton()
    private void setupRemoveButton(ImageButton btnRemove, RecyclerView.ViewHolder vh) {
        if (removeListener != null) {
            btnRemove.setVisibility(View.VISIBLE);
            btnRemove.setOnClickListener(v -> {
                int pos = vh.getAdapterPosition();
                if (pos != RecyclerView.NO_ID && pos < mediaList.size()) {
                    // ✅ Gọi listener TRƯỚC khi xóa — để Fragment còn đọc được giá trị
                    removeListener.onRemove(pos);
                    mediaList.remove(pos);
                    notifyItemRemoved(pos);
                    notifyItemRangeChanged(pos, mediaList.size());
                }
            });
        } else {
            btnRemove.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return mediaList != null ? mediaList.size() : 0;
    }

    // ── Image ViewHolder ──────────────────────────────────────
    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView   imageView;
        ImageButton btnRemove;

        ImageViewHolder(@NonNull View v) {
            super(v);
            imageView = v.findViewById(R.id.imgPost);
            btnRemove = v.findViewById(R.id.btnRemoveImage);
        }
    }

    // ── Video ViewHolder ──────────────────────────────────────
    static class VideoViewHolder extends RecyclerView.ViewHolder {
        ImageView   imgThumbnail;
        ImageView   icPlay;
        ImageButton btnRemove;

        VideoViewHolder(@NonNull View v) {
            super(v);
            imgThumbnail = v.findViewById(R.id.imgVideoThumbnail);
            icPlay       = v.findViewById(R.id.icPlay);
            btnRemove    = v.findViewById(R.id.btnRemoveImage);
        }
    }
}