# MoveUp Docker 部署

> 🚀 一键部署 MoveUp 运动跑步平台

## 快速开始

### 方式一：使用脚本（推荐）

```bash
# 1. 进入项目目录
cd MoveUp

# 2. 运行启动脚本
./docker-start.sh

# 3. 等待服务启动完成
# 部署完成后访问：http://localhost
```

### 方式二：使用 Makefile

```bash
# 1. 初始化项目
make init

# 2. 启动服务
make up

# 3. 查看服务状态
make ps
```

### 方式三：使用 Docker Compose

```bash
# 1. 复制环境变量
cp .env.example .env

# 2. 启动服务
docker-compose up -d

# 3. 查看日志
docker-compose logs -f
```

---

## 服务访问

| 服务 | 地址 | 说明 |
|------|------|------|
| API | http://localhost | 后端 API 服务 |
| 健康检查 | http://localhost/health | 服务健康状态 |
| MinIO 控制台 | http://localhost:9001 | 对象存储管理界面 |
| PostgreSQL | localhost:5432 | 数据库（需工具连接） |
| Redis | localhost:6379 | 缓存（需工具连接） |

**MinIO 控制台默认账号：**
- 用户名：`minioadmin`
- 密码：`minioadmin`（请在 `.env` 中修改）

---

## 常用命令

### 服务管理

```bash
# 查看所有可用命令
make help

# 启动服务
make up

# 停止服务
make down

# 重启服务
make restart

# 查看服务状态
make ps

# 重新构建并启动
make rebuild
```

### 日志查看

```bash
# 查看所有服务日志
make logs

# 查看后端日志
make logs-backend

# 查看数据库日志
make logs-db

# 查看 Redis 日志
make logs-redis
```

### 数据库操作

```bash
# 执行数据库迁移
make migrate

# 回滚迁移
make migrate-rollback

# 查看迁移状态
make migrate-status

# 进入数据库 Shell
make db-shell
```

### 备份与恢复

```bash
# 备份数据库
make backup

# 恢复数据库
make restore FILE=backups/backup.sql
```

### 清理

```bash
# 停止并删除容器和卷
make clean

# 完全清理（包括镜像）
make clean-all

# 清理未使用的 Docker 资源
make prune
```

---

## 环境变量配置

在部署前，请修改 `.env` 文件中的以下配置：

### ⚠️ 必须修改（生产环境）

```env
POSTGRES_PASSWORD=<强密码>
REDIS_PASSWORD=<强密码>
JWT_SECRET=<随机生成的长字符串>
MINIO_SECRET_KEY=<随机生成的长字符串>
```

### 可选配置

```env
# 数据库
POSTGRES_DB=moveup_db
POSTGRES_PORT=5432

# 应用
NODE_ENV=production
BACKEND_PORT=3000
HTTP_PORT=80
HTTPS_PORT=443

# JWT
JWT_EXPIRES_IN=7200

# MinIO
MINIO_PORT=9000
MINIO_CONSOLE_PORT=9001
```

---

## 文件结构

```
MoveUp/
├── backend/              # 后端代码
│   ├── Dockerfile       # 后端 Docker 镜像定义
│   ├── .dockerignore   # Docker 构建忽略文件
│   └── src/           # 源代码
├── nginx/              # Nginx 配置
│   ├── nginx.conf      # Nginx 主配置文件
│   └── ssl/           # SSL 证书目录
├── backups/            # 数据备份目录（自动创建）
├── logs/              # 日志目录（自动创建）
├── docker-compose.yml  # Docker Compose 配置
├── .env.example       # 环境变量模板
├── Makefile          # Make 命令定义
├── docker-start.sh    # 快速启动脚本
├── docker-stop.sh     # 快速停止脚本
└── DOCKER.md         # 本文件
```

---

## 开发模式

### 启动开发环境（带热重载）

```bash
# 开发模式不使用 Docker，直接运行
make dev-server

# 或
cd backend && npm run dev
```

### 运行测试

```bash
# 运行所有测试
make test-api

# 或
cd backend && npm test

# 运行特定测试文件
cd backend && npm test -- sport.test.ts
```

---

## 故障排查

### 服务无法启动

```bash
# 1. 检查 Docker 状态
docker ps
docker-compose ps

# 2. 查看日志
docker-compose logs

# 3. 重新构建
make rebuild
```

### 端口冲突

```bash
# 修改 .env 中的端口配置
BACKEND_PORT=3001
HTTP_PORT=8080

# 然后重新启动
make restart
```

### 数据库连接问题

```bash
# 1. 检查数据库是否就绪
docker-compose exec postgres pg_isready

# 2. 查看数据库日志
make logs-db

# 3. 重新执行迁移
make migrate
```

### 完全重置

```bash
# 停止并删除所有数据
make clean-all

# 删除 Docker 镜像缓存
docker builder prune -af

# 重新启动
./docker-start.sh
```

---

## 生产环境部署

### 安全检查清单

- [ ] 修改所有默认密码
- [ ] 启用 HTTPS
- [ ] 配置防火墙规则
- [ ] 设置资源限制
- [ ] 配置日志轮转
- [ ] 启用监控和告警
- [ ] 配置自动备份
- [ ] 使用强密码策略

### 性能优化

详细的生产环境配置请参考：[Docker 部署指南](docs/deployment/docker-deployment-guide.md)

---

## 停止服务

```bash
# 使用停止脚本
./docker-stop.sh

# 或使用 Makefile
make down

# 或使用 Docker Compose
docker-compose down
```

---

## 更新部署

```bash
# 1. 拉取最新代码
git pull origin main

# 2. 重新构建并启动
make rebuild

# 3. 执行数据库迁移
make migrate
```

---

## 常见问题

### Q: 如何修改数据库密码？
A: 编辑 `.env` 文件中的 `POSTGRES_PASSWORD`，然后重启服务：
```bash
make restart
```

### Q: 如何备份数据？
A: 运行：
```bash
make backup
```
备份文件会保存在 `backups/` 目录下。

### Q: 如何查看服务资源使用情况？
A: 运行：
```bash
make stats
```

### Q: 如何清理 Docker 资源？
A: 运行：
```bash
make prune
```

### Q: 开发模式和 Docker 模式有什么区别？
A:
- **开发模式**：直接在本地运行 Node.js，支持热重载，适合开发
- **Docker 模式**：在容器中运行，环境隔离，适合测试和生产

---

## 文档

- [Docker 部署完整指南](docs/deployment/docker-deployment-guide.md)
- [项目 README](README.md)
- [开发规范](CLAUDE.md)

---

## 支持

如有问题，请：
1. 查看 [故障排查](docs/deployment/docker-deployment-guide.md#故障排查)
2. 查看服务日志：`make logs`
3. 提交 Issue 到 GitHub

---

**最后更新：** 2026-04-13
**维护者：** MoveUp Team
