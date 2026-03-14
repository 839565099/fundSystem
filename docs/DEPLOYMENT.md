# 基金实时查看系统 - 部署文档

## 目录

1. [环境要求](#环境要求)
2. [数据库配置](#数据库配置)
3. [后端部署](#后端部署)
4. [前端部署](#前端部署)
5. [系统配置](#系统配置)
6. [常见问题](#常见问题)

## 环境要求

### 后端环境
- JDK 1.8 或以上
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+

### 前端环境
- Node.js 18+
- npm 9+ 或 yarn 1.22+

## 数据库配置

### 1. 创建数据库

```bash
# 登录MySQL
mysql -h 192.168.1.20 -u root -p

# 执行数据库脚本
source /path/to/fund-system/database/schema.sql
```

### 2. 数据库连接配置

数据库连接信息：
- 主机：192.168.1.20
- 端口：3306
- 数据库名：fund_system
- 用户名：root
- 密码：R00t@qiyeanquan2018

## 后端部署

### 1. 配置文件修改

编辑 `backend/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://192.168.1.20:3306/fund_system?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: R00t@qiyeanquan2018

  redis:
    host: 192.168.1.20
    port: 6378
    password: 
```

### 2. 编译打包

```bash
cd fund-system/backend
mvn clean package -DskipTests
```

### 3. 运行应用

```bash
# 前台运行
java -jar target/fund-system-1.0.0.jar

# 后台运行
nohup java -jar target/fund-system-1.0.0.jar > app.log 2>&1 &

# 指定配置文件运行
java -jar target/fund-system-1.0.0.jar --spring.profiles.active=prod
```

### 4. 健康检查

```bash
# 检查应用是否启动
curl http://localhost:8080/api/fund/hot?limit=5

# 查看日志
tail -f logs/fund-system.log
```

## 前端部署

### 1. 安装依赖

```bash
cd fund-system/frontend
npm install
```

### 2. 开发环境运行

```bash
npm run dev
```

访问 http://localhost:3000

### 3. 生产环境构建

```bash
npm run build
```

构建产物在 `dist` 目录下。

### 4. Nginx部署配置

创建Nginx配置文件 `/etc/nginx/conf.d/fund-system.conf`：

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态文件
    location / {
        root /path/to/fund-system/frontend/dist;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # 后端API代理
    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

重启Nginx：

```bash
nginx -t
nginx -s reload
```

## 系统配置

### Redis配置

Redis连接信息：
- 主机：192.168.1.20
- 端口：6378
- 密码：无

### JVM参数优化

```bash
java -Xms512m -Xmx1024m -XX:+UseG1GC -jar fund-system-1.0.0.jar
```

### 数据更新配置

系统默认每30秒更新一次热门基金数据，可在配置文件中修改：

```yaml
fund:
  api:
    data-update-interval: 30000
```

## Docker部署（可选）

### 后端Dockerfile

```dockerfile
FROM openjdk:8-jdk-alpine
VOLUME /tmp
COPY target/fund-system-1.0.0.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

### 前端Dockerfile (Vue 3 + Vite)

```dockerfile
FROM node:18-alpine as builder
WORKDIR /app
COPY frontend/package*.json ./
RUN npm install
COPY frontend/ .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

### Docker Compose

```yaml
version: '3'
services:
  backend:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:mysql://192.168.1.20:3306/fund_system
      - SPRING_DATASOURCE_USERNAME=root
      - SPRING_DATASOURCE_PASSWORD=R00t@qiyeanquan2018
      - SPRING_REDIS_HOST=192.168.1.20
      - SPRING_REDIS_PORT=6378

  frontend:
    build: ./frontend
    ports:
      - "80:80"
    depends_on:
      - backend
```

启动：

```bash
docker-compose up -d
```

## 常见问题

### 1. 数据库连接失败

- 检查MySQL服务是否启动
- 检查防火墙是否开放3306端口
- 检查用户名密码是否正确
- 检查数据库是否存在

### 2. Redis连接失败

- 检查Redis服务是否启动
- 检查防火墙是否开放6379端口
- 检查是否需要密码认证

### 3. 前端无法访问后端API

- 检查后端是否启动
- 检查CORS配置
- 检查Nginx代理配置

### 4. 基金数据无法获取

- 检查网络连接
- 检查东方财富API是否可访问
- 查看后端日志排查问题

## 性能优化建议

1. **数据库优化**
   - 添加适当的索引
   - 定期清理历史数据
   - 配置数据库连接池

2. **Redis缓存优化**
   - 合理设置缓存过期时间
   - 使用Redis集群提高可用性

3. **JVM优化**
   - 根据服务器内存调整堆大小
   - 使用G1垃圾收集器

4. **前端优化**
   - 开启Gzip压缩
   - 使用CDN加速静态资源
   - 配置浏览器缓存

## 监控与日志

### 日志位置

- 后端日志：`logs/fund-system.log`
- Nginx日志：`/var/log/nginx/`

### 监控指标

- JVM内存使用率
- 数据库连接数
- Redis缓存命中率
- API响应时间

## 安全建议

1. 修改默认的管理员密码
2. 启用HTTPS
3. 配置防火墙规则
4. 定期备份数据库
5. 更新依赖版本修复安全漏洞
