package com.example.doanmxh.CreatePage;

import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.doanmxh.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.File;

public class AudioRecordBottomSheet extends BottomSheetDialogFragment {

    private MediaRecorder mediaRecorder;
    private String audioFilePath;
    private boolean isRecording = false;

    private TextView txtRecordStatus;
    private ImageButton btnRecord;

    private final OnRecordCompleteListener listener;

    // Interface để truyền Uri về cho CreatePostFragment
    public interface OnRecordCompleteListener {
        void onRecordComplete(Uri audioUri);
    }

    public AudioRecordBottomSheet(OnRecordCompleteListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_bottom_sheet_record, container, false);

        txtRecordStatus = view.findViewById(R.id.txtRecordStatus);
        btnRecord = view.findViewById(R.id.btnRecord);

        btnRecord.setOnClickListener(v -> {
            if (!isRecording) {
                startRecording();
            } else {
                stopRecording();
            }
        });

        return view;
    }

    private void startRecording() {
        try {
            File audioFile = new File(requireContext().getCacheDir(), "audio_" + System.currentTimeMillis() + ".m4a");
            audioFilePath = audioFile.getAbsolutePath();

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setOutputFile(audioFilePath);

            mediaRecorder.prepare();
            mediaRecorder.start();

            isRecording = true;
            txtRecordStatus.setText("Đang ghi âm...");
            // Đổi icon sang nút Stop
            btnRecord.setImageResource(R.drawable.ic_pause);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Lỗi khi khởi tạo Micro", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecording() {
        try {
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
            }

            isRecording = false;
            Uri uri = Uri.fromFile(new File(audioFilePath));

            // Trả kết quả về cho Fragment
            if (listener != null) {
                listener.onRecordComplete(uri);
            }

            Toast.makeText(requireContext(), "Đã lưu ghi âm", Toast.LENGTH_SHORT).show();

            // Đóng Bottom Sheet sau khi ghi âm xong
            dismiss();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Đảm bảo giải phóng bộ nhớ nếu người dùng vuốt đóng bảng khi đang ghi âm dở
        if (isRecording && mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }
}