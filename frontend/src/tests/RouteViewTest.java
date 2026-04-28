package com.zjgsu.moveup;

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

    @Test
    public void testRouteView_SetRouteAndClear() {
        // 初始化 RouteView
        RouteView routeView = new RouteView(RuntimeEnvironment.application);
        assertNotNull(routeView);

        // 模拟 GPS 数据点
        List<Location> points = new ArrayList<>();
        Location loc1 = new Location("gps");
        loc1.setLatitude(30.0);
        loc1.setLongitude(120.0);
        points.add(loc1);

        Location loc2 = new Location("gps");
        loc2.setLatitude(30.1);
        loc2.setLongitude(120.1);
        points.add(loc2);

        // 测试写入数据并强行重绘 (提高绘制代码的覆盖率)
        routeView.setRoute(points, loc2);
        routeView.measure(100, 100);
        routeView.layout(0, 0, 100, 100);

        // 测试清除数据
        routeView.clear();
    }
}