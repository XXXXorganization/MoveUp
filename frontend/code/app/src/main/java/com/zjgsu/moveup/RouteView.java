package com.zjgsu.moveup;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.location.Location;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 用 GPS 点在画布上绘制“实时路线折线 + 当前点”。
 * 已增加：支持双指任意放大、缩小，以及单指拖拽平移。
 */
public class RouteView extends View {

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final List<Location> route = new ArrayList<>();
    private @Nullable Location latest;

    // 🌟 新增：用于处理地图缩放和平移的核心组件
    private Matrix matrix = new Matrix();
    private ScaleGestureDetector scaleGestureDetector;
    private float scaleFactor = 1.0f;
    private float lastTouchX;
    private float lastTouchY;
    private boolean isDragging = false;

    public RouteView(Context context) {
        super(context);
        init(context);
    }

    public RouteView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public RouteView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setColor(Color.parseColor("#B8E986")); // lime_accent
        linePaint.setStrokeWidth(6f);

        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setColor(Color.parseColor("#B8E986"));

        // 🌟 初始化双指缩放手势监听器
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float scale = detector.getScaleFactor();
                scaleFactor *= scale;
                // 限制缩放比例：最多缩小到 0.5 倍，最大放大到 10 倍
                scaleFactor = Math.max(0.5f, Math.min(scaleFactor, 10.0f));

                // 以两根手指的中心点为基准进行缩放
                matrix.postScale(scale, scale, detector.getFocusX(), detector.getFocusY());
                invalidate(); // 触发重绘
                return true;
            }
        });
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
        // 清除数据时，恢复默认的视角和缩放比例
        matrix.reset();
        scaleFactor = 1.0f;
        invalidate();
    }

    // 🌟 拦截并处理用户的触摸事件（拖拽与缩放）
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 先让缩放监听器处理事件
        scaleGestureDetector.onTouchEvent(event);

        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                // 记录手指按下的初始位置
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                isDragging = true;
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                // 如果是单指拖拽（不在缩放过程中），则平移画布
                if (isDragging && !scaleGestureDetector.isInProgress()) {
                    float x = event.getX();
                    float y = event.getY();
                    float dx = x - lastTouchX;
                    float dy = y - lastTouchY;

                    matrix.postTranslate(dx, dy);
                    invalidate();

                    lastTouchX = x;
                    lastTouchY = y;
                }
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                isDragging = false;
                break;
            }
        }
        return true; // 表示消费了该触摸事件
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

        // 第一次遍历：计算经纬度范围
        for (Location p : route) {
            if (p == null) continue;
            minLat = Math.min(minLat, p.getLatitude());
            maxLat = Math.max(maxLat, p.getLatitude());
            minLon = Math.min(minLon, p.getLongitude());
            maxLon = Math.max(maxLon, p.getLongitude());
        }

        double latRange = Math.max(1e-6, maxLat - minLat);
        double lonRange = Math.max(1e-6, maxLon - minLon);

        // 🌟 核心：在绘制前，将所有的平移和缩放矩阵应用到 Canvas 上
        canvas.save();
        canvas.concat(matrix);

        Path path = new Path();
        boolean first = true;

        // 第二次遍历：绘制路径折线
        for (Location p : route) {
            if (p == null) continue;

            float x = (float) ((p.getLongitude() - minLon) / lonRange * w);
            float y = (float) ((1d - (p.getLatitude() - minLat) / latRange) * h);
            if (first) {
                path.moveTo(x, y);
                first = false;
            } else {
                path.lineTo(x, y);
            }
        }

        // 🌟 动态调整线宽：防止放大地图时路线变得像面条一样粗
        linePaint.setStrokeWidth(6f / scaleFactor);

        if (route.size() >= 2) {
            canvas.drawPath(path, linePaint);
        }

        // 绘制代表当前位置的圆点
        Location d = latest != null ? latest : route.get(route.size() - 1);
        if (d != null) {
            float x = (float) ((d.getLongitude() - minLon) / lonRange * w);
            float y = (float) ((1d - (d.getLatitude() - minLat) / latRange) * h);
            // 动态调整圆点大小
            canvas.drawCircle(x, y, 10f / scaleFactor, dotPaint);
        }

        // 🌟 恢复 Canvas 状态
        canvas.restore();
    }
}