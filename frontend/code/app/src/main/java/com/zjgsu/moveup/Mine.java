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

    // 🌟 新增这一行：设为 public static 方便测试代码动态修改
    public static String BASE_URL = "http://10.0.2.2:3000";

    private TextView tvUsernameValue;
    private TextView tvEmailValue;
    private TextView tvPhoneValue;
    private TextView tvPasswordValue;

    private Handler mainHandler;
    private String currentUserId;

    // 定义侧边栏容器
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

        // 初始化侧边栏容器
        drawerLayout = findViewById(R.id.drawerLayout);

        // 绑定 UI 元素
        tvUsernameValue = findViewById(R.id.tvUsernameValue);
        tvEmailValue = findViewById(R.id.tvEmailValue);
        tvPhoneValue = findViewById(R.id.tvPhoneValue);
        tvPasswordValue = findViewById(R.id.tvPasswordValue);

        findViewById(R.id.btnEditProfile).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 使用 Intent 从当前的 Mine 界面跳转到 mine_edit 界面
                Intent intent = new Intent(Mine.this, mine_edit.class);
                startActivity(intent);
            }
        });

        if (findViewById(R.id.btnBack) != null) {
            findViewById(R.id.btnBack).setOnClickListener(v -> drawerLayout.openDrawer(findViewById(R.id.drawerMenu)));
        }

        // 初始化侧边栏菜单跳转逻辑
        setupMenuClicks();

        SharedPreferences prefs = getSharedPreferences("moveup_auth", MODE_PRIVATE);
        currentUserId = prefs.getString("user_phone", "13800138000");

        fetchUserProfile();
    }

    /**
     * 与 Main 页面完全一致的侧边栏跳转逻辑
     */
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
                startActivity(new Intent(this, clubterm.class));
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

    private void fetchUserProfile() {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                // 🌟 修改点：使用 BASE_URL 拼接请求地址
                URL url = new URL(BASE_URL + "/v1/user/profile?user_id=" + currentUserId);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);

                if (connection.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();

                    JSONObject resp = new JSONObject(sb.toString());
                    if (resp.optInt("code") == 200) {
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
                mainHandler.post(() -> Toast.makeText(Mine.this, "无法拉取个人资料", Toast.LENGTH_SHORT).show());
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }
}