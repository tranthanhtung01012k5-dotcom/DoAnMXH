package com.example.doanmxh.CreatePage;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doanmxh.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AudioAdapter extends RecyclerView.Adapter<AudioAdapter.ViewHolder> {
    private MediaPlayer mediaPlayer;
    private int playingPosition = -1;
    private final List<String> audioList;
    private final OnDeleteClickListener listener;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                notifyItemChanged(playingPosition); // Cập nhật lại UI cho item đang phát
                handler.postDelayed(this, 500); // Cập nhật mỗi 0.5s
            }
        }
    };

    public interface OnDeleteClickListener {
        void onDelete(int position);
    }

    public AudioAdapter(List<String> audioList, OnDeleteClickListener listener) {
        this.audioList = (audioList != null) ? new ArrayList<>(audioList) : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_audio, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.txtAudioName.setText("Ghi âm " + (position + 1));
// Lấy duration thực của file audio
        String audioPath = audioList.get(position);

        new Thread(() -> {
            try {
                android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever();

                if (audioPath.startsWith("http://") || audioPath.startsWith("https://")) {
                    retriever.setDataSource(audioPath, new java.util.HashMap<>());
                } else {
                    retriever.setDataSource(holder.itemView.getContext(), Uri.parse(audioPath));
                }

                String durationStr = retriever.extractMetadata(
                        android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
                retriever.release();
                long duration = durationStr != null ? Long.parseLong(durationStr) : 0;

                // ✅ Cập nhật UI trên main thread
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    if (holder.txtDuration != null)
                        holder.txtDuration.setText(formatTime(duration));
                });
            } catch (Exception e) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    if (holder.txtDuration != null)
                        holder.txtDuration.setText("00:00");
                });
            }
        }).start();
        // Cập nhật trạng thái Play/Pause
        boolean isPlaying = (position == playingPosition && mediaPlayer != null);
        holder.btnPlay.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play_circle_24);

        if (isPlaying) {
            holder.seekBar.setMax(mediaPlayer.getDuration());
            holder.seekBar.setProgress(mediaPlayer.getCurrentPosition());
            holder.txtTime.setText(formatTime(mediaPlayer.getCurrentPosition()));
        } else {
            holder.seekBar.setProgress(0);
            holder.txtTime.setText("00:00");
        }

        // Tua nhạc
        holder.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null && holder.getAdapterPosition() == playingPosition) {
                    mediaPlayer.seekTo(progress);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Click Play
        holder.btnPlay.setOnClickListener(v -> {
            int currentPos = holder.getAdapterPosition();
            if (currentPos == RecyclerView.NO_POSITION) return;

            if (playingPosition == currentPos) {
                stopPlayer();
                notifyItemChanged(playingPosition);
                playingPosition = -1;
            } else {
                int oldPos = playingPosition;
                stopPlayer();
                try {
                    mediaPlayer = new MediaPlayer();
                    mediaPlayer.setDataSource(holder.itemView.getContext(), Uri.parse(audioList.get(currentPos)));
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                    mediaPlayer.setOnCompletionListener(mp -> {
                        stopPlayer();
                        notifyItemChanged(currentPos);
                        playingPosition = -1;
                    });
                    playingPosition = currentPos;
                    handler.post(progressRunnable);
                    if (oldPos != -1) notifyItemChanged(oldPos);
                    notifyItemChanged(currentPos);
                } catch (Exception e) { e.printStackTrace(); }
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            int currentPos = holder.getAdapterPosition();
            if (currentPos != RecyclerView.NO_POSITION && listener != null) {
                if (playingPosition == currentPos) stopPlayer();
                listener.onDelete(currentPos);
            }
        });
    }
    public void addItem(String path) {
        audioList.add(path);
        notifyItemInserted(audioList.size() - 1);
    }

    public void removeItem(int position) {
        audioList.remove(position);
        notifyItemRemoved(position);
    }

    public String getItem(int position) {
        return audioList.get(position);
    }

    @Override
    public int getItemCount() { // Log để kiểm tra xem có đúng là list đang có phần tử không
        Log.d("AudioAdapter", "Item count: " + audioList.size());
        return audioList.size(); }

    // Kiểm tra AudioAdapter.java — updateList phải như này:
    // Sửa lại updateList - dùng đúng tên field "audioList"
    public void updateList(List<String> newList) {
        this.audioList.clear();
        this.audioList.addAll(newList);
        notifyDataSetChanged();
    }

    public void stopPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        handler.removeCallbacks(progressRunnable);
    }

    private String formatTime(long millis) {
        return String.format(Locale.getDefault(), "%02d:%02d", (millis / 60000), ((millis / 1000) % 60));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtAudioName, txtDuration, txtTime;
        SeekBar seekBar;
        ImageButton btnPlay, btnDelete;

        public ViewHolder(View itemView) {
            super(itemView);
            txtAudioName = itemView.findViewById(R.id.txtAudioName);
            txtDuration = itemView.findViewById(R.id.txtDuration);
            txtTime = itemView.findViewById(R.id.txtTime);
            seekBar = itemView.findViewById(R.id.seekBar);
            btnPlay = itemView.findViewById(R.id.btnPlay);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}