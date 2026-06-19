package com.example.doanmxh.Message;

import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import com.example.doanmxh.R;

/**
 * Dialog hiển thị toàn bộ nội dung "note" của 1 người bạn khi bấm vào flNoteBubble.
 * Dùng newInstance(...) để tạo, không tạo trực tiếp bằng constructor.
 */
public class StoryViewDialog extends DialogFragment {

    private static final String ARG_NAME = "arg_name";
    private static final String ARG_AVATAR_RES = "arg_avatar_res";
    private static final String ARG_NOTE_CONTENT = "arg_note_content";
    private static final String ARG_TIME_TEXT = "arg_time_text";

    public static StoryViewDialog newInstance(String name, String avatarRes,
                                              String noteContent, String timeText) {
        StoryViewDialog dialog = new StoryViewDialog();
        Bundle args = new Bundle();
        args.putString(ARG_NAME, name);
        args.putString(ARG_AVATAR_RES, avatarRes);
        args.putString(ARG_NOTE_CONTENT, noteContent);
        args.putString(ARG_TIME_TEXT, timeText);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            Window window = getDialog().getWindow();
            window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            window.setLayout((int) (screenWidth * 0.85), ViewGroup.LayoutParams.WRAP_CONTENT);

            // --- Làm tối (dim) nền phía sau dialog ---
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.65f); // 0f (không tối) -> 1f (đen hoàn toàn), tùy chỉnh theo ý bạn

            // --- Làm mờ thật (blur) nền phía sau, chỉ chạy từ Android 12 (API 31) ---
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
                window.setBackgroundBlurRadius(40); // độ mờ, càng cao càng mờ
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_story_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView imgAvatar = view.findViewById(R.id.imgAvatar);
        ImageView btnClose = view.findViewById(R.id.btnClose);
        TextView tvName = view.findViewById(R.id.tvName);
        TextView tvTime = view.findViewById(R.id.tvTime);
        TextView tvNoteContent = view.findViewById(R.id.tvNoteContent);

        Bundle args = getArguments();
        if (args != null) {
            String name = args.getString(ARG_NAME, "");
            String avatarRes = args.getString(ARG_AVATAR_RES, "");
            String noteContent = args.getString(ARG_NOTE_CONTENT, "");
            String timeText = args.getString(ARG_TIME_TEXT, "");

            tvName.setText(name);
            tvTime.setText(timeText);
            tvNoteContent.setText(noteContent);

            Glide.with(this)
                    .load(avatarRes)
                    .placeholder(R.drawable.ic_person_outline_24)
                    .circleCrop()
                    .into(imgAvatar);
        }

        btnClose.setOnClickListener(v -> dismiss());
    }
}