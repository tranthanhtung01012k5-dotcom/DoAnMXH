package com.example.doanmxh.HomePage;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doanmxh.R;
import com.github.chrisbanes.photoview.PhotoView;

import java.util.List;

/**
 * Adapter cho ViewPager2 trong FullScreenImageActivity.
 * Dùng PhotoView để hỗ trợ pinch-to-zoom.
 *
 * Yêu cầu thêm dependency vào build.gradle:
 *   implementation 'com.github.chrisbanes:PhotoView:2.3.0'
 * Và trong settings.gradle (nếu chưa có):
 *   maven { url 'https://jitpack.io' }
 */
public class FullScreenImagePagerAdapter
        extends RecyclerView.Adapter<FullScreenImagePagerAdapter.ImageViewHolder> {

    private final Context context;
    private final List<String> imageUrls;

    public FullScreenImagePagerAdapter(Context context, List<String> imageUrls) {
        this.context   = context;
        this.imageUrls = imageUrls;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_fullscreen_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        Glide.with(context)
                .load(imageUrls.get(position))
                .placeholder(R.drawable.ic_placeholder_avatar)
                .into(holder.photoView);
    }

    @Override
    public int getItemCount() {
        return imageUrls != null ? imageUrls.size() : 0;
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        PhotoView photoView;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            photoView = itemView.findViewById(R.id.photoView);
        }
    }
}