# 贸易数据管理系统 - 前端

## 项目简介

基于 Vue 3 + Element Plus 的贸易数据管理系统前端，用于展示和管理哈萨克斯坦贸易数据与新闻数据。

## 技术栈

- **框架**：Vue 3 + Vite
- **UI 库**：Element Plus
- **状态管理**：Pinia
- **路由**：Vue Router 4
- **HTTP 请求**：Axios
- **图表**：ECharts
- **工具库**：Day.js、lodash

## 项目结构

```
frontend/
├── src/
│   ├── api/              # API 接口封装
│   │   └── index.js
│   ├── assets/           # 静态资源
│   ├── components/       # 通用组件
│   │   ├── CaptchaImage.vue   # 验证码组件
│   │   └── UploadCsv.vue      # CSV 上传组件
│   ├── composables/      # 组合式函数
│   ├── layouts/          # 布局组件
│   │   └── MainLayout.vue     # 主布局
│   ├── router/           # 路由配置
│   │   └── index.js
│   ├── stores/           # Pinia 状态管理
│   │   ├── user.js       # 用户状态
│   │   └── app.js        # 应用状态
│   ├── utils/            # 工具函数
│   │   └── request.js    # Axios 封装
│   ├── views/            # 页面视图
│   │   ├── login/        # 登录页
│   │   ├── dashboard/    # 首页看板
│   │   ├── trade/        # 贸易数据管理
│   │   ├── news/         # 新闻数据管理
│   │   ├── stats/        # 统计分析
│   │   └── ai/           # AI 问答
│   ├── App.vue
│   └── main.js
├── .env.development      # 开发环境变量
├── index.html
├── package.json
└── vite.config.js
```

## 快速开始

### 环境要求

- Node.js 16+
- npm 或 yarn

### 安装依赖

```bash
cd frontend
npm install
```

### 启动开发服务器

```bash
npm run dev
```

开发服务器将在 http://localhost:3000 启动

### 构建生产版本

```bash
npm run build
```

### 预览生产构建

```bash
npm run preview
```

## 功能说明

### 1. 用户登录

**路径**：`/login`

**功能**：
- 用户名、密码输入
- 图片验证码（点击刷新）
- 记住密码（可选）
- 表单验证

**默认账号**：
- 管理员：`admin` / `123456`
- 普通用户：`user` / `123456`

### 2. 首页看板

**路径**：`/dashboard`

**权限**：所有登录用户

**功能**：
- 欢迎语与当前时间显示
- 统计卡片（总记录数、总贸易金额、贸易方式数量）
- 月度贸易趋势折线图（最近12个月）
- 贸易方式金额占比饼图
- 最新数据表格（最近5条贸易记录）

### 3. 贸易数据管理

**路径**：`/trade`

**权限**：仅管理员

**功能**：

#### 3.1 数据查询
- 年月范围筛选
- 国家筛选
- 进出口方向筛选
- 关键词搜索（商品/伙伴名称）
- 分页显示

#### 3.2 数据操作
- 新增贸易数据
- 编辑贸易数据
- 删除单条记录
- 批量删除
- 查看详情

#### 3.3 数据导入
- CSV 文件上传（支持拖拽）
- 国家选择
- 进出口方向选择
- 合并模式（跳过/覆盖重复）
- 最大导入行数设置

### 4. 新闻数据管理

**路径**：`/news`

**权限**：仅管理员

**功能**：
- 标题关键词搜索
- 日期范围筛选
- 新增/编辑/删除新闻
- 新闻详情预览（HTML 渲染）

### 5. 统计分析

**路径**：`/stats`

**权限**：所有登录用户

**功能**：
- 国家多选筛选
- 进出口方向筛选
- 年月范围筛选

**图表展示**：
- **趋势图**：进出口金额月度趋势折线图
- **贸易方式分布**：贸易方式金额柱状图
- **国家分布**：Top10 国家贸易金额横向柱状图

### 6. AI 智能问答

**路径**：`/ai`

**权限**：所有登录用户

**功能**：
- 聊天式交互界面
- 历史对话列表
- 实时回答显示
- 加载状态动画
- 清空历史对话

**快捷键**：Ctrl + Enter 发送消息

## 路由权限

| 路径 | 页面 | ADMIN | USER |
|------|------|-------|------|
| /login | 登录页 | ✓ | ✓ |
| /dashboard | 首页看板 | ✓ | ✓ |
| /trade | 贸易数据管理 | ✓ | ✗ |
| /news | 新闻数据管理 | ✓ | ✗ |
| /stats | 统计分析 | ✓ | ✓ |
| /ai | AI 问答 | ✓ | ✓ |

## API 接口

所有接口请求都会自动携带 `Authorization: Bearer {token}` 请求头。

### 认证接口

- `GET /api/auth/captcha` - 获取验证码
- `POST /api/auth/login` - 登录

### 贸易数据接口

- `GET /api/trade/page` - 分页查询
- `GET /api/trade/{id}` - 详情
- `POST /api/trade` - 新增
- `PUT /api/trade` - 修改
- `DELETE /api/trade/{id}` - 删除
- `DELETE /api/trade/batch` - 批量删除
- `POST /api/trade/upload` - 上传 CSV

### 新闻数据接口

- `GET /api/news/page` - 分页查询
- `GET /api/news/{id}` - 详情
- `POST /api/news` - 新增
- `PUT /api/news` - 修改
- `DELETE /api/news/{id}` - 删除

### 统计分析接口

- `GET /api/stats/overview` - 总览统计
- `GET /api/stats/trend` - 月度趋势
- `GET /api/stats/trade-mode-ratio` - 贸易方式占比

### AI 问答接口

- `POST /api/ai/ask` - AI 问答
- `GET /api/ai/history` - 历史对话

## 状态管理

### userStore

```javascript
state: {
  token: '',
  userInfo: { username, role },
  permissions: []
}

actions: {
  login(),      // 登录
  logout(),     // 退出
  getCaptcha()  // 获取验证码
}

getters: {
  isAdmin,      // 是否管理员
  username      // 用户名
}
```

### appStore

```javascript
state: {
  collapsed: false,      // 侧边栏折叠状态
  breadcrumbs: [],       // 面包屑
  tagsView: []           // 标签页
}

actions: {
  toggleCollapsed(),     // 切换侧边栏
  setBreadcrumbs(),      // 设置面包屑
  addTagView(),          // 添加标签页
  removeTagView()        // 移除标签页
}
```

## 开发配置

### 环境变量

在 `.env.development` 中配置：

```env
VITE_API_BASE_URL=http://localhost:8080
```

### 代理配置

在 `vite.config.js` 中配置了开发代理：

```javascript
proxy: {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true
  }
}
```

## 常见问题

### 1. 登录后无法访问某些页面？

检查用户角色权限，某些页面（如贸易数据管理）仅管理员可见。

### 2. API 请求失败？

确保后端服务已启动在 http://localhost:8080，检查网络连接和代理配置。

### 3. 图表不显示？

检查浏览器控制台是否有错误，确保 ECharts 数据格式正确。

### 4. 如何修改主题色？

在 `main.js` 中配置 Element Plus 的主题，或使用自定义 CSS 变量。

## 浏览器支持

- Chrome (推荐)
- Firefox
- Safari
- Edge

建议使用最新版本浏览器以获得最佳体验。

## 开发规范

- 组件命名使用 PascalCase
- 文件名使用 kebab-case 或 PascalCase
- 使用 Composition API（`<script setup>`）
- 遵循 Vue 3 官方风格指南
