package com.example.doanmxh;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
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

        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {

            startActivity(
                    new Intent(this, LoginActivity.class)
            );

            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav =
                findViewById(R.id.bottomNav);

        NavHostFragment navHostFragment =
                (NavHostFragment)
                        getSupportFragmentManager()
                                .findFragmentById(
                                        R.id.nav_host_fragment
                                );

        NavController navController =
                navHostFragment.getNavController();

        NavigationUI.setupWithNavController(
                bottomNav,
                navController
        );
    }
}