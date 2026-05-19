# MoveUp 项目 Docker 部署与运行文档

本文档说明了如何通过 Docker Compose 一键启动 MoveUp 项目的前端（Android App 分发页）与后端（Mock API 接口）服务。

## 1. 环境准备

在运行本项目之前，请确保您的部署机器已安装以下基础环境：
* **Git**: 用于克隆项目代码。
* **Docker**: 建议安装最新版的 [Docker Desktop](https://www.docker.com/products/docker-desktop/)（Windows/Mac）或 Docker Engine（Linux）。
* **网络环境**: 首次构建时，Docker 需要连接外网拉取基础镜像。若国内网络拉取失败，建议配置国内 Docker 镜像源或使用代理。

## 在MoveUp\frontend\code目录下，运行以下命令启动服务：
docker compose -f compose.prod.yaml up -d --build

## 等待命令执行完毕后，输入以下命令查看服务运行状态：
docker compose -f compose.prod.yaml up -d --build

## 等待命令执行完毕后，输入以下命令查看服务运行状态：
docker compose -f compose.prod.yaml ps

2. 服务访问说明
本项目采用前后端分离容器化部署，启动成功后，可通过以下地址访问相应服务：

📱 前端服务 (Android 客户端分发)
访问地址: http://localhost

端口: 80

说明: 为了符合移动端原生应用的真实分发场景，前端采用 Alpine 非 root 版 Nginx 搭建了安全的 App 自动分发页面。用户访问该地址即可一键下载最新版 MoveUp.apk 进行安装。

⚙️ 后端服务 (Node.js Mock API)
访问地址: http://localhost:3000/v1/clubs (以社团列表接口为例)

端口: 3000

说明: 后端接口服务，提供应用所需的全部模拟数据（Mock Data）。服务已通过多阶段构建剔除开发依赖，并采用非 root 用户安全运行。


