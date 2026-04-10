#!/bin/bash
set -euo pipefail

# ================= 配置区 =================
# 本地快速部署脚本（不拉取代码，适合本地测试）
# =========================================

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}==== 🛠️ 开始本地构建流程 (Docker Compose 版) ====${NC}"
echo -e "${YELLOW}⚠️  注意：请确保你已经手动将最新代码上传到当前目录！${NC}"

# 0. 检查环境文件
echo "0. [System] 检查环境配置..."
if [ ! -f ".env" ]; then
    echo -e "${RED}⚠️ 未找到 .env 文件！${NC}"
    echo -e "${YELLOW}请执行: cp .env.example .env 并填入实际配置${NC}"
    exit 1
fi

# 加载环境变量
set -a
source ".env"
set +a

# 基础配置校验
: "${SERVER_IP:?请在 .env 中设置 SERVER_IP}"
: "${JWT_SECRET:?请在 .env 中设置 JWT_SECRET}"
: "${POSTGRES_PASSWORD:?请在 .env 中设置 POSTGRES_PASSWORD}"
: "${REDIS_PASSWORD:?请在 .env 中设置 REDIS_PASSWORD}"
: "${MINIO_ACCESS_KEY:?请在 .env 中设置 MINIO_ACCESS_KEY}"
: "${MINIO_SECRET_KEY:?请在 .env 中设置 MINIO_SECRET_KEY}"

echo -e "${GREEN}   ✓ 配置校验通过${NC}"

# 1. 权限清理
echo "1. [System] 清理旧构建文件..."
if [ -d "target" ] && [ ! -w "target" ]; then
    echo -e "${RED}清理 root 权限残留文件...${NC}"
    sudo rm -rf target
else
    rm -rf target 2>/dev/null || true
fi

# 2. 生成 Maven 配置
echo "2. [Maven] 配置阿里云加速源..."
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

# 3. Maven 打包
echo "3. [Maven] 开始打包..."
mvn clean package -DskipTests -s aliyun-settings.xml
if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Maven 打包失败！${NC}"
    rm -f aliyun-settings.xml
    exit 1
fi

# 4. 停止旧的独立容器（如果存在）
echo "4. [Docker] 检查并停止旧的独立容器..."
OLD_CONTAINERS=("eink-postgres" "eink-redis" "eink-minio" "eink-emqx" "e-ink-container")
for container in "${OLD_CONTAINERS[@]}"; do
    if docker ps -a --format '{{.Names}}' | grep -q "^${container}$"; then
        echo "   停止并删除旧容器: $container"
        docker stop "$container" 2>/dev/null || true
        docker rm "$container" 2>/dev/null || true
    fi
done

# 5. Docker Compose 部署
echo "5. [Docker] 使用 Docker Compose 部署..."

# 构建并启动
docker-compose up -d --build

# 6. 等待服务启动
echo "6. [Health] 等待服务启动..."
sleep 5

echo "   服务状态:"
docker-compose ps | grep -E "eink-|backend" || true

# 7. 清理
echo "7. [Cleanup] 清理临时文件..."
rm -f aliyun-settings.xml

echo -e "${GREEN}==== ✅ 本地构建并部署完成 ====${NC}"
echo ""
echo -e "${GREEN}访问地址:${NC}"
echo "  - 后端 API:       http://${SERVER_IP}:8081"
echo "  - MinIO Console:  http://${SERVER_IP}:9002"
echo "  - EMQX Dashboard: http://${SERVER_IP}:18083"
echo ""
echo -e "${YELLOW}常用命令:${NC}"
echo "  查看日志:   docker-compose logs -f backend"
echo "  查看状态:   docker-compose ps"
echo "  停止服务:   docker-compose down"
echo "  重启服务:   docker-compose restart"
