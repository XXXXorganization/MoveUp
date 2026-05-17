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

    // 🌟 新增：暴露 BASE_URL 供测试修改
    public static String BASE_URL = "http://10.234.4.72:3500";

    private Handler mainHandler;
    private RecyclerView recyclerView;
    private PlanDetailAdapter adapter;
    private List<PlanDetailItem> planList = new ArrayList<>();

    private String targetDay;
    private String currentUserId;

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
        int imageResId = getIntent().getIntExtra("IMAGE_RES_ID", R.drawable.moveup);

        TextView tvDayName = findViewById(R.id.tvDayName);
        ImageView headerImage = findViewById(R.id.headerImage);
        tvDayName.setText(targetDay);
        headerImage.setImageResource(imageResId);

        findViewById(R.id.btnMenu).setOnClickListener(v -> finish());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recyclerPlanDetails);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PlanDetailAdapter(planList);
        recyclerView.setAdapter(adapter);

        adapter.setOnItemLongClickListener(position -> {
            showDeleteConfirmDialog(position);
        });

        FloatingActionButton fabAdd = findViewById(R.id.fabAddPlan);
        fabAdd.setOnClickListener(v -> {
            showAddPlanDialog();
        });

        SharedPreferences prefs = getSharedPreferences("moveup_auth", MODE_PRIVATE);
        currentUserId = prefs.getString("user_phone", "13800138000");

        fetchPlanDetails();
    }

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
                    targetTextView.setTextColor(Color.BLACK);
                }, currentHour, currentMinute, false);

        timePickerDialog.show();
    }

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
                end = "";
            }

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

    private void fetchPlanDetails() {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                // 🌟 修改点：动态拼接 BASE_URL
                String urlString = BASE_URL + "/v1/plan/details?user_id=" + currentUserId + "&day=" + targetDay;
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

    private void addPlanToServer(String startTime, String endTime, String distance) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                // 🌟 修改点：动态拼接 BASE_URL
                URL url = new URL(BASE_URL + "/v1/plan/details");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");

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
                // 🌟 修改点：动态拼接 BASE_URL
                URL url = new URL(BASE_URL + "/v1/plan/details/delete");
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