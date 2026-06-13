package com.example.doanmxh.HomePage;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.doanmxh.R;

public class FullScreenVideoActivity extends AppCompatActivity {

    private static final String EXTRA_VIDEO_URL = "video_url";

    public static void open(Context context, String videoUrl) {
        Intent intent = new Intent(context, FullScreenVideoActivity.class);
        intent.putExtra(EXTRA_VIDEO_URL, videoUrl);
        context.startActivity(intent);
    }

    private VideoView videoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        setContentView(R.layout.activity_full_screen_video);

        videoView = findViewById(R.id.videoView);
        ImageButton btnClose = findViewById(R.id.btnClose);

        String videoUrl = getIntent().getStringExtra(EXTRA_VIDEO_URL);

        if (videoUrl != null) {
            videoView.setVideoURI(Uri.parse(videoUrl));

            videoView.setOnPreparedListener(mp -> {
                mp.setLooping(true);
                mp.start();
            });
        }

        btnClose.setOnClickListener(v -> finish());
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoView != null) {
            videoView.pause();
        }
    }
}