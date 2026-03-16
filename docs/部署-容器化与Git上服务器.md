# 部署：Docker 容器化 + Git 上服务器

> 适用：后端 Spring Boot 2.7.x（Java 17）+ 前端 Vue3/Vite
>
> 目标：**服务器上 docker compose up -d --build** 一条命令拉起全套（MySQL/Redis/Neo4j/backend/frontend）。

---

## 1. 你将得到哪些“产物 / 生成文件”

### 1.1 会新增进仓库（需要提交 Git）的文件

- `Dockerfile`：后端镜像（多阶段：Maven 构建 → JRE 运行）
- `frontend/Dockerfile`：前端镜像（多阶段：Node 构建 → Nginx 运行）
- `frontend/nginx.conf`：Nginx 配置（静态站点 + `/api` 反代后端容器）
- `docker-compose.yml`：一键启动 MySQL/Redis/Neo4j/backend/frontend
- `.dockerignore`：避免把大目录/构建产物打进镜像上下文
- `.gitignore`：避免把构建产物/依赖提交到 Git
- `docs/部署-容器化与Git上服务器.md`：本文档

### 1.2 构建/运行过程中产生（**不应提交 Git**）的产物

- 后端 Maven 构建产物：`target/*.jar`、`target/classes/...`
- 前端构建产物：`frontend/dist/`
- 前端依赖：`frontend/node_modules/`

### 1.3 Docker 运行数据（建议挂载宿主机卷）

在 `docker-compose.yml` 中已配置 named volumes：

- MySQL：`mysql_data`
- Redis：`redis_data`
- Neo4j：`neo4j_data`、`neo4j_logs`

---

## 2. 重要说明：为什么要改 `application.yml` 里的 host

你选择的是 **全套 docker-compose**，后端也在容器里运行。

- 容器内的 `localhost` 指向 **容器自身**，并不是宿主机。
- 所以依赖服务（MySQL/Redis/Neo4j）必须用 **compose service name** 访问。

本仓库已修改：`src/main/resources/application.yml`

- MySQL：`jdbc:mysql://mysql:3306/trade_db...`
- Redis：`host: redis`
- Neo4j：`bolt://neo4j:7687`

> 注意：如果你以后在本机想“非容器方式”启动后端（直接 `mvn spring-boot:run`），那你本机需要也通过 compose 启动依赖，或把这些 host 再改回 `localhost`。

---

## 3. 服务器（Linux）部署步骤（推荐流程）

### 3.1 服务器环境准备

- 安装 Git
- 安装 Docker + Docker Compose v2
- 开放端口：
  - HTTP：80
  - （可选）HTTPS：443

### 3.2 从你的个人 Git 仓库拉代码

```bash
git clone <你的个人仓库URL>
cd trade-data-system
```

### 3.3 首次启动（构建镜像 + 拉起全套）

```bash
docker compose up -d --build
```

访问：

- 前端：`http://<服务器IP或域名>/`
- 后端直连（用于排查/验证）：`http://<服务器IP或域名>:8080/doc.html`

### 3.4 后续更新

```bash
git pull
docker compose up -d --build
```

---

## 4. MySQL 初始化（自动导入 schema.sql/upgrade.sql）

本项目已配置：MySQL 容器启动时自动执行初始化脚本。

对应 compose 配置：

- 宿主机目录：`./src/main/resources/db/`
- 容器目录：`/docker-entrypoint-initdb.d/`

MySQL **首次启动且数据目录为空**时，会自动执行该目录下的：

- `schema.sql`
- `upgrade.sql`

> 注意：如果你已经启动过 MySQL 并产生了 `mysql_data` 卷，后续再改 SQL 文件不会自动重跑。要重置库：需要删除卷（会清空数据）。

---

## 5. HTTPS（443）可选配置（你选择 80/443 反代）

当前仓库默认只启用 80。

如果你需要 HTTPS：

1) 准备证书文件（示例路径）：

- `frontend/certs/fullchain.pem`
- `frontend/certs/privkey.pem`

2) 修改 `docker-compose.yml`：

- 取消注释：
  - `ports: - "443:443"`
  - `volumes: - ./frontend/certs:/etc/nginx/certs:ro`

3) 修改 `frontend/nginx.conf`：

- 取消注释 443 的 `server { ... }` 段

4) 重建并启动：

```bash
docker compose up -d --build
```

---

## 6. 常用排查命令

```bash
# 看容器状态
docker compose ps

# 看日志（按服务名）
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f mysql

# 进容器（示例）
docker exec -it trade-mysql bash
```

---

## 7. Git 初始化 & 推送到服务器上的 Git 仓库（从零开始）

> 说明：当前目录最初不是 git 仓库；同时本仓库包含大量不应入库的产物（`target/`、`frontend/dist/`、`frontend/node_modules/`）。
>
> 我已添加 `.gitignore`，用于在 `git add .` 时自动排除这些目录。

在项目根目录执行：

```bash
# 1) 初始化
git init

# 2) 建议先确认忽略生效（不应出现 target/node_modules/dist）
git status

# 3) 首次提交
git add .
git commit -m "init: dockerize deployment"

# 4) 绑定远端
git remote add origin <你的个人仓库URL>

# 5) 推送（main 或 master 取决于你的仓库默认分支）
git branch -M main
git push -u origin main
```
