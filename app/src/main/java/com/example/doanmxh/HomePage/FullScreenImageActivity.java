package com.example.doanmxh.HomePage;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.doanmxh.BaseActivity;
import com.example.doanmxh.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity xem ảnh full screen với swipe và zoom.
 *
 * Mở bằng:
 *   FullScreenImageActivity.open(context, imageUrls, startIndex);
 *
 * Layout cần: activity_fullscreen_image.xml
 */
public class FullScreenImageActivity extends BaseActivity {

    private static final String EXTRA_IMAGES = "extra_images";
    private static final String EXTRA_INDEX  = "extra_index";

    // ── Static helper ──
    public static void open(Context ctx, List<String> images, int startIndex) {
        Intent intent = new Intent(ctx, FullScreenImageActivity.class);
        intent.putStringArrayListExtra(EXTRA_IMAGES, new ArrayList<>(images));
        intent.putExtra(EXTRA_INDEX, startIndex);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Full screen, đen hoàn toàn
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        setContentView(R.layout.activity_fullscreen_image);

        List<String> images = getIntent().getStringArrayListExtra(EXTRA_IMAGES);
        int startIndex = getIntent().getIntExtra(EXTRA_INDEX, 0);

        if (images == null || images.isEmpty()) {
            finish();
            return;
        }

        ViewPager2 viewPager  = findViewById(R.id.viewPagerImages);
        ImageButton btnClose  = findViewById(R.id.btnClose);
        TextView tvCounter    = findViewById(R.id.tvCounter);

        // Counter "1 / 3"
        tvCounter.setText((startIndex + 1) + " / " + images.size());

        // Adapter swipe ảnh (mỗi trang là 1 ảnh zoom-able)
        FullScreenImagePagerAdapter adapter =
                new FullScreenImagePagerAdapter(this, images);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(startIndex, false);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                tvCounter.setText((position + 1) + " / " + images.size());
            }
        });

        btnClose.setOnClickListener(v -> finish());
    }
}