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
import java.util.ArrayList;
import java.util.List;

public class Main extends AppCompatActivity {

    // 🌟 暴露 BASE_URL 供测试动态拦截
    public static String BASE_URL = "http://10.234.4.72:3500";

    private DrawerLayout drawerLayout;
    private Handler mainHandler;

    // 绑定布局中的两个 activityCard
    private View activityCard1;
    private View activityCard2;

    // 动态社团列表
    private List<Club> dynamicClubList;
    private ClubAdapter clubAdapter;

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

        // 🌟 核心修改：初始化适配器并从后端动态加载列表
        RecyclerView recyclerClubs = findViewById(R.id.recyclerClubs);
        recyclerClubs.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        dynamicClubList = new ArrayList<>();
        clubAdapter = new ClubAdapter(dynamicClubList);
        recyclerClubs.setAdapter(clubAdapter);

        // 🚀 核心逻辑：从后端拉取社团数据，彻底告别写死的假数据
        fetchClubs();

        // 动态获取最新的跑步历史数据
        activityCard1 = findViewById(R.id.activityCard1);
        activityCard2 = findViewById(R.id.activityCard2);

        // 从缓存中获取当前登录的用户ID
        SharedPreferences prefs = getSharedPreferences("moveup_auth", MODE_PRIVATE);
        String currentUserId = prefs.getString("user_phone", "13800138000");

        // 向后端拉取数据
        fetchLatestActivities(currentUserId);

         //召唤 AI 悬浮球 (如果你的项目中包含此管理器)
         AIFloatManager.addFloat(this);
    }

    // 生命周期：当完成跑步等操作返回主页时刷新数据
    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getSharedPreferences("moveup_auth", MODE_PRIVATE);
        String currentUserId = prefs.getString("user_phone", "13800138000");
        fetchLatestActivities(currentUserId);
    }

    /**
     * 🌟 新增：发送 GET 请求拉取线上的社团列表
     */
    private void fetchClubs() {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(BASE_URL + "/v1/clubs");
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

                        List<Club> tempNewList = new ArrayList<>();
                        if (listArray != null) {
                            for (int i = 0; i < listArray.length(); i++) {
                                JSONObject obj = listArray.getJSONObject(i);

                                String id = obj.optString("id", "");
                                String name = obj.optString("name");
                                String location = obj.optString("location");
                                String flag = obj.optString("flag", "🇨🇳");
                                // 🌟 关键：读取后端新提供的图片链接
                                String imageUrl = obj.optString("image_url", "");

                                // 使用 5 个参数的新构造函数
                                tempNewList.add(new Club(id, name, location, imageUrl, flag));
                            }
                        }

                        // 切回主线程刷新界面
                        mainHandler.post(() -> {
                            dynamicClubList.clear();
                            dynamicClubList.addAll(tempNewList);
                            clubAdapter.notifyDataSetChanged();
                        });
                    }
                }
            } catch (Exception e) {
                Log.e("API_TEST", "获取主页社团列表异常", e);
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    /**
     * 发送 GET 请求拉取该用户的跑步记录
     */
    private void fetchLatestActivities(String userId) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
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

                        mainHandler.post(() -> {
                            if (listArray != null && listArray.length() > 0) {
                                bindCardData(activityCard1, listArray.optJSONObject(0));
                                activityCard1.setVisibility(View.VISIBLE);

                                if (listArray.length() > 1) {
                                    bindCardData(activityCard2, listArray.optJSONObject(1));
                                    activityCard2.setVisibility(View.VISIBLE);
                                } else {
                                    activityCard2.setVisibility(View.GONE);
                                }
                            } else {
                                activityCard1.setVisibility(View.GONE);
                                activityCard2.setVisibility(View.GONE);
                            }
                        });
                    }
                }
            } catch (Exception e) {
                Log.e("API_TEST", "获取最新活动异常", e);
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    private void bindCardData(View cardView, JSONObject runData) {
        if (runData == null || cardView == null) return;

        TextView tvTitle = cardView.findViewById(R.id.activityTitle);
        TextView tvDate = cardView.findViewById(R.id.activityDate);
        TextView tvDistance = cardView.findViewById(R.id.distanceValue);
        TextView tvTime = cardView.findViewById(R.id.timeValue);
        TextView tvPace = cardView.findViewById(R.id.paceValue);

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

        if (menuHome != null) menuHome.setOnClickListener(v -> drawerLayout.closeDrawers());

        if (menuHistory != null) menuHistory.setOnClickListener(v -> {
            startActivity(new Intent(Main.this, History.class));
            finish();
        });

        if (menuPlan != null) menuPlan.setOnClickListener(v -> {
            startActivity(new Intent(Main.this, Plan.class));
            finish();
        });

        if (menuClub != null) menuClub.setOnClickListener(v -> {
            startActivity(new Intent(Main.this, clubterm.class));
            finish();
        });

        if (menuProfile != null) menuProfile.setOnClickListener(v -> {
            startActivity(new Intent(Main.this, Mine.class));
            finish();
        });
    }
}