#!/bin/bash

# MoveUp Docker 快速停止脚本

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

# 停止服务
stop_services() {
    print_message "停止所有服务..." "${YELLOW}"
    docker-compose -f docker-compose.yml down
    print_message "服务已停止" "${GREEN}"
}

# 停止服务并删除卷
stop_all() {
    print_message "停止所有服务并删除数据卷..." "${YELLOW}"
    read -p "⚠️  警告：这将删除所有数据，确认继续？(y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        docker-compose -f docker-compose.yml down -v
        print_message "服务已停止，数据卷已删除" "${GREEN}"
    else
        print_message "操作已取消" "${YELLOW}"
    fi
}

# 主函数
main() {
    echo ""
    print_message "========================================" "${BLUE}"
    print_message "MoveUp Docker 停止脚本" "${BLUE}"
    print_message "========================================" "${NC}"
    echo ""

    echo "选择停止方式："
    echo "  1) 停止服务（保留数据）"
    echo "  2) 停止服务并删除数据"
    echo "  3) 取消"
    echo ""
    read -p "请选择 [1-3]: " -n 1 -r
    echo ""

    case $REPLY in
        1)
            stop_services
            ;;
        2)
            stop_all
            ;;
        3)
            print_message "操作已取消" "${YELLOW}"
            exit 0
            ;;
        *)
            print_message "无效的选择" "${RED}"
            exit 1
            ;;
    esac

    print_message "完成！" "${GREEN}"
}

# 运行主函数
main "$@"
