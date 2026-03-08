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

# 是否使用 SSH 隧道代理 (1=开启, 0=关闭)
# 如果你本地挂了 ssh -R 8888:...，这里改为 1 可极大提高成功率
USE_PROXY=1
PROXY_ADDR="http://127.0.0.1:8888"
# =========================================

# 定义颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color



# 1. 拉取最新代码 (强制覆盖模式)
echo "1. [Git] 准备拉取代码..."

# 临时设置代理
if [ "$USE_PROXY" -eq 1 ]; then
    echo -e "   -> 正在应用本地代理: $PROXY_ADDR"
    git config --global http.proxy $PROXY_ADDR
fi

# 强制重置代码，避免合并冲突
git fetch --all
git reset --hard origin/master

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ 代码拉取失败！请检查网络或 SSH 隧道状态。${NC}"
    # 失败后记得清理代理配置，以免影响其他程序
    [ "$USE_PROXY" -eq 1 ] && git config --global --unset http.proxy
    exit 1
fi

# 拉取成功后立即清理代理配置
[ "$USE_PROXY" -eq 1 ] && git config --global --unset http.proxy

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
