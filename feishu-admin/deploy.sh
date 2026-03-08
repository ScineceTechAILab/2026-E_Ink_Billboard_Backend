#!/bin/bash

# 飞书管理后台前端快速部署脚本
# 使用方法: ./deploy.sh

set -euo pipefail

# 定义颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "=========================================="
echo "  飞书管理后台前端部署脚本"
echo "=========================================="
echo ""

# ================= 1. 环境变量检查与加载 =================
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

ENV_FILE=".env"

if [ ! -f "$ENV_FILE" ]; then
    echo -e "${RED}❌ 错误: 环境变量文件 $ENV_FILE 不存在！${NC}"
    echo ""
    echo "请按以下步骤操作："
    echo "  1. 复制 .env.example 为 .env："
    echo "     cp .env.example .env"
    echo "  2. 编辑 .env 文件，填入真实的 SERVER_IP"
    echo ""
    exit 1
fi

echo -e "${GREEN}✅ 加载环境变量文件: $ENV_FILE${NC}"
# shellcheck disable=SC1090
set -a
source "$ENV_FILE"
set +a

if [ -z "${SERVER_IP:-}" ]; then
    echo -e "${RED}❌ 错误: SERVER_IP 未在 $ENV_FILE 中配置！${NC}"
    echo ""
    echo "请编辑 .env 文件，添加或修改："
    echo "  SERVER_IP=你的服务器IP地址"
    echo ""
    exit 1
fi

echo -e "   服务器IP: ${SERVER_IP}"
echo ""

# ================= 2. 系统环境检查 =================

# 检查 Docker 是否安装
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker 未安装，请先安装 Docker${NC}"
    exit 1
fi

# 检查 Docker Compose 是否安装
if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}❌ Docker Compose 未安装，请先安装 Docker Compose${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Docker 和 Docker Compose 检查通过${NC}"
echo ""

# ================= 3. 配置文件处理 =================

# 检查 .env.production 是否存在，不存在则生成
if [ ! -f ".env.production" ]; then
    echo -e "${YELLOW}⚠️  警告: .env.production 文件不存在，正在自动生成...${NC}"
    
    # 默认配置
    DEFAULT_REDIRECT_URI="http://${SERVER_IP}/feishu/callback"
    DEFAULT_API_URL="/api"
    
    # 尝试从环境变量获取 App ID，如果没有则提示输入
    if [ -z "${FEISHU_APP_ID:-}" ]; then
        echo "请输入飞书 App ID (例如 cli_a6xxxx): "
        read -r FEISHU_APP_ID
    fi
    
    if [ -z "$FEISHU_APP_ID" ]; then
        echo -e "${RED}❌ 错误: 必须提供飞书 App ID 才能构建前端镜像！${NC}"
        exit 1
    fi

    echo "正在生成 .env.production ..."
    cat > .env.production <<EOF
VITE_FEISHU_APP_ID=$FEISHU_APP_ID
VITE_FEISHU_REDIRECT_URI=$DEFAULT_REDIRECT_URI
VITE_API_URL=$DEFAULT_API_URL
EOF
    echo -e "${GREEN}✅ .env.production 生成成功！${NC}"
    echo "--------------------------------"
    cat .env.production
    echo "--------------------------------"
    echo ""
fi

if [ ! -f "nginx.conf" ]; then
    echo -e "${YELLOW}⚠️  警告: nginx.conf 文件不存在${NC}"
fi

if [ ! -f "Dockerfile" ]; then
    echo -e "${YELLOW}⚠️  警告: Dockerfile 文件不存在${NC}"
fi

if [ ! -f "docker-compose.yml" ]; then
    echo -e "${YELLOW}⚠️  警告: docker-compose.yml 文件不存在${NC}"
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

# ================= 4. Docker 部署 =================

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
    echo -e "${GREEN}✅ 容器启动成功${NC}"
else
    echo -e "${RED}❌ 容器启动失败，请查看日志${NC}"
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
echo "  http://${SERVER_IP}"
echo ""
echo "🔧 常用命令:"
echo "  停止容器: docker-compose down"
echo "  重启容器: docker-compose restart"
echo "  查看日志: docker-compose logs -f"
echo ""
