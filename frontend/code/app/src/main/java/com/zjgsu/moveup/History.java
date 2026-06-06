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
import java.util.Locale;

public class History extends AppCompatActivity {

    public static String BASE_URL = "http://192.168.25.47:3000";

    private RecyclerView list;
    private HistoryAdapter adapter;
    private List<HistoryRun> runList = new ArrayList<>();
    private Handler mainHandler;

    private String currentUserId;

    private TextView totalKmValue;
    private TextView summaryRunValue;
    private TextView summaryPaceValue;
    private TextView summaryTimeValue;

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_history);

        drawerLayout = findViewById(R.id.drawerLayout);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mainHandler = new Handler(Looper.getMainLooper());

        SharedPreferences prefs = getSharedPreferences("moveup_auth", MODE_PRIVATE);
        currentUserId = prefs.getString("user_phone", "13800138000");

        totalKmValue = findViewById(R.id.totalKmValue);
        summaryRunValue = findViewById(R.id.summaryRunValue);
        summaryPaceValue = findViewById(R.id.summaryPaceValue);
        summaryTimeValue = findViewById(R.id.summaryTimeValue);

        list = findViewById(R.id.recyclerHistory);
        list.setLayoutManager(new LinearLayoutManager(this));

        adapter = new HistoryAdapter(runList, (run, position) -> initiateShareSequence(run));
        list.setAdapter(adapter);

        findViewById(R.id.btnMenu).setOnClickListener(v -> {
            if (drawerLayout != null) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        setupMenuClicks();

        fetchHistoryData();
    }

    private void setupMenuClicks() {
        TextView menuHome = findViewById(R.id.menu_home);
        TextView menuHistory = findViewById(R.id.menu_history);
        TextView menuPlan = findViewById(R.id.menu_plan);
        TextView menuClub = findViewById(R.id.menu_club);
        TextView menuProfile = findViewById(R.id.menu_profile);

        if (menuHome != null) menuHome.setOnClickListener(v -> {
            startActivity(new Intent(History.this, Main.class));
            finish();
        });
        if (menuHistory != null) menuHistory.setOnClickListener(v -> {
            if (drawerLayout != null) drawerLayout.closeDrawers();
        });
        if (menuPlan != null) menuPlan.setOnClickListener(v -> {
            startActivity(new Intent(History.this, Plan.class));
            finish();
        });
        if (menuClub != null) menuClub.setOnClickListener(v -> {
            startActivity(new Intent(History.this, Find.class));
            finish();
        });
        if (menuProfile != null) menuProfile.setOnClickListener(v -> {
            startActivity(new Intent(History.this, Mine.class));
            finish();
        });
    }

    // ------------------- fetchHistoryData -------------------
    private void fetchHistoryData() {
        new Thread(() -> {
            HttpURLConnection connection = null;
            String urlStr = BASE_URL + "/v1/runs";
            boolean success = false;

            MetricsCollector.recordRequestStart(urlStr);

            try {
                URL url = new URL(urlStr);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                SharedPreferences prefs = getSharedPreferences("moveup_auth", MODE_PRIVATE);
                String token = prefs.getString("jwt", "");
                if (!token.isEmpty()) {
                    connection.setRequestProperty("Authorization", "Bearer " + token);
                }

                int httpCode = connection.getResponseCode();
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        httpCode >= 200 && httpCode < 300 ? connection.getInputStream() : connection.getErrorStream()
                ));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                if (httpCode == 200) {
                    JSONObject resp = new JSONObject(sb.toString());
                    if (resp.optInt("code") == 200) {
                        success = true;
                        JSONObject data = resp.getJSONObject("data");
                        JSONArray listArray = data.optJSONArray("list");

                        List<HistoryRun> tempRuns = new ArrayList<>();
                        double totalDistanceKm = 0.0;
                        long totalTimeMs = 0;
                        int totalRunsCount = 0;

                        if (listArray != null) {
                            totalRunsCount = listArray.length();
                            for (int i = 0; i < totalRunsCount; i++) {
                                JSONObject obj = listArray.getJSONObject(i);
                                String id = obj.optString("id", "run_" + i);
                                String date = obj.optString("date", "未知时间");
                                String title = obj.optString("title", "Outdoor Run");
                                String durationStr = obj.optString("duration_str", "00:00.00");
                                String pace = obj.optString("pace", "0'00\"");
                                String distanceStr = obj.optString("distance", "0.00 Km");

                                tempRuns.add(new HistoryRun(id, date, title, durationStr, pace, distanceStr));

                                try {
                                    String cleanDist = distanceStr.replace(" Km", "").trim();
                                    totalDistanceKm += Double.parseDouble(cleanDist);
                                } catch (Exception ignored) {}
                                totalTimeMs += parseDurationToMs(durationStr);
                            }
                        }

                        final String finalTotalKm = String.format(Locale.US, "%.2f", totalDistanceKm);
                        final String finalRunCount = String.valueOf(totalRunsCount);
                        long totalMilliRemainder = totalTimeMs % 1000;
                        long totalSec = (totalTimeMs / 1000) % 60;
                        long totalMin = (totalTimeMs / 60000);
                        final String finalTotalTime = String.format(Locale.US, "%02d:%02d:%03d", totalMin, totalSec, totalMilliRemainder);
                        String tempAvgPace = "0'00\"";
                        if (totalDistanceKm >= 0.01) {
                            long totalSecForPace = totalTimeMs / 1000;
                            float paceSecPerKm = (float) (totalSecForPace / totalDistanceKm);
                            int pm = (int) (paceSecPerKm / 60);
                            int ps = (int) (paceSecPerKm % 60);
                            tempAvgPace = pm + "'" + String.format(Locale.US, "%02d", ps) + "\"";
                        }
                        final String finalAvgPace = tempAvgPace;

                        mainHandler.post(() -> {
                            runList.clear();
                            runList.addAll(tempRuns);
                            totalKmValue.setText(finalTotalKm);
                            summaryRunValue.setText(finalRunCount);
                            summaryTimeValue.setText(finalTotalTime);
                            summaryPaceValue.setText(finalAvgPace);
                            adapter.notifyDataSetChanged();
                        });
                    }
                }
            } catch (Exception e) {
                Log.e("API_TEST", "拉取历史数据异常", e);
                success = false;
            } finally {
                MetricsCollector.recordRequestEnd(urlStr, success);
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    private long parseDurationToMs(String durationStr) {
        long ms = 0;
        try {
            if (durationStr.contains(":")) {
                String[] parts = durationStr.split(":");
                long min = Long.parseLong(parts[0].trim());
                String[] secParts = parts[1].split("\\.");
                long sec = Long.parseLong(secParts[0].trim());
                long milli = 0;
                if (secParts.length > 1) {
                    String fraction = secParts[1].trim();
                    milli = fraction.length() == 2 ? Long.parseLong(fraction) * 10 : Long.parseLong(fraction);
                }
                ms = (min * 60 * 1000) + (sec * 1000) + milli;
            } else if (durationStr.contains(".")) {
                String[] parts = durationStr.split("\\.");
                long min = Long.parseLong(parts[0].trim());
                long sec = parts.length > 1 ? Long.parseLong(parts[1].trim()) : 0;
                ms = (min * 60 * 1000) + (sec * 1000);
            }
        } catch (Exception e) {
            Log.e("History", "解析时间失败: " + durationStr);
        }
        return ms;
    }

    private void initiateShareSequence(@NonNull HistoryRun run) {
        new AlertDialog.Builder(this)
                .setTitle("Share Record")
                .setMessage("Do you want to share this record to your club?")
                .setPositiveButton("Yes", (dialog, which) -> fetchMyClubsAndShowDialog(run))
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ------------------- fetchMyClubsAndShowDialog -------------------
    private void fetchMyClubsAndShowDialog(HistoryRun run) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            String urlStr = BASE_URL + "/v1/user/clubs";
            boolean success = false;

            MetricsCollector.recordRequestStart(urlStr);

            try {
                URL url = new URL(urlStr);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                SharedPreferences prefs = getSharedPreferences("moveup_auth", MODE_PRIVATE);
                String token = prefs.getString("jwt", "");
                if (!token.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                }

                int httpCode = conn.getResponseCode();
                if (httpCode == 200) {
                    success = true;
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
                success = false;
                mainHandler.post(() -> Toast.makeText(History.this, "Network Error", Toast.LENGTH_SHORT).show());
            } finally {
                MetricsCollector.recordRequestEnd(urlStr, success);
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

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

    // ------------------- uploadSharePost -------------------
    private void uploadSharePost(HistoryRun run, Club club, String content) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            String urlStr = BASE_URL + "/v1/clubs/" + club.id + "/posts";
            boolean success = false;

            MetricsCollector.recordRequestStart(urlStr);

            try {
                URL url = new URL(urlStr);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                SharedPreferences prefs = getSharedPreferences("moveup_auth", MODE_PRIVATE);
                String token = prefs.getString("jwt", "");
                if (!token.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                }

                JSONObject body = new JSONObject();
                body.put("run_id", run.id);
                body.put("content", content);
                body.put("timestamp", System.currentTimeMillis());

                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes("UTF-8"));
                os.close();

                int httpCode = conn.getResponseCode();
                if (httpCode == 200) {
                    success = true;
                    mainHandler.post(() -> Toast.makeText(History.this, "Shared successfully to " + club.name, Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                Log.e("API_TEST", "上传分享帖子失败", e);
                success = false;
                mainHandler.post(() -> Toast.makeText(History.this, "Share Failed", Toast.LENGTH_SHORT).show());
            } finally {
                MetricsCollector.recordRequestEnd(urlStr, success);
                if (conn != null) conn.disconnect();
            }
        }).start();
    }
}