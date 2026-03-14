# 基金实时查看系统

一个功能完整的基金实时查看系统，支持基金搜索、实时数据展示、趋势图分析、用户收藏等功能。

## 功能特性

### 核心功能
- 🔍 **基金搜索**：支持按基金代码、名称进行精确搜索和模糊查询
- 📈 **实时趋势图**：使用专业图表展示净值走势，支持多时间维度切换
- 📊 **基金详情**：显示基金基本信息、基金经理、持仓情况、业绩表现等
- 🔄 **实时数据**：对接东方财富API，获取真实基金数据

### 扩展功能
- 👤 **用户系统**：支持用户注册、登录、个人中心管理
- ⭐ **基金收藏**：允许用户收藏关注的基金
- 📉 **业绩对比**：支持多只基金业绩对比分析
- 📰 **资讯模块**：提供基金相关新闻资讯
- 📊 **市场行情**：展示基金市场整体行情

## 技术架构

### 后端技术
- **框架**：Spring Boot 2.7.x
- **数据库**：MySQL 8.0
- **缓存**：Redis
- **ORM**：MyBatis-Plus
- **认证**：JWT

### 前端技术
- **框架**：React 18
- **构建工具**：Vite
- **UI组件**：Ant Design 5
- **图表库**：@ant-design/charts
- **状态管理**：Zustand
- **路由**：React Router 6

## 项目结构

```
fund-system/
├── backend/                    # 后端项目
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/fund/
│   │   │   │   ├── common/     # 通用类
│   │   │   │   ├── config/     # 配置类
│   │   │   │   ├── controller/ # 控制器
│   │   │   │   ├── dto/        # 数据传输对象
│   │   │   │   ├── entity/     # 实体类
│   │   │   │   ├── exception/  # 异常处理
│   │   │   │   ├── external/   # 外部API
│   │   │   │   ├── interceptor/# 拦截器
│   │   │   │   ├── mapper/     # 数据访问层
│   │   │   │   ├── service/    # 服务层
│   │   │   │   └── util/       # 工具类
│   │   │   └── resources/
│   │   │       └── application.yml
│   │   └── test/
│   └── pom.xml
├── frontend/                   # 前端项目
│   ├── src/
│   │   ├── api/               # API接口
│   │   ├── pages/             # 页面组件
│   │   ├── store/             # 状态管理
│   │   ├── types/             # 类型定义
│   │   ├── App.tsx
│   │   ├── main.tsx
│   │   └── index.css
│   ├── index.html
│   ├── package.json
│   ├── tsconfig.json
│   └── vite.config.ts
├── database/                   # 数据库脚本
│   └── schema.sql
└── docs/                       # 文档
    ├── API.md                 # API接口文档
    ├── DATABASE.md            # 数据库设计文档
    └── DEPLOYMENT.md          # 部署文档
```

## 快速开始

### 环境要求
- JDK 1.8+
- Node.js 18+
- MySQL 8.0+
- Redis 6.0+
- Maven 3.6+

### 后端启动

```bash
# 1. 创建数据库并导入数据
mysql -h 192.168.1.20 -u root -p < database/schema.sql

# 2. 进入后端目录
cd backend

# 3. 编译打包
mvn clean package -DskipTests

# 4. 运行
java -jar target/fund-system-1.0.0.jar
```

### 前端启动

```bash
# 1. 进入前端目录
cd frontend

# 2. 安装依赖
npm install

# 3. 启动开发服务器
npm run dev

# 4. 访问 http://localhost:3000
```

## 配置说明

### 数据库配置
编辑 `backend/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://192.168.1.20:3306/fund_system
    username: root
    password: R00t@qiyeanquan2018
```

### Redis配置
```yaml
spring:
  redis:
    host: 192.168.1.20
    port: 6378
```

## API文档

详细API文档请查看 [API.md](docs/API.md)

## 部署文档

详细部署说明请查看 [DEPLOYMENT.md](docs/DEPLOYMENT.md)

## 数据库设计

详细数据库设计请查看 [DATABASE.md](docs/DATABASE.md)

## 性能指标

- 页面加载时间：< 2秒
- 数据更新延迟：< 30秒
- 支持水平扩展

## 安全特性

- JWT认证授权
- 密码BCrypt加密
- CORS跨域配置
- SQL注入防护
- XSS攻击防护

## 许可证

MIT License
