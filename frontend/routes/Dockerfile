# ==========================================
# 阶段 1：Builder (依赖安装阶段)
# ==========================================
# 使用 Alpine 极简版本，大幅减小镜像体积
FROM node:20-alpine AS builder

WORKDIR /app

# 优先复制 package 文件以利用 Docker 缓存机制
COPY package*.json ./

# 只安装生产环境依赖（这会让镜像变得非常小）
RUN npm ci --omit=dev

# ==========================================
# 阶段 2：Production (生产运行阶段)
# ==========================================
FROM node:20-alpine

WORKDIR /app

# 设置环境变量为生产模式
ENV NODE_ENV=production

# 从 builder 阶段把安装好的精简版 node_modules 复制过来
COPY --from=builder /app/node_modules ./node_modules

# 把剩下的代码（server.js, index.js 等）复制过来
COPY . .

# 🌟 安全配置：将文件所有权交给自带的非 root 用户 'node'
RUN chown -R node:node /app

# 🌟 切换到非 root 用户运行应用，符合最高生产安全规范
USER node

# 暴露 server.js 中定义的 3000 端口
EXPOSE 3000

# 启动命令 (对应 package.json 中的 "start": "node server.js")
CMD ["npm", "start"]