package com.example.doanmxh.Search;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.doanmxh.R;

import java.util.ArrayList;

public class TopPostAdapter
        extends RecyclerView.Adapter<TopPostAdapter.ViewHolder> {

    private final ArrayList<Post> posts;

    public TopPostAdapter(ArrayList<Post> posts) {
        this.posts = posts;
    }

    public static class ViewHolder
            extends RecyclerView.ViewHolder {

        TextView tvUsername;
        TextView tvPostContent;

        public ViewHolder(View view) {
            super(view);

            tvUsername =
                    view.findViewById(R.id.tvUsername);

            tvPostContent =
                    view.findViewById(R.id.tvPostContent);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(
            ViewGroup parent,
            int viewType) {

        View view =
                LayoutInflater.from(parent.getContext())
                        .inflate(
                                R.layout.item_post,
                                parent,
                                false
                        );

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            ViewHolder holder,
            int position) {

        Post post = posts.get(position);

        holder.tvUsername.setText(
                post.getNguoi_dung_id()
        );

        holder.tvPostContent.setText(
                post.getNoi_dung()
        );
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }
}