package com.example.doanmxh;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // ✅ Khởi tạo Firebase trước mọi thứ
        FirebaseApp.initializeApp(this);
        Log.d("MyApplication", "Firebase khởi tạo: " + FirebaseApp.getApps(this).size());
    }
}