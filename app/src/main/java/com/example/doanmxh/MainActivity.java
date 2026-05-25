package com.example.doanmxh;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.doanmxh.Log_Res.LoginActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Kiểm tra trạng thái đăng nhập trước khi hiển thị giao diện
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        // 2. Khởi tạo BottomNavigationView (Chỉ khai báo 1 lần duy nhất)
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        // 3. Tự động xử lý khoảng trống cho thanh điều hướng hệ thống (Tránh bị che khuất)
//        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (view, insets) -> {
//            Insets navigationBarsInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
//            view.setPadding(0, 0, 0, navigationBarsInsets.bottom);
//            return insets;
//        });

        // 4. Thiết lập Jetpack Navigation kết nối với FragmentContainerView
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(bottomNav, navController);

            // 5. Xử lý sự kiện click chuyển Tab kết hợp hiệu ứng Animation phóng to Icon cực mượt
            bottomNav.setOnItemSelectedListener(item -> {
                int selectedId = item.getItemId();

                for (int i = 0; i < bottomNav.getMenu().size(); i++) {
                    int id = bottomNav.getMenu().getItem(i).getItemId();
                    View itemView = bottomNav.findViewById(id);

                    if (itemView != null) {
                        if (id == selectedId) {
                            // Phóng to tab được chọn
                            itemView.animate()
                                    .scaleX(1.20f)
                                    .scaleY(1.20f)
                                    .setDuration(150)
                                    .start();
                        } else {
                            // Thu nhỏ các tab còn lại về kích thước mặc định
                            itemView.animate()
                                    .scaleX(1.0f)
                                    .scaleY(1.0f)
                                    .setDuration(150)
                                    .start();
                        }
                    }
                }

                // Chủ động kích hoạt chuyển màn hình Fragment theo ID của Menu
                navController.navigate(selectedId);
                return true;
            });
        }
    }
}