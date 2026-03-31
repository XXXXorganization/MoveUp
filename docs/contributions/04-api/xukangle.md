# API 设计与实现贡献说明
姓名： 徐康勒
学号： 2312190422
日期： 2026-3-25

## 我完成的工作
### 1. API 设计
统一响应规范： 设计并落实了标准的泛型响应结构 StandardResponse<T>，所有接口统一返回 { code, message, data } 格式，极大降低了前后端联调的沟通成本。

业务资源 RESTful API： 完整定义了包含认证（Auth）、用户（User）、跑步记录（Runs）、社交（Friends/Feeds）、游戏化（Challenges/Badges）在内的核心资源接口，并统一增加了 /v1 版本前缀。

查询与数据上报设计： 规范了分页与条件查询参数（如 page/size/start_date/end_date）。

### 2. 文档编写与类型定义
MoveUp\frontend\src\api下编写设计前端引用的apiClient.ts与apiMock.ts文档
MoveUp\backend\app\routes下编写设计index.js与server.js文档

### 3. 前端实现
底层 HTTP 客户端封装（apiClient.ts）： * 基于 fetch 封装了轻量级的请求类，支持自动拼接 Query 参数、自动注入 Authorization Header。

针对图片/头像上传需求，特殊处理了 FormData（避免手动设置 Content-Type 导致 boundary 丢失的问题）。

封装了自定义错误类 ApiError，将 HTTP 状态码与业务 code 统一抛出，方便上层组件进行精细化的错误捕获与 UI 提示。

### 4. 后端实现
Mock 服务端搭建（server.js & index.js）： * 基于 Express 搭建了轻量级的 Mock Server，配置了 CORS 跨域支持和 JSON 解析。

开发增强增强日志中间件： 编写了安全的请求日志拦截器，能够清晰地在终端打印出请求的方法、路径、Query 参数以及 Body 数据（并巧妙避开了 GET 请求携带 Body 造成的解析错误），为联调提供了极大的便利。

实现了约 27 个核心 Mock 路由，并使用 standardResponse 包装器确保返回数据结构的严谨性。

### 5. 测试
Apifox 测试集合： 已建立基础接口集合并覆盖主流程（登录、获取用户信息、开始跑步、上传轨迹、提交动态等）。

后端联调测试： 通过 Node 终端的日志记录，成功验证了前端请求（包括账号密码登录、Token 携带）能够正确到达后端并被解析。

测试用例数量： 27个（覆盖了 index.js 中定义的所有路由接口）。

PR 链接
PR #X: https://github.com/xxx/xxx/pull/X (请替换为实际链接)

遇到的问题和解决
问题：安卓模拟器无法访问本机后端（localhost 无法直连）

解决： 查阅文档后明确了 Android 模拟器的网络隔离机制，将前端默认请求 BaseURL 抽离为常量 DEFAULT_BASE_URL，并在注释中注明将地址更改为 http://10.0.2.2:3000 即可顺利联调。


心得体会
本次任务最大的收获是：“规范先行，体验至上”。通过 OpenAPI 和 TypeScript 类型双向约束，前后端在字段、错误码、返回结构上能快速达成一致，避免了传统的“口头约定”带来的扯皮，显著降低了联调成本。

同时，我意识到一个良好的工程结构不仅是为了实现功能，更是为了提升开发体验。例如我编写的后端日志中间件，让每次请求的数据一目了然；前端准备的无缝 Mock 方案（apiMock.ts），把“等待后端开发”从阻塞项变成了可并行项。后续我会继续补齐重要模块的自动化测试用例，进一步提升 API 的质量与可维护性。