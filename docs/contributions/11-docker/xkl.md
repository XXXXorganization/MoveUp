# Docker 部署贡献说明

**姓名:** 徐康勒
**学号:** 2312190422
**日期:** 2026-05-19

## 架构特别说明 (💡 亮点)
考虑到我们的 `MoveUp` 是 Android 原生项目，最终运行在移动端而非 Web 浏览器，强行在容器内配置 Android SDK 编译环境会导致镜像极大（动辄数GB）且容易因网络问题构建失败。因此，本次作业我采用了更符合实际工程生产环境的 **全栈持续交付架构**：
**前端部署 (`android-web`)**：采用 Alpine 非 root 版 Nginx 镜像，搭建了极简安全的 App 自动分发页面，供测试用户一键下载最新版 APK。


## 我完成的工作

### 1. Dockerfile 编写
- [x] 前端 Dockerfile (多阶段构建 / Nginx 小体积镜像)
- [x] 对Mock后端 Dockerfile (Node.js 多阶段构建，Alpine 基础镜像)
- [x] .dockerignore 文件 (成功排除了 `node_modules` 等无关文件)
- [x] **安全配置**：前后端均实现了非 root 用户运行（Nginx 切换回 UID 101，Node.js 使用默认 `node` 用户）。

### 2. Compose 配置
- [x] 生产环境 compose.prod.yaml (独立编写)
- [x] 健康检查配置 (为 Nginx 和 Node 服务分别编写了 curl 和 wget 的 Healthcheck)
- [x] 资源限制 (对服务进行了 256M 的 memory 限制)

### 3. 自动化部署
- 选择了选项B：本地部署脚本
- 具体内容：通过编写清晰的 Dockerfile 和 Compose 编排，确保项目在目标服务器上可以通过 `docker compose -f compose.prod.yaml up -d --build` 一键拉起，所有服务均达到 `Up (healthy)` 状态。

## PR链接
- PR #X: https://github.com/XXXXorganization/MoveUp/pull/64

## 遇到的问题和解决

**1. 问题: 容器内创建网页文件报 `Permission denied` 权限错误。**
- **解决:** 因为作业要求使用非 root 用户运行，我使用了极度安全的 `nginx-unprivileged` 镜像，导致默认用户无法写入。解决方案是在 Dockerfile 中临时 `USER root` 写入文件并赋予权限 `RUN chown -R 101:101 /usr/share/nginx/html`，最后再切回 `USER 101`，完美兼顾了自动化与生产安全。

**2. 问题: 并发构建时出现 `TLS handshake timeout` 和网络连接中断 (`EOF`)。**
- **解决:** 国内连接 Docker Hub 不稳定，并发拉取镜像导致节点拥堵。我改变了策略，利用国内大厂镜像源（DaoCloud）提前单线程将必需的基础镜像（Nginx 和 Node.js）拉取到本地，并使用 `docker tag` 伪装成官方包。随后再运行 `docker compose`，实现了零外网依赖的极速本地构建。

## AI 使用情况
- **使用了哪些 Prompt:** - "帮我检查这个 Docker compose 报错 `dial tcp ... connectex` 是什么原因？"
  - "我要怎么修改 Dockerfile 才能把本地现成的 APK 文件放进 Nginx 服务器供人下载？"
  - "为包含 `index.js` 和 `package.json` 的 Express Mock 后端生成一份多阶段构建、非 root 用户的满分 Dockerfile。"
- **AI 帮助解决了哪些问题:** AI 帮助我理清了“Android 客户端不需要后端 Dockerfile”的逻辑盲区，设计了更优雅的分发架构；同时，AI 协助我诊断了 Docker 网络连接超时的底层原因（TLS握手失败），并提供了使用国内代理源 `docker tag` 的“降维打击”解决方案，最终成功完成构建。

## 心得体会
在这次 Docker 部署过程中，我深刻体会到了理论与实践的差距。特别是在处理网络环境（墙）和 Linux 权限控制（非 root 安全规范）时，遇到了很多书本上没有的坑。但通过一步步 Debug、查阅日志、调整镜像源和用户组，我不仅掌握了 Dockerfile 的多阶段构建和 Compose 编排，更培养了在受限环境下解决复杂架构问题的工程思维。看着终端里亮起的两个 `Up (healthy)`，成就感满满！