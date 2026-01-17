# 墨水屏广告牌后端 API 文档

## 目录
- [基础信息](#基础信息)
- [认证方式](#认证方式)
- [响应格式](#响应格式)
- [API 接口](#api-接口)
  - [认证相关](#1-认证相关)
  - [图片管理](#2-图片管理)
  - [视频管理](#3-视频管理)
  - [管理员功能](#4-管理员功能)
  - [设备管理](#5-设备管理)
  - [内容推送](#6-内容推送)
  - [MQTT消息格式](#7-mqtt消息格式)
- [数据模型](#数据模型)
- [错误码说明](#错误码说明)
- [枚举值说明](#枚举值说明)

---

## 基础信息

- **Base URL**: `/api`
- **Content-Type**: `application/json` (除文件上传接口外)
- **文件上传**: `multipart/form-data`

---

## 认证方式

大部分接口需要在请求头中携带 JWT Token：

```
Authorization: Bearer <token>
```

Token 格式为 `Bearer <token>`，中间有一个空格。

通过 `/api/auth/login` 接口登录后获取 Token。

---

## 响应格式

所有接口统一返回以下格式：

```json
{
  "code": 200,
  "info": "操作成功",
  "data": {}
}
```

**字段说明**：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| code | Integer | 状态码：200-成功，400-请求错误，401-未授权，404-未找到 |
| info | String | 提示信息 |
| data | Object | 响应数据，类型根据接口而定 |

---

## API 接口

### 1. 认证相关

#### 1.1 微信登录

**接口描述**：使用微信小程序 code 进行登录，获取用户信息和 Token。

- **请求方法**：`POST`
- **请求路径**：`/api/auth/login`
- **是否需要认证**：否

**请求参数**：

| 参数名 | 类型 | 必填 | 位置 | 说明 |
|--------|------|------|------|------|
| code | String | 是 | Body | 微信小程序登录凭证 code |

**请求示例**：

```json
{
  "code": "081abc123def456ghi789jkl012mno34"
}
```

**响应示例**：

```json
{
  "code": 200,
  "info": "登录成功",
  "data": {
    "nickname": "用户昵称",
    "role": "USER",
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

**响应数据说明**：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| nickname | String | 用户昵称 |
| role | String | 用户角色：USER（普通用户）或 ADMIN（管理员） |
| token | String | JWT Token |

**错误响应**：

| 错误码 | 说明 |
|--------|------|
| 400 | 请求参数错误 |
| 401 | 登录失败 |

---

### 2. 图片管理

#### 2.1 上传图片

**接口描述**：上传图片文件，系统会自动处理并返回图片信息。

- **请求方法**：`POST`
- **请求路径**：`/api/image/upload`
- **是否需要认证**：是

**请求参数**：

| 参数名 | 类型 | 必填 | 位置 | 说明 |
|--------|------|------|------|------|
| file | MultipartFile | 是 | Form Data | 图片文件 |
| Authorization | String | 是 | Header | Bearer Token |

**响应示例**：

```json
{
  "code": 200,
  "info": "上传成功",
  "data": {
    "id": 123,
    "url": "https://example.com/images/processed/xxx.jpg"
  }
}
```

**响应数据说明**：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | Long | 图片ID |
| url | String | 处理后的图片URL |

**错误响应**：

| 错误码 | 说明 |
|--------|------|
| 400 | 上传文件不能为空 |
| 401 | Token 无效 |

---

#### 2.2 删除图片

**接口描述**：删除指定的图片。普通用户只能删除自己的图片，管理员可以删除任意图片。

- **请求方法**：`DELETE`
- **请求路径**：`/api/image/{id}`
- **是否需要认证**：是

**路径参数**：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 图片ID |

**请求头**：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| Authorization | String | 是 | Bearer Token |

**响应示例**：

```json
{
  "code": 200,
  "info": "删除成功",
  "data": null
}
```

**错误响应**：

| 错误码 | 说明 |
|--------|------|
| 400 | 删除失败（如无权限、图片不存在等） |
| 401 | Token 无效 |

---

#### 2.3 查询图片列表

**接口描述**：分页查询图片列表。普通用户只能查看自己的图片，管理员可以查看所有图片并支持筛选。

- **请求方法**：`GET`
- **请求路径**：`/api/image/list`
- **是否需要认证**：是

**查询参数**：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| current | Long | 否 | 当前页码，默认为 1 |
| size | Long | 否 | 每页大小，默认为 10 |
| userId | Long | 否 | 用户ID（仅管理员可用） |
| auditStatus | String | 否 | 审核状态：PENDING（待审核）、APPROVED（已通过）、REJECTED（已拒绝） |

**请求头**：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| Authorization | String | 是 | Bearer Token |

**响应示例**：

```json
{
  "code": 200,
  "info": "查询成功",
  "data": {
    "records": [
      {
        "id": 123,
        "userId": 1,
        "fileName": "image.jpg",
        "fileSize": 102400,
        "originalUrl": "https://example.com/images/original/xxx.jpg",
        "processedUrl": "https://example.com/images/processed/xxx.jpg",
        "auditStatus": "APPROVED",
        "auditReason": null,
        "createTime": "2026-01-07T10:30:00"
      }
    ],
    "total": 100,
    "current": 1,
    "size": 10,
    "pages": 10
  }
}
```

**响应数据说明**：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| records | Array | 图片列表 |
| total | Long | 总记录数 |
| current | Long | 当前页码 |
| size | Long | 每页大小 |
| pages | Long | 总页数 |

**ImageVO 字段说明**：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | Long | 图片ID |
| userId | Long | 用户ID |
| fileName | String | 文件名 |
| fileSize | Long | 文件大小（字节） |
| originalUrl | String | 原图URL |
| processedUrl | String | 处理后的图片URL |
| auditStatus | String | 审核状态：PENDING、APPROVED、REJECTED |
| auditReason | String | 审核原因（审核未通过时会有值） |
| createTime | String | 创建时间（ISO 8601 格式） |

**错误响应**：

| 错误码 | 说明 |
|--------|------|
| 400 | 审核状态参数无效 |
| 401 | Token 无效 |

---

### 3. 视频管理

#### 3.1 上传视频

**接口描述**：上传视频文件。视频处理是异步的，上传后立即返回视频ID。

- **请求方法**：`POST`
- **请求路径**：`/api/video/upload`
- **是否需要认证**：是

**请求参数**：

| 参数名 | 类型 | 必填 | 位置 | 说明 |
|--------|------|------|------|------|
| file | MultipartFile | 是 | Form Data | 视频文件 |
| Authorization | String | 是 | Header | Bearer Token |

**响应示例**：

```json
{
  "code": 200,
  "info": "上传成功",
  "data": 456
}
```

**响应数据说明**：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| data | Long | 视频ID |

**错误响应**：

| 错误码 | 说明 |
|--------|------|
| 401 | Token 无效 |

---

#### 3.2 查询视频处理状态

**接口描述**：查询视频的处理状态。

- **请求方法**：`GET`
- **请求路径**：`/api/video/status/{id}`
- **是否需要认证**：否

**路径参数**：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 视频ID |

**响应示例**：

```json
{
  "code": 200,
  "info": "成功",
  "data": {
    "status": "SUCCESS",
    "processedUrl": "https://example.com/videos/processed/xxx.mp4",
    "failReason": null
  }
}
```

**响应数据说明**：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| status | String | 处理状态：PROCESSING（处理中）、SUCCESS（成功）、FAILED（失败） |
| processedUrl | String | 处理后的视频URL（处理成功时才有值） |
| failReason | String | 失败原因（处理失败时才有值） |

**错误响应**：

| 错误码 | 说明 |
|--------|------|
| 404 | 视频不存在 |

---

#### 3.3 删除视频

**接口描述**：删除指定的视频。普通用户只能删除自己的视频，管理员可以删除任意视频。

- **请求方法**：`DELETE`
- **请求路径**：`/api/video/{id}`
- **是否需要认证**：是

**路径参数**：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 视频ID |

**请求头**：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| Authorization | String | 是 | Bearer Token |

**响应示例**：

```json
{
  "code": 200,
  "info": "删除成功",
  "data": null
}
```

**错误响应**：

| 错误码 | 说明 |
|--------|------|
| 400 | 删除失败（如无权限、视频不存在等） |
| 401 | Token 无效 |

---

#### 3.4 查询视频列表

**接口描述**：分页查询视频列表。普通用户只能查看自己的视频，管理员可以查看所有视频并支持筛选。

- **请求方法**：`GET`
- **请求路径**：`/api/video/list`
- **是否需要认证**：是

**查询参数**：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| current | Long | 否 | 当前页码，默认为 1 |
| size | Long | 否 | 每页大小，默认为 10 |
| userId | Long | 否 | 用户ID（仅管理员可用） |
| auditStatus | String | 否 | 审核状态：PENDING、APPROVED、REJECTED |
| processingStatus | String | 否 | 处理状态：PROCESSING、SUCCESS、FAILED |

**请求头**：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| Authorization | String | 是 | Bearer Token |

**响应示例**：

```json
{
  "code": 200,
  "info": "查询成功",
  "data": {
    "records": [
      {
        "id": 456,
        "userId": 1,
        "fileName": "video.mp4",
        "fileSize": 10485760,
        "originalUrl": "https://example.com/videos/original/xxx.mp4",
        "processedUrl": "https://example.com/videos/processed/xxx.mp4",
        "duration": 30,
        "frameCount": 900,
        "auditStatus": "APPROVED",
        "processingStatus": "SUCCESS",
        "failReason": null,
        "createTime": "2026-01-07T10:30:00"
      }
    ],
    "total": 50,
    "current": 1,
    "size": 10,
    "pages": 5
  }
}
```

**响应数据说明**：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| records | Array | 视频列表 |
| total | Long | 总记录数 |
| current | Long | 当前页码 |
| size | Long | 每页大小 |
| pages | Long | 总页数 |

**VideoVO 字段说明**：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | Long | 视频ID |
| userId | Long | 用户ID |
| fileName | String | 文件名 |
| fileSize | Long | 文件大小（字节） |
| originalUrl | String | 原视频URL |
| processedUrl | String | 处理后的视频URL |
| duration | Integer | 时长（秒） |
| frameCount | Integer | 总帧数 |
| auditStatus | String | 审核状态：PENDING、APPROVED、REJECTED |
| processingStatus | String | 处理状态：PROCESSING、SUCCESS、FAILED |
| failReason | String | 失败原因（处理失败时才有值） |
| createTime | String | 创建时间（ISO 8601 格式） |

**错误响应**：

| 错误码 | 说明 |
|--------|------|
| 401 | Token 无效 |

---

### 4. 管理员功能

#### 4.1 获取统计数据

**接口描述**：获取系统统计数据，包括在线设备数、待审核内容数、已通过内容数等。仅管理员可用。

- **请求方法**：`GET`
- **请求路径**：`/api/admin/stats`
- **是否需要认证**：是

**请求头**：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| Authorization | String | 是 | Bearer Token |

**响应示例**：

```json
{
  "code": 200,
  "info": "操作成功",
  "data": {
    "onlineDevices": 10,
    "pendingAudits": 5,
    "approvedContent": 100,
    "online": 10,
    "pending": 5,
    "approved": 100
  }
}
```

**响应数据说明**：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| onlineDevices | Integer | 在线设备数量 |
| pendingAudits | Integer | 待审核内容数量 |
| approvedContent | Integer | 已通过内容数量 |
| online | Integer | 在线设备数量（兼容字段） |
| pending | Integer | 待审核内容数量（兼容字段） |
| approved | Integer | 已通过内容数量（兼容字段） |

**错误响应**：

| 错误码 | 说明 |
|--------|------|
| 401 | Token 无效或非管理员用户 |

---

### 5. 设备管理

#### 5.1 获取设备列表

**接口描述**：获取设备列表。游客只能看到在线设备，管理员可以看到所有设备。

- **请求方法**：`GET`
- **请求路径**：`/api/device/list`
- **是否需要认证**：是

**请求头**：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| Authorization | String | 是 | Bearer Token |

**响应示例**：

```json
{
  "code": 200,
  "info": "查询成功",
  "data": [
    {
      "id": 1,
      "deviceName": "大厅屏幕1",
      "deviceCode": "ESP32_001",
      "mqttTopic": "device/ESP32_001/cmd",
      "status": "ONLINE",
      "lastHeartbeat": "2026-01-07T10:30:00",
      "currentContentId": 123,
      "currentContentType": "IMAGE",
      "currentContent": {
        "contentId": 123,
        "contentType": "IMAGE",
        "fileName": "example.jpg",
        "thumbnailUrl": "http://minio-host/bucket/original/example.jpg",
        "fileSize": 102400
      },
      "location": "大厅",
      "description": "大厅入口处的墨水屏",
      "createTime": "2026-01-01T00:00:00"
    }
  ]
}
```

**响应数据说明**：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | Long | 设备ID |
| deviceName | String | 设备名称 |
| deviceCode | String | 设备编码（唯一标识） |
| mqttTopic | String | MQTT主题 |
| status | String | 设备状态：ONLINE（在线）、OFFLINE（离线） |
| lastHeartbeat | String | 最后心跳时间 |
| currentContentId | Long | 当前显示的内容ID |
| currentContentType | String | 当前内容类型：IMAGE（图片）、VIDEO（视频） |
| currentContent | Object | 当前内容详细信息（如果正在播放） |
| location | String | 位置信息 |
| description | String | 设备描述 |
| createTime | String | 创建时间 |

**错误响应**：

| 错误码 | 说明 |
|--------|------|
| 401 | Token 无效 |

---

#### 5.2 获取设备详情

**接口描述**：根据设备ID获取设备详细信息。

- **请求方法**：`GET`
- **请求路径**：`/api/device/{id}`
- **是否需要认证**：是

**路径参数**：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 设备ID |

**请求头**：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| Authorization | String | 是 | Bearer Token |

**响应示例**：同 5.1 获取设备列表（单个设备对象）

**错误响应**：

| 错误码 | 说明 |
|--------|------|
| 401 | Token 无效 |
| 400 | 设备不存在 |

---

#### 5.3 添加设备（仅管理员）

**接口描述**：添加新设备。仅管理员可用。

- **请求方法**：`POST`
- **请求路径**：`/api/device`
- **是否需要认证**：是（管理员）

**请求头**：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| Authorization | String | 是 | Bearer Token |

**请求体**：

```json
{
  "deviceName": "大厅屏幕1",
  "deviceCode": "ESP32_001",
  "mqttTopic": "device/ESP32_001/cmd",
  "location": "大厅",
  "description": "大厅入口处的墨水屏"
}
```

**请求参数说明**：

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| deviceName | String | 是 | 设备名称 |
| deviceCode | String | 是 | 设备编码（唯一标识，如ESP32的MAC地址） |
| mqttTopic | String | 否 | MQTT主题（如果不提供，会自动生成：device/{deviceCode}/cmd） |
| location | String | 否 | 位置信息 |
| description | String | 否 | 设备描述 |

**响应示例**：

```json
{
  "code": 200,
  "info": "添加成功",
  "data": {
    "id": 1,
    "deviceName": "大厅屏幕1",
    "deviceCode": "ESP32_001",
    "mqttTopic": "device/ESP32_001/cmd",
    "status": "OFFLINE",
    "createTime": "2026-01-07T10:30:00"
  }
}
```

**错误响应**：

| 错误码 | 说明 |
|--------|------|
| 401 | Token 无效 |
| 403 | 无权执行此操作，需要管理员权限 |
| 400 | 设备编码已存在 |

---

#### 5.4 更新设备（仅管理员）

**接口描述**：更新设备信息。仅管理员可用。

- **请求方法**：`PUT`
- **请求路径**：`/api/device/{id}`
- **是否需要认证**：是（管理员）

**路径参数**：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 设备ID |

**请求体**：同 5.3 添加设备

**响应示例**：

```json
{
  "code": 200,
  "info": "更新成功"
}
```

**错误响应**：

| 错误码 | 说明 |
|--------|------|
| 401 | Token 无效 |
| 403 | 无权执行此操作，需要管理员权限 |
| 400 | 设备不存在或设备编码已存在 |

---

#### 5.5 删除设备（仅管理员）

**接口描述**：删除设备。仅管理员可用。删除设备会级联删除所有推送记录。

- **请求方法**：`DELETE`
- **请求路径**：`/api/device/{id}`
- **是否需要认证**：是（管理员）

**路径参数**：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 设备ID |

**响应示例**：

```json
{
  "code": 200,
  "info": "删除成功"
}
```

**错误响应**：

| 错误码 | 说明 |
|--------|------|
| 401 | Token 无效 |
| 403 | 无权执行此操作，需要管理员权限 |
| 400 | 设备不存在 |

---

#### 5.6 获取设备播放队列列表（仅管理员）

**接口描述**：获取指定设备的播放队列列表。队列按优先级排序（游客内容优先于管理员内容）。仅管理员可用。

- **请求方法**：`GET`
- **请求路径**：`/api/device/{id}/queue`
- **是否需要认证**：是（管理员）

**路径参数**：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 设备ID |

**请求头**：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| Authorization | String | 是 | Bearer Token |

**响应示例**：

```json
{
  "code": 200,
  "info": "查询成功",
  "data": [
    {
      "contentId": 123,
      "contentType": "IMAGE",
      "pushId": 1,
      "fileName": "example.jpg",
      "thumbnailUrl": "http://minio-host/bucket/original/example.jpg",
      "fileSize": 102400,
      "duration": null,
      "userId": 1,
      "userName": "张三",
      "userRole": "VISITOR",
      "position": 1
    },
    {
      "contentId": 456,
      "contentType": "VIDEO",
      "pushId": 2,
      "fileName": "video.mp4",
      "thumbnailUrl": "http://minio-host/bucket/original/video.mp4",
      "fileSize": 10485760,
      "duration": 30,
      "userId": 2,
      "userName": "管理员",
      "userRole": "ADMIN",
      "position": 2
    }
  ]
}
```

**响应数据说明**：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| contentId | Long | 内容ID |
| contentType | String | 内容类型：IMAGE（图片）、VIDEO（视频） |
| pushId | Long | 推送记录ID |
| fileName | String | 文件名 |
| thumbnailUrl | String | 缩略图URL（图片为原图URL，视频为第一帧或封面URL） |
| fileSize | Long | 文件大小（字节） |
| duration | Integer | 视频时长（秒，仅视频有，图片为null） |
| userId | Long | 用户ID |
| userName | String | 用户昵称 |
| userRole | String | 用户角色：ADMIN（管理员）、VISITOR（游客） |
| position | Integer | 队列中的位置（从1开始，1表示下一个播放） |

**错误响应**：

| 错误码 | 说明 |
|--------|------|
| 401 | Token 无效 |
| 403 | 无权执行此操作，需要管理员权限 |
| 400 | 设备不存在 |

---

#### 5.7 删除设备播放队列中的项（仅管理员）

**接口描述**：删除设备播放队列中的指定项。可以一次删除多个队列项。仅管理员可用。

- **请求方法**：`DELETE`
- **请求路径**：`/api/device/{id}/queue`
- **是否需要认证**：是（管理员）

**路径参数**：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 设备ID |

**请求头**：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| Authorization | String | 是 | Bearer Token |

**请求体**：

```json
{
  "pushIds": [1, 2, 3]
}
```

**请求参数说明**：

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| pushIds | List<Long> | 是 | 要删除的推送记录ID列表 |

**响应示例**：

```json
{
  "code": 200,
  "info": "删除成功",
  "data": 3
}
```

**响应数据说明**：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| data | Integer | 成功删除的队列项数量 |

**错误响应**：

| 错误码 | 说明 |
|--------|------|
| 401 | Token 无效 |
| 403 | 无权执行此操作，需要管理员权限 |
| 400 | 设备不存在、推送记录ID列表不能为空 |

---

### 6. 内容推送

#### 6.1 推送图片到设备

**接口描述**：推送图片到指定设备。游客只能推送自己的已审核通过的图片，管理员可以推送所有已审核通过的图片。

- **请求方法**：`POST`
- **请求路径**：`/api/push/image`
- **是否需要认证**：是

**请求头**：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| Authorization | String | 是 | Bearer Token |

**请求体**：

```json
{
  "deviceId": 1,
  "imageId": 123
}
```

**请求参数说明**：

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| deviceId | Long | 是 | 设备ID |
| imageId | Long | 是 | 图片ID |

**响应示例**：

```json
{
  "code": 200,
  "info": "推送成功"
}
```

**错误响应**：

| 错误码 | 说明 |
|--------|------|
| 401 | Token 无效 |
| 400 | 设备不存在、设备离线（游客）、图片不存在、图片未审核通过、无权推送此图片、今日播放次数已达上限（游客） |

**业务规则**：
- 游客只能推送到在线设备
- 游客只能推送自己的已审核通过的图片
- 游客每日最多推送5个内容
- 管理员可以推送到所有设备（包括离线设备）
- 管理员可以推送所有已审核通过的内容

---

#### 6.2 推送视频到设备

**接口描述**：推送视频到指定设备。逻辑同推送图片。

- **请求方法**：`POST`
- **请求路径**：`/api/push/video`
- **是否需要认证**：是

**请求体**：

```json
{
  "deviceId": 1,
  "videoId": 456
}
```

**请求参数说明**：

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| deviceId | Long | 是 | 设备ID |
| videoId | Long | 是 | 视频ID |

**响应示例**：同 6.1 推送图片

**错误响应**：同 6.1 推送图片

---

#### 6.3 批量推送到多个设备

**接口描述**：将同一内容推送到多个设备。

- **请求方法**：`POST`
- **请求路径**：`/api/push/batch`
- **是否需要认证**：是

**请求体**：

```json
{
  "deviceIds": [1, 2, 3],
  "contentId": 123,
  "contentType": "IMAGE"
}
```

**请求参数说明**：

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| deviceIds | List<Long> | 是 | 设备ID列表 |
| contentId | Long | 是 | 内容ID（图片或视频） |
| contentType | String | 是 | 内容类型：IMAGE（图片）、VIDEO（视频） |

**响应示例**：

```json
{
  "code": 200,
  "info": "批量推送成功"
}
```

**错误响应**：同 6.1 推送图片

---

#### 6.4 查询推送历史

**接口描述**：查询推送历史记录（分页）。游客只能查看自己的推送记录，管理员可以查看所有推送记录。

- **请求方法**：`GET`
- **请求路径**：`/api/push/history`
- **是否需要认证**：是

**查询参数**：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| current | Long | 否 | 当前页码（默认1） |
| size | Long | 否 | 每页大小（默认10，最大100） |
| deviceId | Long | 否 | 设备ID（筛选条件） |
| userId | Long | 否 | 用户ID（筛选条件，仅管理员可用） |

**响应示例**：

```json
{
  "code": 200,
  "info": "查询成功",
  "data": {
    "records": [
      {
        "id": 1,
        "deviceId": 1,
        "deviceName": "大厅屏幕1",
        "contentId": 123,
        "contentType": "IMAGE",
        "pushStatus": "SUCCESS",
        "pushTime": "2026-01-07T10:30:00",
        "userId": 1,
        "userName": "张三",
        "errorMessage": null
      }
    ],
    "total": 100,
    "current": 1,
    "size": 10,
    "pages": 10
  }
}
```

**响应数据说明**：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | Long | 推送记录ID |
| deviceId | Long | 设备ID |
| deviceName | String | 设备名称 |
| contentId | Long | 内容ID |
| contentType | String | 内容类型：IMAGE、VIDEO |
| pushStatus | String | 推送状态：PENDING（待发送）、SENT（已发送）、SUCCESS（成功）、FAILED（失败） |
| pushTime | String | 推送时间 |
| userId | Long | 用户ID |
| userName | String | 用户昵称 |
| errorMessage | String | 错误信息（失败时才有） |

**错误响应**：

| 错误码 | 说明 |
|--------|------|
| 401 | Token 无效 |

---

### 7. MQTT消息格式

#### 7.1 后端 → ESP32（命令消息）

**Topic格式**：`device/{deviceCode}/cmd`

**消息格式**：

```json
{
  "type": "IMAGE",
  "contentId": 123,
  "url": "http://minio-host/bucket/processed/image_dithered.bin?X-Amz-Algorithm=...",
  "size": 10240,
  "md5": "abc123def456...",
  "timestamp": 1704614400000,
  "messageId": "uuid-string"
}
```

**字段说明**：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| type | String | 内容类型：IMAGE（图片）、VIDEO（视频） |
| contentId | Long | 内容ID |
| url | String | 下载URL（Presigned URL，有效期2小时） |
| size | Long | 文件大小（字节） |
| md5 | String | 文件MD5值（可选） |
| timestamp | Long | 时间戳（毫秒） |
| messageId | String | 消息ID（UUID，用于状态上报时关联） |

---

#### 7.2 ESP32 → 后端（状态上报）

**Topic格式**：`device/{deviceCode}/status`

**消息格式**：

```json
{
  "status": "SUCCESS",
  "messageId": "uuid-string",
  "error": "错误信息（失败时才有）",
  "timestamp": 1704614400000
}
```

**字段说明**：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| status | String | 状态：SUCCESS（成功）、FAILED（失败）、DOWNLOADING（下载中）、DISPLAYING（显示中） |
| messageId | String | 对应的命令消息ID |
| error | String | 错误信息（失败时才有） |
| timestamp | Long | 时间戳（毫秒） |

---

#### 7.3 ESP32 → 后端（心跳消息）

**Topic格式**：`device/{deviceCode}/heartbeat`

**消息格式**：

```json
{
  "deviceCode": "ESP32_001",
  "status": "ONLINE",
  "currentContentId": 123,
  "currentContentType": "IMAGE",
  "battery": 85,
  "signal": -65,
  "timestamp": 1704614400000
}
```

**字段说明**：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| deviceCode | String | 设备编码 |
| status | String | 设备状态：ONLINE |
| currentContentId | Long | 当前显示的内容ID（可选） |
| currentContentType | String | 当前内容类型：IMAGE、VIDEO（可选） |
| battery | Integer | 电池电量（0-100，可选） |
| signal | Integer | WiFi信号强度（dBm，可选） |
| timestamp | Long | 时间戳（毫秒） |

---

## 数据模型

### Response<T>

统一响应格式。

| 字段名 | 类型 | 说明 |
|--------|------|------|
| code | Integer | 状态码：200-成功，400-请求错误，401-未授权，404-未找到 |
| info | String | 提示信息 |
| data | T | 响应数据，类型根据接口而定 |

### PageResult<T>

分页结果格式。

| 字段名 | 类型 | 说明 |
|--------|------|------|
| records | List<T> | 数据列表 |
| total | Long | 总记录数 |
| current | Long | 当前页码 |
| size | Long | 每页大小 |
| pages | Long | 总页数 |

### LoginDTO

登录请求参数。

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| code | String | 是 | 微信小程序登录凭证 code |

### LoginVO

登录响应数据。

| 字段名 | 类型 | 说明 |
|--------|------|------|
| nickname | String | 用户昵称 |
| role | String | 用户角色：USER（普通用户）或 ADMIN（管理员） |
| token | String | JWT Token |

### ImageUploadVO

图片上传响应数据。

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | Long | 图片ID |
| url | String | 处理后的图片URL |

### ImageVO

图片信息。

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | Long | 图片ID |
| userId | Long | 用户ID |
| fileName | String | 文件名 |
| fileSize | Long | 文件大小（字节） |
| originalUrl | String | 原图URL |
| processedUrl | String | 处理后的图片URL |
| auditStatus | String | 审核状态：PENDING、APPROVED、REJECTED |
| auditReason | String | 审核原因 |
| createTime | String | 创建时间（ISO 8601 格式） |

### VideoVO

视频信息。

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | Long | 视频ID |
| userId | Long | 用户ID |
| fileName | String | 文件名 |
| fileSize | Long | 文件大小（字节） |
| originalUrl | String | 原视频URL |
| processedUrl | String | 处理后的视频URL |
| duration | Integer | 时长（秒） |
| frameCount | Integer | 总帧数 |
| auditStatus | String | 审核状态：PENDING、APPROVED、REJECTED |
| processingStatus | String | 处理状态：PROCESSING、SUCCESS、FAILED |
| failReason | String | 失败原因 |
| createTime | String | 创建时间（ISO 8601 格式） |

### StatsVO

统计数据。

| 字段名 | 类型 | 说明 |
|--------|------|------|
| onlineDevices | Integer | 在线设备数量 |
| pendingAudits | Integer | 待审核内容数量 |
| approvedContent | Integer | 已通过内容数量 |
| online | Integer | 在线设备数量（兼容字段） |
| pending | Integer | 待审核内容数量（兼容字段） |
| approved | Integer | 已通过内容数量（兼容字段） |

### QueueItemVO

播放队列项信息。

| 字段名 | 类型 | 说明 |
|--------|------|------|
| contentId | Long | 内容ID |
| contentType | String | 内容类型：IMAGE（图片）、VIDEO（视频） |
| pushId | Long | 推送记录ID |
| fileName | String | 文件名 |
| thumbnailUrl | String | 缩略图URL（图片为原图URL，视频为第一帧或封面URL） |
| fileSize | Long | 文件大小（字节） |
| duration | Integer | 视频时长（秒，仅视频有，图片为null） |
| userId | Long | 用户ID |
| userName | String | 用户昵称 |
| userRole | String | 用户角色：ADMIN（管理员）、VISITOR（游客） |
| position | Integer | 队列中的位置（从1开始，1表示下一个播放） |

### DeleteQueueItemsDTO

删除队列项请求参数。

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| pushIds | List<Long> | 是 | 要删除的推送记录ID列表 |

---

## 错误码说明

| 错误码 | 说明 |
|--------|------|
| 200 | 操作成功 |
| 400 | 请求错误 |
| 401 | 未授权 |
| 404 | 资源不存在 |

---

## 枚举值说明

### AuditStatus（审核状态）

- `PENDING`：待审核
- `APPROVED`：审核通过
- `REJECTED`：审核未通过

### ProcessingStatus（处理状态）

- `PROCESSING`：处理中
- `SUCCESS`：处理成功
- `FAILED`：处理失败

### UserRole（用户角色）

- `USER`：普通用户
- `ADMIN`：管理员
