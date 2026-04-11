#!/bin/bash
set -euo pipefail

# ================= 配置区 =================
# 是否使用 SSH 隧道代理 (1=开启, 0=关闭)
USE_PROXY=1
PROXY_ADDR="http://127.0.0.1:8888"

# 环境变量文件
ENV_FILE=".env"
# =========================================

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}==== 开始 Docker Compose 极速部署流程 ====${NC}"

# 0. 检查环境文件
echo "0. [System] 检查环境配置..."
if [ ! -f "$ENV_FILE" ]; then
    echo -e "${RED}⚠️ 未找到 $ENV_FILE 文件！${NC}"
    echo -e "${YELLOW}请执行: cp .env.example .env 并填入实际配置${NC}"
    exit 1
fi

# 加载环境变量用于校验
set -a
source "$ENV_FILE"
set +a

# 1. 拉取最新代码
echo "1. [Git] 准备拉取代码..."

if [ "$USE_PROXY" -eq 1 ]; then
    echo "   -> 正在应用本地代理: $PROXY_ADDR"
    git config --global http.proxy "$PROXY_ADDR"
fi

git fetch --all
git reset --hard origin/master

if [ "$USE_PROXY" -eq 1 ]; then
    git config --global --unset http.proxy || true
fi

# 2. 必填校验（避免运行时才发现配置错误）
echo "2. [Config] 校验必要配置..."
: "${SERVER_IP:?请在 .env 中设置 SERVER_IP}"
: "${JWT_SECRET:?请在 .env 中设置 JWT_SECRET}"
: "${POSTGRES_PASSWORD:?请在 .env 中设置 POSTGRES_PASSWORD}"
: "${REDIS_PASSWORD:?请在 .env 中设置 REDIS_PASSWORD}"
: "${MINIO_ACCESS_KEY:?请在 .env 中设置 MINIO_ACCESS_KEY}"
: "${MINIO_SECRET_KEY:?请在 .env 中设置 MINIO_SECRET_KEY}"

echo -e "${GREEN}   ✓ 配置校验通过${NC}"

# 3. 生成阿里云 Maven 配置
echo "3. [Maven] 配置阿里云加速源..."
cat > aliyun-settings.xml <<EOF
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                          https://maven.apache.org/xsd/settings-1.0.0.xsd">
    <localRepository>./.m2/repository</localRepository>
    <mirrors>
        <mirror>
            <id>aliyunmaven</id>
            <mirrorOf>*</mirrorOf>
            <name>阿里云公共仓库</name>
            <url>https://maven.aliyun.com/repository/public</url>
        </mirror>
    </mirrors>
</settings>
EOF

# 4. Maven 打包
echo "4. [Maven] 开始打包..."
mvn clean package -DskipTests -s aliyun-settings.xml

# 5. 停止旧的独立容器（如果存在）
echo "5. [Docker] 检查并停止旧的独立容器..."
OLD_CONTAINERS=("eink-postgres" "eink-redis" "eink-minio" "eink-emqx" "e-ink-container" "feishu-admin-frontend")
for container in "${OLD_CONTAINERS[@]}"; do
    if docker ps -a --format '{{.Names}}' | grep -q "^${container}$"; then
        echo "   停止并删除旧容器: $container"
        docker stop "$container" 2>/dev/null || true
        docker rm "$container" 2>/dev/null || true
    fi
done

# 6. Docker Compose 部署
echo "6. [Docker] 使用 Docker Compose 部署服务..."

# 检查是否首次部署（判断网络是否存在）
if ! docker network ls | grep -q "eink-net"; then
    echo -e "${YELLOW}   首次部署，将创建 Docker 网络...${NC}"
fi

# 构建并启动（会自动处理依赖关系和启动顺序）
docker-compose up -d --build

# 7. 等待服务健康检查
echo "7. [Health] 等待服务启动..."
sleep 5

# 检查关键服务状态
echo "   检查服务状态:"
docker-compose ps | grep -E "eink-|backend" || true

# 8. 清理临时文件
echo "8. [Cleanup] 清理临时文件..."
rm -f aliyun-settings.xml

echo -e "${GREEN}==== 部署完成 ====${NC}"
echo ""
echo -e "${GREEN}访问地址:${NC}"
echo "  - 后端 API:       http://${SERVER_IP}:8081"
echo "  - MinIO Console:  http://${SERVER_IP}:9002"
echo "  - EMQX Dashboard: http://${SERVER_IP}:18083"
echo ""
echo -e "${YELLOW}常用命令:${NC}"
echo "  查看日志:   docker-compose logs -f"
echo "  查看状态:   docker-compose ps"
echo "  重启服务:   docker-compose restart"
echo "  停止服务:   docker-compose down"
echo "  更新部署:   ./deploy.sh"
