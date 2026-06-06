package com.zjgsu.moveup;

import android.app.Application;

public class MoveUpApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // 启动本地健康检查 HTTP 服务器
        LocalHealthServer.startServer();
        // 初始化结构化日志
        StructuredLogger.init(this);
        // 初始化指标收集器
        MetricsCollector.init(this);
        // 可选：初始化 Sentry 错误追踪
        // SentryHelper.init(this);
    }

    @Override
    public void onTerminate() {
        LocalHealthServer.stopServer();
        super.onTerminate();
    }
}