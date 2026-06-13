package com.example.doanmxh.HomePage;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doanmxh.R;

import java.util.List;

public class MediaListAdapter
        extends RecyclerView.Adapter<MediaListAdapter.MediaVH> {

    private final Context context;
    private final List<MediaItem> mediaList;

    public MediaListAdapter(Context context,
                            List<MediaItem> mediaList) {
        this.context = context;
        this.mediaList = mediaList;
    }

    @NonNull
    @Override
    public MediaVH onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_media_bottom,
                        parent,
                        false);

        return new MediaVH(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull MediaVH holder,
            int position) {

        MediaItem item = mediaList.get(position);

        String thumbnail =
                item.getType() == MediaItem.Type.VIDEO
                        ? item.getThumbnail()
                        : item.getUrl();

        Glide.with(context)
                .load(thumbnail)
                .centerCrop()
                .into(holder.imageView);

        holder.itemView.setOnClickListener(v -> {

            if (item.getType() == MediaItem.Type.VIDEO) {

                FullScreenVideoActivity.open(
                        context,
                        item.getUrl()
                );

            } else {

                FullScreenImageActivity.open(
                        context,
                        extractImages(mediaList),
                        position
                );
            }
        });
    }

    @Override
    public int getItemCount() {
        return mediaList.size();
    }

    private List<String> extractImages(List<MediaItem> list) {

        java.util.ArrayList<String> urls =
                new java.util.ArrayList<>();

        for (MediaItem item : list) {
            if (item.getType() == MediaItem.Type.IMAGE) {
                urls.add(item.getUrl());
            }
        }

        return urls;
    }

    static class MediaVH extends RecyclerView.ViewHolder {

        ImageView imageView;

        MediaVH(@NonNull View itemView) {
            super(itemView);

            imageView =
                    itemView.findViewById(R.id.imageMedia);
        }
    }
}