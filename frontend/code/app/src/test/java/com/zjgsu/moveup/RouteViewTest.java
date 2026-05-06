package com.zjgsu.moveup;

import android.graphics.Canvas;
import android.location.Location;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class RouteViewTest {

    // ==========================================
    // 1. 覆盖所有构造函数分支
    // ==========================================
    @Test
    public void testConstructors() {
        RouteView view1 = new RouteView(RuntimeEnvironment.application);
        RouteView view2 = new RouteView(RuntimeEnvironment.application, null);
        RouteView view3 = new RouteView(RuntimeEnvironment.application, null, 0);

        assertNotNull(view1);
        assertNotNull(view2);
        assertNotNull(view3);
    }

    // ==========================================
    // 2. 覆盖空数据保护和清空逻辑
    // ==========================================
    @Test
    public void testSetRoute_WithNullAndEmpty() {
        RouteView routeView = new RouteView(RuntimeEnvironment.application);

        // 覆盖分支: setRoute() 中 if (points != null) 为 false 的情况
        routeView.setRoute(null, null);

        // 覆盖分支: onDraw() 中 if (route.isEmpty()) return;
        routeView.draw(new Canvas());

        // 覆盖 clear() 方法
        routeView.clear();
    }

    // ==========================================
    // 3. 覆盖 View 尺寸异常分支
    // ==========================================
    @Test
    public void testOnDraw_ZeroDimensions() {
        RouteView routeView = new RouteView(RuntimeEnvironment.application);

        List<Location> points = new ArrayList<>();
        points.add(new Location("gps"));
        routeView.setRoute(points, null);

        // 不调用 measure 和 layout，此时 w=0, h=0
        // 覆盖分支: onDraw() 中 if (w <= 0 || h <= 0) return;
        routeView.draw(new Canvas());
    }

    // ==========================================
    // 4. 覆盖单点数据逻辑 & latest为空的回退逻辑
    // ==========================================
    @Test
    public void testOnDraw_SinglePoint_And_NullLatest() {
        RouteView routeView = new RouteView(RuntimeEnvironment.application);
        routeView.measure(100, 100);
        routeView.layout(0, 0, 100, 100);

        List<Location> points = new ArrayList<>();
        Location loc1 = new Location("gps");
        loc1.setLatitude(30.0);
        loc1.setLongitude(120.0);
        points.add(loc1);

        // 故意让 latest 传 null
        routeView.setRoute(points, null);

        // 覆盖分支 1: onDraw() 中 if (route.size() >= 2) 为 false (不会绘制连线)
        // 覆盖分支 2: onDraw() 中 latest != null 为 false，从而回退获取 route.get(size-1)
        routeView.draw(new Canvas());
    }

    // ==========================================
    // 5. 覆盖完整多点逻辑 & 列表中包含坏(null)点防崩溃逻辑
    // ==========================================
    @Test
    public void testOnDraw_MultiplePoints_WithNullPoint() {
        RouteView routeView = new RouteView(RuntimeEnvironment.application);
        routeView.measure(100, 100);
        routeView.layout(0, 0, 100, 100);

        List<Location> points = new ArrayList<>();

        Location loc1 = new Location("gps");
        loc1.setLatitude(30.0);
        loc1.setLongitude(120.0);
        points.add(loc1);

        // 故意在列表中塞入一个 null，覆盖 onDraw 循环中 if (p == null) continue; 分支
        points.add(null);

        Location loc2 = new Location("gps");
        loc2.setLatitude(30.1);
        loc2.setLongitude(120.1);
        points.add(loc2);

        Location latestLoc = new Location("gps");
        latestLoc.setLatitude(30.2);
        latestLoc.setLongitude(120.2);

        routeView.setRoute(points, latestLoc);

        // 覆盖标准的路线生成、画布渲染以及 latest 点优先逻辑
        routeView.draw(new Canvas());
    }
}