# MoveUp

[![CI](https://github.com/XXXXorganization/MoveUp/actions/workflows/ci.yml/badge.svg)](https://github.com/XXXXorganization/MoveUp/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/XXXXorganization/MoveUp/graph/badge.svg?token=VVQXTMZQTE)](https://codecov.io/gh/XXXXorganization/MoveUp)
[![Backend](https://codecov.io/gh/XXXXorganization/MoveUp/graph/badge.svg?flag=backend)](https://codecov.io/gh/XXXXorganization/MoveUp)
[![Frontend](https://codecov.io/gh/XXXXorganization/MoveUp/graph/badge.svg?flag=frontend)](https://codecov.io/gh/XXXXorganization/MoveUp)

一款面向跑步爱好者的移动端运动软件，提供实时运动追踪、AI 语音教练、社团社交和游戏化挑战激励。

## 团队成员

| 姓名 | 学号 | 分工 |
|---|---|---|
| 徐康勒 | 2312190422 | Android 前端 |
| 蔡燚翔 | 2312190426 | 后端架构与数据库 |

## 项目简介

MoveUp 专为跑步初学者和进阶爱好者设计，通过游戏化激励与科学化指导降低跑步门槛。

核心功能：
- **实时运动追踪**：GPS 轨迹记录、配速/距离/卡路里实时计算、心率监测
- **AI 语音教练**：跑步过程中实时播报配速和位置，结合周围景点给予鼓励和导航
- **社团社交**：加入跑团、发动态、点赞评论、分享跑步记录
- **训练计划**：按天制定跑步任务，打勾标记完成，展示每周实际里程

## 技术栈

### 后端

| 层级 | 技术 |
|---|---|
| 运行时 | Node.js 18 + TypeScript |
| Web 框架 | Express.js |
| 数据库 | PostgreSQL 15 |
| ORM / 迁移 | Knex.js |
| 认证 | JWT（Bearer Token） |
| AI | DeepSeek（运动数据分析）+ 通义千问 Qwen 2.5 7B（语音助手） |
| 对象存储 | 无（图片使用 PostgreSQL JSONB 字段存储） |
| 测试 | Jest + Supertest（300+ 用例） |
| CI/CD | GitHub Actions（自动测试 + 安全扫描 + Docker 镜像推送） |
| 容器化 | Docker + Docker Compose |
| 云端部署 | Render（PostgreSQL + Web Service） |

### 前端

| 层级 | 技术 |
|---|---|
| 平台 | Android 原生 |
| 语言 | Java |
| UI | XML 布局（ConstraintLayout / DrawerLayout） |
| 地图 | 高德地图 SDK |
| 网络 | HttpURLConnection |
| 图片加载 | Glide |
| 测试 | Robolectric + MockWebServer |

### 数据库

- **PostgreSQL** 29 张表：用户、运动记录、GPS 轨迹、社团动态、训练计划等
- **Knex 迁移脚本** 16 个：版本化数据库结构变更

## 快速开始

### 后端

```bash
cd backend/docker
docker compose --env-file .env.prod up -d
# 访问 http://localhost:3000/health
```

### 前端

用 Android Studio 打开 `frontend/code` 目录，Sync Gradle → Run。

### 线上地址

`https://moveup-v7mf.onrender.com`

## 项目结构

```
MoveUp/
├── backend/
│   ├── src/
│   │   ├── app.ts               # Express 入口 + 依赖注入
│   │   ├── middleware/
│   │   │   ├── auth.ts          # JWT 认证中间件
│   │   │   └── errorHandler.ts  # 全局错误处理
│   │   ├── modules/             # 7 个业务模块
│   │   │   ├── user/            # 用户
│   │   │   ├── sport/           # 运动
│   │   │   ├── club/            # 社团
│   │   │   ├── social/          # 社交
│   │   │   ├── coaching/        # 训练指导
│   │   │   ├── challenge/       # 挑战激励
│   │   │   └── ai/              # AI 分析
│   │   ├── routes/              # 路由定义（57 个端点）
│   │   └── utils/
│   │       ├── errors.ts        # 自定义错误类
│   │       └── llm.ts           # LLM 客户端
│   ├── migrations/              # 16 个数据库迁移
│   ├── tests/                   # 17 个测试套件
│   ├── docker/
│   │   ├── docker-compose.yml   # 5 服务编排
│   │   └── .env.prod            # 生产环境变量
│   └── Dockerfile               # 多阶段构建
├── frontend/
│   └── code/                    # Android 项目
├── docs/
│   ├── architecture.md          # 系统架构设计
│   ├── database.md              # 数据库设计（ER 图）
│   ├── api.md                   # API 接口文档
│   └── backend.md               # 后端技术文档
└── .github/workflows/           # CI/CD 流水线
```

## 文档

- [后端技术文档](docs/backend.md) — 完整 API 列表、数据库结构、启动方式
- [架构设计](docs/architecture.md)
- [数据库设计](docs/database.md)
- [API 接口文档](docs/api.md)
- [Figma UI 设计](https://www.figma.com/design/IKpsxQMrrc4alOJIQWFdDe/Move-Up?node-id=0-1&t=b55PvDZSBtFpf062-1)
- [数据库 ER 图](https://dbdiagram.io/d/69c23dbb78c6c4bc7a5191bf)
