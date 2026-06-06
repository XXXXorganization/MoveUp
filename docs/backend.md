# MoveUp 后端文档

> 最后更新: 2026-06-03 | 维护者: 蔡燚翔

## 1. 技术栈

| 层级 | 技术 |
|---|---|
| 运行时 | Node.js 18 + TypeScript |
| 框架 | Express.js |
| 数据库 | PostgreSQL 15（主库，29 张表） |
| 查询构建器 | Knex.js（迁移 + 种子） |
| 认证 | JWT（Bearer Token，有效期 2 小时） |
| AI | DeepSeek（数据分析）+ 通义千问 Qwen 2.5 7B（语音助手） |
| 对象存储 | MinIO（S3 兼容） |
| 缓存 | Redis 7 |
| 反向代理 | Nginx |
| 部署 | Docker + Docker Compose |
| 测试 | Jest + Supertest |

## 2. 启动方式

```bash
# 本地开发
cd backend && npm run dev

# Docker 生产部署
cd backend/docker
docker compose --env-file .env.prod up -d

# 查看状态
docker compose ps
curl http://localhost:3000/health
```

## 3. 环境变量

| 变量 | 说明 | 默认值 |
|---|---|---|
| `DATABASE_URL` | PostgreSQL 连接字符串 | - |
| `DB_HOST` / `DB_PORT` / `DB_USER` / `DB_PASSWORD` / `DB_NAME` | 数据库连接（无 DATABASE_URL 时） | postgres:5432 |
| `JWT_SECRET` | JWT 签名密钥 | 必填 |
| `JWT_EXPIRES_IN` | Token 有效期（秒） | 7200 |
| `DEEPSEEK_API_KEY` | DeepSeek API 密钥 | - |
| `DEEPSEEK_BASE_URL` | DeepSeek API 地址 | https://api.deepseek.com |
| `QWEN_API_KEY` | 通义千问 API 密钥 | - |
| `QWEN_BASE_URL` | SiliconFlow API 地址 | https://api.siliconflow.cn |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Redis 连接 | - |
| `MINIO_ENDPOINT` / `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` | MinIO 配置 | - |

## 4. 认证机制

- 所有 API（除 `/auth/*`、`/health`、`/calculate-calories`）需要在 Header 中携带 `Authorization: Bearer <token>`
- 登录接口返回 `{ token, expires_in, user: { id, nickname, avatar } }`
- Token 从 JWT payload 中提取 `userId`，注入到 `req.user`
- 过期/无效 Token 返回 401
- 密码登录使用 bcrypt 哈希存储

## 5. 标准响应格式

```json
// 成功
{ "code": 200, "message": "success", "data": { ... } }

// 业务错误
{ "code": 400, "message": "错误描述", "data": null }

// 认证错误
{ "code": 401, "message": "访问令牌缺失", "data": null }
```

## 6. 完整 API 列表

> 全部挂载在 `/v1` 前缀下，共 57 个端点

### 6.1 用户模块 (`routes/user.ts`)

| 方法 | 路径 | Auth | 说明 | 请求体 |
|---|---|---|---|---|
| POST | `/auth/code` | 否 | 发送验证码 | `{ phone, type }` |
| POST | `/auth/register` | 否 | 密码注册 | `{ phone, username, password }` |
| POST | `/auth/login` | 否 | 登录（验证码/密码双模式） | `{ phone, code }` |
| GET | `/user/profile` | 是 | 获取用户资料 | - |
| PUT | `/user/profile` | 是 | 更新用户资料 | `{ nickname, gender, height, weight, ... }` |

### 6.2 运动模块 (`routes/sport.ts`)

| 方法 | 路径 | Auth | 说明 |
|---|---|---|---|
| POST | `/sport/start` | 是 | 开始运动记录 |
| GET | `/sport` | 是 | 获取运动记录列表 |
| GET | `/sport/stats` | 是 | 运动统计 |
| GET | `/sport/:id` | 是 | 获取单条记录 |
| PUT | `/sport/:id/update` | 是 | 实时更新（距离/时长/心率） |
| PUT | `/sport/:id/stop` | 是 | 结束运动（计算最终统计） |
| GET | `/sport/:id/realtime` | 是 | 获取实时数据 |
| GET | `/sport/:id/pace-segments` | 是 | 分段配速分析 |
| POST | `/sport/:id/gps/batch` | 是 | 批量上传 GPS 轨迹点 |
| GET | `/sport/:id/gps` | 是 | 获取 GPS 轨迹 |
| POST | `/sport/:id/heart-rate/batch` | 是 | 批量上传心率数据 |
| GET | `/sport/:id/heart-rate` | 是 | 获取心率数据 |
| POST | `/calculate-calories` | 否 | 计算卡路里 |

### 6.3 社团模块 (`routes/club.ts`) — 新建

| 方法 | 路径 | Auth | 说明 |
|---|---|---|---|
| GET | `/clubs` | 是 | 社团列表（含 is_member + member_count） |
| GET | `/clubs/:id` | 是 | 社团详情 |
| POST | `/clubs/:id/toggle` | 是 | 加入/退出 → `{ joined }` |
| GET | `/user/clubs` | 是 | 我加入的社团 |
| GET | `/clubs/:id/posts` | 是 | 社团动态（含 author 对象 + run_summary） |
| POST | `/clubs/:id/posts` | 是 | 发布动态 `{ content, run_id, images }` |
| POST | `/posts/:id/like` | 是 | 点赞切换 → `{ is_liked, like_count }` |
| POST | `/posts/:id/comment` | 是 | 评论 `{ content, reply_to_id }` |
| GET | `/posts/:id/comments` | 是 | 评论列表（含 author 对象） |

### 6.4 社交模块 (`routes/social.ts`)

| 方法 | 路径 | Auth | 说明 |
|---|---|---|---|
| GET | `/social/users/search` | 是 | 搜索用户 |
| GET | `/social/friends` | 是 | 好友列表 |
| GET | `/social/friends/requests` | 是 | 待处理好友请求 |
| POST | `/social/friends/request` | 是 | 发送好友请求 |
| PUT | `/social/friends/respond` | 是 | 响应好友请求 |
| DELETE | `/social/friends/:id` | 是 | 删除好友 |
| GET | `/social/posts` | 是 | 社区动态流 |
| POST | `/social/posts` | 是 | 发布动态 |
| POST | `/social/posts/:id/like` | 是 | 点赞 |
| POST | `/social/posts/:id/comments` | 是 | 评论 |
| GET | `/social/posts/:id/comments` | 是 | 评论列表 |
| GET | `/social/leaderboard` | 是 | 排行榜 |

### 6.5 训练指导模块 (`routes/coaching.ts`)

| 方法 | 路径 | Auth | 说明 |
|---|---|---|---|
| GET | `/coaching/plans` | 是 | 训练计划列表 |
| POST | `/coaching/plans/recommend` | 是 | AI 推荐计划 |
| POST | `/coaching/plans/:id/adopt` | 是 | 采纳计划 |
| GET | `/coaching/my-plan` | 是 | 我的当前计划 |
| GET | `/coaching/today-task` | 是 | 今日任务 |
| PUT | `/coaching/my-plan/:id/progress` | 是 | 更新进度 |
| DELETE | `/coaching/my-plan/:id` | 是 | 退出计划 |
| POST | `/coaching/voice-guidance` | 是 | 语音指导 |
| POST | `/coaching/segment-advice` | 是 | 分段建议 |
| GET | `/coaching/stretching` | 是 | 拉伸指南 |
| GET | `/coaching/injury-prevention` | 是 | 伤病预防 |

### 6.6 挑战激励模块 (`routes/challenge.ts`)

| 方法 | 路径 | Auth | 说明 |
|---|---|---|---|
| GET | `/challenge/tasks` | 是 | 所有任务 |
| GET | `/challenge/tasks/my` | 是 | 我的任务 |
| PUT | `/challenge/tasks/progress` | 是 | 更新任务进度 |
| GET | `/challenge/badges` | 是 | 所有徽章 |
| GET | `/challenge/badges/my` | 是 | 我的成就 |
| GET | `/challenge/challenges` | 是 | 挑战列表 |
| POST | `/challenge/challenges/:id/join` | 是 | 加入挑战 |
| GET | `/challenge/challenges/:id/ranking` | 是 | 挑战排行 |
| GET | `/challenge/membership/plans` | 是 | 会员套餐 |
| GET | `/challenge/membership/my` | 是 | 我的会员 |
| POST | `/challenge/membership/purchase` | 是 | 购买会员 |
| GET | `/challenge/points` | 是 | 积分汇总 |
| GET | `/challenge/points/logs` | 是 | 积分日志 |

### 6.7 AI 模块 (`routes/ai.ts`)

| 方法 | 路径 | Auth | 说明 |
|---|---|---|---|
| POST | `/ai/sport-summary` | 是 | 单次运动总结（DeepSeek） |
| POST | `/ai/history-summary` | 是 | 历史运动总结（DeepSeek） |

### 6.8 Android 兼容路由 (`routes/compatibility.ts`) — 新建

将旧 Android 客户端 API 映射到新后端服务。

| 方法 | 路径 | Auth | 说明 |
|---|---|---|---|
| POST | `/runs/start` | 是 | → sport.startSportRecord |
| POST | `/runs/:id/points` | 是 | 字段映射 `lat→latitude, lng→longitude, points→gpsPoints` |
| POST | `/runs/finish` | 是 | → sport.stopSportRecord（自动找活跃记录） |
| GET | `/runs` | 是 | → sport.getSportRecordsByUserId（Android 格式输出） |
| GET | `/plan/total_distance` | 是 | 本周实际跑步总里程（从 sport_records 计算） |
| GET | `/plan/details?day=MONDAY` | 是 | 某天训练计划列表 |
| POST | `/plan/details` | 是 | 添加计划项 `{ day, start_time, end_time, distance }` |
| POST | `/plan/details/delete` | 是 | 删除计划项 `{ day, index }` |
| PUT | `/plan/toggle_complete` | 是 | 切换完成状态 `{ day, index }` → `{ is_completed }` |
| POST | `/ai/chat` | 是 | AI 语音助手（通义千问，含 System Prompt） |

## 7. 数据库

共 **29 张表**（含 Knex 迁移记录表 `knex_migrations`、`knex_migrations_lock`）。

### 核心业务表

#### users — 用户
| 列 | 类型 | 约束 |
|---|---|---|
| id | UUID | PK |
| phone | varchar(20) | NOT NULL, UNIQUE |
| nickname | varchar(50) | NOT NULL |
| password_hash | varchar(255) | bcrypt |
| avatar | varchar(255) | 头像 URL |
| gender | smallint | 0未知/1男/2女 |
| height | smallint | cm |
| weight | decimal(5,2) | kg |
| target_distance | integer | 目标距离(米) |
| target_time | integer | 目标时间(分钟) |
| role | varchar(20) | user/admin |
| created_at / updated_at | timestamp | 自动 |

#### sport_records — 运动记录
| 列 | 类型 | 约束 |
|---|---|---|
| id | UUID | PK |
| user_id | UUID | FK→users |
| start_time | timestamp | NOT NULL |
| end_time | timestamp | 结束时填充 |
| distance | decimal(12,2) | 米（GPS 计算） |
| duration | decimal(10,2) | 秒 |
| calories | decimal(8,2) | kcal |
| average_pace | decimal(10,2) | 秒/公里 |
| max_heart_rate | decimal(6,2) | BPM |
| average_heart_rate | decimal(6,2) | BPM |
| status | varchar | active / completed |
| 索引 | (user_id, start_time) | |

#### gps_points — GPS 轨迹点
| 列 | 类型 | 约束 |
|---|---|---|
| record_id | UUID | FK→sport_records |
| latitude | decimal(10,8) | NOT NULL |
| longitude | decimal(11,8) | NOT NULL |
| timestamp | timestamp | NOT NULL |
| speed | decimal(5,2) | m/s |
| altitude | decimal(7,1) | 米 |

### 社团表（5 张，新建）

| 表 | 用途 | 关键字段 |
|---|---|---|
| clubs | 社团 | name, location, image_url, flag |
| club_members | 成员 | club_id, user_id, role（唯一） |
| club_posts | 动态 | club_id, user_id, content, run_id, images(JSONB) |
| club_comments | 评论 | post_id, user_id, content, reply_to_id |
| club_post_likes | 点赞 | post_id, user_id（唯一） |

### 计划表

| 表 | 用途 | 关键字段 |
|---|---|---|
| user_plan_items | 训练计划 | user_id, day_of_week, start_time, distance_km, is_completed |

### 其他表

| 表 | 用途 |
|---|---|
| friendships | 好友关系（复合 PK: user_id + friend_id） |
| posts / comments / likes | 社交动态系统 |
| badges / user_achievements | 徽章成就系统 |
| challenges / user_challenges | 挑战系统 |
| training_plans / user_plans | 训练计划系统 |
| membership_plans / user_memberships | 会员系统 |
| tasks / user_tasks | 每日任务 |
| points_logs | 积分日志 |
| routes | 跑步路线 |
| user_devices | 登录设备 |
| heart_rates | 心率数据 |

## 8. 目录结构

```
backend/
├── src/
│   ├── app.ts                  # Express 应用入口 + 依赖注入
│   ├── server.ts               # HTTP 服务器
│   ├── config/database.ts      # Knex DB 连接
│   ├── middleware/
│   │   ├── auth.ts             # JWT 认证中间件
│   │   ├── errorHandler.ts     # 全局错误处理
│   │   └── rateLimiter.ts      # 速率限制
│   ├── modules/
│   │   ├── user/               # 用户（controller/service/repository/model/types）
│   │   ├── sport/              # 运动
│   │   ├── social/             # 社交
│   │   ├── coaching/           # 训练指导
│   │   ├── challenge/          # 挑战激励
│   │   ├── ai/                 # AI 分析
│   │   └── club/               # 社团（新建）
│   ├── routes/
│   │   ├── user.ts / sport.ts / social.ts / coaching.ts
│   │   ├── challenge.ts / ai.ts / club.ts
│   │   └── compatibility.ts    # Android 兼容路由（新建）
│   └── utils/
│       ├── errors.ts           # AppError 类
│       └── llm.ts              # LLM 客户端（DeepSeek + Qwen）
├── migrations/                 # 16 个迁移脚本
├── tests/                      # 17 个测试套件
├── docker/
│   ├── docker-compose.yml      # 5 服务编排
│   ├── .env.prod               # 生产环境变量
│   └── nginx/nginx.conf        # 反向代理配置
└── Dockerfile                  # 多阶段构建
```

## 9. AI 模块

| 功能 | 模型 | API 端点 |
|---|---|---|
| 运动数据总结 | DeepSeek `deepseek-chat` | https://api.deepseek.com |
| 语音助手 | Qwen `Qwen2.5-7B-Instruct` | https://api.siliconflow.cn |

**语音助手 System Prompt**：
> 你是 MoveUp 跑步语音助理教练。根据用户的跑步数据（距离、配速、卡路里、位置）给出个性化指导。搜索周围1公里内的景点/地标，告诉用户距离多远、跑步过去要几分钟。用口语化中文回复，每次控制在60字以内。

## 10. 架构说明

- **分层架构**：Controller → Service → Repository → Model（每组一个模块）
- **依赖注入**：`app.ts` 中手动组装，无 IOC 容器
- **兼容层**：`compatibility.ts` 提供旧 Android 端 API 路径映射，待前端重构后可移除
- **迁移自动执行**：`server.ts` 启动时自动调用 `db.migrate.latest()`
- **测试覆盖**：17 个测试套件，304+ 测试用例，覆盖率 >60%
