package com.example.doanmxh.HomePage;

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

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {

    private List<String> imageList;
    private OnImageRemoveListener removeListener;

    public interface OnImageRemoveListener {
        void onRemove(int position);
    }

    // Constructor không có listener → dùng ở chỗ chỉ xem (không xóa)
    public ImageAdapter(List<String> imageList) {
        this.imageList = imageList;
    }

    // Constructor có listener → dùng ở EditPostActivity
    public ImageAdapter(List<String> imageList, OnImageRemoveListener removeListener) {
        this.imageList = imageList;
        this.removeListener = removeListener;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        Glide.with(holder.itemView.getContext())
                .load(imageList.get(position))
                .into(holder.imageView);

        // Hiện nút X nếu có listener, ẩn nếu không
        if (removeListener != null) {
            holder.btnRemove.setVisibility(View.VISIBLE);
            holder.btnRemove.setOnClickListener(v -> {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_ID) {
                    imageList.remove(pos);
                    notifyItemRemoved(pos);
                    removeListener.onRemove(pos);
                }
            });
        } else {
            holder.btnRemove.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return imageList != null ? imageList.size() : 0;
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageButton btnRemove;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imgPost);
            btnRemove = itemView.findViewById(R.id.btnRemoveImage);
        }
    }
}