# 前端开发贡献说明

姓名：xkl（请替换为真实姓名）
学号：[请填写学号]
技术栈：Android、Node.js (Mock后端)
日期：2026-04-14

## 我完成的工作

### 页面开发
- [x] 登录/注册页面（`Login.java`, `Register.java`）
- [x] 首页/列表页面（`Main.java`, `MainActivity.java`, `History.java`, `Club.java`, `clubterm.java`）
- [x] 详情页面（`PostDetailActivity.java`, `Plan_details.java`）
- [x] 个人中心（`Mine.java`, `mine_edit.java`）
- [x] 其他：运动记录与跑步页面 (`Runing.java`, `Start.java`)，发现组件 (`Find.java`)

### 组件/模块封装
- 组件 1：`RouteView`（自定义 View 绘制组件，核心用于渲染和勾勒跑者的运动轨迹路线）
- 组件 2：各类 `Adapter` 列表组件（如 `ClubAdapter`、`ClubTermPostAdapter`、`HistoryAdapter`、`PlanDetailAdapter`，高度封装了不同场景下长列表的 UI 呈现及复用机制）
- 组件 3：Mock 后端请求路由模块（在 `index.js` 与 `server.js` 中抽象了 `standardResponse`，统一封装了数十个 API 的响应结构和日志中间件）

### API 对接
- [x] 封装网络请求层：基于项目约定的 HTTP 客户端，封装了统一请求与回调
- [x] 对接后端接口（接口名称：包含了用户认证体系 `/auth/login`, `/auth/register`；俱乐部与社区 `/clubs/**/*.posts`；跑步详情与运动计划 `/plan/*`, `/runs` 等，编写并联调了自己负责的 Mock 后端）
- [x] 处理加载状态和错误：完善了网络请求的异常回调机制与防抖，优化 Mock 数据的加载体验

## PR 链接
- PR #X: https://github.com/XXXXorganization/MoveUp/pull/14
## 遇到的问题和解决

1. **问题**：在跑步社交动态页（社区/俱乐部等）中，需要承载带有图文、多级评论和点赞的复杂层级列表，维护难度高、UI更新容易冲突。
   **解决**：抽取抽象了多维度的 `Adapter`（如 `ClubTermPostAdapter`）及相应的实体类（`ClubTermPost.java`, `ClubComment.java`），并在我负责的 Mock 引擎中将 API 响应结构扁平标准化，既方便了前端数据解析也提升了渲染性能。

2. **问题**：跑步状态及运动轨迹需要处理多种组件（如地图状态、开始/暂停控制区数据计算），状态经常难以统一。
   **解决**：将这部分逻辑抽离至独立的 `Runing.java` 和 `RouteView.java` 中，针对特定试图模块化；并通过 Node Mock 接口进行高频坐标点下发测试，排查了多种并发更新界面的 Bug。

## 心得体会

在本次 MoveUp 运动社交 App 开发项目中，我不局限于单纯的 Android 客户端界面还原，而是深入探究了全栈式开发的流程。针对要求，我不仅完成了登录注册、个人中心及大量社交与计划列表等多个核心场景的前端开发，还主动使用 Node.js / Express 技术栈自建了一套高完整度的本地 Mock 系统（包含基础的模拟数据库与复杂的接口路由关系），以此实现了高效的独立闭环开发。

这次经历让我对前后端通信（RESTful 标准与 JSON 处理）有了极大的把握提升，同时也切实锻炼了我在 Android 复杂 UI 构建（主要是各类列表复用和自定义绘图类 RouteView）及性能排错上的实战能力。总的来说，这是一次挑战与收获并存的优质沉淀。
