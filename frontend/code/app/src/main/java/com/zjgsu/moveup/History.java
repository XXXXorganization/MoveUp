package com.zjgsu.moveup;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class History extends AppCompatActivity {

    private RecyclerView list;
    private HistoryAdapter adapter;
    private List<HistoryRun> runList = new ArrayList<>();
    private Handler mainHandler;

    private String currentUserId; // 🌟 存储当前用户ID

    // 绑定顶部的统计视图
    private TextView totalKmValue;
    private TextView summaryRunValue;
    private TextView summaryPaceValue;
    private TextView summaryTimeValue;

    // 增加 DrawerLayout 引用
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_history);

        // 初始化 DrawerLayout
        drawerLayout = findViewById(R.id.drawerLayout);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mainHandler = new Handler(Looper.getMainLooper());

        // 获取并存储用户 ID
        SharedPreferences prefs = getSharedPreferences("moveup_auth", MODE_PRIVATE);
        currentUserId = prefs.getString("user_phone", "13800138000");

        // 初始化视图
        totalKmValue = findViewById(R.id.totalKmValue);
        summaryRunValue = findViewById(R.id.summaryRunValue);
        summaryPaceValue = findViewById(R.id.summaryPaceValue);
        summaryTimeValue = findViewById(R.id.summaryTimeValue);

        // 设置 RecyclerView 与空的 Adapter
        list = findViewById(R.id.recyclerHistory);
        list.setLayoutManager(new LinearLayoutManager(this));

        // 🌟 点击分享按钮后，启动分享流程
        adapter = new HistoryAdapter(runList, (run, position) -> initiateShareSequence(run));
        list.setAdapter(adapter);

        // 修改此处逻辑：点击菜单按钮展开侧滑菜单，而不是返回
        findViewById(R.id.btnMenu).setOnClickListener(v -> {
            if (drawerLayout != null) {
                // 使用 GravityCompat.START 从屏幕左侧划出菜单
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        // ================= 新增：绑定侧滑菜单的点击跳转逻辑 =================
        setupMenuClicks();
        // =================================================================

        // 开始向后端拉取当前账号的历史数据
        fetchHistoryData();
    }

    // 新增的方法：与 Main 保持一致的菜单跳转逻辑
    private void setupMenuClicks() {
        TextView menuHome = findViewById(R.id.menu_home);
        TextView menuHistory = findViewById(R.id.menu_history);
        TextView menuPlan = findViewById(R.id.menu_plan);
        TextView menuClub = findViewById(R.id.menu_club);
        TextView menuProfile = findViewById(R.id.menu_profile);

        // Home → 跳转回主页
        menuHome.setOnClickListener(v -> {
            startActivity(new Intent(History.this, Main.class));
            finish();
        });

        // History → 当前已在 History 页面，直接关闭侧滑菜单即可
        menuHistory.setOnClickListener(v -> {
            if (drawerLayout != null) {
                drawerLayout.closeDrawers();
            }
        });

        // Plan
        menuPlan.setOnClickListener(v -> {
            startActivity(new Intent(History.this, Plan.class));
            finish();
        });

        // Club
        menuClub.setOnClickListener(v -> {
            startActivity(new Intent(History.this, clubterm.class));
            finish();
        });

        // Profile → Mine
        menuProfile.setOnClickListener(v -> {
            startActivity(new Intent(History.this, Mine.class));
            finish();
        });
    }

    private void fetchHistoryData() {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL("http://10.0.2.2:3000/v1/runs?user_id=" + currentUserId);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int httpCode = connection.getResponseCode();
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        httpCode >= 200 && httpCode < 300 ? connection.getInputStream() : connection.getErrorStream()
                ));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                if (httpCode == 200) {
                    JSONObject resp = new JSONObject(sb.toString());
                    if (resp.optInt("code") == 200) {
                        JSONObject data = resp.getJSONObject("data");

                        JSONObject stats = data.optJSONObject("stats");
                        String totalDistance = stats != null ? stats.optString("total_distance", "0") : "0";
                        String totalRuns = stats != null ? stats.optString("total_runs", "0") : "0";
                        String avgPace = stats != null ? stats.optString("avg_pace", "0'00\"") : "0'00\"";
                        String totalDurationStr = stats != null ? stats.optString("total_duration_str", "0h") : "0h";

                        JSONArray listArray = data.optJSONArray("list");
                        runList.clear();
                        if (listArray != null) {
                            for (int i = 0; i < listArray.length(); i++) {
                                JSONObject obj = listArray.getJSONObject(i);
                                runList.add(new HistoryRun(
                                        obj.optString("id", "run_" + i), // 🌟 获取记录ID
                                        obj.optString("date", "未知时间"),
                                        obj.optString("title", "跑步记录"),
                                        obj.optString("duration_str", "0.00"),
                                        obj.optString("pace", "0'00\""),
                                        obj.optString("distance", "0.00 Km")
                                ));
                            }
                        }

                        mainHandler.post(() -> {
                            totalKmValue.setText(totalDistance);
                            summaryRunValue.setText(totalRuns);
                            summaryPaceValue.setText(avgPace);
                            summaryTimeValue.setText(totalDurationStr);
                            adapter.notifyDataSetChanged();
                        });
                    }
                }
            } catch (Exception e) {
                Log.e("API_TEST", "拉取历史数据异常", e);
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    // ===================== 🌟 新增的分享系列弹窗流程 =====================

    /** 1. 确认分享弹窗 */
    private void initiateShareSequence(@NonNull HistoryRun run) {
        new AlertDialog.Builder(this)
                .setTitle("Share Record")
                .setMessage("Do you want to share this record to your club?")
                .setPositiveButton("Yes", (dialog, which) -> fetchMyClubsAndShowDialog(run))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** 2. 拉取用户已加入的社团，并显示选择弹窗 */
    private void fetchMyClubsAndShowDialog(HistoryRun run) {
        new Thread(() -> {
            try {
                URL url = new URL("http://10.0.2.2:3000/v1/user/clubs?user_id=" + currentUserId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                if (conn.getResponseCode() == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    JSONObject res = new JSONObject(br.readLine());
                    JSONArray list = res.getJSONObject("data").getJSONArray("list");

                    List<Club> myClubs = new ArrayList<>();
                    for (int i = 0; i < list.length(); i++) {
                        JSONObject c = list.getJSONObject(i);
                        myClubs.add(new Club(
                                c.getString("id"),
                                c.getString("name"),
                                c.getString("location"),
                                R.drawable.moveup,
                                c.optString("flag", "🇨🇳")
                        ));
                    }

                    mainHandler.post(() -> showClubSelectionDialog(run, myClubs));
                }
            } catch (Exception e) {
                Log.e("API_TEST", "获取用户社团失败", e);
                mainHandler.post(() -> Toast.makeText(History.this, "Network Error", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    /** 3. 选择要分享到的跑团 */
    private void showClubSelectionDialog(HistoryRun run, List<Club> myClubs) {
        if (myClubs.isEmpty()) {
            Toast.makeText(this, "You haven't joined any clubs yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] clubNames = new String[myClubs.size()];
        for (int i = 0; i < myClubs.size(); i++) {
            clubNames[i] = myClubs.get(i).name;
        }

        new AlertDialog.Builder(this)
                .setTitle("Select a Club to Share")
                .setItems(clubNames, (dialog, which) -> {
                    Club selectedClub = myClubs.get(which);
                    showTextInputDialog(run, selectedClub);
                })
                .show();
    }

    /** 4. 填写分享的评论文本 */
    private void showTextInputDialog(HistoryRun run, Club club) {
        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(60, 20, 60, 20);

        EditText input = new EditText(this);
        input.setHint("Write something about this run...");
        input.setLayoutParams(params);
        container.addView(input);

        new AlertDialog.Builder(this)
                .setTitle("Share to " + club.name)
                .setView(container)
                .setPositiveButton("Post", (dialog, which) -> {
                    String content = input.getText().toString().trim();
                    uploadSharePost(run, club, content);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** 5. 上传相关的所有数据（用户ID, 评论时间, 历史ID, 俱乐部ID, 分享文本）到 Mock 后端 */
    private void uploadSharePost(HistoryRun run, Club club, String content) {
        new Thread(() -> {
            try {
                URL url = new URL("http://10.0.2.2:3000/v1/clubs/" + club.id + "/posts");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                // 构建请求的 JSON 体
                JSONObject body = new JSONObject();
                body.put("user_id", currentUserId);
                body.put("run_id", run.id);
                body.put("content", content);
                body.put("timestamp", System.currentTimeMillis());

                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes("UTF-8"));
                os.close();

                if (conn.getResponseCode() == 200) {
                    mainHandler.post(() -> Toast.makeText(History.this, "Shared successfully to " + club.name, Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                Log.e("API_TEST", "上传分享帖子失败", e);
                mainHandler.post(() -> Toast.makeText(History.this, "Share Failed", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}