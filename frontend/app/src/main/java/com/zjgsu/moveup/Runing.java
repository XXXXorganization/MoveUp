package com.zjgsu.moveup;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import java.util.ArrayList;

public class Runing extends AppCompatActivity {

    private TextView tvGps;
    private RouteView routeView;
    private Location lastLocation;
    private ArrayList<Location> routePoints = new ArrayList<>();
    private boolean isTracking = true;
    private float totalMeters = 0;
    private float lastAltitudeMeters = Float.NaN;
    private float elevationGainMeters = 0;

    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_runing);

        tvGps = findViewById(R.id.tvGps);
        routeView = findViewById(R.id.routeView);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locationListener);
    }

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            tvGps.setText("GPS");

            if (lastLocation != null) {
                float delta = lastLocation.distanceTo(location);
                if (routePoints.isEmpty()) {
                    routePoints.add(location);
                } else if (isTracking && delta > 0.8f) {
                    totalMeters += delta;
                    routePoints.add(location);
                }
            } else {
                if (routePoints.isEmpty()) {
                    routePoints.add(location);
                }
            }

            // ====================== 报错已修复 ======================
            float alt = location.hasAltitude() ? (float) location.getAltitude() : Float.NaN;

            if (!Float.isNaN(alt)) {
                if (isTracking && !Float.isNaN(lastAltitudeMeters)) {
                    float dAlt = alt - lastAltitudeMeters;
                    if (dAlt > 0) {
                        elevationGainMeters += dAlt;
                    }
                }
                lastAltitudeMeters = alt;
            }

            lastLocation = location;

            if (routeView != null) {
                routeView.setRoute(routePoints, location);
            }
            renderStats();
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
            tvGps.setText("GPS OFF");
        }
    };

    private void renderStats() {
        // 你的显示逻辑（不变）
    }
}