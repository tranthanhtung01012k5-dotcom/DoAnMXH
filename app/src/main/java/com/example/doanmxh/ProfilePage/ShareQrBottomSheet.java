package com.example.doanmxh.ProfilePage;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Build;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.doanmxh.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.qrcode.QRCodeWriter;

public class ShareQrBottomSheet
        extends BottomSheetDialogFragment {

    private final String username;

    public ShareQrBottomSheet(String username) {
        this.username = username;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {

        View view = inflater.inflate(
                R.layout.bottom_sheet_share_qr,
                container,
                false
        );

        ImageView ivQrCode =
                view.findViewById(R.id.ivQrCode);

        TextView tvUsername =
                view.findViewById(R.id.tvUsername);

        LinearLayout btnCopyLink =
                view.findViewById(R.id.btnCopyLink);

        LinearLayout btnShare =
                view.findViewById(R.id.btnShare);

        LinearLayout btnDownload =
                view.findViewById(R.id.btnDownload);

        LinearLayout btnScan =
                view.findViewById(R.id.btnScan);

        tvUsername.setText(username);

        String profileLink =
                "https://threads.net/@"
                        + username;

        generateQr(ivQrCode, profileLink);

        // copy link
        btnCopyLink.setOnClickListener(v -> {

            ClipboardManager clipboard =
                    (ClipboardManager)
                            requireContext().getSystemService(
                                    Context.CLIPBOARD_SERVICE);

            ClipData clip =
                    ClipData.newPlainText(
                            "profile_link",
                            profileLink
                    );

            clipboard.setPrimaryClip(clip);

            Toast.makeText(
                    requireContext(),
                    "Đã sao chép liên kết",
                    Toast.LENGTH_SHORT
            ).show();
        });

        // share
        btnShare.setOnClickListener(v -> {

            Intent intent = new Intent(Intent.ACTION_SEND);

            intent.setType("text/plain");

            intent.putExtra(
                    Intent.EXTRA_TEXT,
                    profileLink
            );

            startActivity(
                    Intent.createChooser(
                            intent,
                            "Chia sẻ"
                    )
            );
        });

        // download
        btnDownload.setOnClickListener(v -> {

            Toast.makeText(
                    requireContext(),
                    "Chức năng tải xuống",
                    Toast.LENGTH_SHORT
            ).show();
        });

        // scan
        btnScan.setOnClickListener(v -> {

            Toast.makeText(
                    requireContext(),
                    "Chức năng quét QR",
                    Toast.LENGTH_SHORT
            ).show();
        });

        return view;
    }
    private void hideSystemUI() {

        if (getDialog() == null ||
                getDialog().getWindow() == null) return;

        Window window = getDialog().getWindow();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            window.setDecorFitsSystemWindows(false);

            WindowInsetsController controller =
                    window.getInsetsController();

            if (controller != null) {

                controller.hide(
                        WindowInsets.Type.statusBars()
                                | WindowInsets.Type.navigationBars()
                );

                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }

        } else {

            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
            );
        }
    }
    @Override
    public void onStart() {
        super.onStart();

        if (getDialog() != null &&
                getDialog().getWindow() != null) {

            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
        }

        hideSystemUI();
    }
    private void generateQr(
            ImageView imageView,
            String text
    ) {

        try {

            QRCodeWriter writer =
                    new QRCodeWriter();

            var bitMatrix =
                    writer.encode(
                            text,
                            BarcodeFormat.QR_CODE,
                            800,
                            800
                    );

            Bitmap bitmap =
                    Bitmap.createBitmap(
                            800,
                            800,
                            Bitmap.Config.RGB_565
                    );

            for (int x = 0; x < 800; x++) {

                for (int y = 0; y < 800; y++) {

                    bitmap.setPixel(
                            x,
                            y,
                            bitMatrix.get(x, y)
                                    ? android.graphics.Color.WHITE
                                    : android.graphics.Color.BLACK
                    );
                }
            }

            imageView.setImageBitmap(bitmap);

        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}