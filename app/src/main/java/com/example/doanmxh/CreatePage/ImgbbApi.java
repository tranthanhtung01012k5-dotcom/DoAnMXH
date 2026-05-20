package com.example.doanmxh.CreatePage;

import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

import okhttp3.MultipartBody;

public interface ImgbbApi {

    @Multipart
    @POST("1/upload")
    Call<ImgbbResponse> uploadImage(
            @Query("key") String apiKey,
            @Part MultipartBody.Part image
    );
}