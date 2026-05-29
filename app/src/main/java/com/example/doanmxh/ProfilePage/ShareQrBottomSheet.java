package com.example.doanmxh.ProfilePage;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.doanmxh.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.OutputStream;

public class ShareQrBottomSheet extends BottomSheetDialogFragment {
    private final String username;
    private Bitmap qrBitmap;

    // ── Interface để Activity xử lý kết quả scan ─────────────────────────
    public interface OnScanRequestListener {
        void onScanRequested();
    }

    private OnScanRequestListener scanListener;

    public void setScanRequestListener(OnScanRequestListener listener) {
        this.scanListener = listener;
    }

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
        View view = inflater.inflate(R.layout.bottom_sheet_share_qr, container, false);

        ImageView    ivQrCode    = view.findViewById(R.id.ivQrCode);
        TextView     tvUsername  = view.findViewById(R.id.tvUsername);
        LinearLayout btnCopyLink = view.findViewById(R.id.btnCopyLink);
        LinearLayout btnShare    = view.findViewById(R.id.btnShare);
        LinearLayout btnDownload = view.findViewById(R.id.btnDownload);
        LinearLayout btnScan     = view.findViewById(R.id.btnScan);

        tvUsername.setText(username);

        FirebaseFirestore.getInstance()
                .collection("nguoi_dung")
                .whereEqualTo("ten_dang_nhap", username)
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {
                    String profileLink;
                    if (!query.isEmpty()) {
                        String saved = query.getDocuments().get(0).getString("link_lien_ket");
                        profileLink = (saved != null && !saved.isEmpty())
                                ? saved
                                : "https://DoAnMXH.net/@" + username;
                    } else {
                        profileLink = "https://DoAnMXH.net/@" + username;
                    }

                    qrBitmap = generateQr(profileLink);
                    if (qrBitmap != null) ivQrCode.setImageBitmap(qrBitmap);

                    btnCopyLink.setOnClickListener(v -> {
                        ClipboardManager clipboard =
                                (ClipboardManager) requireContext()
                                        .getSystemService(Context.CLIPBOARD_SERVICE);
                        clipboard.setPrimaryClip(
                                ClipData.newPlainText("profile_link", profileLink));
                        Toast.makeText(requireContext(),
                                "Đã sao chép liên kết", Toast.LENGTH_SHORT).show();
                    });

                    btnShare.setOnClickListener(v -> {
                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.setType("text/plain");
                        intent.putExtra(Intent.EXTRA_TEXT, profileLink);
                        startActivity(Intent.createChooser(intent, "Chia sẻ"));
                    });

                    btnDownload.setOnClickListener(v -> {
                        if (qrBitmap == null) return;
                        saveQrToGallery(qrBitmap);
                    });
                })
                .addOnFailureListener(e -> {
                    String profileLink = "https://DoAnMXH.net/@" + username;
                    qrBitmap = generateQr(profileLink);
                    if (qrBitmap != null) ivQrCode.setImageBitmap(qrBitmap);
                });

        // ── Quét QR: uỷ qua Activity xử lý ──────────────────────────────
        btnScan.setOnClickListener(v -> {
            dismiss();
            if (scanListener != null) scanListener.onScanRequested();
        });

        return view;
    }

    // ── Tạo QR: foreground ĐEN, background TRONG SUỐT ────────────────────
    private Bitmap generateQr(String text) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 800, 800);

            int size = 800;
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);

            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    // true = module đen (foreground), false = trong suốt (background)
                    bitmap.setPixel(x, y, bitMatrix.get(x, y)
                            ? android.graphics.Color.BLACK
                            : android.graphics.Color.TRANSPARENT);
                }
            }
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ── Lưu QR vào thư viện ảnh ──────────────────────────────────────────
    private void saveQrToGallery(Bitmap bitmap) {
        try {
            // ── Tạo bitmap mới có nền TRẮNG để lưu ──────────────────────
            Bitmap exportBitmap = Bitmap.createBitmap(
                    bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(exportBitmap);
            canvas.drawColor(android.graphics.Color.WHITE);      // nền trắng
            canvas.drawBitmap(bitmap, 0, 0, null);               // QR đen lên trên

            String fileName = "QR_" + username + "_" + System.currentTimeMillis() + ".png";

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/QRCode");

                Uri uri = requireContext().getContentResolver()
                        .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                if (uri != null) {
                    try (OutputStream out =
                                 requireContext().getContentResolver().openOutputStream(uri)) {
                        exportBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                    }
                    Toast.makeText(requireContext(),
                            "Đã lưu ảnh vào thư viện", Toast.LENGTH_SHORT).show();
                }
            } else {
                String path = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES).toString();
                java.io.File file = new java.io.File(path, fileName);
                try (OutputStream out = new java.io.FileOutputStream(file)) {
                    exportBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                }
                Intent mediaScan = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaScan.setData(Uri.fromFile(file));
                requireContext().sendBroadcast(mediaScan);

                Toast.makeText(requireContext(),
                        "Đã lưu ảnh vào thư viện", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Lưu ảnh thất bại", Toast.LENGTH_SHORT).show();
        }
    }

    // ── Ẩn system UI ─────────────────────────────────────────────────────
    private void hideSystemUI() {
        if (getDialog() == null || getDialog().getWindow() == null) return;
        Window window = getDialog().getWindow();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars()
                        | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
        }
        hideSystemUI();
    }
}