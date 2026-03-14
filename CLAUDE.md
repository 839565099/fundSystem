# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

基金实时查看系统，支持基金搜索、实时数据展示、趋势图分析、用户收藏等功能。对接东方财富 API 获取真实基金数据。

## 常用命令

### 后端 (backend/)
```bash
# 编译打包
mvn clean package -DskipTests

# 运行应用
java -jar target/fund-system-1.0.0.jar

# 运行测试
mvn test
```

### 前端 (frontend/)
```bash
# 安装依赖
npm install

# 开发模式 (端口 3000)
npm run dev

# 生产构建
npm run build
```

## 架构

### 后端架构
基于 Spring Boot 2.7.x 的分层架构：

- **controller/** - REST API 控制器
  - `AuthController` - 用户认证（登录/注册）
  - `FundController` - 基金搜索、详情、净值历史
  - `FundInfoController` - 基金经理、持仓明细
  - `HotFundController` - 热门基金
  - `MarketController` - 市场行情
  - `FavoriteController` - 用户收藏
  - `CompareController` - 基金对比
  - `NewsController` - 资讯
  - `ExportController` - 数据导出

- **service/** - 业务逻辑层，接口定义
- **service/impl/** - 业务逻辑实现
- **mapper/** - MyBatis-Plus 数据访问层
- **entity/** - 数据库实体类
- **dto/** - 数据传输对象（请求）
- **vo/** - 视图对象（响应）
- **external/** - 外部 API 服务（东方财富数据获取、资讯爬虫）
- **config/** - 配置类（Redis、WebSocket、Web）
- **interceptor/** - JWT 认证拦截器
- **util/** - 工具类（JWT 工具）

### 前端架构
Vue 3 + TypeScript + Vite + Naive UI + Pinia + ECharts：

- **api/** - API 请求封装（Axios 实例配置，自动添加 Bearer Token）
- **views/** - 页面组件
  - `Home` - 首页（热门基金、市场行情）
  - `Search` - 基金搜索
  - `FundDetail` - 基金详情（净值走势图）
  - `Favorites` - 我的收藏
  - `Compare` - 基金对比
  - `Ranking` - 基金排行
  - `News` / `NewsDetail` - 资讯
  - `Login` / `Register` / `Profile` - 用户相关
- **stores/** - Pinia 状态管理
- **components/** - 通用组件
- **router/** - Vue Router 路由配置

### API 认证
- 使用 JWT Bearer Token 认证
- Token 存储在 localStorage，由前端 axios 拦截器自动添加到请求头
- 需要认证的接口：用户信息、收藏、个人中心

### 数据流向
1. 前端请求 → Vite 代理 → 后端 API (`/api/*`)
2. 后端通过 `FundDataApiService` 调用东方财富 API 获取基金数据
3. 数据缓存到 Redis（默认 5 分钟过期）
4. WebSocket 推送实时数据更新

## 配置

### 后端配置 (application.yml)
- 数据库：MySQL 8.0 (192.168.1.20:3306/fund_system)
- Redis：192.168.1.20:6378
- 服务端口：8080
- Context Path：`/api`

### 前端配置 (frontend/vite.config.ts)
- 开发端口：3000
- API 代理：`/api` → `http://127.0.0.1:8080`

## 数据库表

主要表（详见 docs/DATABASE.md）：
- `t_user` - 用户
- `t_fund` - 基金基本信息
- `t_fund_nav_history` - 净值历史
- `t_fund_manager` / `t_fund_manager_relation` - 基金经理
- `t_fund_holdings` - 持仓明细
- `t_user_favorite` - 用户收藏
- `t_fund_news` - 资讯
- `t_market_data` - 市场行情

## 开发注意事项

- 后端使用 Lombok 减少样板代码
- MyBatis-Plus 自动填充 `create_time` / `update_time`
- 前端使用 Naive UI 组件库
- 图表使用 ECharts
- 支持明暗主题切换
