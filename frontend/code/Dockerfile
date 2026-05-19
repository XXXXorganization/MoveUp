# 使用官方推荐的非 Root Nginx Alpine 镜像 (完美契合作业安全与小体积要求)
FROM nginxinc/nginx-unprivileged:alpine

# 🌟 临时切换到 root 用户，以便有权限创建网页文件
USER root

# 切换到 Nginx 默认的网页存放目录
WORKDIR /usr/share/nginx/html

# 直接将本地刚才准备好的 APK 复制进镜像中
COPY moveup-app.apk ./moveup-app.apk

# 自动生成一个极简的 HTML 下载页面
RUN echo '<!DOCTYPE html><html><head><meta charset="utf-8"><title>MoveUp App Download</title><meta name="viewport" content="width=device-width,initial-scale=1"></head><body style="text-align:center;padding:50px;font-family:sans-serif;"><h1>🏃‍♂️ MoveUp 运动世界</h1><p>Android 客户端已准备就绪</p><a href="moveup-app.apk" style="display:inline-block;padding:15px 30px;background:#C7FB58;color:#000;text-decoration:none;border-radius:25px;font-weight:bold;margin-top:20px;">⬇️ 点击下载安装包 (APK)</a></body></html>' > index.html

# 赋予这些文件正确的读取权限
RUN chown -R 101:101 /usr/share/nginx/html

# 🌟 核心操作：切回安全的非 root 用户 (UID 101 是 nginx-unprivileged 的默认普通用户)
USER 101

# 暴露 8080 端口
EXPOSE 8080

# 启动 Nginx
CMD ["nginx", "-g", "daemon off;"]