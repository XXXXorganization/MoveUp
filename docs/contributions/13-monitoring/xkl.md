# 监控配置贡献说明

**姓名**：徐康勒 
**学号**：2312190422  
**日期**：2026-06-02  

---

## 我完成的工作

### 1. 日志配置
- [x] 结构化日志格式
- [x] 日志级别配置

**实现**：  
- 创建 `StructuredLogger` 类，输出 JSON 格式日志到 `app_log.json` 和 Logcat。  
- 日志字段包含 `time`、`level`、`module`、`message`。  
- 支持 INFO、DEBUG、WARN、ERROR 等级别。

### 2. 健康检查
- [x] `/health` 端点实现
- [x] 健康检查逻辑

**实现**：  
- 创建 `LocalHealthServer` 类（基于 `ServerSocket`，无第三方依赖）。  
- 在 `MoveUpApplication.onCreate()` 中启动 HTTP 服务器，监听 8080 端口。  
- 返回 JSON：`{"status":"ok","timestamp":xxx,"uptime":xxx}`。

### 3. 指标收集
- [x] 请求计数
- [x] 响应时间
- [x] 错误率

**实现**：  
- 创建 `MetricsCollector` 类，使用 `ConcurrentHashMap` 存储每个 API 的请求次数、总响应时间和错误次数。  
- 在所有网络请求方法中埋点：`recordRequestStart(url)` 和 `recordRequestEnd(url, success)`。  
- 每分钟输出一次汇总指标（结构化 JSON 格式）到日志文件/Logcat。  
- 修改了以下 15 个文件中的网络请求方法（Login、Register、AItalk、ClubCommunityActivity、clubterm、ClubTermPostAdapter、Find、History、Main、Mine、mine_edit、Plan、Plan_details、PostDetailActivity、Runing）。



## PR 链接
- PR #1: https://github.com/XXXXorganization/MoveUp/pull/66
---

## 遇到的问题和解决

1. **问题**：Sentry 依赖导致应用启动崩溃（DSN 无效）  
   **解决**：移除 Sentry（可选任务，不影响核心评分），保留日志、健康检查、指标收集。

2. **问题**：健康检查端点无法访问  
   **解决**：使用纯 `ServerSocket` 实现 HTTP 服务器，避免引入 `nanohttpd` 依赖，并在 `AndroidManifest.xml` 中注册 `MoveUpApplication`。

3. **问题**：多处网络请求代码重复，指标记录容易遗漏  
   **解决**：逐一审查每个 Activity 中的 `HttpURLConnection` 调用，统一添加 `MetricsCollector` 埋点模式（`start`、`success` 标志、`end`）。

---

## 心得体会

通过本次监控配置作业，我深入理解了可观测性的三个支柱：**结构化日志**、**健康检查** 和 **指标收集**。在 Android 应用中实现这些功能虽然不如服务端方便，但通过自定义 `LocalHealthServer` 和 `MetricsCollector`，仍然可以为前端应用建立基础的运维能力。今后我会将这些监控思想应用到更多项目中，提升代码的可维护性和故障排查效率。
