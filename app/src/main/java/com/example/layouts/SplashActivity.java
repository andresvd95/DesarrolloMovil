package com.example.layouts;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.layouts.auth.SessionManager;
import com.example.layouts.ui.ThemeController;

public class SplashActivity extends AppCompatActivity {

    private final Handler navigationHandler = new Handler(Looper.getMainLooper());
    private Runnable navigationRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeController.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        animateSplashContent(findViewById(R.id.splashContent));

        navigationRunnable = () -> {
            Class<?> destination = new SessionManager(this).isLoggedIn()
                    ? HomeActivity.class
                    : LoginActivity.class;
            startActivity(new Intent(SplashActivity.this, destination));
            finish();
        };
        navigationHandler.postDelayed(navigationRunnable, 1800L);
    }

    @Override
    protected void onDestroy() {
        if (navigationRunnable != null) {
            navigationHandler.removeCallbacks(navigationRunnable);
        }
        super.onDestroy();
    }

    private void animateSplashContent(View content) {
        content.setAlpha(0f);
        content.setScaleX(0.94f);
        content.setScaleY(0.94f);
        content.setTranslationY(18f);
        content.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(700L)
                .start();
    }
}
