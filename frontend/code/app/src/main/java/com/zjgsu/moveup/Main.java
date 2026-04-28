package com.zjgsu.moveup;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class Main extends AppCompatActivity {

    // 🌟 新增：暴露 BASE_URL 供测试动态拦截
    public static String BASE_URL = "http://10.0.2.2:3000";

    private DrawerLayout drawerLayout;
    private Handler mainHandler;

    // 绑定布局中的两个 activityCard
    private View activityCard1;
    private View activityCard2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main2);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mainHandler = new Handler(Looper.getMainLooper());

        // 抽屉菜单
        drawerLayout = findViewById(R.id.drawerLayout);
        findViewById(R.id.btnMenu).setOnClickListener(v -> {
            drawerLayout.openDrawer(findViewById(R.id.drawerMenu));
        });

        // 菜单点击
        setupMenuClicks();

        // 跑步跳转
        ImageView ivStart = findViewById(R.id.ivStart);
        ivStart.setOnClickListener(v -> {
            Intent intent = new Intent(Main.this, Runing.class);
            startActivity(intent);
        });

        // See All 跳转到 Find 界面
        TextView btnSeeAllClubs = findViewById(R.id.btnSeeAllClubs);
        btnSeeAllClubs.setOnClickListener(v -> {
            Intent intent = new Intent(Main.this, Find.class);
            startActivity(intent);
        });

        // 俱乐部列表
        RecyclerView clubList = findViewById(R.id.recyclerClubs);
        clubList.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        clubList.setAdapter(new ClubAdapter(buildSampleClubs()));

        // 动态获取最新的跑步历史数据
        activityCard1 = findViewById(R.id.activityCard1);
        activityCard2 = findViewById(R.id.activityCard2);

        // 从缓存中获取当前登录的用户ID（如果没有默认使用测试账号）
        SharedPreferences prefs = getSharedPreferences("moveup_auth", MODE_PRIVATE);
        String currentUserId = prefs.getString("user_phone", "13800138000");

        // 向后端拉取数据
        fetchLatestActivities(currentUserId);

        // 召唤 AI 悬浮球
        AIFloatManager.addFloat(this);
    }

    // 生命周期：当从其他页面（如完成跑步）返回时自动刷新主页数据
    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getSharedPreferences("moveup_auth", MODE_PRIVATE);
        String currentUserId = prefs.getString("user_phone", "13800138000");
        fetchLatestActivities(currentUserId);
    }

    /**
     * 发送 GET 请求拉取该用户的跑步记录
     */
    private void fetchLatestActivities(String userId) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                // 🌟 修改：使用动态拼接的 BASE_URL
                URL url = new URL(BASE_URL + "/v1/runs?user_id=" + userId);
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
                        JSONArray listArray = data.optJSONArray("list");

                        // 切回主线程更新 UI
                        mainHandler.post(() -> {
                            if (listArray != null && listArray.length() > 0) {
                                // 有数据：绑定最新的一条数据到卡片1
                                bindCardData(activityCard1, listArray.optJSONObject(0));
                                activityCard1.setVisibility(View.VISIBLE);

                                // 检查是否有第二条数据
                                if (listArray.length() > 1) {
                                    bindCardData(activityCard2, listArray.optJSONObject(1));
                                    activityCard2.setVisibility(View.VISIBLE);
                                } else {
                                    // 没有第二条记录，隐藏卡片2
                                    activityCard2.setVisibility(View.GONE);
                                }
                            } else {
                                // 该账号一条记录都没有，隐藏全部卡片
                                activityCard1.setVisibility(View.GONE);
                                activityCard2.setVisibility(View.GONE);
                            }
                        });
                    }
                }
            } catch (Exception e) {
                Log.e("API_TEST", "拉取主页动态数据异常", e);
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    /**
     * 将后端返回的 JSON 数据绑定到卡片的组件上
     */
    private void bindCardData(View cardView, JSONObject runData) {
        if (runData == null || cardView == null) return;

        // 在被 <include> 进来的卡片布局中查找对应 TextView
        TextView tvTitle = cardView.findViewById(R.id.activityTitle);
        TextView tvDate = cardView.findViewById(R.id.activityDate);
        TextView tvDistance = cardView.findViewById(R.id.distanceValue);
        TextView tvTime = cardView.findViewById(R.id.timeValue);
        TextView tvPace = cardView.findViewById(R.id.paceValue);

        // 绑定 Mock 后端返回的对应字段数据
        if (tvTitle != null) tvTitle.setText(runData.optString("title", "Running"));
        if (tvDate != null) tvDate.setText(runData.optString("date", "未知时间"));
        if (tvDistance != null) tvDistance.setText(runData.optString("distance", "0.00 Km"));
        if (tvTime != null) tvTime.setText(runData.optString("duration_str", "0.00"));
        if (tvPace != null) tvPace.setText(runData.optString("pace", "0'00\""));
    }

    private void setupMenuClicks() {
        TextView menuHome = findViewById(R.id.menu_home);
        TextView menuHistory = findViewById(R.id.menu_history);
        TextView menuPlan = findViewById(R.id.menu_plan);
        TextView menuClub = findViewById(R.id.menu_club);
        TextView menuProfile = findViewById(R.id.menu_profile);

        // Home → 主页（关闭菜单即可）
        if (menuHome != null) menuHome.setOnClickListener(v -> {
            drawerLayout.closeDrawers();
        });

        // History
        if (menuHistory != null) menuHistory.setOnClickListener(v -> {
            startActivity(new Intent(Main.this, History.class));
            finish();
        });

        // Plan
        if (menuPlan != null) menuPlan.setOnClickListener(v -> {
            startActivity(new Intent(Main.this, Plan.class));
            finish();
        });

        // Club
        if (menuClub != null) menuClub.setOnClickListener(v -> {
            startActivity(new Intent(Main.this, clubterm.class));
            finish();
        });

        // Profile → Mine
        if (menuProfile != null) menuProfile.setOnClickListener(v -> {
            startActivity(new Intent(Main.this, Mine.class));
            finish();
        });
    }

    @NonNull
    private List<Club> buildSampleClubs() {
        return Arrays.asList(
                new Club(
                        getString(R.string.club_tangerang),
                        getString(R.string.club_tangerang_loc),
                        R.drawable.moveup),
                new Club(
                        getString(R.string.club_jakarta),
                        getString(R.string.club_jakarta_loc),
                        R.drawable.moveup),
                new Club(
                        getString(R.string.club_bandung),
                        getString(R.string.club_bandung_loc),
                        R.drawable.moveup));
    }
}