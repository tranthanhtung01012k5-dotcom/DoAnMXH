package com.example.doanmxh.Message;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.example.doanmxh.R;
import java.util.List;

public class FriendStoryAdapter extends RecyclerView.Adapter<FriendStoryAdapter.VH> {

    public interface OnItemClickListener {
        void onClick(FriendStoryItem item);
    }

    private final List<FriendStoryItem> list;
    private OnItemClickListener listener;

    public FriendStoryAdapter(List<FriendStoryItem> list) {
        this.list = list;
    }

    public void setOnItemClickListener(OnItemClickListener l) {
        this.listener = l;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend_story, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        FriendStoryItem item = list.get(position);

        Glide.with(h.itemView.getContext())
                .load(item.getAvatarRes())
                .placeholder(R.drawable.ic_person_outline_24)
                .error(R.drawable.ic_person_outline_24)
                .into(h.imgAvatar);
        h.tvName.setText(item.getName());

        // Chấm online
        h.viewOnlineDot.setVisibility(item.isOnline() ? View.VISIBLE : View.GONE);

        // Ring story
        switch (item.getStoryState()) {
            case NEW:
                h.viewRing.setVisibility(View.VISIBLE);
                h.viewRing.setBackgroundResource(R.drawable.bg_story_ring_gradient);
                break;
            case SEEN:
                h.viewRing.setVisibility(View.VISIBLE);
                h.viewRing.setBackgroundResource(R.drawable.bg_story_ring_seen);
                break;
            case NONE:
            default:
                h.viewRing.setVisibility(View.INVISIBLE);
                break;
        }

        // Preview status
        String preview = item.getStatusPreview();
        if (preview != null && !preview.isEmpty()) {
            h.tvStatusPreview.setText(preview);
            h.tvStatusPreview.setVisibility(View.VISIBLE);
        } else {
            h.tvStatusPreview.setVisibility(View.GONE);
        }

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(item);
        });
    }
    // Trong FriendStoryAdapter.java
    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size(); // ← thêm null check
    }

    static class VH extends RecyclerView.ViewHolder {
        ShapeableImageView imgAvatar;
        TextView           tvName, tvStatusPreview;
        View               viewOnlineDot, viewRing;

        VH(@NonNull View itemView) {
            super(itemView);
            imgAvatar       = itemView.findViewById(R.id.imgFriendAvatar);
            tvName          = itemView.findViewById(R.id.tvFriendName);
            tvStatusPreview = itemView.findViewById(R.id.tvStatusPreview);
            viewOnlineDot   = itemView.findViewById(R.id.viewOnlineDot);
            viewRing        = itemView.findViewById(R.id.viewStoryRing);
        }
    }
}