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
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
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
    // 🌟 核心修改点：将 private static final 改为 public static，以便测试类动态修改拦截地址
    public static String BASE_URL = "http://10.0.2.2:3000/v1";

    private static final String PREFS_AUTH = "moveup_auth";
    private static final String KEY_JWT = "jwt";
    private static final int REQ_LOCATION_AUDIO = 1001;

    private TextView tvGps, tvDistanceMain, tvPace, tvDuration, tvCalories;
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

    private boolean isTracking = true;
    private float totalMeters;
    private long sessionStartMs;
    private String runId;

    // 实时数据状态缓存，用于发给 AI & 结算界面
    private String currentAddress = "未知路线";
    private String currentPaceStr = "0'00\"";
    private String currentDistStr = "0.00";
    private String currentDurationStr = "00.00";
    private String currentKcalStr = "0";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable uploadRunnable;
    private final ArrayList<JSONObject> pendingPoints = new ArrayList<>();

    // AI 语音教练专属组件
    private VoiceCoachManager voiceCoach;
    private SpeechRecognizer speechRecognizer;
    private FloatingActionButton fabVoice;
    private boolean isListening = false;
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
            aMap.getUiSettings().setZoomControlsEnabled(false);
        }

        sessionStartMs = System.currentTimeMillis();

        btnLeft.setOnClickListener(v -> toggleMapView());
        btnCenter.setOnClickListener(v -> togglePause());

        // 🌟 点击 FINISH：结束跑步，展示结算界面，并上传历史记录
        btnFinish.setOnClickListener(v -> finishRun());

        // 🌟 结算界面：点击 BACK TO HOME
        btnBackHome.setOnClickListener(v -> {
            finish(); // 结束当前 Activity，返回主页
        });

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
        renderStats();
    }

    private void initViews() {
        tvGps = findViewById(R.id.tvGps);
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

    // ==================== 1. 悬浮语音助手核心逻辑 ====================

    private void initVoiceAssistant() {
        voiceCoach = VoiceCoachManager.getInstance(this);

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {
                Toast.makeText(Runing.this, "AI教练正在聆听...", Toast.LENGTH_SHORT).show();
            }
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {
                isListening = false;
                if(fabVoice != null) fabVoice.setImageResource(android.R.drawable.ic_btn_speak_now);
            }
            @Override public void onError(int error) {
                isListening = false;
                if(fabVoice != null) fabVoice.setImageResource(android.R.drawable.ic_btn_speak_now);
                Toast.makeText(Runing.this, "没听清，请重试", Toast.LENGTH_SHORT).show();
            }
            @Override public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String userSpeech = matches.get(0);
                    askAI(userSpeech);
                }
            }
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        addVoiceFloatButton();
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
                            if (!isListening) startListening();
                            else stopListening();
                        }
                        return true;
                }
                return false;
            }
        });
        rootView.addView(fabVoice);
    }

    private void startListening() {
        isListening = true;
        fabVoice.setImageResource(android.R.drawable.presence_audio_busy);
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN");
        speechRecognizer.startListening(intent);
    }

    private void stopListening() {
        isListening = false;
        fabVoice.setImageResource(android.R.drawable.ic_btn_speak_now);
        speechRecognizer.stopListening();
    }

    private void askAI(String userSpeech) {
        Toast.makeText(this, "AI教练思考中...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                String contextPrompt = String.format(Locale.CHINA,
                        "我在跑步。当前距离: %s Km，配速: %s，我的位置在: %s。我对你说: '%s'。请你作为私人语音教练简短回答我(限40字内)，可以结合风景介绍鼓励我，或对配速提出指导。",
                        currentDistStr, currentPaceStr, currentAddress, userSpeech);

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

            // 只要不是处于结算展示模式，镜头就跟着人走
            if (layoutSummary.getVisibility() != View.VISIBLE) {
                aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17f));
            }

            if (isTracking) {
                if (!latLngPoints.isEmpty()) {
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

        long elapsed = Math.max(0, System.currentTimeMillis() - sessionStartMs);
        long totalSec = elapsed / 1000;
        long min = totalSec / 60;
        long sec = totalSec % 60;
        currentDurationStr = String.format(Locale.US, "%02d.%02d", min, sec);

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
        tvDuration.setText(currentDurationStr);
        tvPace.setText(currentPaceStr);
        tvCalories.setText(kcal + " kcal");
        tvMapPaceValue.setText(currentPaceStr);
        tvMapDurationValue.setText(currentDurationStr);
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
        } else {
            btnCenter.setText("CONTINUE");
            btnFinish.setVisibility(View.VISIBLE);
            spaceFinish.setVisibility(View.VISIBLE);
        }
    }

    // 🌟 核心：结束跑步，展示总结面板，并通知后端
    private void finishRun() {
        isTracking = false;
        if (locationClient != null) {
            locationClient.stopLocation();
        }

        // 隐藏不必要的UI，强制展示地图和覆盖层
        findViewById(R.id.headerRunning).setVisibility(View.GONE);
        panelControls.setVisibility(View.GONE);
        statsScroll.setVisibility(View.GONE);
        findViewById(R.id.mapStatsRow).setVisibility(View.GONE);
        if (fabVoice != null) fabVoice.setVisibility(View.GONE);

        // 展开地图并展示结算弹窗
        mapArea.setVisibility(View.VISIBLE);
        layoutSummary.setVisibility(View.VISIBLE);

        // 填充结算数据
        tvSumDistance.setText(currentDistStr);
        tvSumPace.setText(currentPaceStr);
        tvSumDuration.setText(currentDurationStr);
        tvSumCalories.setText(currentKcalStr);

        // 将地图镜头缩放至刚好能看到全部运动轨迹
        if (latLngPoints.size() > 1) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (LatLng point : latLngPoints) builder.include(point);
            aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 150));
        } else if (latLngPoints.size() == 1) {
            aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLngPoints.get(0), 16f));
        }

        // 通知后端保存这次跑步数据
        postRunFinish();
    }

    // 向后端请求，把本次结算数据丢给历史记录库保存
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
        if (uploadRunnable != null) mainHandler.removeCallbacks(uploadRunnable);
        if (locationClient != null) { locationClient.stopLocation(); locationClient.onDestroy(); }
        flushPointsUpload();

        if (speechRecognizer != null) speechRecognizer.destroy();
        if (voiceCoach != null) voiceCoach.shutdown();
    }
}