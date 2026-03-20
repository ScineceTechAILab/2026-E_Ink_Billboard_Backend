# E_Ink_Billboard_Backend

墨水屏广告牌后端服务，基于 Spring Boot 3 构建，提供用户认证、媒体上传、设备管理、内容推送、播放队列与后台统计能力。

## 功能概览

- 用户认证：微信小程序登录、飞书管理员登录、JWT 鉴权
- 媒体管理：图片/视频上传、列表查询、删除、视频异步处理
- 设备管理：设备增删改查、设备状态查询、网络配置下发、当前播放内容查询
- 推送能力：单图推送、单视频推送、批量推送、推送历史查询
- MQTT 通信：设备心跳/状态上报与命令下发
- 平台能力：内容审核、公告、用户行为分析、设备状态统计

## 技术栈

- Java 17
- Spring Boot 3.2.1
- MyBatis-Plus 3.5.5
- PostgreSQL
- Redis
- MinIO
- MQTT (Spring Integration MQTT)
- JWT (jjwt)
- JavaCV / FFmpeg（视频处理）

## 项目结构

```text
.
├─src/main/java/com/stalab/e_ink_billboard_backend
│  ├─controller      # API 接口层
│  ├─service         # 业务逻辑层
│  ├─mapper          # MyBatis 数据访问层
│  ├─model           # DTO/VO
│  ├─config          # 配置类（MQTT、MinIO、Redis、异步等）
│  └─common          # 通用返回、异常、工具类
├─src/main/resources
│  ├─application.yml
│  ├─application-dev.yml
│  ├─application-prod.yml
│  └─dev-config.yml
├─docs               # SQL、MQTT、运维与操作文档
├─Dockerfile
└─pom.xml
```

## 运行环境

启动前建议准备以下依赖服务：

- PostgreSQL
- Redis
- MinIO
- MQTT Broker（如 EMQX）

## 配置说明

默认端口：`8083`

默认激活环境：`dev`（见 `src/main/resources/application.yml`）

项目配置中使用了占位符，主要变量包括：

- `myip`
- `pwd`
- `redis_password`
- `jwt_secret`
- `wechat_appid`
- `wechat_secret`
- `wx_mp_appid`
- `wx_mp_secret`
- `wx_mp_token`
- `wx_mp_aes_key`
- `feishu_app_id`
- `feishu_app_secret`
- `minio_access_key`
- `minio_secret_key`
- `emqx_username`
- `emqx_password`

建议做法：

1. 本地开发使用未提交到仓库的私有配置文件（或环境变量）。
2. 生产环境统一通过容器环境变量注入，不要把真实密钥写入仓库。

## 本地启动

1. 安装 JDK 17、Maven 3.9+
2. 准备数据库与中间件服务
3. 配置 `application-dev.yml` 引用的变量
4. 执行：

```bash
mvn clean package -DskipTests
mvn spring-boot:run
```

应用启动后访问：`http://localhost:8083`

## Docker 部署

项目根目录已提供：

- `Dockerfile`
- `docs/devops/deploy.sh`
- `docs/devops/deploy_local.sh`

常见流程：

```bash
mvn clean package -DskipTests
docker build -t e-ink-backend:latest .
docker run -d --name e-ink-container -p 8081:8083 e-ink-backend:latest
```

实际部署时请按脚本要求注入数据库、Redis、JWT、微信、飞书、MinIO、MQTT 等环境变量。

## 主要接口前缀

- `/api/auth`：登录认证
- `/api/image`：图片管理
- `/api/video`：视频管理
- `/api/device`：设备管理
- `/api/push`：推送管理
- `/api/admin`：后台管理
- `/api/admin/analytics`：统计分析
- `/api/wechat/mp/portal`：微信公众号接入

## 文档索引

- 接口文档：`API文档.md`
- 数据库脚本：`docs/database_schema_mqtt.sql`、`docs/audit_log.sql`
- MQTT 快速开始：`docs/MQTT_FX_QUICK_START.md`
- MQTT 测试指南：`docs/MQTT_TESTING_GUIDE.md`
- 操作手册：`docs/操作手册.md`
- 飞书登录集成说明：`docs/飞书管理员登录集成说明书.md`

## 开发建议

- 新增接口时保持统一响应结构（`Response`）
- 在 `docs/` 同步更新接口/部署说明
- 提交前执行一次打包校验：

```bash
mvn clean package -DskipTests
```
