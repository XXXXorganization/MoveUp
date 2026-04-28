package com.zjgsu.moveup;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import android.location.Location;

/**
 * 用 GPS 点在画布上绘制“实时路线折线 + 当前点”。
 * 不依赖地图 SDK，满足你需要的“定位追踪功能”展示。
 */
public class RouteView extends View {

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final List<Location> route = new ArrayList<>();
    private @Nullable Location latest;

    public RouteView(Context context) {
        super(context);
        init();
    }

    public RouteView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RouteView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setColor(Color.parseColor("#B8E986")); // lime_accent
        linePaint.setStrokeWidth(6f);

        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setColor(Color.parseColor("#B8E986"));
    }

    public void setRoute(@Nullable List<Location> points, @Nullable Location latestPoint) {
        route.clear();
        if (points != null) {
            route.addAll(points);
        }
        latest = latestPoint;
        invalidate();
    }

    public void clear() {
        route.clear();
        latest = null;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (route.isEmpty()) return;

        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        double minLat = Double.MAX_VALUE;
        double maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE;
        double maxLon = -Double.MAX_VALUE;

        for (Location p : route) {
            if (p == null) continue;
            minLat = Math.min(minLat, p.getLatitude());
            maxLat = Math.max(maxLat, p.getLatitude());
            minLon = Math.min(minLon, p.getLongitude());
            maxLon = Math.max(maxLon, p.getLongitude());
        }

        double latRange = Math.max(1e-6, maxLat - minLat);
        double lonRange = Math.max(1e-6, maxLon - minLon);

        Path path = new Path();
        boolean first = true;
        for (Location p : route) {
            float x = (float) ((p.getLongitude() - minLon) / lonRange * w);
            float y = (float) ((1d - (p.getLatitude() - minLat) / latRange) * h);
            if (first) {
                path.moveTo(x, y);
                first = false;
            } else {
                path.lineTo(x, y);
            }
        }
        if (route.size() >= 2) {
            canvas.drawPath(path, linePaint);
        }

        Location d = latest != null ? latest : route.get(route.size() - 1);
        if (d != null) {
            float x = (float) ((d.getLongitude() - minLon) / lonRange * w);
            float y = (float) ((1d - (d.getLatitude() - minLat) / latRange) * h);
            canvas.drawCircle(x, y, 10f, dotPaint);
        }
    }
}

