# MoveUp Docker 部署指南

## 目录

- [环境要求](#环境要求)
- [快速开始](#快速开始)
- [详细配置](#详细配置)
- [常用命令](#常用命令)
- [生产环境部署](#生产环境部署)
- [故障排查](#故障排查)

---

## 环境要求

### 必需
- Docker 20.10+
- Docker Compose 2.0+
- 至少 2GB 可用内存
- 至少 5GB 可用磁盘空间

### 推荐
- 4GB+ 内存
- 20GB+ 磁盘空间
- Linux/macOS 系统

---

## 快速开始

### 1. 克隆项目
```bash
git clone https://github.com/your-username/MoveUp.git
cd MoveUp
```

### 2. 配置环境变量
```bash
# 复制环境变量模板
cp .env.example .env

# 编辑 .env 文件，修改必要的配置
nano .env  # 或使用其他编辑器
```

### 3. 启动所有服务
```bash
# 构建并启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps
```

### 4. 等待服务就绪
```bash
# 查看服务日志
docker-compose logs -f backend

# 等待看到类似输出：
# "Database connected successfully"
# "Server is running on port 3000"
```

### 5. 验证部署
```bash
# 测试健康检查
curl http://localhost/health

# 测试 API
curl http://localhost/api/v1/auth/code \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"phone":"13800138000","type":"login"}'
```

---

## 详细配置

### 服务架构

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Nginx    │────▶│   Backend   │────▶│  PostgreSQL │
│   (80/443) │     │   (3000)    │     │   (5432)    │
└─────────────┘     └─────────────┘     └─────────────┘
                          │
                          ├────▶│  Redis   │
                          │      │ (6379)   │
                          └────▶│  MinIO   │
                                 │ (9000)   │
                                 └───────────┘
```

### 环境变量说明

#### 数据库配置
```env
POSTGRES_USER=postgres           # 数据库用户名
POSTGRES_PASSWORD=postgres123   # 数据库密码（生产环境请修改！）
POSTGRES_DB=moveup_db        # 数据库名称
POSTGRES_PORT=5432            # 宿主机端口映射
```

#### Redis 配置
```env
REDIS_PASSWORD=redis123        # Redis 密码
REDIS_PORT=6379              # 宿主机端口映射
```

#### 应用配置
```env
NODE_ENV=production           # 环境模式：development/production
PORT=3000                   # 应用内部端口
BACKEND_PORT=3000            # 宿主机端口映射
HTTP_PORT=80                 # Nginx HTTP 端口
HTTPS_PORT=443               # Nginx HTTPS 端口
```

#### JWT 配置
```env
JWT_SECRET=your-secret-key    # JWT 签名密钥（生产环境必须修改！）
JWT_EXPIRES_IN=7200          # Token 过期时间（秒）
```

#### MinIO 配置
```env
MINIO_ACCESS_KEY=minioadmin   # MinIO 访问密钥
MINIO_SECRET_KEY=change-me   # MinIO 密钥（生产环境必须修改！）
MINIO_PORT=9000             # MinIO API 端口
MINIO_CONSOLE_PORT=9001      # MinIO 控制台端口
MINIO_BUCKET=moveup-uploads  # 存储桶名称
```

---

## 常用命令

### 服务管理
```bash
# 启动所有服务
docker-compose up -d

# 停止所有服务
docker-compose down

# 重启所有服务
docker-compose restart

# 查看服务状态
docker-compose ps

# 查看资源使用情况
docker stats
```

### 日志管理
```bash
# 查看所有服务日志
docker-compose logs

# 查看特定服务日志
docker-compose logs backend
docker-compose logs postgres
docker-compose logs redis

# 实时跟踪日志
docker-compose logs -f backend

# 查看最近 100 行日志
docker-compose logs --tail=100 backend
```

### 数据库操作
```bash
# 进入 PostgreSQL 容器
docker-compose exec postgres psql -U postgres -d moveup_db

# 执行数据库迁移
docker-compose exec backend npx knex migrate:latest

# 回滚数据库迁移
docker-compose exec backend npx knex migrate:rollback

# 查看迁移状态
docker-compose exec backend npx knex migrate:status
```

### 备份与恢复
```bash
# 备份数据库
docker-compose exec postgres pg_dump -U postgres moveup_db > backup.sql

# 恢复数据库
docker-compose exec -T postgres psql -U postgres moveup_db < backup.sql

# 备份 Redis
docker-compose exec redis redis-cli -a redis123 BGSAVE

# 备份 MinIO 数据（通过卷）
docker run --rm -v moveup_minio_data:/data -v $(pwd):/backup \
  alpine tar czf /backup/minio-backup.tar.gz -C /data .
```

### 清理
```bash
# 停止并删除容器
docker-compose down

# 停止并删除容器及匿名卷
docker-compose down -v

# 停止并删除容器、匿名卷、镜像
docker-compose down -v --rmi all

# 清理未使用的 Docker 资源
docker system prune -a
```

---

## 生产环境部署

### 安全配置

1. **修改所有默认密码**
```bash
# 编辑 .env 文件，修改以下密码：
POSTGRES_PASSWORD=<强密码>
REDIS_PASSWORD=<强密码>
JWT_SECRET=<随机生成的长字符串>
MINIO_SECRET_KEY=<随机生成的长字符串>
```

2. **启用 HTTPS**
```bash
# 创建 SSL 目录
mkdir -p nginx/ssl

# 放置证书文件
cp /path/to/cert.pem nginx/ssl/cert.pem
cp /path/to/key.pem nginx/ssl/key.pem

# 修改 nginx.conf，取消 HTTPS 配置的注释
```

3. **配置防火墙**
```bash
# 仅开放必要端口
ufw allow 80/tcp    # HTTP
ufw allow 443/tcp   # HTTPS
ufw enable
```

### 性能优化

1. **资源限制**
```yaml
# 在 docker-compose.yml 中添加：
backend:
  deploy:
    resources:
      limits:
        cpus: '2'
        memory: 2G
      reservations:
        cpus: '1'
        memory: 512M
```

2. **数据库优化**
```yaml
# 在 docker-compose.yml 中添加：
postgres:
  command:
    - postgres
    - -c
    - shared_buffers=256MB
    - -c
    - max_connections=200
```

3. **启用日志轮转**
```yaml
# 在 docker-compose.yml 中添加：
services:
  backend:
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
```

### 监控配置

1. **启用 Prometheus 监控（可选）**
```yaml
# 添加到 docker-compose.yml：
prometheus:
  image: prom/prometheus
  volumes:
    - ./prometheus.yml:/etc/prometheus/prometheus.yml
  ports:
    - "9090:9090"

grafana:
  image: grafana/grafana
  ports:
    - "3001:3000"
```

2. **配置日志收集（可选）**
```yaml
# 添加到 docker-compose.yml：
elasticsearch:
  image: elasticsearch:8.0.0
  ports:
    - "9200:9200"

kibana:
  image: kibana:8.0.0
  ports:
    - "5601:5601"
```

---

## 故障排查

### 问题1：服务无法启动

**症状：** `docker-compose up` 失败

**解决步骤：**
```bash
# 1. 检查端口占用
netstat -tlnp | grep -E '3000|5432|6379|9000'

# 2. 查看详细错误
docker-compose up --no-daemon

# 3. 清理并重建
docker-compose down -v
docker-compose up -d --force-recreate
```

### 问题2：数据库连接失败

**症状：** 后端日志显示数据库连接错误

**解决步骤：**
```bash
# 1. 检查数据库是否就绪
docker-compose exec postgres pg_isready -U postgres

# 2. 查看数据库日志
docker-compose logs postgres

# 3. 检查网络连接
docker-compose exec backend ping postgres

# 4. 验证环境变量
docker-compose exec backend env | grep DB_
```

### 问题3：服务健康检查失败

**症状：** `docker-compose ps` 显示服务 unhealthy

**解决步骤：**
```bash
# 1. 手动测试健康检查端点
docker-compose exec backend wget -O- http://localhost:3000/health

# 2. 查看健康检查日志
docker inspect moveup-backend | grep -A 10 Health

# 3. 增加健康检查超时时间
# 在 docker-compose.yml 中修改 healthcheck 配置
```

### 问题4：磁盘空间不足

**症状：** `docker-compose up` 报磁盘空间错误

**解决步骤：**
```bash
# 1. 清理 Docker 缓存
docker builder prune

# 2. 清理未使用的镜像和容器
docker system prune -a

# 3. 检查磁盘使用情况
df -h

# 4. 清理系统日志
sudo journalctl --vacuum-time=7d
```

### 问题5：MinIO 访问失败

**症状：** 无法访问 MinIO 控制台或存储

**解决步骤：**
```bash
# 1. 检查 MinIO 是否就绪
docker-compose logs minio

# 2. 重新创建存储桶
docker-compose exec backend node -e "
  const { S3Client, CreateBucketCommand } = require('@aws-sdk/client-s3');
  const client = new S3Client({
    endpoint: 'http://minio:9000',
    credentials: { accessKeyId: 'minioadmin', secretAccessKey: 'minioadmin' },
    region: 'us-east-1',
    forcePathStyle: true,
  });
  await client.send(new CreateBucketCommand({ Bucket: 'moveup-uploads' }));
"
```

---

## 附录

### 端口清单

| 服务 | 内部端口 | 外部端口 | 说明 |
|------|---------|---------|------|
| Nginx | 80 | 80 | HTTP |
| Nginx | 443 | 443 | HTTPS |
| Backend | 3000 | 3000 | API 服务 |
| PostgreSQL | 5432 | 5432 | 数据库 |
| Redis | 6379 | 6379 | 缓存 |
| MinIO API | 9000 | 9000 | 对象存储 API |
| MinIO Console | 9001 | 9001 | 管理控制台 |

### 目录映射

| 宿主机路径 | 容器路径 | 说明 |
|-----------|---------|------|
| ./backend/migrations | /app/migrations | 数据库迁移文件 |
| postgres_data (volume) | /var/lib/postgresql/data | 数据库数据 |
| redis_data (volume) | /data | Redis 数据 |
| minio_data (volume) | /data | MinIO 数据 |

### 有用的 Docker 命令

```bash
# 查看容器资源使用
docker stats

# 进入运行中的容器
docker-compose exec backend sh

# 复制文件到容器
docker-compose cp ./local-file backend:/app/

# 从容器复制文件
docker-compose cp backend:/app/file.txt ./

# 查看容器日志文件
docker-compose logs --tail=1000 backend > backend.log

# 查看网络配置
docker network inspect moveup_moveup-network

# 查看卷配置
docker volume ls
docker volume inspect moveup_postgres_data
```

---

## 支持

如有问题，请：
1. 查看本文档的故障排查部分
2. 检查服务日志：`docker-compose logs`
3. 提交 Issue 到 GitHub

---

**最后更新：** 2026-04-13
**维护者：** MoveUp Team
