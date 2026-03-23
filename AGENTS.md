# Repository Guidelines

## 项目结构与模块组织
本仓库采用前后端分离：`backend/`（Spring Boot API）与 `frontend/`（Vue 3 + TypeScript + Vite）。

- `backend/src/main/java/com/fund/`：后端分层代码（`controller`、`service`、`mapper`、`entity`、`dto`、`vo`、`config`）。
- `backend/src/main/resources/`：运行配置（`application*.yml`）、日志配置、数据库迁移 SQL。
- `backend/src/test/java/com/fund/`：基于 JUnit/Mockito 的后端测试（controller/service/common）。
- `frontend/src/`：前端业务代码（`views`、`components`、`api`、`stores`、`router`、`types`、`styles`）。
- `database/`：数据库建表与优化脚本。
- `docs/`：API、数据库、部署等文档。

## 构建、测试与开发命令
- `cd backend && mvn test`：运行后端单元测试（CI 同步执行）。
- `cd backend && mvn clean package -DskipTests`：打包后端 JAR。
- `cd backend && mvn spring-boot:run`：本地启动后端。
- `cd frontend && npm ci`：按锁文件安装前端依赖。
- `cd frontend && npm run dev`：启动 Vite 开发服务。
- `cd frontend && npm run build`：执行类型检查并构建前端产物。
- `docker compose up --build`：一键启动 MySQL、Redis、backend、frontend。

## 代码风格与命名规范
遵循现有代码风格并与相邻文件保持一致：

- Java：4 空格缩进；类名 `UpperCamelCase`；方法/字段 `lowerCamelCase`；包路径统一 `com.fund`。
- Vue/TS：`.vue` 模板普遍为 2 空格缩进；页面/组件文件使用 `PascalCase`（如 `FundDetail.vue`）；变量与函数使用 `camelCase`。
- 前端接口按领域拆分在 `frontend/src/api/*.ts`；后端按控制器划分接口。
- 当前仓库未统一强制 lint/format 配置，提交前请自行检查格式一致性。

## 测试规范
- 后端测试框架为 JUnit 5 + Mockito（`spring-boot-starter-test`）。
- 测试类命名以 `Test` 结尾，并与目标模块对应（如 `UserServiceTest`）。
- 优先编写聚焦的单元测试，外部依赖（数据库、JWT、第三方接口）使用 mock。
- 提交 PR 前至少执行一次 `cd backend && mvn test`。

## 提交与合并请求规范
- 提交信息建议使用 Conventional Commit：`feat`、`fix`、`chore`，可带 scope（如 `feat(profile): ...`）。
- 提交描述使用祈使句并具体明确，避免 `xxx` 这类无信息内容。
- PR 建议包含：
  - 变更摘要与影响模块（`backend`、`frontend`、`database`）；
  - 关联 issue/任务编号（如有）；
  - UI 改动的截图或录屏；
  - 本地验证记录（如 `mvn test`、`npm run build`）。

回复思考过程全部使用中文