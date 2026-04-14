#!/bin/bash

# MoveUp Docker 快速启动脚本

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 打印带颜色的消息
print_message() {
    echo -e "${2}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

# 检查 Docker 是否安装
check_docker() {
    if ! command -v docker &> /dev/null; then
        print_message "Docker 未安装，请先安装 Docker" "${RED}"
        exit 1
    fi

    if ! command -v docker-compose &> /dev/null; then
        print_message "Docker Compose 未安装，请先安装 Docker Compose" "${RED}"
        exit 1
    fi

    print_message "Docker 和 Docker Compose 已就绪" "${GREEN}"
}

# 检查环境变量文件
check_env() {
    if [ ! -f .env ]; then
        print_message "未找到 .env 文件，从 .env.example 复制..." "${YELLOW}"
        cp .env.example .env
        print_message "已创建 .env 文件，请根据需要修改配置" "${GREEN}"
    fi
}

# 创建必要的目录
create_directories() {
    print_message "创建必要的目录..." "${BLUE}"
    mkdir -p nginx/ssl
    mkdir -p backups
    mkdir -p logs
}

# 拉取最新镜像
pull_images() {
    print_message "拉取最新的基础镜像..." "${BLUE}"
    docker-compose -f docker-compose.yml pull
}

# 构建镜像
build_images() {
    print_message "构建 Docker 镜像..." "${BLUE}"
    docker-compose -f docker-compose.yml build
}

# 启动服务
start_services() {
    print_message "启动所有服务..." "${BLUE}"
    docker-compose -f docker-compose.yml up -d
}

# 等待服务就绪
wait_for_services() {
    print_message "等待服务启动..." "${YELLOW}"

    # 等待后端服务
    local max_attempts=30
    local attempt=0

    while [ $attempt -lt $max_attempts ]; do
        if curl -s http://localhost/health > /dev/null 2>&1; then
            print_message "后端服务已就绪！" "${GREEN}"
            break
        fi

        attempt=$((attempt + 1))
        sleep 2
        echo -n "."
    done

    if [ $attempt -eq $max_attempts ]; then
        print_message "服务启动超时，请查看日志" "${RED}"
        exit 1
    fi
}

# 执行数据库迁移
run_migrations() {
    print_message "执行数据库迁移..." "${BLUE}"
    docker-compose -f docker-compose.yml exec -T backend npx knex migrate:latest
}

# 显示服务状态
show_status() {
    echo ""
    print_message "服务状态：" "${BLUE}"
    docker-compose -f docker-compose.yml ps
    echo ""
    print_message "访问地址：" "${GREEN}"
    echo "  - API: http://localhost"
    echo "  - 健康检查: http://localhost/health"
    echo "  - MinIO 控制台: http://localhost:9001"
    echo ""
    print_message "常用命令：" "${GREEN}"
    echo "  - 查看日志: docker-compose -f docker-compose.yml logs -f"
    echo "  - 停止服务: docker-compose -f docker-compose.yml down"
    echo "  - 重启服务: docker-compose -f docker-compose.yml restart"
    echo ""
}

# 主函数
main() {
    echo ""
    print_message "========================================" "${BLUE}"
    print_message "MoveUp Docker 部署脚本" "${BLUE}"
    print_message "========================================" "${NC}"
    echo ""

    check_docker
    check_env
    create_directories

    read -p "是否拉取最新镜像？(y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        pull_images
    fi

    build_images
    start_services
    wait_for_services

    read -p "是否执行数据库迁移？(y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        run_migrations
    fi

    show_status

    print_message "部署完成！" "${GREEN}"
}

# 捕获错误
trap 'print_message "部署失败！" "${RED}"' ERR

# 运行主函数
main "$@"
