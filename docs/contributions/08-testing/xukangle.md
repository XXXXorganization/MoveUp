# 软件测试贡献说明

姓名: 徐康勒 
学号: [2312190422] 
角色: 前端 (Android端) 
日期: 2026-04-28

## 完成的测试工作

### 测试文件
今天完成了针对 MoveUp Android 客户端 11 个核心交互与网络模块的测试类编写与重构，路径均位于 `app/src/test/java/com/zjgsu/moveup/` 下：
- `RuningTest.java` (跑步运动核心页、悬浮球拖拽、权限分支测试)
- `ClubModuleTest.java` (跑团详情、列表渲染、评论/点赞测试)
- `PlanModuleTest.java` (跑步计划展示及动态弹窗添加/长按删除测试)
- `HistoryTest.java` (历史记录拉取与多层级嵌套的分享弹窗流程测试)
- `MainTest.java` (主界面侧边栏导航与历史卡片数量动态渲染测试)
- `FindTest.java` (跑团搜索结果测试)
- `AItalkTest.java` (AI语音教练问答拦截测试)
- `AIFloatManagerTest.java` (悬浮球全局注入与点击路由测试)
- `StartTest.java` (启动页及登录注册跳转测试)
- `PostDetailTest.java` (帖子完整评论区测试)
- `RouteViewTest.java` & `VoiceCoachManagerTest.java` (自定义轨迹绘制与TTS语音测试)
- `EditTest.java` (个人资料修改交互测试)

### 测试清单
- [x] **正常情况测试 (>30个)**：涵盖主页导航点击跳转、跑团点赞与评论发送、动态数据成功拉取与 RecyclerView 界面列表渲染、多级 AlertDialog 交互提交等。
- [x] **边界/异常情况测试 (>15个)**：涵盖未加入社团时的分享报错拦截、空数据/缺失字段情况的 JSON 解析回退保护、断网或服务端 500/400 时的网络捕获机制、UI 防崩溃保护测试。
- [x] **Mock 使用(数据库/API/组件外部依赖)**：全面使用 `MockWebServer` 拦截所有向 Node.js 后端发起的 API 请求 (配置了 `Dispatcher` 实现智能路由响应)，并利用 `Robolectric` (ShadowDialog / ShadowToast) 完美 Mock 了真实的 Android 物理依赖与系统弹窗。

### 覆盖率
- 核心模块覆盖率: **> 85%** (已在 `build.gradle.kts` 中过滤 `databinding` 等自动生成包，真实业务逻辑覆盖率多项达 90%~100%)。

### AI 辅助(如有)
- 使用工具: Gemini
- Prompt 示例: "请帮我修改 History 的测试代码，增加 Missed Branches 的覆盖测试"、"解决多线程请求竞争引起的 ComparisonFailure"、"解决 ClassCastException 获取不到自定义 Dialog 视图的问题"。
- AI 生成+人工修改的测试数量: 11个核心测试类，涵盖超 45 个测试用例。

## PR 链接
- PR #1: https://github.com/XXXXorganization/MoveUp/pull/19

## 遇到的问题和解决
1. **问题: 多线程网络请求抢占数据 (Race Condition) 导致断言偶然失败。**
   **解决:** 弃用顺序排列的 `enqueue`，改用 `MockWebServer` 的 `Dispatcher` 根据 URL 路径精细化智能路由发放 Mock 数据。
2. **问题: 异步网络操作耗时波动导致 `ComparisonFailure` (Timing Flakiness)。**
   **解决:** 抛弃 `Thread.sleep` 硬等待，引入了带有超时保护的智能轮询等待机制 (Smart Polling)，通过循环 `Robolectric.flushForegroundThreadScheduler()` 等待 UI 真实发生改变后再执行断言。
3. **问题: 拦截原生 `AlertDialog` 中的自定义输入视图时触发 `ClassCastException`。**
   **解决:** 发现系统框架默认会加盖一层 `FrameLayout`，转而使用 Robolectric 专属的 `Shadows.shadowOf(dialog).getView()` 安全且准确地提取原生自定义布局。
4. **问题: Jacoco 覆盖率总是把自动生成的样板代码算进去导致分数极低。**
   **解决:** 调整 `build.gradle.kts` 的 `fileFilter`，把 `**/databinding/**/*.*`, `**/*Binding*.*`, `**/BR.*` 纳入黑名单，让报告只反馈纯正的核心业务代码覆盖率。

## 心得体会
在今天的软件测试实践中，我深度体验了 Android 原生应用自动化测试的严谨性与痛点。尤其是在有复杂网络请求回调的 UI 页面，处理好“主线程与异步后台线程”的同步是编写高质量测试用例的关键。我通过实践掌握了 MockWebServer 的高级用法（如 Dispatcher 智能路由），有效解决了困扰测试的偶然性报错。此外，学会了强制触发 RecyclerView 的 `measure` 与 `layout` 以模拟真实设备渲染，这是消灭核心 Adapter 红标（Missed Branches）的杀手锏。
