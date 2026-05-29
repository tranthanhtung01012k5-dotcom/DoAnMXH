package com.example.doanmxh.Mention;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.doanmxh.R;
import com.google.android.material.imageview.ShapeableImageView;
import java.util.List;

public class MentionAdapter extends RecyclerView.Adapter<MentionAdapter.VH> {

    public interface OnUserSelectedListener {
        void onUserSelected(MentionUser user);
    }

    private final List<MentionUser> users;
    private final OnUserSelectedListener listener;

    public MentionAdapter(List<MentionUser> users, OnUserSelectedListener listener) {
        this.users = users;
        this.listener = listener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mention_user, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        MentionUser u = users.get(pos);
        h.tvName.setText(u.hoVaTen);
        h.tvUsername.setText("@" + u.tenDangNhap);
        if (u.anhDaiDien != null && !u.anhDaiDien.isEmpty()) {
            Glide.with(h.ivAvatar).load(u.anhDaiDien).circleCrop().into(h.ivAvatar);
        }
        h.itemView.setOnClickListener(v -> listener.onUserSelected(u));
    }

    @Override public int getItemCount() { return users.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ShapeableImageView ivAvatar;
        TextView tvName, tvUsername;
        VH(@NonNull View v) {
            super(v);
            ivAvatar   = v.findViewById(R.id.ivMentionAvatar);
            tvName     = v.findViewById(R.id.tvMentionName);
            tvUsername = v.findViewById(R.id.tvMentionUsername);
        }
    }
}