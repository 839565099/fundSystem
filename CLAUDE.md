# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

基金实时查看系统，前后端分离架构。后端 Spring Boot 2.7.18 (Java 8) + MyBatis-Plus，前端 Vue 3 + TypeScript + Vite + Naive UI + Pinia。对接东方财富 API 获取真实基金数据。

**注意**：README.md 中技术栈描述有误，实际前端是 Vue 3 + Naive UI，而非 React + Ant Design。

## 常用命令

### 后端 (backend/)
```bash
mvn spring-boot:run          # 本地启动后端
mvn test                     # 运行测试
mvn clean package -DskipTests # 打包 JAR
java -jar target/fund-system-1.0.0.jar  # 运行 JAR
```

### 前端 (frontend/)
```bash
npm ci                       # 按锁文件安装依赖
npm run dev                  # 开发模式 (端口 3000)
npm run build                # 类型检查 + 生产构建
npm run preview              # 预览生产构建
```

### Docker
```bash
docker compose up --build    # 一键启动 MySQL + Redis + backend + frontend
```

## 架构

### 后端分层 (`backend/src/main/java/com/fund/`)

标准 Spring Boot 分层：`controller` → `service`/`service/impl` → `mapper` → 数据库。包路径统一 `com.fund`。

- **common/** — 通用响应封装 (`Result`, `PageResult`)、MyBatis-Plus 自动填充处理器 (`MyMetaObjectHandler`)
- **config/** — WebConfig（CORS + 拦截器注册）、RedisConfig、WebSocketConfig、DatabaseInitializer、AIConfig
- **interceptor/** — `JwtInterceptor`（JWT 认证）+ `AdminInterceptor`（基于 `@RequireAdmin` 注解的权限校验）
- **external/** — 外部 API 对接：`FundDataApiService`（东方财富基金数据）、`SectorDataApiService`（板块数据）、`NewsCrawlerService`（资讯爬虫）
- **dto/** — 请求对象，**vo/** — 响应对象，**entity/** — 数据库实体（均使用 Lombok）

### 前端结构 (`frontend/src/`)

- **api/** — Axios 封装，按领域拆分（`auth.ts`、`fund.ts`、`admin.ts` 等）。实例 baseURL 为 `/api`，请求拦截器自动附加 Bearer Token
- **views/** — 页面组件（PascalCase 命名）。子目录 `admin/` 包含管理后台页面
- **components/** — 通用组件（`FundTrendChart`、`MobileNav`、`NotificationBell`、`Skeleton` 等）
- **stores/** — Pinia 状态管理（`auth.ts` 管理登录态和角色、`theme.ts` 管理明暗主题）
- **router/index.ts** — 路由配置，所有页面懒加载

### 认证与权限

三层权限模型：
1. **公开接口** — WebConfig 中排除 JWT 拦截的路径（搜索、行情、资讯等）
2. **登录用户** — 需 JWT Token，`requiresAuth: true` 的前端路由会检查 localStorage 中的 token
3. **管理员** — 后端 `@RequireAdmin` 注解 + 前端 `requiresAdmin: true` 路由守卫，检查 `authStore.isAdmin`

前端 401 响应自动跳转登录页并携带 `redirect` 参数。

### 数据流向

1. 前端请求 → Vite 代理 (`/api` → `http://127.0.0.1:8080`) → 后端 API
2. 后端通过 `external/` 服务调用东方财富 API
3. 数据缓存到 Redis（TTL 300 秒）
4. WebSocket 推送实时数据更新

### 管理后台

独立路由模块 `/admin/*`，包含用户管理、操作日志等功能。后端通过 `AdminController` 提供 API，使用 `AdminInterceptor` 拦截 `/admin/**` 路径校验管理员权限。

### AI 助手

后端 `AIController` → `AIConfig` 配置 Claude API 客户端。支持通过环境变量 `AI_API_KEY`、`AI_BASE_URL`、`AI_MODEL` 配置，默认模型 `claude-sonnet-4-6`。

## 配置

### 环境变量覆盖 (application.yml)

所有关键配置支持环境变量覆盖，无需修改代码即可适配不同环境：
- **数据库**：`DB_HOST`、`DB_PORT`、`DB_NAME`、`DB_USERNAME`、`DB_PASSWORD`（默认 192.168.1.20:3306/fund_system）
- **Redis**：`REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD`（默认 192.168.1.20:6378）
- **JWT**：`JWT_SECRET`（过期时间 24 小时）
- **AI**：`AI_API_KEY`、`AI_BASE_URL`、`AI_MODEL`
- **邮件**：`MAIL_HOST`、`MAIL_PORT`、`MAIL_USERNAME`、`MAIL_PASSWORD`

### 前端配置 (vite.config.ts)
- 路径别名：`@` → `./src`
- 开发端口：3000
- API 代理：`/api` → `http://127.0.0.1:8080`

## 代码风格

遵循现有代码风格并与相邻文件保持一致：

- **Java**：4 空格缩进；类名 `UpperCamelCase`；方法/字段 `lowerCamelCase`
- **Vue/TS**：2 空格缩进；组件文件 `PascalCase`；变量/函数 `camelCase`
- 提交信息使用 Conventional Commit：`feat`、`fix`、`chore`，可带 scope（如 `feat(profile): ...`）

## 测试

- 后端：JUnit 5 + Mockito（`spring-boot-starter-test`），测试类以 `Test` 结尾
- 前端：`npm run build` 会先执行 `vue-tsc` 类型检查
- CI（`.github/workflows/ci.yml`）：PR 触发后端测试 + 前端构建，push 到 master 额外构建 Docker 镜像

## 数据库

MySQL 8.0，12 张核心表（详见 `docs/DATABASE.md`）。`database/schema.sql` 为建表脚本，`backend/src/main/resources/db/` 包含初始化和迁移 SQL。MyBatis-Plus 自动填充 `create_time`/`update_time`，支持逻辑删除。

## 其他文档

- `docs/API.md` — 完整 API 文档
- `docs/DEPLOYMENT.md` — 部署指南（含 Docker、Nginx、JVM 调优）
- `docs/DATABASE.md` — 数据库设计文档
- `docs/FEATURE_ENHANCEMENT.md` — 功能增强记录
- `AGENTS.md` — 仓库规范（提交规范、PR 模板等）
