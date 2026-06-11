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
    private Mode mode;

    public enum Mode {
        CHAT_LIST,
        ARCHIVE_LIST
    }

    public ChatListAdapter(List<ChatUser> list, Mode mode) {
        this.chatList = list;
        this.mode = mode;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_thread_message, parent, false);
        return new ChatViewHolder(view);
    }

    // ───────────────── CLICK ─────────────────
    public interface OnItemClickListener {
        void onItemClick(ChatUser chatUser);
    }

    private OnItemClickListener clickListener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }

    // ───────────────── LONG CLICK CHAT ─────────────────
    public interface OnChatLongClickListener {
        void onChatLongClick(ChatUser chatUser);
    }

    private OnChatLongClickListener chatLongClickListener;

    public void setOnChatLongClickListener(OnChatLongClickListener listener) {
        this.chatLongClickListener = listener;
    }

    // ───────────────── LONG CLICK ARCHIVE ─────────────────
    public interface OnArchiveLongClickListener {
        void onArchiveLongClick(ChatUser chatUser);
    }

    private OnArchiveLongClickListener archiveLongClickListener;

    public void setOnArchiveLongClickListener(OnArchiveLongClickListener listener) {
        this.archiveLongClickListener = listener;
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

        // ── TIME SAFE ──
        Timestamp timestamp = chat.getChatTime();
        if (timestamp != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            holder.txtChatTime.setText(sdf.format(timestamp.toDate()));
        } else {
            holder.txtChatTime.setText("");
        }

        // ── AVATAR ──
        Glide.with(holder.itemView.getContext())
                .load(chat.getAvatarResId())
                .placeholder(R.drawable.ic_person_outline_24)
                .error(R.drawable.ic_person_outline_24)
                .into(holder.demoAvatar);

        // ── CLICK ──
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onItemClick(chat);
            }
        });

        // ── LONG CLICK THEO MODE ──
        holder.itemView.setOnLongClickListener(v -> {

            if (mode == Mode.CHAT_LIST) {
                if (chatLongClickListener != null) {
                    chatLongClickListener.onChatLongClick(chat);
                }
                return true;
            }

            if (mode == Mode.ARCHIVE_LIST) {
                if (archiveLongClickListener != null) {
                    archiveLongClickListener.onArchiveLongClick(chat);
                }
                return true;
            }

            return false;
        });

        // ── UNREAD UI ──
        if (chat.isChuaDoc()) {
            holder.txtUsername.setTypeface(null, android.graphics.Typeface.BOLD);
            holder.txtNguoiGui.setTypeface(null, android.graphics.Typeface.BOLD);
            holder.txtLastMessageContent.setTypeface(null, android.graphics.Typeface.BOLD);
            holder.txtLastMessageContent.setTextColor(0xFFFFFFFF);
            holder.txtChatTime.setTypeface(null, android.graphics.Typeface.BOLD);
            if (holder.unreadDot != null) holder.unreadDot.setVisibility(View.VISIBLE);
        } else {
            holder.txtUsername.setTypeface(null, android.graphics.Typeface.NORMAL);
            holder.txtNguoiGui.setTypeface(null, android.graphics.Typeface.NORMAL);
            holder.txtLastMessageContent.setTypeface(null, android.graphics.Typeface.NORMAL);
            holder.txtLastMessageContent.setTextColor(0xFF8E8E93);
            holder.txtChatTime.setTypeface(null, android.graphics.Typeface.NORMAL);
            if (holder.unreadDot != null) holder.unreadDot.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return chatList != null ? chatList.size() : 0;
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        ImageView demoAvatar;
        TextView txtUsername, txtLastMessageContent, txtChatTime, txtNguoiGui;
        View unreadDot, viewOnline;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            demoAvatar = itemView.findViewById(R.id.demoAvatar);
            txtUsername = itemView.findViewById(R.id.txtUsername);
            txtNguoiGui = itemView.findViewById(R.id.txtNguoiGui);
            txtLastMessageContent = itemView.findViewById(R.id.txtLastMessageContent);
            viewOnline = itemView.findViewById(R.id.viewOnline);
            txtChatTime = itemView.findViewById(R.id.txtChatTime);
            unreadDot = itemView.findViewById(R.id.unreadDot);
        }
    }
}