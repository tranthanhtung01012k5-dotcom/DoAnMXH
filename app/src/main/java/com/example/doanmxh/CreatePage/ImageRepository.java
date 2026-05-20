package com.example.doanmxh.CreatePage;

import android.content.Context;
import android.net.Uri;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.Callback;
import retrofit2.Response;

public class ImageRepository {

    private final ImgbbApi api;
    private final String API_KEY = "8d037487773ceb2ed4bedf790eea8234";

    public ImageRepository() {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.imgbb.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        api = retrofit.create(ImgbbApi.class);
    }

    // CUSTOM CALLBACK
    public interface UploadCallback {
        void onSuccess(String url);
        void onError();
    }

    public void uploadImage(Context context, Uri uri, UploadCallback callback) {

        try {
            File file = ImageUtils.uriToFile(context, uri);

            RequestBody requestFile =
                    RequestBody.create(file, MediaType.parse("image/*"));

            MultipartBody.Part body =
                    MultipartBody.Part.createFormData(
                            "image",
                            file.getName(),
                            requestFile
                    );

            api.uploadImage(API_KEY, body)
                    .enqueue(new Callback<ImgbbResponse>() {

                        @Override
                        public void onResponse(Call<ImgbbResponse> call,
                                               Response<ImgbbResponse> response) {

                            if (response.body() != null
                                    && response.body().data != null) {

                                callback.onSuccess(
                                        response.body().data.display_url
                                );

                            } else {
                                callback.onError();
                            }
                        }

                        @Override
                        public void onFailure(Call<ImgbbResponse> call, Throwable t) {
                            callback.onError();
                        }
                    });

        } catch (Exception e) {
            e.printStackTrace();
            callback.onError();
        }
    }
}