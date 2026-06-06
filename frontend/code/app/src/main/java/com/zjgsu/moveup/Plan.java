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

    public static String BASE_URL = "https://moveup-v7mf.onrender.com";

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

        tv42 = findViewById(R.id.tv42);
        drawerLayout = findViewById(R.id.drawerLayout);

        SharedPreferences prefs = getSharedPreferences("moveup_auth", MODE_PRIVATE);
        currentUserId = prefs.getString("user_phone", "13800138000");

        findViewById(R.id.btnMenu).setOnClickListener(v -> {
            drawerLayout.openDrawer(findViewById(R.id.drawerMenu));
        });

        setupMenuClicks();

        setupDayCard(R.id.cardDay1, "MONDAY", R.drawable.day1);
        setupDayCard(R.id.cardDay2, "TUESDAY", R.drawable.day2);
        setupDayCard(R.id.cardDay3, "WEDNESDAY", R.drawable.day3);
        setupDayCard(R.id.cardDay4, "THURSDAY", R.drawable.day4);
        setupDayCard(R.id.cardDay5, "FRIDAY", R.drawable.day5);
        setupDayCard(R.id.cardDay6, "SATURDAY", R.drawable.day6);
        setupDayCard(R.id.cardDay7, "SUNDAY", R.drawable.day7);
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchTotalDistance();
    }

    private void setupMenuClicks() {
        TextView menuHome = findViewById(R.id.menu_home);
        TextView menuHistory = findViewById(R.id.menu_history);
        TextView menuPlan = findViewById(R.id.menu_plan);
        TextView menuClub = findViewById(R.id.menu_club);
        TextView menuProfile = findViewById(R.id.menu_profile);

        if (menuHome != null) menuHome.setOnClickListener(v -> {
            startActivity(new Intent(Plan.this, Main.class));
            finish();
        });
        if (menuHistory != null) menuHistory.setOnClickListener(v -> {
            startActivity(new Intent(Plan.this, History.class));
            finish();
        });
        if (menuPlan != null) menuPlan.setOnClickListener(v -> {
            drawerLayout.closeDrawers();
        });
        if (menuClub != null) menuClub.setOnClickListener(v -> {
            startActivity(new Intent(Plan.this, Find.class));
            finish();
        });
        if (menuProfile != null) menuProfile.setOnClickListener(v -> {
            startActivity(new Intent(Plan.this, Mine.class));
            finish();
        });
    }

    // ------------------- fetchTotalDistance -------------------
    private void fetchTotalDistance() {
        new Thread(() -> {
            HttpURLConnection connection = null;
            String urlStr = BASE_URL + "/v1/plan/total_distance";
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
                        String totalStr = data.optString("total_distance", "0");
                        mainHandler.post(() -> {
                            if (tv42 != null) tv42.setText(totalStr);
                        });
                    }
                }
            } catch (Exception e) {
                Log.e("API_TEST", "获取总距离失败", e);
                success = false;
            } finally {
                MetricsCollector.recordRequestEnd(urlStr, success);
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

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