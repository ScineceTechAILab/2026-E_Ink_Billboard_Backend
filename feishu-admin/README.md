
# 墨水屏广告牌管理后台（飞书版）

一个基于 Vue 3 + TypeScript + Vite 构建的现代化管理后台，支持飞书免登授权。

## ✨ 功能特性

- 🎨 **轻松活泼的 UI 设计** - 明亮渐变配色（#FFCF48 → #FF6B6B）、圆角卡片、流畅动画
- 🔐 **飞书免登授权** - 完整的飞书 OAuth 2.0 流程
- 📸 **图片上传模块** - 支持拖拽、点击、粘贴三种方式
- 🔄 **图片压缩** - 自动压缩至 1920px 宽度，质量 0.8
- 📊 **进度展示** - 单文件圆形进度 + 总体线性进度
- 🔁 **自动重试** - 失败文件自动重试 3 次
- 💾 **安全存储** - Token 使用 secure-ls AES 加密存储
- 📦 **工程规范** - ESLint + Prettier + Husky 完整配置

## 🛠️ 技术栈

- **框架**: Vue 3.4 + TypeScript 5.3
- **构建工具**: Vite 5.0
- **状态管理**: Pinia 2.1
- **路由**: Vue Router 4.2
- **UI 组件**: Element Plus 2.4
- **样式**: Tailwind CSS 3.4
- **HTTP**: Axios 1.6
- **图片压缩**: browser-image-compression 2.12
- **安全存储**: secure-ls 1.2
- **测试**: Vitest 1.1 + @vue/test-utils 2.4

## 📁 目录结构

```
feishu-admin/
├── src/
│   ├── api/              # API 接口封装
│   │   └── index.ts
│   ├── assets/           # 静态资源
│   ├── components/       # 通用组件
│   ├── router/           # 路由配置
│   │   └── index.ts
│   ├── stores/           # Pinia 状态管理
│   │   └── user.ts
│   ├── styles/           # 全局样式
│   │   └── index.css
│   ├── types/            # TypeScript 类型定义
│   │   └── index.ts
│   ├── utils/            # 工具函数
│   │   └── request.ts
│   ├── views/            # 页面组件
│   │   ├── LoginView.vue
│   │   ├── FeishuCallbackView.vue
│   │   ├── DashboardView.vue
│   │   └── UploadView.vue
│   ├── App.vue
│   └── main.ts
├── .env.development      # 开发环境变量
├── .eslintrc.cjs         # ESLint 配置
├── .prettierrc.json      # Prettier 配置
├── index.html
├── package.json
├── postcss.config.js
├── tailwind.config.js
├── tsconfig.json
├── tsconfig.node.json
├── vite.config.mts
└── README.md
```

## 🚀 快速开始

### 环境准备

- Node.js 18+
- npm 9+ 或 pnpm 8+

### 安装依赖

```bash
cd feishu-admin
npm install
```

### 开发环境

```bash
npm run dev
```

访问 http://localhost:3000

### 构建生产版本

```bash
npm run build
```

### 预览构建结果

```bash
npm run preview
```

## 🔧 配置说明

### 环境变量

在 `.env.development` 中配置：

```env
VITE_API_URL=http://localhost:8083
VITE_FEISHU_APP_ID=cli_a9e75d8301f85bd6
VITE_FEISHU_REDIRECT_URI=http://localhost:3000/feishu/callback
```

### 飞书应用配置

1. 访问 [飞书开放平台](https://open.feishu.cn/)
2. 创建企业自建应用
3. 配置重定向 URL：`http://localhost:3000/feishu/callback`
4. 获取 App ID 和 App Secret（后端使用）

## 📋 可用命令

| 命令 | 说明 |
|------|------|
| `npm run dev` | 启动开发服务器 |
| `npm run build` | 构建生产版本 |
| `npm run preview` | 预览构建结果 |
| `npm run lint` | 运行 ESLint 检查并修复 |
| `npm run format` | 运行 Prettier 格式化 |
| `npm run test` | 运行单元测试 |
| `npm run test:coverage` | 运行测试并生成覆盖率报告 |

## 🎨 设计规范

### 配色方案

- **主色调**: 从 #FFCF48（琥珀色）渐变到 #FF6B6B（玫瑰红）
- **背景**: 渐变色 - 从 amber-50 到 rose-50
- **卡片**: 纯白色，圆角 16px，柔和阴影

### 动画效果

- `bounce-in` - 弹性进入动画
- `fade-in` - 淡入动画
- `slide-up` - 上滑进入动画
- `pulse-soft` - 柔和脉冲动画

### 字体

- **英文**: Poppins
- **中文**: Noto Sans SC
- **字号**: 14-24px
- **行高**: 1.6

## ✅ 验收标准

### 登录模块
- [x] 飞书 OAuth 流程完整
- [x] Token 安全存储（secure-ls AES 加密）
- [x] 自动写入 axios Authorization 头
- [x] 登录成功跳转 /dashboard
- [x] 欢迎 toast 提示
- [x] 401/403/500 错误插画提示
- [x] 一键重试功能

### 图片上传模块
- [x] 拖拽、点击、粘贴三种上传方式
- [x] 文件类型校验（jpg/png/gif/webp）
- [x] 文件大小限制（≤ 5MB）
- [x] 数量限制（≤ 20 张）
- [x] 上传前自动压缩（1920px, 0.8 质量）
- [x] 显示压缩后大小和节省流量
- [x] 单文件圆形进度条
- [x] 总体线性进度条
- [x] 支持取消上传
- [x] 失败自动重试 3 次
- [x] X-Upload-Id 头用于重试续传
- [x] 瀑布流缩略图展示
- [x] 点击查看原图
- [x] 一键复制外链
- [x] 一键删除（DELETE 接口）
- [x] 完整的 aria-label 无障碍属性
- [x] 键盘可操作
- [x] WCAG 2.1 AA 焦点样式

## 🧪 测试

### 单元测试

```bash
npm run test
```

### 覆盖率报告

```bash
npm run test:coverage
```

目标覆盖率 ≥ 90%

## 📝 开发说明

### 新增页面

1. 在 `src/views/` 下创建页面组件
2. 在 `src/router/index.ts` 中添加路由配置
3. 如需认证，设置 `meta: { requiresAuth: true }`

### 新增 API

1. 在 `src/types/index.ts` 中定义类型
2. 在 `src/api/index.ts` 中添加接口函数
3. 使用 `request` 工具函数发送请求

### 状态管理

1. 在 `src/stores/` 下创建新 store
2. 使用 `defineStore` 定义状态和方法
3. 在组件中使用 `useXxxStore()` 访问

## 📄 License

MIT License

