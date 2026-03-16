# 贸易数据管理系统（trade-data-system）新手快速上手 + Postman 接口测试用例

> 适用工程路径：`D:/my-claude-project/Project/trade-data-system`

## 1. 项目是什么？（一句话）
一个基于 Spring Boot 2.7 + MyBatis-Plus + Spring Security(JWT) 的“贸易数据管理”演示系统，包含：登录验证码、JWT 鉴权、贸易数据/新闻 CRUD、统计分析、Knife4j 接口文档、AI 智能问答、以及「新闻知识图谱（Neo4j + 大模型三元组抽取 + 前端 G6 可视化）」等。

---

## 2. 运行前准备（必须）

### 2.1 环境依赖
- JDK：17
- MySQL：8.x
- Redis：验证码依赖
- Maven：用于编译运行（`mvn` 或 IDE 内置 Maven）
- Neo4j：用于知识图谱存储（Bolt 默认 `7687`，HTTP 默认 `7474`）
- Node.js：18+（前端）

### 2.2 初始化数据库（MySQL）
1. 启动 MySQL
2. 执行：`src/main/resources/db/schema.sql`
   - 会创建数据库 `trade_db` 以及必要表
   - 会创建知识图谱历史表：`graph_history`
   - 会插入默认用户：
     - `admin / 123456`
     - `user / 123456`

> 如果你是“老库升级”，请执行：`src/main/resources/db/upgrade.sql`，否则知识图谱功能会报错：
> `Table 'trade_db.graph_history' doesn't exist`

### 2.3 启动 Neo4j（知识图谱必需）
- 本地启动 Neo4j（Desktop 或 docker 均可）
- 确认 Bolt 可用：`bolt://localhost:7687`
- 设置账号密码（默认通常是 `neo4j/neo4j`，首次登录会要求修改）

后端配置文件位置：`src/main/resources/application.yml`

```yaml
spring:
  neo4j:
    uri: bolt://localhost:7687
    authentication:
      username: neo4j
      password: neo4j
```

### 2.4 配置文件（后端 application.yml）
- `src/main/resources/application.yml`
  - MySQL：`spring.datasource.*`
  - Redis：`spring.redis.*`
  - JWT：`jwt.secret` / `jwt.expiration`
  - 鉴权开关：`app.security.enabled`（本地联调可临时关闭）
  - AI：`app.ai.enabled` + `app.ai.api-key`（知识图谱三元组抽取依赖）

---

## 3. 启动与验证

### 3.1 启动后端
```bash
mvn -DskipTests package
mvn spring-boot:run
```

### 3.2 接口文档（Knife4j）
浏览器打开：
- `http://localhost:8080/doc.html`

### 3.3 启动前端
前端位于 `frontend/`：

```bash
cd frontend
npm install
npm run dev
```

前端开发环境两种请求方式（二选一即可）：
1) **Vite 代理（推荐）**：`vite.config.js` 已配置 `/api -> http://localhost:8080`
2) **直连后端（可选）**：在 `frontend/.env.development` 设置 `VITE_API_BASE_URL=http://localhost:8080`

> 常见问题：如果你前端端口是 5173（Vite 默认），后端 CORS 需要允许 `http://localhost:*`（项目已做过对应配置）。

---

## 4. Postman 使用说明（推荐按这个顺序测）

### 4.1 建议先建一个 Postman Environment
Environment 变量建议：
- `baseUrl`：`http://localhost:8080`
- `token`：留空（登录后自动填充）

### 4.2 通用 Header 约定
除登录/验证码外，受保护接口一般需要：
- `Authorization: Bearer {{token}}`

你可以在 Postman 的 Collection 级别配置一个 Authorization（Bearer Token），值写 `{{token}}`。

---

## 5. Postman 接口测试用例（可直接照抄）

> 说明：以下用例按“能跑通演示”的最短路径组织。

### 用例 1：获取验证码
- Name：`Auth - Get Captcha`
- Method：`GET`
- URL：`{{baseUrl}}/api/auth/captcha`

预期：
- 返回 `data.uuid` 与 `data.img`（Base64 图片）

备注：
- 把返回的 `uuid`、以及图片对应的验证码值记录下来，下一步登录要用。

---

### 用例 2：登录获取 Token
- Name：`Auth - Login`
- Method：`POST`
- URL：`{{baseUrl}}/api/auth/login`
- Headers：`Content-Type: application/json`

Body（字段名兼容 uuid/captchaKey、code/captchaCode）：
```json
{
  "username": "admin",
  "password": "123456",
  "uuid": "上一步返回的uuid",
  "code": "上一步图片对应的值"
}
```

Tests（自动保存 token）：
```js
pm.test("login success", function () {
  pm.response.to.have.status(200);
});

const json = pm.response.json();
const token = json?.data?.token;
if (token) {
  pm.environment.set("token", token);
}
```

预期：
- `{{token}}` 被写入

---

### 用例 3：分页查询新闻（为知识图谱准备数据）
- Name：`News - Page`
- Method：`GET`
- URL：`{{baseUrl}}/api/news/page?current=1&size=5`

预期：
- 返回分页 records，记录 `id` 供知识图谱生成使用

---

### 用例 4：生成知识图谱（返回 historyId）
- Name：`KG - Generate`
- Method：`POST`
- URL：`{{baseUrl}}/api/knowledge-graph/generate`
- Headers：`Content-Type: application/json`

Body（示例）：
```json
{
  "newsIds": [1, 2]
}
```

预期：
- 返回：`data.historyId`
- 注意：后端采用**异步构建**，返回 historyId 后图谱可能仍在构建中

---

### 用例 5：查询构建进度（推荐）
- Name：`KG - Progress`
- Method：`GET`
- URL：`{{baseUrl}}/api/knowledge-graph/progress/{{historyId}}`

预期：
- 返回：
  - `data.status`：`BUILDING` / `SUCCEEDED` / `FAILED`
  - `data.progress`：0-100
  - `data.message`：阶段提示

建议：
- 生成后先轮询该接口，直到 `status=SUCCEEDED` 再调用“加载图谱”接口

---

### 用例 6：加载图谱数据（nodes/edges）
- Name：`KG - Load Graph`
- Method：`GET`
- URL：`{{baseUrl}}/api/knowledge-graph/{{historyId}}`

预期：
- 当进度已完成时，返回：`data.nodes` / `data.edges`

可能返回的业务错误：
- 构建中：`ErrorCode=KG_BUILDING`
- 构建失败：`ErrorCode=KG_BUILD_FAILED`

- Name：`KG - Save History Name`
- Method：`POST`
- URL：`{{baseUrl}}/api/knowledge-graph/history`

Body：
```json
{
  "historyId": 123,
  "graphName": "我的第一个图谱"
}
```

预期：
- 返回成功

---

### 用例 7：分页查询当前用户图谱历史
- Name：`KG - Page History`
- Method：`GET`
- URL：`{{baseUrl}}/api/knowledge-graph/history?current=1&size=10`

预期：
- 返回 records（包含 id/graphName/newsIds/createdAt）

---

### 用例 8：删除图谱历史（可选清理 Neo4j）
- Name：`KG - Delete History`
- Method：`DELETE`

1) 仅删历史：
- URL：`{{baseUrl}}/api/knowledge-graph/history/123?deleteGraph=false`

2) 删历史并清理图数据：
- URL：`{{baseUrl}}/api/knowledge-graph/history/123?deleteGraph=true`

---

## 6. 常见问题（快速排查）

### 6.1 打开网页“无法获取数据”
- 如果后端开启鉴权（`app.security.enabled=true`），必须先登录获取 token
- 如果前端端口是 5173，确认后端 CORS 允许 `http://localhost:*`
- 推荐使用 Vite 代理（`/api`）避免跨域问题

### 6.2 知识图谱接口 500
优先看后端控制台异常栈，最常见：
- `graph_history` 表不存在（老库未升级）
- AI 未启用或 Key 无效
- Neo4j 连接失败（Bolt 地址/账号密码不对）

---

## 7. 建议的新手阅读入口
1. `src/main/java/com/trade/config/SecurityConfig.java`（鉴权与放行规则）
2. `src/main/java/com/trade/security/JwtAuthenticationFilter.java`（JWT 解析与注入上下文）
3. `src/main/java/com/trade/controller/KnowledgeGraphController.java`（知识图谱接口）
4. `src/main/java/com/trade/service/impl/KnowledgeGraphServiceImpl.java`（生成/历史/删除/查询逻辑）
5. `src/main/java/com/trade/service/kg/KgAiExtractService.java`（AI 三元组抽取：严格 JSON + 解析容错）
6. `src/main/java/com/trade/service/kg/Neo4jGraphRepository.java`（Neo4j 写入/查询/删除，按 graphKey 隔离）
