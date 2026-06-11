package com.example.doanmxh.Search;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doanmxh.R;

import java.util.ArrayList;

public class TrendingAdapter extends RecyclerView.Adapter<TrendingAdapter.ViewHolder> {

    public interface OnKeywordClick {
        void onClick(String keyword);
    }

    private final ArrayList<String> list;
    private final OnKeywordClick    listener;

    public TrendingAdapter(ArrayList<String> list, OnKeywordClick listener) {
        this.list     = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trending, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String keyword = list.get(position);
        holder.tvRank.setText(String.valueOf(position + 1));
        holder.tvKeyword.setText(keyword);
        holder.itemView.setOnClickListener(v -> listener.onClick(keyword));
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRank, tvKeyword;
        ViewHolder(View itemView) {
            super(itemView);
            tvRank    = itemView.findViewById(R.id.tvRank);
            tvKeyword = itemView.findViewById(R.id.tvKeyword);
        }
    }
}