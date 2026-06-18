# 云服务部署贡献说明

**姓名**：蔡燚翔
**学号**：2312190426
**角色**：后端
**日期**：2026-06-02

---

## 我完成的工作

### 1. 云平台选型与配置

- [√] 云平台调研与选型（Render）
- [√] Render Web Service 创建与配置
- [√] Render PostgreSQL 托管数据库创建与关联
- [√] 环境变量配置（JWT_SECRET / DEEPSEEK_API_KEY / QWEN_API_KEY）
- [√] 自动部署流程（GitHub push → Render 自动构建）
- [√] 线上 API 验证（`/health` 端点正常响应）

### 2. 部署架构

```
GitHub push → Render Web Service（Node.js 18）
                  ├── Express Backend（端口 3000）
                  └── Render PostgreSQL（托管数据库）
```

**线上地址**：`https://dashboard.render.com/web/srv-d8htg4j7uimc73a5aeb0`

### 3. Render 配置详情

| 配置项 | 值 | 说明 |
|--------|-----|------|
| 构建命令 | `npm install && npm run build` | 安装依赖 + TypeScript 编译 |
| 启动命令 | `npm start` | 执行 `node dist/server.js` |
| 源目录 | `backend/` | 仅构建后端子目录 |
| 运行时 | Node.js 18 | 与本地开发一致 |
| 健康检查路径 | `/health` | Render 自动探活 |
| 自动 HTTPS | Render 自动签发 Let's Encrypt 证书 | 无需手动配置 SSL |

### 4. 环境变量管理

| 变量 | 说明 | 配置方式 |
|------|------|---------|
| `DATABASE_URL` | PostgreSQL 连接串 | Render 自动注入（关联托管数据库） |
| `JWT_SECRET` | JWT 签名密钥 | Render Dashboard 环境变量面板 |
| `DEEPSEEK_API_KEY` | DeepSeek API 密钥 | Render Dashboard 环境变量面板 |
| `QWEN_API_KEY` | 通义千问 API 密钥 | Render Dashboard 环境变量面板 |
| `JWT_EXPIRES_IN` | Token 有效期 | 默认 7200 秒 |

**安全原则**：所有密钥通过 Render 环境变量面板配置，不入 Git 仓库，避免密钥泄露。

---

## 选型理由

选择 **Render** 的理由：

- **免费套餐**：适合课程演示与联调，零成本上线
- **原生 Node.js 支持**：无需额外配置，直接识别 `package.json` 脚本
- **托管 PostgreSQL**：免去手动安装配置数据库，Render 自动提供连接串
- **自动 HTTPS**：Let's Encrypt 证书自动签发与续期，无需配置 Nginx
- **GitHub 集成**：push 自动触发部署，与 CI/CD 流水线无缝衔接
- **健康检查**：内置探活机制，服务异常自动重启

**与 Railway/Vercel 的对比**：

| 平台 | 优势 | 劣势 |
|------|------|------|
| Render | 免费 PostgreSQL、自动 HTTPS、Node.js 原生支持 | 免费套餐 15 分钟无请求休眠 |
| Railway | 更灵活的服务编排 | 免费额度较低 |
| Vercel | 前端部署体验极佳 | 不适合 Node.js 后端长期运行 |

最终选择 Render 是因为其免费套餐同时覆盖 **Web Service + 托管数据库**，对于课程项目性价比最高。

---

## PR 链接

- PR #XX: https://github.com/XXXXorganization/MoveUp/pull/XX

---

## 在线地址

`https://moveup-v7mf.onrender.com`

验证命令：`curl https://moveup-v7mf.onrender.com/health`

预期响应：`{ "status": "healthy", "timestamp": "...", "uptime": ... }`

---

## 遇到的问题和解决

1. **问题**：Render 免费套餐 15 分钟无请求后 Web Service 进入休眠状态，冷启动需 30-60 秒，影响演示体验

   **解决**：演示前提前通过浏览器或 curl 访问 `/health` 端点预热服务；长期方案为升级 Render 付费套餐或使用 UptimeRobot 定时 ping 保持活跃。

2. **问题**：本地 Docker Compose 配置与 Render 环境不一致——本地用 `docker-compose.yml` 管理多个容器，而 Render 使用独立的 Web Service + 托管数据库

   **解决**：Render 部署仅依赖 Dockerfile（单容器），环境变量通过 Render Dashboard 配置，不依赖 docker-compose。本地开发和 Render 部署使用同一份 Dockerfile 保证构建一致性。

3. **问题**：Render PostgreSQL 连接串格式与本地 `DB_HOST/DB_PORT/DB_USER/DB_PASSWORD` 分离式配置不同，使用单一 `DATABASE_URL` 格式

   **解决**：`config/database.ts` 中兼容两种配置方式——优先读取 `DATABASE_URL`（生产），无此变量时从 `DB_HOST` 等分离字段构建连接对象（本地开发）。

4. **问题**：首次部署时 TypeScript 编译失败，日志显示 `npx tsc` 找不到某些类型定义

   **解决**：将 `@types/*` 依赖从 `devDependencies` 部分移到安装阶段可用的范围，Render 构建命令 `npm install` 会安装所有依赖，确保 tsc 编译时类型可用。

---

## 心得体会

通过本次云服务部署实践，我深刻体会到了"本地能跑 ≠ 线上能跑"这一 DevOps 核心痛点。本地 Docker Compose 环境可以随意编排多服务、暴露端口、使用弱密码；但上线时需要考虑 HTTPS 证书、密钥安全、数据库连接方式、构建缓存、冷启动延迟等一系列生产环境特有的问题。

Render 的选择体现了"简单优于复杂"的工程原则——与其在 VPS 上手动装 PostgreSQL、配 Nginx、申请 SSL 证书，不如直接使用托管服务，将精力集中在业务代码上。

最大的收获是理解了环境变量在云部署中的角色：它是连接代码与云基础设施的桥梁。代码中永远不应该硬编码配置，而应通过 `process.env` 读取，由部署平台在运行时注入——这既是安全最佳实践，也是 12-Factor App 的核心原则之一。

此外，Dockerfile 作为"构建契约"贯穿本地开发与云端部署，一份 Dockerfile 在本地 `docker compose up` 和 Render 自动构建中产生完全一致的结果，这是容器化最大的价值——环境一致性。
