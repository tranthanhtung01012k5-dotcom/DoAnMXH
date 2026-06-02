package com.example.doanmxh.Message;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doanmxh.R;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

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
    public interface OnItemClickListener {
        void onItemClick(ChatUser chatUser);
    }

    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {

        ChatUser chat = chatList.get(position);
        holder.txtUsername.setText(chat.getUsername());
        holder.txtNguoiGui.setText(chat.getTenNguoiGui());
        Log.d("ADAPTER",
                "username=" + chat.getUsername()
                        + ", nguoiGui=" + chat.getTenNguoiGui());        holder.txtLastMessageContent.setText(chat.getLastMessage());
        Timestamp timestamp = chat.getChatTime();

        SimpleDateFormat sdf =
                new SimpleDateFormat("HH:mm", Locale.getDefault());

        holder.txtChatTime.setText(
                sdf.format(timestamp.toDate())
        );
//        holder.demoAvatar.setImageResource(chat.getAvatarResId());
        Glide.with(holder.itemView.getContext())
                .load(chat.getAvatarResId())
                .placeholder(R.drawable.ic_person_outline_24)
                .error(R.drawable.ic_person_outline_24)
                .into(holder.demoAvatar);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(chat);
        });
    }

    @Override
    public int getItemCount() {
        return chatList != null ? chatList.size() : 0;
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        ImageView demoAvatar;
        TextView txtUsername, txtLastMessageContent, txtChatTime,txtNguoiGui;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            demoAvatar = itemView.findViewById(R.id.demoAvatar);
            txtUsername = itemView.findViewById(R.id.txtUsername);
            txtNguoiGui = itemView.findViewById(R.id.txtNguoiGui);
            txtLastMessageContent = itemView.findViewById(R.id.txtLastMessageContent);
            txtChatTime = itemView.findViewById(R.id.txtChatTime);
        }
    }
}