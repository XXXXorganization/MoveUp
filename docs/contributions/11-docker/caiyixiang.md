# Docker 容器化部署贡献说明

姓名：蔡燚翔  学号：2312190426  角色：后端  日期：2026-05-19

## 我完成的工作

### 1. Dockerfile 编写
- [√] 后端 Dockerfile（多阶段构建：构建阶段 + 运行阶段分离）
- [√] .dockerignore 文件（排除 dist / node_modules，保留 TypeScript 源码供 Docker 内编译）
- [√] dumb-init 信号处理 + HEALTHCHECK 健康检查
- [√] 非 root 用户（nodejs:nodejs）运行容器

### 2. Compose 配置
- [√] 开发环境 `docker-compose.yml`：PostgreSQL 15 + Redis 7 + MinIO + Nginx + Backend
- [√] 生产环境 `compose.prod.yaml`：资源限制（CPU/内存上限）、权限最小化（cap_drop）、只读文件系统、日志轮转
- [√] 健康检查配置（5个服务全部配置 healthcheck + start_period）
- [√] 双环境变量管理：`.env`（开发）/ `.env.prod`（生产），均有 example 模板
- [√] 环境变量强制校验：`${VAR:?required}`，未填拒绝启动

### 3. 自动化部署
- 选择了选项 A：构建并推送镜像到 GHCR
- 具体内容：
  - `.github/workflows/docker-publish.yml`：git push → Docker Build → Trivy 安全扫描 → Push to GHCR
  - 自动打标签：`:latest` / `:main` / `:develop` / `:commit-sha`
  - 扫描结果仅报告不阻断（exit-code: 0），漏洞记录在 Actions 日志中

### 4. 安全检查清单
- [√] 非 root 用户运行容器
- [√] .env 已加入 .gitignore，仓库中有 .env.example
- [√] 生产密码不硬编码在配置文件中
- [√] 镜像安全扫描通过（Trivy 集成在 CI）
- [√] 应用可通过浏览器正常访问（`http://localhost/health`）
- [√] 数据库数据持久化（Volume 挂载，重启容器数据不丢失）

## PR 链接
- PR #XX: https://github.com/XXXXorganization/MoveUp/pull/XX

## 遇到的问题和解决

1. 问题：Docker 镜像源 `dockerproxy.net` 返回 500，无法拉取 `node:18-alpine`

解决：在 Docker Desktop Settings → Docker Engine 中配置国内镜像源 `docker.1ms.run`、`docker.xuanyuan.me`、`hub.rat.dev`。

2. 问题：Windows 端口 3000 被系统保留在 TCP 排除范围 2959-3058 内，容器无法绑定宿主机端口

解决：修改 `.env` 中 `BACKEND_PORT=3500`，避开 Windows 保留端口段。同时前端 `BASE_URL` 相应调整为 `http://10.234.4.72:3500`。

3. 问题：`.dockerignore` 中 `*.ts` 排除了所有 TypeScript 源码，Docker 内 `npx tsc` 无文件可编译，容器运行的是宿主机遗留的过期 dist

解决：从 `.dockerignore` 移除 `*.ts` 规则，同时添加 `dist` 防止宿主机旧编译产物混入。确保每次 Docker 构建都从源码全新编译。

4. 问题：`database.ts` 中 `import knexConfig from '@knexfile'` 的 TS 路径别名编译后保留为 `require('@knexfile')`，Node.js 运行时无法解析

解决：将 `database.ts` 改为直接读取环境变量构建 Knex 配置对象，不再依赖路径别名。Knex CLI 仍通过独立的 knexfile.js 执行迁移命令。

5. 问题：compose.prod.yaml 中 PostgreSQL 的 `cap_drop: ALL` 导致 `chmod: Operation not permitted`，无法初始化数据目录

解决：PostgreSQL 移除 `cap_drop: ALL`，仅保留 `no-new-privileges`。Redis 将 YAML block scalar `>` 改为单行 command，删除行内中文注释。

6. 问题：`express-rate-limit` 依赖已装但未写入 package-lock.json，Docker 内 `npm ci` 报 `Missing from lock file`

解决：宿主机执行 `npm install express-rate-limit --save` 确保 lock 文件同步后重新构建。

7. 问题：生产环境后端容器反复重启，每次报错不同（DB 连接 → CMD 路径 → 别名解析 → lock 文件）

解决：通过 `docker compose logs` 逐轮排查 7 个累积配置问题，全部修复后五容器全部 Healthy。

## AI 使用情况
- 使用了哪些 Prompt：安全审查 OWASP Top 10 检查清单、Docker Compose 多服务编排、TypeScript 路径别名运行时解析、GHCR 推送工作流编写、Trivy 集成配置
- AI 帮助解决了哪些问题：Dockerfile 调试（CMD 路径修正、knexfile 复制、.dockerignore 优化）、compose.prod.yaml 安全加固（资源限制、cap_drop、read_only）、TS 别名编译时与运行时断层诊断、Express 中间件报错 `Route.post() requires a callback function but got a [object Undefined]` 排查、国内镜像源配置建议

## 心得体会

Docker 容器化看起来只是"写一个 Dockerfile"，实际遇到的坑往往是开发环境和容器环境不一致的累积。这次实践体会最深的是：多阶段构建的价值不仅在于减小镜像体积，更在于严格分离构建和运行环境 — 构建需要 TypeScript/ESLint，运行只需要 JS 文件加生产依赖，混淆了要么膨胀要么报错。

Docker Compose 的双环境设计（dev vs prod）是必要的。开发需要暴露端口方便调试；生产必须关闭端口暴露、强制强密码、限制 CPU/内存、最小化权限。两个环境共用 Dockerfile，差异全部在 compose 文件层面体现。

TypeScript 路径别名在容器运行时失效是编译时与运行时之间的典型断层。`import from '@alias'` 编译后仍是 `require('@alias')`，Node.js 不认。最简洁的方案是源码层面避开别名，用环境变量或相对路径替代。

Docker 缓存机制是把双刃剑 — 多次出现改了代码容器还是旧版的问题。`--no-cache` 和 `.dockerignore` 是保证构建可复现的关键。

最后，自动化 CI/CD（Build → Scan → Push to GHCR）将"在我电脑上能跑"提升为"在任何有 Docker 的地方都能跑"，无需再装 Node、配置数据库、逐项解决环境问题。
