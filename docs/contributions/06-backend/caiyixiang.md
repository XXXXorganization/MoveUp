# 后端开发贡献说明
姓名：蔡燚翔
学号：2312190426
日期：2026-04-14
## 我完成的工作

### API 实现
- [ ] 用户认证 API（注册 / 登录）
- [ ] 个人资料管理 API
- [ ] 账号安全 API（注册 / 登录）
- [ ] 实时运动追踪 CRUD
- [ ] 运动记录管理 CRUD
- [ ] 路线管理 CRUD

### 数据库
- [√] 数据模型定义（ER 图或模型文件）
- [√] ORM 配置
- [√] 数据库迁移脚本 knexfile.js
### 部署
- [√] Dockerfile 编写
- [√] docker-compose.yml 配置
- [√] 本地联调验证

## PR 链接 - PR #X: https://github.com/xxx/xxx/pull/X

## 遇到的问题和解决

1、问题：Docker 环境部署与本地测试环境不连通运行 npm test 时出现 getaddrinfo ENOTFOUND postgres 错误，测试无法连接 Docker 内的 PostgreSQL 数据库，导致大量接口测试失败。
解决：不能直接在本地执行测试命令，需要进入 Docker 后端容器内部执行测试，使测试与数据库处于同一网络环境：
plaintext
docker exec -it moveup-backend npm test

2、问题：Docker 服务启动后，本地环境与容器网络隔离本地直接访问服务名（如 postgres、redis）无法解析，导致连接失败。
解决：理解 Docker 网络隔离机制，所有依赖服务（PostgreSQL、Redis、MinIO、Nginx）统一通过 docker-compose 管理，后端服务运行在容器内部，测试与调试必须在容器内执行。

3、问题：Docker 服务启动状态判断与日志排查不清楚服务是否正常启动，无法判断容器是否健康。
解决：使用 docker-compose up -d 启动服务，通过 docker ps 查看运行状态，通过 docker logs 容器名 查看后端日志，快速定位启动异常。

## 心得体会
给你写一段**简洁、正式、适合写在总结/报告里**的「后端开发收获」，不啰嗦、点到位，你直接复制用：

### 收获分享
1. 熟练掌握**Node.js + TypeScript**后端项目结构搭建，理解接口设计、业务逻辑分层与模块化开发思想，提升了代码规范与可维护性意识。
2. 学会使用**Jest**进行单元测试与接口自动化测试，能够通过测试用例验证业务正确性，提升了问题定位与质量保障能力。
3. 深入理解**Docker + Docker Compose**容器化部署流程，掌握多服务（PostgreSQL、Redis、MinIO、Nginx）编排、网络隔离与环境一致性管理，解决了本地与部署环境不一致的核心问题。
4. 提升了**问题排查与工程化能力**，包括环境配置、依赖管理、日志分析、网络连通性排查等，形成了更完整的后端开发与部署思维。
5. 强化了**数据库操作、RESTful API 设计**与前后端协作能力，对完整后端项目的开发、测试、部署流程有了全面实践。

