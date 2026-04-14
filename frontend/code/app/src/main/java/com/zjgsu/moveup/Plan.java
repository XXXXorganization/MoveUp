package com.zjgsu.moveup;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

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

public class Plan extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private Handler mainHandler;
    private TextView tv42;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_plan);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mainHandler = new Handler(Looper.getMainLooper());

        // 绑定组件
        tv42 = findViewById(R.id.tv42);
        drawerLayout = findViewById(R.id.drawerLayout);

        // 获取当前登录用户
        SharedPreferences prefs = getSharedPreferences("moveup_auth", MODE_PRIVATE);
        currentUserId = prefs.getString("user_phone", "13800138000");

        // 🌟 侧边栏呼出逻辑
        findViewById(R.id.btnMenu).setOnClickListener(v -> {
            drawerLayout.openDrawer(findViewById(R.id.drawerMenu));
        });

        // 🌟 初始化侧边栏内每个按钮的跳转逻辑
        setupMenuClicks();

        // 绑定 7 天的卡片点击事件
        setupDayCard(R.id.cardDay1, "MONDAY", R.drawable.day1);
        setupDayCard(R.id.cardDay2, "TUESDAY", R.drawable.day2);
        setupDayCard(R.id.cardDay3, "WEDNESDAY", R.drawable.day3);
        setupDayCard(R.id.cardDay4, "THURSDAY", R.drawable.day4);
        setupDayCard(R.id.cardDay5, "FRIDAY", R.drawable.day5);
        setupDayCard(R.id.cardDay6, "SATURDAY", R.drawable.day6);
        setupDayCard(R.id.cardDay7, "SUNDAY", R.drawable.day7);
    }

    /**
     * 每次回到这个页面都会自动刷新每周计划总里程
     */
    @Override
    protected void onResume() {
        super.onResume();
        fetchTotalDistance();
    }

    /**
     * 🌟 初始化侧边栏菜单跳转，逻辑与 Main.java 完全一致
     */
    private void setupMenuClicks() {
        TextView menuHome = findViewById(R.id.menu_home);
        TextView menuHistory = findViewById(R.id.menu_history);
        TextView menuPlan = findViewById(R.id.menu_plan);
        TextView menuClub = findViewById(R.id.menu_club);
        TextView menuProfile = findViewById(R.id.menu_profile);

        // Home
        menuHome.setOnClickListener(v -> {
            startActivity(new Intent(Plan.this, Main.class));
            finish(); // 返回主页，关闭当前页
        });

        // History
        menuHistory.setOnClickListener(v -> {
            startActivity(new Intent(Plan.this, History.class));
            finish();
        });

        // Plan: 因为已经在这个页面了，直接关闭侧边栏即可
        menuPlan.setOnClickListener(v -> {
            drawerLayout.closeDrawers();
        });

        // Club
        menuClub.setOnClickListener(v -> {
            startActivity(new Intent(Plan.this, clubterm.class));
            finish();
        });

        // Profile → Mine
        menuProfile.setOnClickListener(v -> {
            startActivity(new Intent(Plan.this, Mine.class));
            finish();
        });
    }

    /**
     * 🌟 获取用户当周计划的总跑步距离
     */
    private void fetchTotalDistance() {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL("http://10.0.2.2:3000/v1/plan/total_distance?user_id=" + currentUserId);
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
                        String totalStr = data.optString("total_distance", "0");

                        mainHandler.post(() -> {
                            if (tv42 != null) {
                                tv42.setText(totalStr);
                            }
                        });
                    }
                }
            } catch (Exception e) {
                Log.e("API_TEST", "获取总距离失败", e);
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    /**
     * 封装点击跳转逻辑，携带星期名字和图片资源 ID
     */
    private void setupDayCard(int cardId, String dayName, int imageResId) {
        View card = findViewById(cardId);
        if (card != null) {
            card.setOnClickListener(v -> {
                Intent intent = new Intent(Plan.this, Plan_details.class);
                intent.putExtra("DAY_NAME", dayName);
                intent.putExtra("IMAGE_RES_ID", imageResId);
                startActivity(intent);
            });
        }
    }
}