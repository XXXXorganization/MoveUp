# 徐康勒（xkl）MoveUp 项目综合贡献说明

# 「本文本由 AI 辅助生成，经人工审核修改」


**姓名**：徐康勒  
**学号**：2312190422  
**角色**：Android 前端开发（兼 Mock 后端、AI 集成、工程化）  
**项目**：MoveUp 跑步运动 App  
**汇总日期**：2026-06-09  

---

## 一、贡献总览

本人主要负责 MoveUp 项目的 **Android 原生客户端** 全链路开发，并承担 UI 设计、Mock 后端、AI 功能集成、单元测试、CI/CD（前端侧）、安全审查（前端侧）、Docker 前端分发、客户端可观测性等工作。以下按课程章节汇总各模块贡献。

| 章节 | 主题 | 完成度 | 贡献文档 |
|------|------|--------|---------|
| 02 | UI/UX 设计 | ✅ 完成 | `02-ui/xukangle.md` |
| 03 | 软件架构（前端） | ✅ 完成 | `03-architecture/xukangle.md` |
| 04 | API 设计与 Mock | ✅ 完成 | `04-api/xukangle.md` |
| 05 | 前端实现 | ✅ 完成 | `05-frontend/xkl.md` |
| 07 | AI 功能集成 | ✅ 完成 | `07-ai/xkl.md` |
| 08 | 软件测试 | ✅ 完成 | `08-testing/xukangle.md` |
| 09 | CI/CD | ✅ 完成 | `09-cicd/xkl.md` |
| 10 | 安全审查（前端） | ✅ 完成 | `10-security/xkl.md` |
| 11 | Docker 部署（前端） | ✅ 完成 | `11-docker/xkl.md` |
| 12 | 云服务部署 | ⚪ 未直接参与 | `12-cloud/xkl.md` |
| 13 | 可观测性与监控 | ✅ 完成 | `13-monitoring/xkl.md` |

---

## 二、各模块详细贡献

### 2.1 UI/UX 设计（2026-03-16）

**完成工作**：

1. **Figma 团队创建与管理**，建立项目设计协作空间。
2. **设计素材整理**：搜集运动类 App 设计风格，建立草稿与参考库。
3. **页面设计**（主导完成）：
   - 主界面、登录/注册、个人中心（Mine）
   - 历史记录、实时跑步追踪、社区（Club）、跑步计划（Plan）
   - 页面布局、元素排版、跳转逻辑可视化
   - Figma 中配置基础交互效果

**Figma 链接**：https://www.figma.com/design/IKpsxQMrrc4alOJIQWFdDe/Move-Up

**关键问题**：缺乏复杂自定义部件经验，设计效率低。  
**解决方案**：利用 Figma 社区部件库检索复用优质组件，微调适配项目风格。

---

### 2.2 软件架构设计——前端部分（2026-03-25）

**架构方案**：

- **单 Activity + 多 Fragment** 结构
- **技术栈**：Android Studio + Java + XML 布局
- **布局**：ConstraintLayout 自适应 + DrawerLayout 侧边导航

**完成工作**：

- [x] Android 原生 APP 整体架构设计
- [x] 登录、注册、主页、个人中心、编辑资料、跑步定位等页面代码
- [x] 页面跳转、按钮交互、菜单功能等核心流程
- [x] 项目初始化（包名、资源文件、JDK/SDK/模拟器配置）
- [x] 布局/图片/样式资源创建与配置

**PR**：#7 — https://github.com/XXXXorganization/MoveUp/pull/7

**典型问题与修复**：

| 问题 | 解决方案 |
|------|---------|
| double → float 类型不兼容，海拔数据无法获取 | 添加 `(float)` 强制类型转换 |
| 按钮背景颜色不生效 | 删除 `android:background`，保留 `backgroundTint` |
| ConstraintLayout 找不到同级 ID | 调整布局层级，规范约束关系 |

---

### 2.3 API 设计与 Mock 后端（2026-03-25）

**API 设计**：

- 统一响应结构 `{ code, message, data }`（StandardResponse）
- RESTful 资源设计：Auth、User、Runs、Friends/Feeds、Challenges/Badges
- 统一 `/v1` 版本前缀
- 分页与条件查询参数规范（page/size/start_date/end_date）

**Mock 后端实现**（Node.js / Express）：

- `server.js` & `index.js`：约 **27 个 Mock 路由**
- CORS 跨域 + JSON 解析
- 安全请求日志中间件（打印 Method/Path/Query/Body，规避 GET Body 解析错误）
- `standardResponse` 包装器保证返回结构一致

**前端 API 层**（早期 Mock 阶段）：

- `apiClient.ts`：fetch 封装、Query 拼接、Authorization 注入、FormData 上传
- `ApiError` 自定义错误类
- `apiMock.ts` 无缝 Mock 方案

**测试**：Apifox 测试集合覆盖登录、用户信息、开始跑步、上传轨迹、提交动态等主流程（27 个用例）。

**PR**：#10 — https://github.com/XXXXorganization/MoveUp/pull/10

---

### 2.4 前端核心实现（2026-04-14）

**技术栈**：Android Java 原生 + HttpURLConnection + 高德地图 SDK + Glide

#### 页面开发

| 模块 | 文件 |
|------|------|
| 认证 | `Login.java`, `Register.java`, `Start.java` |
| 主导航/首页 | `Main.java`, `MainActivity.java`, `FirstFragment.java`, `SecondFragment.java` |
| 跑步 | `Runing.java`（核心，GPS + 地图 + 后端同步） |
| 历史 | `History.java` |
| 计划 | `Plan.java`, `Plan_details.java` |
| 社团 | `Club.java`, `clubterm.java`, `ClubCommunityActivity.java`, `PostDetailActivity.java` |
| 发现 | `Find.java` |
| 个人中心 | `Mine.java`, `mine_edit.java` |
| AI 对话 | `AItalk.java`, `AIFloatManager.java` |

#### 组件/模块封装

1. **`RouteView`**：自定义 View，渲染跑者运动轨迹
2. **Adapter 系列**：`ClubAdapter`、`ClubTermPostAdapter`、`HistoryAdapter`、`PlanDetailAdapter` 等，封装 RecyclerView 长列表复用
3. **Mock 后端路由**：`index.js` / `server.js` 中 `standardResponse` + 日志中间件
4. **`VoiceCoachManager`**：TTS 语音教练播报

#### API 对接

- 用户认证：`/auth/login`, `/auth/register`
- 跑步：`/runs/start`, `/runs/:id/points`, `/runs/finish`, `/runs`
- 社团：`/clubs`, `/clubs/:id/posts`, 点赞/评论
- 计划：`/plan/details`, `/plan/toggle_complete`, `/plan/total_distance`
- AI：`/ai/chat`
- 线上 BaseURL：`https://moveup-v7mf.onrender.com/v1`

**PR**：#14 — https://github.com/XXXXorganization/MoveUp/pull/14



---

### 2.5 AI 功能集成（2026-04-21）

**功能类型**：智能跑步教练 + Agent 自动排训练计划

**使用模型**：Qwen/Qwen2.5-7B-Instruct（硅基流动 API）

**实现内容**：

| 层级 | 实现 |
|------|------|
| 后端 | `/v1/ai/chat` 接口；API Key 存服务端；注入历史跑步数据与周计划作为 Context；正则提取 `###PLAN:[...]###` 暗号并写入数据库 |
| 前端 | `AItalk.java` + `activity_aitalk.xml` 聊天气泡 UI；子线程 HTTP 请求 |
| 容错 | 前端网络异常拦截 + UI 等待态；后端 JSON 格式容错正则 + 15s 超时；System Prompt 角色限制 |

**PR**：#16 — https://github.com/XXXXorganization/MoveUp/pull/16

**核心收获**：从「纯聊天」升级为「AI Agent 工作流」—— Prompt 约束输出格式 + 后端正则解析 + 异常兜底，实现 AI 修改业务数据库的闭环。

---

### 2.6 软件测试（2026-04-28）

**角色**：Android 前端测试  
**框架**：Robolectric + MockWebServer + JaCoCo

#### 测试文件（11+ 个，路径 `app/src/test/java/com/zjgsu/moveup/`）

| 测试类 | 覆盖模块 |
|--------|---------|
| `RuningTest.java` | 跑步核心页、悬浮球拖拽、权限分支 |
| `RouteViewTest.java` | 自定义轨迹绘制 |
| `VoiceCoachManagerTest.java` | TTS 语音 |
| `ClubModuleTest.java` | 跑团详情、点赞/评论 |
| `PlanModuleTest.java` | 计划展示、弹窗增删 |
| `HistoryTest.java` | 历史记录、分享弹窗 |
| `MainTest.java` | 侧边栏导航、历史卡片 |
| `FindTest.java` | 跑团搜索 |
| `AItalkTest.java` | AI 问答拦截 |
| `AIFloatManagerTest.java` | 悬浮球注入与路由 |
| `StartTest.java` | 启动页、登录跳转 |
| `PostDetailActivity` / `EditTest` | 评论区、资料编辑 |

#### 测试统计

- 正常情况测试：**> 30 个**
- 边界/异常测试：**> 15 个**
- Mock 方案：`MockWebServer` Dispatcher 智能路由 + Robolectric ShadowDialog/Toast
- **核心模块覆盖率 > 75%**

#### AI 辅助测试

- 工具：Gemini
- AI 生成 + 人工修改：11 个测试类，45+ 用例

**PR**：#19 — https://github.com/XXXXorganization/MoveUp/pull/19


### 2.7 CI/CD 配置（2026-05-04）

**完成工作**：

- [x] 参与编写/审查 `.github/workflows/ci.yml`
- [x] 配置 Codecov 覆盖率上传（backend/frontend 双 flag）
- [x] 添加 README 状态徽章
- [x] 配置 Dependabot 自动更新依赖
- [x] 本地测试命令与 CI 一致：`./gradlew :app:testDebugUnitTest :app:jacocoTestReport`


**PR**：#27 — https://github.com/XXXXorganization/MoveUp/pull/27  
**CI 链接**：https://github.com/XXXXorganization/MoveUp/actions/workflows/ci.yml

---

### 2.8 安全审查——前端（2026-05-11）

**审查范围**：

- 认证：`Login.java`, `Register.java`
- 语音：`VoiceCoachManager.java`, `Runing.java`
- Mock 后端：`index.js`, `server.js`

**AI 发现并修复的问题**：

| 编号 | 问题 | 修复 |
|------|------|------|
| 1 | 登录页未处理 HTTP 429 限流 | `Login.java` 捕获 429/401/400，友好提示 |
| 2 | Android 8.0+ 音频焦点缺少 Listener 导致 Crash | 添加 `OnAudioFocusChangeListener` |
| 3 | 网络响应格式异常导致闪退 | `JSONObject` 构造请求 + try-catch 解析 |
| 4 | HttpURLConnection 未读 ErrorStream | 重构 `>= 400` 时读取 ErrorStream |
| 5 | AI Prompt 注入风险 | System Prompt 角色限制 + JSON 格式校验 |

**安全检查清单**：JWT 存储、接口鉴权、越权防护、JSON 注入防护、依赖扫描均已完成。

**PR**：#47 — https://github.com/XXXXorganization/MoveUp/pull/47

---

### 2.9 Docker 部署——前端（2026-05-19）

**架构亮点**：Android 原生 App 不适合容器内编译（镜像数 GB），采用 **Nginx APK 分发页** 替代传统 Web 前端部署。

**完成工作**：

| 类别 | 内容 |
|------|------|
| Dockerfile | 前端 Nginx 多阶段构建（`nginx-unprivileged:alpine`）；Mock 后端 Node.js 多阶段构建 |
| 安全 | 非 root 运行（Nginx UID 101，Node `node` 用户） |
| Compose | `compose.prod.yaml`；Healthcheck（curl/wget）；内存限制 256M |
| 部署 | `docker compose -f compose.prod.yaml up -d --build` 一键拉起，服务 `Up (healthy)` |

**PR**：#64 — https://github.com/XXXXorganization/MoveUp/pull/64


### 2.10 云服务部署（2026-06-02）

**说明**：作为 Android 前端开发者，本次**未直接参与** Render/Railway 等云平台配置（Android 应用通过 APK/应用商店分发，不属于 Web 云端部署范畴）。

**PR**：#66 — https://github.com/XXXXorganization/MoveUp/pull/66

---

### 2.11 可观测性与监控——客户端（2026-06-02）

#### 1. 结构化日志（`StructuredLogger.java`）

- JSON 格式输出到 `app_log.json` 和 Logcat
- 字段：`time`, `level`, `module`, `message`
- 级别：INFO / DEBUG / WARN / ERROR

#### 2. 健康检查（`LocalHealthServer.java`）

- 基于 `ServerSocket`，无第三方依赖
- `MoveUpApplication.onCreate()` 启动，监听 **8080** 端口
- 响应：`{"status":"ok","timestamp":xxx,"uptime":xxx}`

#### 3. 指标收集（`MetricsCollector.java`）

- `ConcurrentHashMap` 存储各 API 请求次数、总响应时间、错误次数
- 埋点模式：`recordRequestStart(url)` → `recordRequestEnd(url, success)`
- 每分钟 JSON 汇总输出
- 覆盖 **15 个文件**：Login、Register、AItalk、Club 系列、Find、History、Main、Mine、Plan、Runing 等

**PR**：#66 — https://github.com/XXXXorganization/MoveUp/pull/66

**典型问题**：

1. Sentry DSN 无效导致崩溃 → 移除 Sentry，保留日志/健康检查/指标
2. 健康检查端点无法访问 → 纯 ServerSocket 实现 + 注册 `MoveUpApplication`
3. 网络请求埋点遗漏 → 逐一审查 15 个 Activity 的 HttpURLConnection 调用

---

## 三、PR 汇总

| PR | 主题 | 链接 |
|----|------|------|
| #7 | 前端架构与页面初版 | https://github.com/XXXXorganization/MoveUp/pull/7 |
| #10 | API 设计与 Mock 后端 | https://github.com/XXXXorganization/MoveUp/pull/10 |
| #14 | Android 前端核心实现 | https://github.com/XXXXorganization/MoveUp/pull/14 |
| #16 | AI 语音助手集成 | https://github.com/XXXXorganization/MoveUp/pull/16 |
| #19 | Android 单元测试 | https://github.com/XXXXorganization/MoveUp/pull/19 |
| #27 | CI/CD 与 Codecov | https://github.com/XXXXorganization/MoveUp/pull/27 |
| #47 | 前端安全审查修复 | https://github.com/XXXXorganization/MoveUp/pull/47 |
| #64 | Docker 前端/APK 分发 | https://github.com/XXXXorganization/MoveUp/pull/64 |
| #66 | 客户端可观测性 | https://github.com/XXXXorganization/MoveUp/pull/66 |

---

## 四、核心代码产出清单

### Android 源文件（`frontend/code/app/src/main/java/com/zjgsu/moveup/`）

**Activity / 页面**：MainActivity, Main, Start, Login, Register, Runing, History, Plan, Plan_details, Club, clubterm, ClubCommunityActivity, PostDetailActivity, Find, Mine, mine_edit, AItalk, Log

**组件 / 工具**：RouteView, VoiceCoachManager, AIFloatManager, StructuredLogger, LocalHealthServer, MetricsCollector, MoveUpApplication

**Adapter / 实体**：ClubAdapter, ClubTermPostAdapter, HistoryAdapter, PlanDetailAdapter, ClubTermPost, ClubComment, HistoryRun, PlanDetailItem

**测试**（`app/src/test/`）：11+ 测试类，45+ 用例

### 工程化配置

- `.github/workflows/ci.yml`（前端测试任务）
- `codecov.yml`（前端覆盖率路径映射）
- `frontend/code/compose.prod.yaml`（Docker Compose）
- `frontend/code` Dockerfile（Nginx APK 分发）

---

## 五、问题与解决——综合归纳

| 类别 | 代表性问题 | 解决思路 |
|------|-----------|---------|
| UI/布局 | ConstraintLayout 约束错误、按钮颜色不生效 | 规范布局层级；使用 backgroundTint |
| 网络联调 | 模拟器无法访问 localhost | BaseURL 改用 10.0.2.2 |
| 列表性能 | 社团动态复杂层级列表 | Adapter 抽象 + Mock 响应扁平化 |
| 跑步模块 | GPS/地图/后端多状态并发 | Runing + RouteView 模块化；Mock 高频坐标测试 |
| AI 集成 | JSON 格式不可控 | 后端正则容错 + 超时 + System Prompt |
| 单元测试 | Race Condition / Flaky 测试 | MockWebServer Dispatcher + Smart Polling |
| CI/CD | Codecov 路径不匹配 / Jacoco 任务缺失 | codecov.yml + build.gradle.kts 补全 |
| 安全 | 429 未处理 / 音频焦点 Crash | ErrorStream 读取 + AudioFocus Listener |
| Docker | 权限错误 / 镜像拉取超时 | chown 非 root + 国内镜像预拉取 |
| 监控 | Sentry 崩溃 / 埋点遗漏 | 移除 Sentry；15 文件统一 MetricsCollector 模式 |

---

## 六、技术收获与心得

### 6.1 移动开发

- 掌握 Android 原生开发全流程：XML 布局、Activity 生命周期、DrawerLayout 导航、RecyclerView 复用、自定义 View（RouteView）
- 熟练集成高德地图 SDK 实现 GPS 轨迹记录与 Polyline 绘制
- 理解 HttpURLConnection 网络层封装、JWT 存储、子线程 + Handler 异步 UI 更新

### 6.2 全栈协作

- 自建 Node.js Mock 后端（27 路由），实现前端独立闭环开发
- RESTful API 规范设计（StandardResponse），降低前后端联调成本
- Android 兼容层对接正式后端（`/runs/*`, `/plan/*`, `/ai/chat`）

### 6.3 AI 工程化

- AI Agent 工作流：Prompt 格式约束 → 后端正则解析 → 数据库写入
- Vibe Debugging：用 AI 辅助排查 Docker 网络、音频焦点 Crash 等疑难问题
- 安全审查二次 Prompt：AI 生成代码必须经安全视角复审

### 6.4 工程化与质量

- Robolectric + MockWebServer 编写 45+ 单元测试，核心覆盖率 > 85%
- GitHub Actions CI/CD 双端流水线 + Codecov 徽章
- 客户端可观测性三支柱：StructuredLogger + LocalHealthServer + MetricsCollector
- Docker 非 root 安全部署 + APK 分发架构设计

### 6.5 总体感悟

MoveUp 项目中，我不局限于「画界面写页面」，而是主动承担 Mock 后端、AI 集成、测试、CI/CD、安全、Docker、监控等工程化工作，形成了 **设计 → 开发 → 测试 → 部署 → 运维** 的完整移动开发闭环。最大的体会是：**规范先行、测试保障、安全兜底**——AI 可以加速开发，但边界条件、异常处理和安全防护必须人工把关。

---

## 七、Git 协作数据

| 成员标识 | 提交次数（约） |
|---------|--------------|
| XuKangle | 36 |


