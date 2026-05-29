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

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatViewHolder> {

    private List<ChatUser> chatList;

    public ChatListAdapter(List<ChatUser> chatList) {
        this.chatList = chatList;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Nạp file xml item dòng chat bạn vừa sửa xong (Có tính năng dính sát thời gian)
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_thread_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatUser chat = chatList.get(position);
        holder.txtUsername.setText(chat.getUsername());
        holder.txtLastMessageContent.setText(chat.getLastMessage());
        holder.txtChatTime.setText(chat.getChatTime());
        holder.demoAvatar.setImageResource(chat.getAvatarResId());
    }

    @Override
    public int getItemCount() {
        return chatList != null ? chatList.size() : 0;
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        ImageView demoAvatar;
        TextView txtUsername, txtLastMessageContent, txtChatTime;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            demoAvatar = itemView.findViewById(R.id.demoAvatar);
            txtUsername = itemView.findViewById(R.id.txtUsername);
            txtLastMessageContent = itemView.findViewById(R.id.txtLastMessageContent);
            txtChatTime = itemView.findViewById(R.id.txtChatTime);
        }
    }
}