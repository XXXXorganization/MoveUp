package com.zjgsu.moveup;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
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
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;
import com.google.android.material.button.MaterialButton;

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
    private static final String BASE_URL = "http://10.0.2.2:3000/v1";
    private static final String PREFS_AUTH = "moveup_auth";
    private static final String KEY_JWT = "jwt";
    private static final int REQ_LOCATION = 1001;

    private TextView tvGps;
    private TextView tvDistanceMain;
    private TextView tvPace;
    private TextView tvDuration;
    private TextView tvCalories;
    private TextView tvMapPaceValue;
    private TextView tvMapDurationValue;
    private TextView tvMapCaloriesValue;
    private ScrollView statsScroll;
    private View mapArea;
    private MaterialButton btnCenter;
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

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable uploadRunnable;
    private String runId;
    private long sessionStartMs;
    private final ArrayList<JSONObject> pendingPoints = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 必须在初始化 AMap SDK 前同意隐私政策，否则会报错
        AMapLocationClient.updatePrivacyShow(this, true, true);
        AMapLocationClient.updatePrivacyAgree(this, true);

        setContentView(R.layout.activity_runing);

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

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState); // 此方法必须调用
        if (aMap == null) {
            aMap = mapView.getMap();
            aMap.getUiSettings().setZoomControlsEnabled(false); // 隐藏缩放按钮，保持界面清爽
        }

        sessionStartMs = System.currentTimeMillis();

        btnLeft.setOnClickListener(v -> toggleMapView());
        btnCenter.setOnClickListener(v -> togglePause());

        if (hasLocationPermission()) {
            initGaodeLocation();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, REQ_LOCATION);
        }

        postRunsStart();
        renderStats();
    }

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initGaodeLocation();
        }
    }

    private void initGaodeLocation() {
        try {
            locationClient = new AMapLocationClient(getApplicationContext());
            locationOption = new AMapLocationClientOption();
            // 设置高精度定位模式
            locationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            // 2秒定位一次
            locationOption.setInterval(2000);

            locationClient.setLocationOption(locationOption);
            locationClient.setLocationListener(this);
            locationClient.startLocation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLocationChanged(AMapLocation amapLocation) {
        if (amapLocation != null) {
            if (amapLocation.getErrorCode() == 0) {
                tvGps.setText("GPS");

                LatLng currentLatLng = new LatLng(amapLocation.getLatitude(), amapLocation.getLongitude());

                // 将地图镜头移动到当前位置
                aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17f));

                if (isTracking) {
                    if (!latLngPoints.isEmpty()) {
                        LatLng lastLatLng = latLngPoints.get(latLngPoints.size() - 1);
                        // 高德计算球面距离，比原生更精确
                        float dist = AMapUtils.calculateLineDistance(lastLatLng, currentLatLng);
                        if (dist > 1.0f) { // 过滤微小漂移
                            totalMeters += dist;
                            latLngPoints.add(currentLatLng);
                            drawRoute();
                        }
                    } else {
                        latLngPoints.add(currentLatLng);
                    }

                    // 构建原生 Location 对象，以兼容原有的上传 API 方法
                    Location loc = new Location("gaode");
                    loc.setLatitude(amapLocation.getLatitude());
                    loc.setLongitude(amapLocation.getLongitude());
                    loc.setAltitude(amapLocation.getAltitude());
                    loc.setSpeed(amapLocation.getSpeed());
                    loc.setTime(amapLocation.getTime());

                    if (runId != null) {
                        enqueuePoint(loc);
                    }
                }
                renderStats();
            } else {
                Log.e("AmapError", "location Error, ErrCode:" + amapLocation.getErrorCode()
                        + ", errInfo:" + amapLocation.getErrorInfo());
                tvGps.setText("GPS OFF");
            }
        }
    }

    private void drawRoute() {
        if (latLngPoints.size() < 2) return;

        if (polyline != null) {
            polyline.remove(); // 移除旧线
        }

        polyline = aMap.addPolyline(new PolylineOptions()
                .addAll(latLngPoints)
                .width(16f) // 线条宽度
                .color(Color.parseColor("#C7FB58"))); // 你的主题绿
    }

    private void toggleMapView() {
        mapVisible = !mapVisible;
        if (mapVisible) {
            statsScroll.setVisibility(View.GONE);
            mapArea.setVisibility(View.VISIBLE);
        } else {
            statsScroll.setVisibility(View.VISIBLE);
            mapArea.setVisibility(View.GONE);
        }
    }

    private void togglePause() {
        isTracking = !isTracking;
        btnCenter.setText(isTracking ? "PAUSE" : "RESUME");
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

                if (code != 200) {
                    Log.w(TAG, "runs/start HTTP " + code + " body=" + sb);
                    return;
                }
                JSONObject resp = new JSONObject(sb.toString());
                if (resp.optInt("code", -1) != 200) {
                    Log.w(TAG, "runs/start api code " + resp.optInt("code"));
                    return;
                }
                JSONObject data = resp.optJSONObject("data");
                if (data != null) {
                    runId = data.optString("run_id", null);
                }
                schedulePeriodicUpload();
            } catch (Exception e) {
                Log.e(TAG, "runs/start failed", e);
                mainHandler.post(() ->
                        Toast.makeText(Runing.this, "无法连接跑步服务", Toast.LENGTH_SHORT).show());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    private void schedulePeriodicUpload() {
        if (uploadRunnable != null) {
            mainHandler.removeCallbacks(uploadRunnable);
        }
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
        if (runId == null || runId.isEmpty()) {
            return;
        }
        final ArrayList<JSONObject> batch = new ArrayList<>();
        synchronized (pendingPoints) {
            if (pendingPoints.isEmpty()) {
                return;
            }
            batch.addAll(pendingPoints);
            pendingPoints.clear();
        }

        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                JSONObject body = new JSONObject();
                JSONArray arr = new JSONArray();
                for (JSONObject p : batch) {
                    arr.put(p);
                }
                body.put("points", arr);

                URL url = new URL(BASE_URL + "/runs/" + runId + "/points");
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
                OutputStream os = connection.getOutputStream();
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                os.close();
                int code = connection.getResponseCode();
            } catch (Exception e) {
                Log.e(TAG, "upload points failed", e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
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
            if (location.hasAltitude()) {
                pt.put("altitude", location.getAltitude());
            }
            if (location.hasSpeed()) {
                pt.put("speed", (double) location.getSpeed());
            }
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

    private void renderStats() {
        float km = totalMeters / 1000f;
        DecimalFormatSymbols sym = DecimalFormatSymbols.getInstance(Locale.GERMANY);
        DecimalFormat df = new DecimalFormat("00.00", sym);
        String distStr = df.format(km);

        long elapsed = System.currentTimeMillis() - sessionStartMs;
        if (elapsed < 0) elapsed = 0;
        long totalSec = elapsed / 1000;
        long min = totalSec / 60;
        long sec = totalSec % 60;
        String durationStr = String.format(Locale.US, "%02d.%02d", min, sec);

        String paceStr = "0'00\"";
        if (km >= 0.01f && totalSec > 0) {
            float paceSecPerKm = totalSec / km;
            int pm = (int) (paceSecPerKm / 60f);
            int ps = (int) (paceSecPerKm % 60f);
            paceStr = pm + "'" + String.format(Locale.US, "%02d", ps) + "\"";
        }

        int kcal = Math.round(km * 62f);

        tvDistanceMain.setText(distStr);
        tvDuration.setText(durationStr);
        tvPace.setText(paceStr);
        tvCalories.setText(kcal + " kcal");
        tvMapPaceValue.setText(paceStr);
        tvMapDurationValue.setText(durationStr);
        tvMapCaloriesValue.setText(kcal + " kcal");
    }

    // 地图与定位的生命周期管理，防止内存泄漏
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
        if (uploadRunnable != null) {
            mainHandler.removeCallbacks(uploadRunnable);
        }
        if (locationClient != null) {
            locationClient.stopLocation();
            locationClient.onDestroy();
        }
        flushPointsUpload();
    }
}