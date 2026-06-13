package com.example.doanmxh;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.cloudinary.android.MediaManager;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
public class MyApplication extends Application {
    private static final String PREF_SETTINGS = "app_settings";
    private static final String KEY_DARK_MODE = "dark_mode";

    @Override
    public void onCreate() {
        super.onCreate();
        applySavedTheme();

        // ✅ Init Cloudinary với cloud_name
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dm7unzl4b"); // ← sửa chỗ này
        MediaManager.init(this, config);

        FirebaseApp.initializeApp(this);
        Log.d("MyApplication", "Firebase khởi tạo: " + FirebaseApp.getApps(this).size());

        // Bật offline persistence
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        // Theo dõi lifecycle app
        ProcessLifecycleOwner.get().getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onStart(LifecycleOwner owner) {
                setUserStatus("online");
            }

            @Override
            public void onStop(LifecycleOwner owner) {
                setUserStatus("offline");
            }
        });
    }

    // Gọi sau khi login thành công
    public static void setupPresence() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String userId = user.getUid();
        DatabaseReference connectedRef = FirebaseDatabase.getInstance()
                .getReference(".info/connected");
        DatabaseReference presenceRef = FirebaseDatabase.getInstance()
                .getReference("presence/" + userId);

        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Boolean connected = snapshot.getValue(Boolean.class);
                if (connected == null || !connected) return;

                // Đăng ký với Firebase SERVER
                // Dù app bị kill/crash, server vẫn tự chạy lệnh này
                Map<String, Object> offlineData = new HashMap<>();
                offlineData.put("status", "offline");
                offlineData.put("lastSeen", ServerValue.TIMESTAMP);

                presenceRef.onDisconnect().setValue(offlineData)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Map<String, Object> onlineData = new HashMap<>();
                                onlineData.put("status", "online");
                                onlineData.put("lastSeen", ServerValue.TIMESTAMP);
                                presenceRef.setValue(onlineData);
                            }
                        });
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("Presence", "Lỗi: " + error.getMessage());
            }
        });
    }

    private void setUserStatus(String status) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String userId = user.getUid();
        DatabaseReference presenceRef = FirebaseDatabase.getInstance()
                .getReference("presence/" + userId);

        Map<String, Object> data = new HashMap<>();
        data.put("status", status);
        data.put("lastSeen", ServerValue.TIMESTAMP);
        presenceRef.setValue(data);

        Log.d("Presence", "Set status: " + status + " cho user: " + userId);
    }

    private void applySavedTheme() {
        SharedPreferences prefs = getSharedPreferences(PREF_SETTINGS, Context.MODE_PRIVATE);
        boolean darkMode = prefs.getBoolean(KEY_DARK_MODE, false);
        AppCompatDelegate.setDefaultNightMode(
                darkMode
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO
        );
    }
}