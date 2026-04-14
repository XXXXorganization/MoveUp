package com.zjgsu.moveup;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class Plan_details extends AppCompatActivity {

    private Handler mainHandler;
    private RecyclerView recyclerView;
    private PlanDetailAdapter adapter;
    private List<PlanDetailItem> planList = new ArrayList<>();

    private String targetDay; // 目标星期
    private String currentUserId; // 当前用户ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_plan_details);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mainHandler = new Handler(Looper.getMainLooper());

        targetDay = getIntent().getStringExtra("DAY_NAME");
        if (targetDay == null) targetDay = "MONDAY";
        int imageResId = getIntent().getIntExtra("IMAGE_RES_ID", R.drawable.moveup); // 默认占位图

        TextView tvDayName = findViewById(R.id.tvDayName);
        ImageView headerImage = findViewById(R.id.headerImage);
        tvDayName.setText(targetDay);
        headerImage.setImageResource(imageResId);

        findViewById(R.id.btnMenu).setOnClickListener(v -> finish());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // 配置 RecyclerView
        recyclerView = findViewById(R.id.recyclerPlanDetails);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PlanDetailAdapter(planList);
        recyclerView.setAdapter(adapter);

        // 处理列表项的长按删除事件
        adapter.setOnItemLongClickListener(position -> {
            showDeleteConfirmDialog(position);
        });

        // 处理加号点击添加事件
        FloatingActionButton fabAdd = findViewById(R.id.fabAddPlan);
        fabAdd.setOnClickListener(v -> {
            showAddPlanDialog();
        });

        // 获取用户ID
        SharedPreferences prefs = getSharedPreferences("moveup_auth", MODE_PRIVATE);
        currentUserId = prefs.getString("user_phone", "13800138000");

        // 拉取初始数据
        fetchPlanDetails();
    }

    /**
     * 唤起系统原生时间选择器
     */
    private void showTimePicker(TextView targetTextView) {
        Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        int currentMinute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    Calendar time = Calendar.getInstance();
                    time.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    time.set(Calendar.MINUTE, minute);
                    SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.US);
                    targetTextView.setText(sdf.format(time.getTime()));
                    targetTextView.setTextColor(Color.BLACK); // 选中后文字变黑
                }, currentHour, currentMinute, false);

        timePickerDialog.show();
    }

    /**
     * 弹窗添加计划
     */
    private void showAddPlanDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Plan");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 40, 60, 10);

        final TextView tvStartTime = new TextView(this);
        tvStartTime.setHint("Tap to select Start Time");
        tvStartTime.setTextSize(16);
        tvStartTime.setPadding(0, 30, 0, 30);
        tvStartTime.setOnClickListener(v -> showTimePicker(tvStartTime));
        layout.addView(tvStartTime);

        final TextView tvEndTime = new TextView(this);
        tvEndTime.setHint("Tap to select End Time (Optional)");
        tvEndTime.setTextSize(16);
        tvEndTime.setPadding(0, 30, 0, 30);
        tvEndTime.setOnClickListener(v -> showTimePicker(tvEndTime));
        layout.addView(tvEndTime);

        final EditText etDistance = new EditText(this);
        etDistance.setHint("Distance (e.g. 5)");
        etDistance.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(etDistance);

        builder.setView(layout);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String start = tvStartTime.getText().toString().trim();
            String end = tvEndTime.getText().toString().trim();
            String distance = etDistance.getText().toString().trim();

            if (start.isEmpty() || start.contains("Tap to select") || distance.isEmpty()) {
                Toast.makeText(this, "Please enter start time and distance", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!distance.toLowerCase().contains("km")) {
                distance = distance + " Km";
            }

            if (end.contains("Tap to select")) {
                end = ""; // 若未选择结束时间，则置空
            }

            // 🌟 调用新的上传方法，分别传入 start 和 end
            addPlanToServer(start, end, distance);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showDeleteConfirmDialog(int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Plan")
                .setMessage("Are you sure you want to delete this plan?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deletePlanFromServer(position);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ==================== 网络请求部分 ====================

    private void fetchPlanDetails() {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                String urlString = "http://10.0.2.2:3000/v1/plan/details?user_id=" + currentUserId + "&day=" + targetDay;
                URL url = new URL(urlString);
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

                        planList.clear();
                        if (listArray != null) {
                            for (int i = 0; i < listArray.length(); i++) {
                                JSONObject obj = listArray.getJSONObject(i);
                                planList.add(new PlanDetailItem(
                                        obj.optString("time", ""),
                                        obj.optString("distance", "")
                                ));
                            }
                        }
                        mainHandler.post(() -> adapter.notifyDataSetChanged());
                    }
                }
            } catch (Exception e) {
                Log.e("API_TEST", "获取计划异常", e);
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    /**
     * 🌟 修改：POST 时分别发送 start_time, end_time, day, distance
     */
    private void addPlanToServer(String startTime, String endTime, String distance) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL("http://10.0.2.2:3000/v1/plan/details");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");

                // 将各个参数独立打包
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("user_id", currentUserId);
                jsonBody.put("day", targetDay);
                jsonBody.put("start_time", startTime);
                jsonBody.put("end_time", endTime);
                jsonBody.put("distance", distance);

                OutputStream os = connection.getOutputStream();
                os.write(jsonBody.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                if (connection.getResponseCode() == 200) {
                    mainHandler.post(() -> {
                        // 拼接好字符串以便在本地列表展示
                        String displayTime = startTime;
                        if (!endTime.isEmpty()) {
                            displayTime += " - " + endTime;
                        }

                        planList.add(new PlanDetailItem(displayTime, distance));
                        adapter.notifyItemInserted(planList.size() - 1);
                        Toast.makeText(Plan_details.this, "Added!", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                Log.e("API_TEST", "添加计划异常", e);
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    private void deletePlanFromServer(int position) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL("http://10.0.2.2:3000/v1/plan/details/delete");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");

                JSONObject jsonBody = new JSONObject();
                jsonBody.put("user_id", currentUserId);
                jsonBody.put("day", targetDay);
                jsonBody.put("index", position);

                OutputStream os = connection.getOutputStream();
                os.write(jsonBody.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                if (connection.getResponseCode() == 200) {
                    mainHandler.post(() -> {
                        planList.remove(position);
                        adapter.notifyItemRemoved(position);
                        Toast.makeText(Plan_details.this, "Deleted!", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                Log.e("API_TEST", "删除计划异常", e);
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }
}