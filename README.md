# 贸易数据管理系统
## 项目概述
基于 Spring Boot + MyBatis Plus 的贸易数据管理系统，用于管理哈萨克斯坦贸易数据和新闻数据，并支持基于新闻生成「知识图谱」进行可视化展示。

## 技术栈
- Java 17
- Spring Boot 2.7.18
- MyBatis Plus 3.5.5
- MySQL 8.0
- Redis
- Spring Security + JWT
- Neo4j（知识图谱存储）
- LangChain4j + DashScope Qwen（AI 三元组抽取）
- Hutool
- EasyExcel
- Knife4j (Swagger)
- 前端：Vue3 + ElementPlus + Vite + G6

## 快速开始

### 1. 环境要求
- JDK 17+
- MySQL 8.0+
- Redis 5.0+
- Maven 3.6+
- Neo4j 4.x/5.x（需开启 Bolt：默认 7687）
- Node.js 18+

### 2. 数据库初始化
执行 `src/main/resources/db/schema.sql` 脚本创建数据库和表：

```bash
mysql -u root -p < src/main/resources/db/schema.sql
```

> 如为已有库升级，可执行：`src/main/resources/db/upgrade.sql`（包含 graph_history 表升级）。

默认用户：
- 管理员：admin / 123456
- 普通用户：user / 123456

### 3. 修改配置
编辑 `src/main/resources/application.yml`，修改数据库、Redis、Neo4j 连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/trade_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false
    username: root
    password: your_password

  redis:
    host: localhost
    port: 6379

  neo4j:
    uri: bolt://localhost:7687
    authentication:
      username: neo4j
      password: neo4j
```

### 4. 启动后端
```bash
cd trade-data-system

# 说明：本项目目前未提供 mvnw（Maven Wrapper），需要你本机安装 Maven
# 或使用 IDEA/VSCode 自带的 Maven 运行。

mvn clean install
mvn spring-boot:run
```

### 5. 启动前端
前端位于 `frontend/` 目录：

```bash
cd frontend
npm install
npm run dev
```

前端请求后端的两种方式：
- **推荐（开发环境）**：使用 Vite 代理（`vite.config.js` 已配置 `/api -> http://localhost:8080`），此时前端请求走相对路径 `/api`。
- **直连后端（可选）**：设置 `frontend/.env.development` 的 `VITE_API_BASE_URL`（例如 `http://localhost:8080`），前端会请求 `http://localhost:8080/api`。

> 注意：后端已允许 `http://localhost:*` / `http://127.0.0.1:*` 跨域（用于 Vite 默认 5173 或自定义端口）。

### 6. 访问 API 文档
启动成功后，访问：http://localhost:8080/doc.html

## 主要功能

### 认证管理
- `GET /api/auth/captcha` - 获取验证码
- `POST /api/auth/login` - 登录

### 贸易数据管理
- `GET /api/trade/page` - 分页查询
- `GET /api/trade/{id}` - 详情
- `POST /api/trade` - 新增（管理员）
- `PUT /api/trade` - 修改（管理员）
- `DELETE /api/trade/{id}` - 删除（管理员）
- `DELETE /api/trade/batch` - 批量删除（管理员）
- `POST /api/trade/upload` - 上传 CSV（管理员）

### 新闻数据管理
- `GET /api/news/page` - 分页查询
- `GET /api/news/{id}` - 详情
- `POST /api/news` - 新增（管理员）
- `PUT /api/news` - 修改（管理员）
- `DELETE /api/news/{id}` - 删除（管理员）

### 统计分析
- `GET /api/stats/overview` - 总览统计
- `GET /api/stats/trend` - 月度趋势
- `GET /api/stats/trade-mode-ratio` - 贸易方式占比

### AI 智能问答
- `POST /api/ai/ask` - AI 问答
- `GET /api/ai/history` - 历史对话

### 新闻知识图谱
- `POST /api/knowledge-graph/generate` - 生成图谱（返回 historyId；后端异步构建，前端可轮询进度）
- `GET  /api/knowledge-graph/progress/{historyId}` - 获取构建进度（status: BUILDING/SUCCEEDED/FAILED）
- `POST /api/knowledge-graph/history` - 保存/命名历史
- `GET /api/knowledge-graph/history` - 分页查询当前用户历史
- `DELETE /api/knowledge-graph/history/{id}?deleteGraph=true|false` - 删除历史（可选清理 Neo4j）
- `GET /api/knowledge-graph/{historyId}` - 加载图谱 nodes/edges

## 项目结构
```
trade-data-system/
├── src/main/java/com/trade/
│   ├── TradeApplication.java      # 主启动类
│   ├── config/                    # 配置类
│   ├── controller/                # 控制器
│   ├── service/                   # 服务层
│   ├── mapper/                    # 数据访问层
│   ├── entity/                    # 实体类
│   ├── dto/                       # 数据传输对象
│   ├── vo/                        # 视图对象
│   ├── common/                    # 公共类
│   ├── annotation/                # 自定义注解
│   ├── aspect/                    # AOP 切面
│   ├── security/                  # 安全相关
│   └── util/                      # 工具类
├── src/main/resources/
│   ├── application.yml
│   └── db/
│       ├── schema.sql
│       └── upgrade.sql
└── frontend/                       # 前端项目（Vue3 + ElementPlus + G6）
```
