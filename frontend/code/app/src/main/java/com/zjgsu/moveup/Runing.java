package com.zjgsu.moveup;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.MapsInitializer;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class Runing extends AppCompatActivity implements AMapLocationListener {

    private static final String TAG = "RuningAPI";
    public static String BASE_URL = "http://10.234.4.72:3500/v1";

    private static final String PREFS_AUTH = "moveup_auth";
    private static final String KEY_JWT = "jwt";
    private static final int REQ_LOCATION_AUDIO = 1001;

    private TextView tvGps, tvWeather, tvDistanceMain, tvPace, tvDuration, tvCalories;
    private TextView tvMapPaceValue, tvMapDurationValue, tvMapCaloriesValue;
    private ScrollView statsScroll;
    private View mapArea;
    private MaterialButton btnCenter, btnFinish;
    private View spaceFinish, panelControls, layoutSummary;
    private TextView tvSumDistance, tvSumPace, tvSumDuration, tvSumCalories;
    private MaterialButton btnBackHome;
    private ImageButton btnLeft;
    private boolean mapVisible;

    // 高德地图和定位组件
    private MapView mapView;
    private AMap aMap;
    private AMapLocationClient locationClient;
    private AMapLocationClientOption locationOption;
    private Polyline polyline;
    private final ArrayList<LatLng> latLngPoints = new ArrayList<>();

    // 核心状态与时间控制变量
    private boolean isTracking = true;
    private boolean isJustResumed = false;

    private float totalMeters;
    private long accumulatedTimeMs = 0;
    private long lastResumeTimeMs = 0;
    private Runnable timerRunnable;
    private String runId;

    // 实时数据状态缓存
    private String currentAddress = "未知路线";
    private String currentPaceStr = "0'00\"";
    private String currentDistStr = "0.00";
    private String currentDurationStr = "00:00.00";
    private String currentKcalStr = "0";

    private boolean isWeatherFetched = false;
    private boolean isFirstLocation = true;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable uploadRunnable;
    private final ArrayList<JSONObject> pendingPoints = new ArrayList<>();

    // 🌟 AI 语音教练专属组件
    private VoiceCoachManager voiceCoach;
    private FloatingActionButton fabVoice;
    private boolean isListening = false; // 记录当前麦克风是否正在录音
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MapsInitializer.updatePrivacyShow(this, true, true);
        MapsInitializer.updatePrivacyAgree(this, true);
        try {
            AMapLocationClient.updatePrivacyShow(this, true, true);
            AMapLocationClient.updatePrivacyAgree(this, true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        setContentView(R.layout.activity_runing);

        SharedPreferences prefs = getSharedPreferences(PREFS_AUTH, MODE_PRIVATE);
        currentUserId = prefs.getString("user_phone", "13800138000");

        initViews();

        mapView.onCreate(savedInstanceState);
        if (aMap == null) {
            aMap = mapView.getMap();
            aMap.getUiSettings().setZoomControlsEnabled(true);
        }

        btnLeft.setOnClickListener(v -> toggleMapView());
        btnCenter.setOnClickListener(v -> togglePause());

        // 结算界面：点击 FINISH
        btnFinish.setOnClickListener(v -> finishRun());
        btnBackHome.setOnClickListener(v -> finish()); // 返回主页

        // 检查定位和录音权限
        if (hasPermissions()) {
            initGaodeLocation();
            initVoiceAssistant();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.RECORD_AUDIO
            }, REQ_LOCATION_AUDIO);
        }

        postRunsStart();

        // 启动精确到 0.01 秒的全局计时器
        startTimer();
    }

    private void initViews() {
        tvGps = findViewById(R.id.tvGps);
        tvWeather = findViewById(R.id.tvWeather);
        tvDistanceMain = findViewById(R.id.tvDistanceMain);
        tvPace = findViewById(R.id.tvPace);
        tvDuration = findViewById(R.id.tvDuration);
        tvCalories = findViewById(R.id.tvCalories);
        tvMapPaceValue = findViewById(R.id.tvMapPaceValue);
        tvMapDurationValue = findViewById(R.id.tvMapDurationValue);
        tvMapCaloriesValue = findViewById(R.id.tvMapCaloriesValue);
        statsScroll = findViewById(R.id.statsScroll);
        mapArea = findViewById(R.id.mapArea);
        btnLeft = findViewById(R.id.btnLeft);
        btnCenter = findViewById(R.id.btnCenter);
        btnFinish = findViewById(R.id.btnFinish);
        spaceFinish = findViewById(R.id.spaceFinish);
        mapView = findViewById(R.id.mapView);

        panelControls = findViewById(R.id.panelControls);
        layoutSummary = findViewById(R.id.layoutSummary);
        tvSumDistance = findViewById(R.id.tvSumDistance);
        tvSumPace = findViewById(R.id.tvSumPace);
        tvSumDuration = findViewById(R.id.tvSumDuration);
        tvSumCalories = findViewById(R.id.tvSumCalories);
        btnBackHome = findViewById(R.id.btnBackHome);
    }

    private boolean hasPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION_AUDIO && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initGaodeLocation();
            initVoiceAssistant();
        }
    }

    // ==================== 1. 精确计时的 10ms 循环引擎 ====================

    private void startTimer() {
        lastResumeTimeMs = System.currentTimeMillis();
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isTracking) {
                    updateTimerUI();
                }
                mainHandler.postDelayed(this, 10);
            }
        };
        mainHandler.post(timerRunnable);
    }

    private void updateTimerUI() {
        long elapsedMs = accumulatedTimeMs;
        if (isTracking) {
            elapsedMs += (System.currentTimeMillis() - lastResumeTimeMs);
        }

        long totalCenti = (elapsedMs / 10) % 100;
        long totalSec = (elapsedMs / 1000) % 60;
        long totalMin = (elapsedMs / 60000);

        currentDurationStr = String.format(Locale.US, "%02d:%02d.%02d", totalMin, totalSec, totalCenti);

        tvDuration.setText(currentDurationStr);
        tvMapDurationValue.setText(currentDurationStr);
    }

    // ==================== 2. 点按语音 AI 对话系统 ====================

    private void initVoiceAssistant() {
        voiceCoach = VoiceCoachManager.getInstance(this);

        // 注册监听器：接收录音状态并更新UI
        voiceCoach.setVoiceListener(new VoiceCoachManager.VoiceListener() {
            @Override
            public void onRecognized(String text) {
                // 听到话后自动发给 AI
                askAI(text);
            }

            @Override
            public void onStatus(String status) {
                mainHandler.post(() -> {
                    if (fabVoice != null) {
                        if ("Listening".equals(status)) {
                            fabVoice.setImageResource(android.R.drawable.presence_audio_busy);
                            isListening = true;
                        } else {
                            fabVoice.setImageResource(android.R.drawable.ic_btn_speak_now);
                            isListening = false;
                        }
                    }
                });
            }
        });

        addVoiceFloatButton();
        Toast.makeText(this, "AI 教练已上线，点击右下角麦克风对话！", Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void addVoiceFloatButton() {
        ViewGroup rootView = findViewById(android.R.id.content);
        fabVoice = new FloatingActionButton(this);
        fabVoice.setImageResource(android.R.drawable.ic_btn_speak_now);
        fabVoice.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#C7FB58")));

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.BOTTOM | Gravity.END;
        params.setMargins(0, 0, 40, 300);
        fabVoice.setLayoutParams(params);

        // 点击麦克风开始/停止语音输入
        fabVoice.setOnTouchListener(new View.OnTouchListener() {
            private float dX, dY;
            private long startTime;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = view.getX() - event.getRawX();
                        dY = view.getY() - event.getRawY();
                        startTime = System.currentTimeMillis();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        view.setX(event.getRawX() + dX);
                        view.setY(event.getRawY() + dY);
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (System.currentTimeMillis() - startTime < 200) {
                            if (!isListening) {
                                voiceCoach.startListening();
                                Toast.makeText(Runing.this, "教练正在听...", Toast.LENGTH_SHORT).show();
                            } else {
                                voiceCoach.stopListening();
                            }
                        }
                        return true;
                }
                return false;
            }
        });
        rootView.addView(fabVoice);
    }

    private void askAI(String userSpeech) {
        voiceCoach.cancelListening(); // 请求期间关麦

        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                // 🌟 核心修改点：把实时数据全部传给 AI，并强制它关注后端的“历史”和“计划”！
                String contextPrompt = String.format(Locale.CHINA,
                        "我在运动中。当前位置: %s。本次已跑距离: %s Km，当前配速: %s，已用时: %s，消耗热量: %s kcal。我对你说: '%s'。请你作为私人语音教练简短回答我(限50字内)。请结合你在系统已知信息里看到的我的【跑步历史总记录】和【本周跑步计划】，以及我目前的实时数据和周边风景，给我专业的指导或鼓励！",
                        currentAddress, currentDistStr, currentPaceStr, currentDurationStr, currentKcalStr, userSpeech);

                JSONObject userMsgObj = new JSONObject();
                userMsgObj.put("role", "user");
                userMsgObj.put("content", contextPrompt);

                JSONArray chatHistory = new JSONArray();
                chatHistory.put(userMsgObj);

                JSONObject requestBody = new JSONObject();
                requestBody.put("user_id", currentUserId);
                requestBody.put("chat_history", chatHistory);

                URL url = new URL(BASE_URL + "/ai/chat");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                connection.setDoOutput(true);
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                OutputStream os = connection.getOutputStream();
                os.write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
                os.close();

                int code = connection.getResponseCode();
                if (code == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();

                    JSONObject respJson = new JSONObject(sb.toString());
                    if (respJson.optInt("code") == 200) {
                        String aiReply = respJson.getJSONObject("data").getString("reply");
                        mainHandler.post(() -> voiceCoach.speak(aiReply));
                    }
                }
            } catch (Exception e) {
                mainHandler.post(() -> voiceCoach.speak("网络信号不好，请按自己的节奏继续跑！"));
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    // ==================== 3. 高德定位与数据渲染逻辑 ====================

    private void initGaodeLocation() {
        try {
            locationClient = new AMapLocationClient(getApplicationContext());
            locationOption = new AMapLocationClientOption();
            locationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            locationOption.setInterval(2000);
            locationOption.setNeedAddress(true);
            locationClient.setLocationOption(locationOption);
            locationClient.setLocationListener(this);
            locationClient.startLocation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLocationChanged(AMapLocation amapLocation) {
        if (amapLocation != null && amapLocation.getErrorCode() == 0) {
            tvGps.setText("GPS");

            if (amapLocation.getAddress() != null && !amapLocation.getAddress().isEmpty()) {
                currentAddress = amapLocation.getCity() + amapLocation.getDistrict() + amapLocation.getStreet() + amapLocation.getPoiName();
            }

            LatLng currentLatLng = new LatLng(amapLocation.getLatitude(), amapLocation.getLongitude());

            if (!isWeatherFetched) {
                isWeatherFetched = true;
                fetchRealWeather(amapLocation.getLatitude(), amapLocation.getLongitude());
            }

            if (layoutSummary.getVisibility() != View.VISIBLE) {
                if (isFirstLocation) {
                    aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17f));
                    isFirstLocation = false;

                    // 🌟 核心修改点：第一次定位成功后，AI 主动问风景，并提示结合历史数据
                    String initPrompt = String.format(Locale.CHINA,
                            "【系统指令】用户目前位于: %s。请主动开口向用户打招呼，结合你在系统中看到的用户的【历史跑步数据或计划】夸奖他，并主动询问他是否需要为你讲解一下附近（%s）的景色？（限50字内，语气要热情，绝对不能说出这是指令）",
                            currentAddress, currentAddress);
                    askAI(initPrompt);

                } else {
                    aMap.animateCamera(CameraUpdateFactory.changeLatLng(currentLatLng));
                }
            }

            if (isTracking) {
                if (isJustResumed) {
                    latLngPoints.add(currentLatLng);
                    isJustResumed = false;
                } else if (!latLngPoints.isEmpty()) {
                    LatLng lastLatLng = latLngPoints.get(latLngPoints.size() - 1);
                    float dist = AMapUtils.calculateLineDistance(lastLatLng, currentLatLng);
                    if (dist > 1.0f) {
                        totalMeters += dist;
                        latLngPoints.add(currentLatLng);
                        drawRoute();
                    }
                } else {
                    latLngPoints.add(currentLatLng);
                }

                Location loc = new Location("gaode");
                loc.setLatitude(amapLocation.getLatitude());
                loc.setLongitude(amapLocation.getLongitude());
                loc.setAltitude(amapLocation.getAltitude());
                loc.setSpeed(amapLocation.getSpeed());
                loc.setTime(amapLocation.getTime());

                if (runId != null) enqueuePoint(loc);
            }

            renderStats();
        } else {
            tvGps.setText("GPS OFF");
        }
    }

    private void fetchRealWeather(double lat, double lon) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                String urlString = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon + "&current_weather=true";
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
                    if (resp.has("current_weather")) {
                        JSONObject currentWeather = resp.getJSONObject("current_weather");
                        double temp = currentWeather.optDouble("temperature", 0.0);
                        int displayTemp = (int) Math.round(temp);

                        mainHandler.post(() -> {
                            if (tvWeather != null) {
                                tvWeather.setText(displayTemp + "°C");
                            }
                        });
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "获取真实天气失败", e);
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    private void drawRoute() {
        if (latLngPoints.size() < 2) return;
        if (polyline != null) polyline.remove();
        polyline = aMap.addPolyline(new PolylineOptions().addAll(latLngPoints).width(16f).color(Color.parseColor("#C7FB58")));
    }

    private void renderStats() {
        float km = totalMeters / 1000f;
        DecimalFormatSymbols sym = DecimalFormatSymbols.getInstance(Locale.GERMANY);
        DecimalFormat df = new DecimalFormat("00.00", sym);
        currentDistStr = df.format(km);

        long elapsedMs = accumulatedTimeMs;
        if (isTracking) {
            elapsedMs += (System.currentTimeMillis() - lastResumeTimeMs);
        }
        long totalSec = elapsedMs / 1000;

        currentPaceStr = "0'00\"";
        if (km >= 0.01f && totalSec > 0) {
            float paceSecPerKm = totalSec / km;
            int pm = (int) (paceSecPerKm / 60f);
            int ps = (int) (paceSecPerKm % 60f);
            currentPaceStr = pm + "'" + String.format(Locale.US, "%02d", ps) + "\"";
        }

        int kcal = Math.round(km * 62f);
        currentKcalStr = String.valueOf(kcal);

        tvDistanceMain.setText(currentDistStr);
        tvPace.setText(currentPaceStr);
        tvCalories.setText(kcal + " kcal");
        tvMapPaceValue.setText(currentPaceStr);
        tvMapCaloriesValue.setText(kcal + " kcal");
    }

    // ==================== 4. 界面切换与后端同步逻辑 ====================

    private void toggleMapView() {
        mapVisible = !mapVisible;
        statsScroll.setVisibility(mapVisible ? View.GONE : View.VISIBLE);
        mapArea.setVisibility(mapVisible ? View.VISIBLE : View.GONE);
    }

    private void togglePause() {
        isTracking = !isTracking;
        if (isTracking) {
            btnCenter.setText("PAUSE");
            btnFinish.setVisibility(View.GONE);
            spaceFinish.setVisibility(View.GONE);

            lastResumeTimeMs = System.currentTimeMillis();
            isJustResumed = true;
        } else {
            btnCenter.setText("CONTINUE");
            btnFinish.setVisibility(View.VISIBLE);
            spaceFinish.setVisibility(View.VISIBLE);

            accumulatedTimeMs += (System.currentTimeMillis() - lastResumeTimeMs);
        }
    }

    private void finishRun() {
        if (isTracking) {
            accumulatedTimeMs += (System.currentTimeMillis() - lastResumeTimeMs);
        }
        isTracking = false;

        if (timerRunnable != null) {
            mainHandler.removeCallbacks(timerRunnable);
        }
        if (locationClient != null) {
            locationClient.stopLocation();
        }

        findViewById(R.id.headerRunning).setVisibility(View.GONE);
        panelControls.setVisibility(View.GONE);
        statsScroll.setVisibility(View.GONE);
        findViewById(R.id.mapStatsRow).setVisibility(View.GONE);
        if (fabVoice != null) fabVoice.setVisibility(View.GONE);

        mapArea.setVisibility(View.VISIBLE);
        layoutSummary.setVisibility(View.VISIBLE);

        tvSumDistance.setText(currentDistStr);
        tvSumPace.setText(currentPaceStr);
        tvSumDuration.setText(currentDurationStr);
        tvSumCalories.setText(currentKcalStr);

        if (latLngPoints.size() > 1) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (LatLng point : latLngPoints) builder.include(point);
            aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 150));
        } else if (latLngPoints.size() == 1) {
            aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLngPoints.get(0), 16f));
        }

        postRunFinish();
    }

    private void postRunFinish() {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(BASE_URL + "/runs/finish");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(8000);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");

                JSONObject body = new JSONObject();
                body.put("user_id", currentUserId);
                body.put("distance", currentDistStr);
                body.put("duration_str", currentDurationStr);
                body.put("pace", currentPaceStr);
                body.put("calories", currentKcalStr);

                OutputStream os = connection.getOutputStream();
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                os.close();

                int code = connection.getResponseCode();
                if (code == 200) {
                    Log.d(TAG, "跑步数据已成功上传并保存到历史记录");
                }
            } catch (Exception e) {
                Log.e(TAG, "上传跑步总结失败", e);
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    private void postRunsStart() {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                JSONObject body = new JSONObject();
                body.put("run_type", "outdoor");

                URL url = new URL(BASE_URL + "/runs/start");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(8000);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                String token = getAuthToken();
                if (token != null && !token.isEmpty()) {
                    connection.setRequestProperty("Authorization", "Bearer " + token);
                }

                byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
                OutputStream os = connection.getOutputStream();
                os.write(bytes);
                os.close();

                int code = connection.getResponseCode();
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream(),
                        StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                if (code == 200) {
                    JSONObject resp = new JSONObject(sb.toString());
                    JSONObject data = resp.optJSONObject("data");
                    if (data != null) {
                        runId = data.optString("run_id", null);
                    }
                    schedulePeriodicUpload();
                }
            } catch (Exception e) {
                Log.e(TAG, "runs/start failed", e);
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    private void schedulePeriodicUpload() {
        if (uploadRunnable != null) mainHandler.removeCallbacks(uploadRunnable);
        uploadRunnable = new Runnable() {
            @Override
            public void run() {
                flushPointsUpload();
                mainHandler.postDelayed(this, 12000);
            }
        };
        mainHandler.postDelayed(uploadRunnable, 12000);
    }

    private void flushPointsUpload() {
        if (runId == null || runId.isEmpty()) return;
        final ArrayList<JSONObject> batch = new ArrayList<>();
        synchronized (pendingPoints) {
            if (pendingPoints.isEmpty()) return;
            batch.addAll(pendingPoints);
            pendingPoints.clear();
        }

        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                JSONObject body = new JSONObject();
                JSONArray arr = new JSONArray();
                for (JSONObject p : batch) arr.put(p);
                body.put("points", arr);

                URL url = new URL(BASE_URL + "/runs/" + runId + "/points");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(8000);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                String token = getAuthToken();
                if (token != null && !token.isEmpty()) {
                    connection.setRequestProperty("Authorization", "Bearer " + token);
                }
                OutputStream os = connection.getOutputStream();
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                os.close();
                connection.getResponseCode();
            } catch (Exception e) {
                Log.e(TAG, "upload points failed", e);
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    private String getAuthToken() {
        SharedPreferences prefs = getSharedPreferences(PREFS_AUTH, MODE_PRIVATE);
        return prefs.getString(KEY_JWT, null);
    }

    private void enqueuePoint(Location location) {
        try {
            JSONObject pt = new JSONObject();
            pt.put("lat", location.getLatitude());
            pt.put("lng", location.getLongitude());
            if (location.hasAltitude()) pt.put("altitude", location.getAltitude());
            if (location.hasSpeed()) pt.put("speed", (double) location.getSpeed());
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            pt.put("timestamp", sdf.format(new Date(location.getTime())));
            synchronized (pendingPoints) {
                pendingPoints.add(pt);
            }
        } catch (Exception e) {
            Log.e(TAG, "enqueue point", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        if (timerRunnable != null) mainHandler.removeCallbacks(timerRunnable);
        if (uploadRunnable != null) mainHandler.removeCallbacks(uploadRunnable);
        if (locationClient != null) { locationClient.stopLocation(); locationClient.onDestroy(); }
        flushPointsUpload();

        if (voiceCoach != null) voiceCoach.shutdown();
    }
}