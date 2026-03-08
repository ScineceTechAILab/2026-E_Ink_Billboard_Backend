#!/bin/bash

# 飞书管理后台前端快速部署脚本
# 使用方法: ./deploy.sh

set -e

echo "=========================================="
echo "  飞书管理后台前端部署脚本"
echo "=========================================="
echo ""

# 检查 Docker 是否安装
if ! command -v docker &> /dev/null; then
    echo "❌ Docker 未安装，请先安装 Docker"
    exit 1
fi

# 检查 Docker Compose 是否安装
if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose 未安装，请先安装 Docker Compose"
    exit 1
fi

echo "✅ Docker 和 Docker Compose 检查通过"
echo ""

# 获取当前目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# 检查配置文件
if [ ! -f ".env.production" ]; then
    echo "⚠️  警告: .env.production 文件不存在"
fi

if [ ! -f "nginx.conf" ]; then
    echo "⚠️  警告: nginx.conf 文件不存在"
fi

if [ ! -f "Dockerfile" ]; then
    echo "⚠️  警告: Dockerfile 文件不存在"
fi

if [ ! -f "docker-compose.yml" ]; then
    echo "⚠️  警告: docker-compose.yml 文件不存在"
fi

echo ""
read -p "是否继续部署? (y/n): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "部署已取消"
    exit 0
fi

echo ""
echo "=========================================="
echo "  开始部署..."
echo "=========================================="
echo ""

# 停止并删除旧容器（如果存在）
echo "1. 停止旧容器..."
docker-compose down 2>/dev/null || true

echo ""
echo "2. 构建并启动新容器..."
docker-compose up -d --build

echo ""
echo "3. 等待容器启动..."
sleep 5

echo ""
echo "4. 检查容器状态..."
if docker-compose ps | grep -q "Up"; then
    echo "✅ 容器启动成功"
else
    echo "❌ 容器启动失败，请查看日志"
    docker-compose logs
    exit 1
fi

echo ""
echo "=========================================="
echo "  部署完成！"
echo "=========================================="
echo ""
echo "📊 容器状态:"
docker-compose ps
echo ""
echo "📝 查看日志命令:"
echo "  docker-compose logs -f"
echo ""
echo "🌐 访问地址:"
echo ""
echo "🔧 常用命令:"
echo "  停止容器: docker-compose down"
echo "  重启容器: docker-compose restart"
echo "  查看日志: docker-compose logs -f"
echo ""
