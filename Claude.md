# Moveup 项目规则

## 项目概述
Moveup 是一款运动跑步软件，提供用户管理、运动数据追踪、社交互动、智能指导、挑战激励等功能。

## 技术栈

### 后端
- 运行时：Node.js (LTS)
- 框架：Express.js
- 语言：TypeScript
- 数据库：PostgreSQL (主库) + InfluxDB (时序数据库，可选)
- ORM/查询构建器：Knex.js
- 缓存：Redis (会话、排行榜)
- 对象存储：MinIO / 阿里云 OSS
- 消息队列：RabbitMQ (异步任务)
- 实时通信：WebSocket
- 认证：JWT

### 前端
- 平台：原生 Android
- 语言：Kotlin / Java
- 网络：Retrofit + OkHttp
- 数据持久化：Room / SharedPreferences
- 地图：高德地图 SDK / 谷歌地图 SDK
- 图表：MPAndroidChart

### 部署与运维
- 容器化：Docker + Docker Compose
- 反向代理：Nginx
- 进程管理：PM2
- CI/CD：GitHub Actions (可选)

## 目录结构

### 整体结构
```
Moveup/
├── backend/                # 后端代码
├── frontend/               # 前端代码（原生 Android）
├── docs/                   # 项目文档
│   ├── architecture.md     # 系统架构设计
│   ├── database.md         # 数据库设计（ER图）
│   ├── api.md              # API 接口文档
│   └── contributions/      # 个人贡献说明
├── .gitignore
├── README.md
└── CLAUDE.md               # AI 辅助开发规则（本文件）
```

### 后端目录结构
```
backend/
├── app/
│   ├── models/       # 数据模型
│   ├── routes/       # 路由/控制器
│   ├── services/     # 业务逻辑
│   └── utils/        # 工具函数
├── tests/            # 测试文件
├── Dockerfile
└── requirements.txt  # 或 package.json / go.mod

```

### 前端目录结构（Android）
```

```

## 代码规范

### 后端 (TypeScript)
- 使用 **ESLint** + **Prettier** 统一代码风格。
- 模块化设计：每个业务模块包含 controller、service、repository、model、types。
- 统一错误处理：使用自定义 `AppError` 类，返回 `{ code, message, data }` 结构。
- 参数校验：使用 Joi 或 class-validator 在 controller 层进行。
- 数据库操作：repository 层封装所有 SQL 查询，使用 Knex 查询构建器。
- 避免 `any` 类型，使用 `unknown` 或具体类型替代。
- 异步操作优先使用 `async/await`，避免回调。
- 敏感信息（如 JWT 密钥、数据库密码）通过环境变量注入，不硬编码。

### 前端 (Android)
- 遵循 **Kotlin 编码规范**，使用 Kotlin 优先。
- 架构模式：推荐 **MVVM** + **Repository** + **UseCase**。
- UI 组件：使用 **Jetpack Compose**（可选）或 **XML** 布局，避免硬编码尺寸。
- 依赖注入：使用 Hilt / Dagger 管理依赖。
- 网络请求：统一使用 Retrofit 封装，响应解析为 `Result<T>` 或 `Resource<T>` 类型。
- 线程：使用协程（Coroutines）处理异步任务。
- 命名：类名用 PascalCase，方法/变量用 camelCase，资源文件用小写加下划线。
- 禁止在 UI 线程执行耗时操作。

## 数据库规范
- 所有表使用 UUID 作为主键。
- 时间字段统一使用 `TIMESTAMPTZ`，默认 `now()`。
- 外键关联使用 `ON DELETE CASCADE` 或 `RESTRICT` 根据业务决定。
- 索引：为常用查询字段创建索引（如 `user_id`、`created_at`）。
- 迁移脚本：使用 Knex 管理，每个变更单独一个文件，包含 `up` 和 `down`。
- 种子数据：仅包含必要的初始数据（如默认徽章、会员套餐），不包含测试数据。

## 常用命令

### 后端
```bash
cd backend
npm run dev          # 启动开发服务器（热重载）
npm run build        # 编译 TypeScript
npm start            # 生产模式启动
npm run migrate      # 运行所有迁移
npm run migrate:rollback  # 回滚上一次迁移
npm run seed         # 运行种子
```

### 前端
```bash
# 使用 Android Studio 打开 frontend/ 目录，或通过命令行构建
./gradlew build      # 编译 APK
./gradlew installDebug  # 安装到设备
```

## 禁止事项
- **不要使用 `any` 类型**（后端 TypeScript）或 `Any`（前端 Kotlin）。
- **不要内联样式**（Android 需定义在 XML 或 Compose 主题中）。
- **不要直接操作 DOM**（后端无 DOM，前端 Android 无 DOM）。
- **不要修改配置文件**（如 `knexfile.js`、`gradle.properties`）除非明确要求。
- **不要提交敏感信息**（密码、密钥）到 Git 仓库。
- **不要忽略数据库迁移**（所有结构变更必须通过迁移文件）。
- **不要在 API 路径中硬编码版本号**（应通过路由前缀管理）。

## AI 辅助开发注意事项
- 在生成代码前，请先理解当前模块的业务逻辑和已有代码风格。
- 添加新表时，请同时生成对应的迁移脚本（`migrations/`）和可能需要的种子数据（`seeds/`）。
- 新增 API 端点时，请提供对应的请求/响应类型定义（`types.ts`）。
- 遵循现有的目录结构，将代码放在正确的模块下。
- 如果涉及敏感操作（如认证、支付），请标注安全注意事项。
- 生成的 SQL 语句必须符合 PostgreSQL 语法，并考虑性能（索引、查询优化）。

---

**最后更新**: 2026-03-25
**维护者**: 蔡燚翔
```