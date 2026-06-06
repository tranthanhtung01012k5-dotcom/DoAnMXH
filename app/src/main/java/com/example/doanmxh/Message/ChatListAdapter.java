package com.example.doanmxh.Message;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity; // Thêm import này
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
        holder.txtLastMessageContent.setText(chat.getLastMessage());
        holder.viewOnline.setBackgroundResource(
                chat.isActive()
                        ? R.drawable.bg_online_dot
                        : R.drawable.bg_offline_dot
        );

        Timestamp timestamp = chat.getChatTime();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        holder.txtChatTime.setText(sdf.format(timestamp.toDate()));

        Glide.with(holder.itemView.getContext())
                .load(chat.getAvatarResId())
                .placeholder(R.drawable.ic_person_outline_24)
                .error(R.drawable.ic_person_outline_24)
                .into(holder.demoAvatar);

        // ── UNREAD: phải đặt SAU setText ──
        if (chat.isChuaDoc()) {
            holder.txtUsername.setTypeface(null, android.graphics.Typeface.BOLD);
            holder.txtNguoiGui.setTypeface(null, android.graphics.Typeface.BOLD);
            holder.txtLastMessageContent.setTypeface(null, android.graphics.Typeface.BOLD);
            holder.txtLastMessageContent.setTextColor(0xFFFFFFFF); // trắng
            holder.txtChatTime.setTypeface(null, android.graphics.Typeface.BOLD);
            if (holder.unreadDot != null) holder.unreadDot.setVisibility(View.VISIBLE);
        } else {
            holder.txtUsername.setTypeface(null, android.graphics.Typeface.NORMAL);
            holder.txtNguoiGui.setTypeface(null, android.graphics.Typeface.NORMAL);
            holder.txtLastMessageContent.setTypeface(null, android.graphics.Typeface.NORMAL);
            holder.txtLastMessageContent.setTextColor(0xFF8E8E93); // xám
            holder.txtChatTime.setTypeface(null, android.graphics.Typeface.NORMAL);
            if (holder.unreadDot != null) holder.unreadDot.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(chat);
        });


        // ĐOẠN ĐÃ SỬA LỖI Ở ĐÂY:
        holder.btnMore.setOnClickListener(v -> {
            android.content.Context context = holder.itemView.getContext();
            if (context instanceof AppCompatActivity) {
                BottomMessageMore bottomSheet = BottomMessageMore.newInstance(
                        chat.getUid(),
                        chat.getUsername(),       // tên hiển thị
                        chat.getLastMessage(),    // tin nhắn cuối
                        chat.getAvatarResId()     // URL avatar (String)
                );
                bottomSheet.show(
                        ((AppCompatActivity) context).getSupportFragmentManager(),
                        "BottomMessageMoreTag"
                );
            }
        });
    }

    @Override
    public int getItemCount() {
        return chatList != null ? chatList.size() : 0;
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        ImageView demoAvatar, btnMore;
        TextView txtUsername, txtLastMessageContent, txtChatTime, txtNguoiGui;
        View unreadDot,viewOnline; // chấm tròn báo chưa đọc

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            demoAvatar            = itemView.findViewById(R.id.demoAvatar);
            txtUsername           = itemView.findViewById(R.id.txtUsername);
            txtNguoiGui           = itemView.findViewById(R.id.txtNguoiGui);
            txtLastMessageContent = itemView.findViewById(R.id.txtLastMessageContent);
            viewOnline            = itemView.findViewById(R.id.viewOnline);
            txtChatTime           = itemView.findViewById(R.id.txtChatTime);
            btnMore               = itemView.findViewById(R.id.btnMore);
            unreadDot             = itemView.findViewById(R.id.unreadDot); // thêm vào layout nếu muốn
        }
    }
}