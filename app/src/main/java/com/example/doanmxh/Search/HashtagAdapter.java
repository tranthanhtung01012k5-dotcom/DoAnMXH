package com.example.doanmxh.Search;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doanmxh.R;

import java.util.List;

public class HashtagAdapter extends RecyclerView.Adapter<HashtagAdapter.VH> {

    public interface OnHashtagClick { void onClick(String tag); }

    private List<HashtagItem> list;
    private OnHashtagClick listener;

    public HashtagAdapter(List<HashtagItem> list, OnHashtagClick listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_hastag, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        HashtagItem item = list.get(position);
        holder.tvTag.setText("#" + item.ten);
        holder.tvCount.setText(item.soBaiViet + " bài viết");
        holder.itemView.setOnClickListener(v -> listener.onClick(item.ten));
    }

    @Override public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTag, tvCount;
        VH(@NonNull View v) {
            super(v);
            tvTag   = v.findViewById(R.id.tvTag);
            tvCount = v.findViewById(R.id.tvCount);
        }
    }
}