package com.example.doanmxh.Message;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.doanmxh.R;
import java.util.List;

public class ActiveFriendsAdapter extends RecyclerView.Adapter<ActiveFriendsAdapter.FriendViewHolder> {

    private List<ChatUser> friendList;

    public ActiveFriendsAdapter(List<ChatUser> friendList) {
        this.friendList = friendList;
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Nạp file xml dòng bạn bè ngang bạn đã làm
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_active_friend, parent, false);
        return new FriendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        ChatUser user = friendList.get(position);
        holder.txtFriendName.setText(user.getUsername());
        holder.imgFriendAvatar.setImageResource(user.getAvatarResId());
    }

    @Override
    public int getItemCount() {
        return friendList != null ? friendList.size() : 0;
    }

    public static class FriendViewHolder extends RecyclerView.ViewHolder {
        ImageView imgFriendAvatar;
        TextView txtFriendName;

        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            imgFriendAvatar = itemView.findViewById(R.id.imgFriendAvatar);
            txtFriendName = itemView.findViewById(R.id.txtFriendName);
        }
    }
}