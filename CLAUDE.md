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
基于 Spring Boot 2.7.18 + MyBatis-Plus 的分层架构：

- **controller/** - REST API 控制器
  - `AuthController` - 用户认证（登录/注册）
  - `FundController` - 基金搜索、详情、净值历史
  - `FundInfoController` - 基金经理、持仓明细
  - `HotFundController` - 热门基金
  - `MarketController` - 市场行情
  - `FavoriteController` - 用户收藏
  - `CompareController` - 基金对比
  - `NewsController` - 资讯
  - `SectorController` - 板块数据
  - `PortfolioController` - 投资组合
  - `AlertController` - 预警管理
  - `AnalyticsController` - 高级分析
  - `RecommendController` - 智能推荐
  - `AIController` - AI 助手
  - `ExportController` - 数据导出（Excel/PDF）

- **service/** - 业务逻辑层接口
- **service/impl/** - 业务逻辑实现
- **mapper/** - MyBatis-Plus 数据访问层
- **entity/** - 数据库实体类
- **dto/** - 数据传输对象（请求）
- **vo/** - 视图对象（响应）
- **external/** - 外部 API 服务
  - `FundDataApiService` - 东方财富基金数据
  - `SectorDataApiService` - 板块数据
  - `NewsCrawlerService` - 资讯爬虫
- **config/** - 配置类（Redis、WebSocket、Web）
- **interceptor/** - JWT 认证拦截器
- **util/** - 工具类

### 前端架构
Vue 3 + TypeScript + Vite + Naive UI + Pinia + ECharts：

- **api/** - API 请求封装（Axios 实例，自动添加 Bearer Token）
- **views/** - 页面组件
  - `Home` - 首页（热门基金、市场行情）
  - `Dashboard` - 投资概览（需登录）
  - `Search` - 基金搜索
  - `FundDetail` - 基金详情（净值走势图）
  - `Ranking` - 基金排行
  - `SectorRanking` / `SectorDetail` - 板块排行/详情
  - `MarketDetail` - 大盘详情
  - `Favorites` - 我的收藏（需登录）
  - `Compare` - 基金对比
  - `Portfolio` - 投资组合（需登录）
  - `Alerts` - 预警管理（需登录）
  - `Analytics` - 高级分析
  - `Recommend` - 智能推荐
  - `AIAssistant` - AI 助手
  - `News` / `NewsDetail` - 资讯中心/详情
  - `Login` / `Register` / `Profile` - 用户相关
- **stores/** - Pinia 状态管理（auth、theme）
- **components/** - 通用组件（如 FundTrendChart）
- **router/** - Vue Router 路由配置

### API 认证
- 使用 JWT Bearer Token 认证
- Token 存储在 localStorage，由 axios 拦截器自动添加到请求头
- 401 响应自动跳转登录页
- 需认证的页面：Dashboard、Favorites、Portfolio、Alerts、Profile

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
- Druid 监控：`/druid/*` (admin/admin123)

### 前端配置 (vite.config.ts)
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
- `t_hot_fund_rank` - 热门基金排行

## 开发注意事项

- 后端使用 Lombok 减少样板代码
- MyBatis-Plus 自动填充 `create_time` / `update_time`
- 前端使用 Naive UI 组件库
- 图表使用 ECharts（vue-echarts）
- 支持明暗主题切换
- README.md 中的技术栈描述有误：实际前端是 Vue 3 + Naive UI，而非 React + Ant Design
