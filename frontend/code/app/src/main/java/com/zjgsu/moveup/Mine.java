package com.zjgsu.moveup;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class Mine extends AppCompatActivity {

    public static String BASE_URL = "https://moveup-v7mf.onrender.com";

    private TextView tvUsernameValue;
    private TextView tvEmailValue;
    private TextView tvPhoneValue;
    private TextView tvPasswordValue;

    private Handler mainHandler;
    private String currentUserId;
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mine);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mainHandler = new Handler(Looper.getMainLooper());
        drawerLayout = findViewById(R.id.drawerLayout);

        tvUsernameValue = findViewById(R.id.tvUsernameValue);
        tvEmailValue = findViewById(R.id.tvEmailValue);
        tvPhoneValue = findViewById(R.id.tvPhoneValue);
        tvPasswordValue = findViewById(R.id.tvPasswordValue);

        findViewById(R.id.btnEditProfile).setOnClickListener(v -> {
            Intent intent = new Intent(Mine.this, mine_edit.class);
            startActivity(intent);
        });

        if (findViewById(R.id.btnBack) != null) {
            findViewById(R.id.btnBack).setOnClickListener(v -> drawerLayout.openDrawer(findViewById(R.id.drawerMenu)));
        }

        setupMenuClicks();

        SharedPreferences prefs = getSharedPreferences("moveup_auth", MODE_PRIVATE);
        currentUserId = prefs.getString("user_phone", "13800138000");

        fetchUserProfile();
    }

    private void setupMenuClicks() {
        if (findViewById(R.id.menu_home) != null) {
            findViewById(R.id.menu_home).setOnClickListener(v -> {
                startActivity(new Intent(this, Main.class));
                finish();
            });
        }
        if (findViewById(R.id.menu_history) != null) {
            findViewById(R.id.menu_history).setOnClickListener(v -> {
                startActivity(new Intent(this, History.class));
                finish();
            });
        }
        if (findViewById(R.id.menu_plan) != null) {
            findViewById(R.id.menu_plan).setOnClickListener(v -> {
                startActivity(new Intent(this, Plan.class));
                finish();
            });
        }
        if (findViewById(R.id.menu_club) != null) {
            findViewById(R.id.menu_club).setOnClickListener(v -> {
                startActivity(new Intent(this, Find.class));
                finish();
            });
        }
        if (findViewById(R.id.menu_profile) != null) {
            findViewById(R.id.menu_profile).setOnClickListener(v -> {
                drawerLayout.closeDrawers();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchUserProfile();
    }

    // ------------------- fetchUserProfile -------------------
    private void fetchUserProfile() {
        new Thread(() -> {
            HttpURLConnection connection = null;
            String urlStr = BASE_URL + "/v1/user/profile";
            boolean success = false;

            MetricsCollector.recordRequestStart(urlStr);

            try {
                URL url = new URL(urlStr);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                SharedPreferences prefs = getSharedPreferences("moveup_auth", MODE_PRIVATE);
                String token = prefs.getString("jwt", "");
                if (!token.isEmpty()) {
                    connection.setRequestProperty("Authorization", "Bearer " + token);
                }

                int httpCode = connection.getResponseCode();
                if (httpCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();

                    JSONObject resp = new JSONObject(sb.toString());
                    if (resp.optInt("code") == 200) {
                        success = true;
                        JSONObject data = resp.getJSONObject("data");
                        final String username = data.optString("username", "未设置");
                        final String email = data.optString("email", "未设置");
                        final String phone = data.optString("phone", currentUserId);
                        final String password = data.optString("password", "********");

                        mainHandler.post(() -> {
                            if (tvUsernameValue != null) tvUsernameValue.setText(username);
                            if (tvEmailValue != null) tvEmailValue.setText(email);
                            if (tvPhoneValue != null) tvPhoneValue.setText(phone);
                            if (tvPasswordValue != null) tvPasswordValue.setText(password);
                        });
                    }
                }
            } catch (Exception e) {
                Log.e("API_TEST", "获取个人资料异常", e);
                success = false;
                mainHandler.post(() -> Toast.makeText(Mine.this, "无法拉取个人资料", Toast.LENGTH_SHORT).show());
            } finally {
                MetricsCollector.recordRequestEnd(urlStr, success);
                if (connection != null) connection.disconnect();
            }
        }).start();
    }
}