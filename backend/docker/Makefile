# MoveUp Docker 部署 Makefile

.PHONY: help build up down restart logs ps clean backup restore

# 默认目标
.DEFAULT_GOAL := help

# 颜色输出
BLUE := \033[0;34m
GREEN := \033[0;32m
YELLOW := \033[0;33m
RED := \033[0;31m
NC := \033[0m # No Color

##@ 帮助信息
help: ## 显示帮助信息
	@echo "$(BLUE)MoveUp Docker 部署命令$(NC)"
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "$(GREEN)%-20s$(NC) %s\n", $$1, $$2}'

##@ 开发命令
dev: ## 启动开发环境
	@echo "$(BLUE)启动开发环境...$(NC)"
	docker-compose -f docker-compose.dev.yml up -d

dev-logs: ## 查看开发环境日志
	docker-compose -f docker-compose.dev.yml logs -f

##@ 生产命令
build: ## 构建所有服务镜像
	@echo "$(BLUE)构建 Docker 镜像...$(NC)"
	docker-compose -f docker-compose.yml build

up: ## 启动所有服务
	@echo "$(GREEN)启动所有服务...$(NC)"
	docker-compose -f docker-compose.yml up -d
	@echo "$(GREEN)服务启动完成！$(NC)"
	@echo "使用 'make ps' 查看服务状态"
	@echo "使用 'make logs' 查看日志"

down: ## 停止所有服务
	@echo "$(YELLOW)停止所有服务...$(NC)"
	docker-compose -f docker-compose.yml down

restart: ## 重启所有服务
	@echo "$(BLUE)重启所有服务...$(NC)"
	docker-compose -f docker-compose.yml restart

ps: ## 查看服务状态
	@echo "$(BLUE)服务状态：$(NC)"
	docker-compose -f docker-compose.yml ps

logs: ## 查看所有服务日志
	docker-compose -f docker-compose.yml logs -f

logs-backend: ## 查看后端服务日志
	docker-compose -f docker-compose.yml logs -f backend

logs-db: ## 查看数据库日志
	docker-compose -f docker-compose.yml logs -f postgres

logs-redis: ## 查看 Redis 日志
	docker-compose -f docker-compose.yml logs -f redis

##@ 数据库操作
migrate: ## 执行数据库迁移
	@echo "$(BLUE)执行数据库迁移...$(NC)"
	docker-compose -f docker-compose.yml exec backend npx knex migrate:latest

migrate-rollback: ## 回滚数据库迁移
	@echo "$(YELLOW)回滚数据库迁移...$(NC)"
	docker-compose -f docker-compose.yml exec backend npx knex migrate:rollback

migrate-status: ## 查看迁移状态
	@echo "$(BLUE)数据库迁移状态：$(NC)"
	docker-compose -f docker-compose.yml exec backend npx knex migrate:status

db-shell: ## 进入数据库 Shell
	docker-compose -f docker-compose.yml exec postgres psql -U postgres -d moveup_db

##@ 备份与恢复
backup: ## 备份数据库
	@echo "$(BLUE)备份数据库...$(NC)"
	@mkdir -p backups
	docker-compose -f docker-compose.yml exec -T postgres pg_dump -U postgres moveup_db > backups/moveup-backup-$$(date +%Y%m%d-%H%M%S).sql
	@echo "$(GREEN)备份完成！$(NC)"

restore: FILE= ## 恢复数据库（使用 make restore FILE=backup.sql）
	@if [ -z "$(FILE)" ]; then \
		echo "$(RED)错误：请指定备份文件，例如：make restore FILE=backup.sql$(NC)"; \
		exit 1; \
	fi
	@echo "$(BLUE)恢复数据库从 $(FILE)...$(NC)"
	docker-compose -f docker-compose.yml exec -T postgres psql -U postgres moveup_db < $(FILE)
	@echo "$(GREEN)恢复完成！$(NC)"

##@ 清理操作
clean: ## 停止并删除容器和卷
	@echo "$(YELLOW)停止并删除所有容器和卷...$(NC)"
	docker-compose -f docker-compose.yml down -v
	@echo "$(GREEN)清理完成！$(NC)"

clean-all: ## 完全清理（包括镜像）
	@echo "$(YELLOW)完全清理（包括删除镜像）...$(NC)"
	docker-compose -f docker-compose.yml down -v --rmi all
	@echo "$(GREEN)清理完成！$(NC)"

prune: ## 清理未使用的 Docker 资源
	@echo "$(YELLOW)清理未使用的 Docker 资源...$(NC)"
	docker system prune -f

##@ 监控命令
stats: ## 查看资源使用情况
	docker stats

health: ## 检查服务健康状态
	@echo "$(BLUE)检查服务健康状态...$(NC)"
	@curl -s http://localhost/health | jq '.' || echo "健康检查失败"

test-api: ## 运行 API 测试
	@echo "$(BLUE)运行 API 测试...$(NC)"
	docker-compose exec backend npm test

##@ 初始化命令
init: ## 初始化项目（复制配置文件并启动）
	@echo "$(BLUE)初始化项目...$(NC)"
	@if [ ! -f .env ]; then \
		cp .env.example .env; \
		echo "$(GREEN)已创建 .env 文件，请根据需要修改配置$(NC)"; \
	fi
	@mkdir -p nginx/ssl backups
	@echo "$(GREEN)初始化完成！$(NC)"
	@echo "运行 'make up' 启动服务"

##@ 更新命令
pull: ## 拉取最新的基础镜像
	@echo "$(BLUE)拉取最新镜像...$(NC)"
	docker-compose -f docker-compose.yml pull

rebuild: ## 重新构建并启动服务
	@echo "$(BLUE)重新构建并启动服务...$(NC)"
	docker-compose -f docker-compose.yml up -d --build

##@ 开发工具
install: ## 安装依赖
	@echo "$(BLUE)安装依赖...$(NC)"
	cd backend && npm install

dev-server: ## 启动本地开发服务器（不使用 Docker）
	@echo "$(BLUE)启动本地开发服务器...$(NC)"
	cd backend && npm run dev
