package com.example.doanmxh.CreatePage;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;

import java.util.Map;

public class ImageRepository {

    private static final String TAG = "CloudinaryUpload";
    private static final String UPLOAD_PRESET = "DoAnMXH";

    public interface UploadCallback {
        void onSuccess(String url);
        void onError();
    }

    // =========================
    // Upload Image
    // =========================

    public void uploadImage(
            Context context,
            Uri uri,
            UploadCallback callback
    ) {

        Log.d(TAG, "Upload image: " + uri);

        MediaManager.get()
                .upload(uri)
                .unsigned(UPLOAD_PRESET)
                .option("folder", "posts/images")
                .option("resource_type", "image")
                .callback(new com.cloudinary.android.callback.UploadCallback() {

                    @Override
                    public void onStart(String requestId) {
                        Log.d(TAG, "Image upload started");
                    }

                    @Override
                    public void onProgress(
                            String requestId,
                            long bytes,
                            long totalBytes
                    ) {

                        int percent =
                                totalBytes > 0
                                        ? (int) ((bytes * 100) / totalBytes)
                                        : 0;

                        Log.d(TAG,
                                "Image progress: " + percent + "%");
                    }

                    @Override
                    public void onSuccess(
                            String requestId,
                            Map resultData
                    ) {

                        String url =
                                (String) resultData.get("secure_url");

                        Log.d(TAG,
                                "Image uploaded OK: " + url);

                        if (url != null) {
                            callback.onSuccess(url);
                        } else {
                            callback.onError();
                        }
                    }

                    @Override
                    public void onError(
                            String requestId,
                            ErrorInfo error
                    ) {

                        Log.e(
                                TAG,
                                "Image upload error: "
                                        + error.getDescription()
                        );

                        callback.onError();
                    }

                    @Override
                    public void onReschedule(
                            String requestId,
                            ErrorInfo error
                    ) {

                        Log.e(
                                TAG,
                                "Image upload reschedule: "
                                        + error.getDescription()
                        );

                        callback.onError();
                    }
                })
                .dispatch(context);
    }

    // =========================
    // Upload Video
    // =========================

    public void uploadVideo(
            Context context,
            Uri uri,
            UploadCallback callback
    ) {

        Log.d(TAG, "Upload video: " + uri);

        MediaManager.get()
                .upload(uri)
                .unsigned(UPLOAD_PRESET)
                .option("folder", "posts/videos")
                .option("resource_type", "video")
                .callback(new com.cloudinary.android.callback.UploadCallback() {

                    @Override
                    public void onStart(String requestId) {
                        Log.d(TAG, "Video upload started");
                    }

                    @Override
                    public void onProgress(
                            String requestId,
                            long bytes,
                            long totalBytes
                    ) {

                        int percent =
                                totalBytes > 0
                                        ? (int) ((bytes * 100) / totalBytes)
                                        : 0;

                        Log.d(TAG,
                                "Video progress: " + percent + "%");
                    }

                    @Override
                    public void onSuccess(
                            String requestId,
                            Map resultData
                    ) {

                        String url =
                                (String) resultData.get("secure_url");

                        Log.d(TAG,
                                "Video uploaded OK: " + url);

                        if (url != null) {
                            callback.onSuccess(url);
                        } else {
                            callback.onError();
                        }
                    }

                    @Override
                    public void onError(
                            String requestId,
                            ErrorInfo error
                    ) {

                        Log.e(
                                TAG,
                                "Video upload error: "
                                        + error.getDescription()
                        );

                        callback.onError();
                    }

                    @Override
                    public void onReschedule(
                            String requestId,
                            ErrorInfo error
                    ) {

                        Log.e(
                                TAG,
                                "Video upload reschedule: "
                                        + error.getDescription()
                        );

                        callback.onError();
                    }
                })
                .dispatch(context);
    }
    public void uploadAudio(Context context, Uri uri, UploadCallback callback) {
        MediaManager.get().upload(uri)
                .unsigned("DoAnMXH")
                .option("folder", "posts/audio")
                .option("resource_type", "video")
                .callback(new com.cloudinary.android.callback.UploadCallback() {
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        callback.onSuccess((String) resultData.get("secure_url"));
                    }
                    @Override
                    public void onError(String requestId, com.cloudinary.android.callback.ErrorInfo error) {
                        callback.onError();
                    }
                    @Override public void onReschedule(String requestId, com.cloudinary.android.callback.ErrorInfo error) {}
                }).dispatch(context);
    }
}