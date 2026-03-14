# 基金系统功能增强实现文档

## 一、骨架屏加载功能

### 1.1 功能概述
实现页面加载过程中的视觉过渡效果，提升用户体验。

### 1.2 技术实现

**组件位置**: `frontend/src/components/Skeleton.tsx`

**核心组件**:
- `FundCardSkeleton` - 基金卡片骨架屏
- `FundDetailSkeleton` - 基金详情页骨架屏
- `ChartSkeleton` - 图表骨架屏
- `MarketCardSkeleton` - 市场行情卡片骨架屏
- `NewsCardSkeleton` - 新闻卡片骨架屏
- `RankingTableSkeleton` - 排行榜表格骨架屏
- `ProfileSkeleton` - 个人中心骨架屏
- `LoadingPage` - 全页加载组件

**样式特点**:
- 使用渐变动画模拟加载效果
- 支持深色模式
- 响应式布局

### 1.3 使用示例

```tsx
import { FundCardSkeleton } from '../components/Skeleton'

const FundList = () => {
  const [loading, setLoading] = useState(true)
  
  return (
    <Spin spinning={loading}>
      {loading ? (
        <>
          <FundCardSkeleton />
          <FundCardSkeleton />
        </>
      ) : (
        funds.map(fund => <FundCard key={fund.id} fund={fund} />)
      )}
    </Spin>
  )
}
```

---

## 二、深色模式功能

### 2.1 功能概述
实现深色/浅色模式切换，支持系统级自动跟随。

### 2.2 技术实现

**状态管理**: `frontend/src/store/themeStore.ts`

**主题切换组件**: `frontend/src/components/ThemeSwitcher.tsx`

**深色主题样式**: `frontend/src/styles/dark-theme.css`

### 2.3 支持的模式

| 模式 | 说明 |
|------|------|
| `light` | 浅色模式 |
| `dark` | 深色模式 |
| `system` | 跟随系统设置 |

### 2.4 核心功能

1. **自动持久化**: 使用 Zustand persist 中间件保存用户偏好
2. **系统跟随**: 监听 `prefers-color-scheme` 媒体查询变化
3. **全局应用**: 通过 ConfigProvider 动态切换 Ant Design 主题算法
4. **CSS 变量**: 使用 `data-theme` 属性控制自定义样式

### 2.5 使用方式

```tsx
import { useThemeStore } from '../store/themeStore'
import { ThemeSwitcher } from '../components/ThemeSwitcher'

// 在组件中使用
const { mode, actualMode, setMode } = useThemeStore()

// 渲染主题切换器
<ThemeSwitcher />
```

---

## 三、实时数据推送功能

### 3.1 功能概述
基于 WebSocket 实现基金数据、市场行情的实时推送。

### 3.2 技术架构

**后端**:
- Spring Boot WebSocket (STOMP协议)
- 配置文件: `backend/src/main/java/com/fund/config/WebSocketConfig.java`
- 推送服务: `backend/src/main/java/com/fund/service/RealtimeDataService.java`

**前端**:
- WebSocket服务: `frontend/src/services/websocket.ts`
- 使用 STOMP over SockJS

### 3.3 推送频道

| 频道 | 路径 | 说明 |
|------|------|------|
| 基金更新 | `/topic/fund/{fundCode}` | 单个基金数据更新 |
| 市场行情 | `/topic/market` | 市场指数数据 |
| 净值历史 | `/topic/nav-history/{fundCode}` | 净值历史更新 |
| 用户通知 | `/user/queue/notification` | 个人通知消息 |
| 系统告警 | `/topic/alert` | 全局告警广播 |

### 3.4 使用示例

```tsx
import { wsService } from '../services/websocket'

// 连接WebSocket
await wsService.connect()

// 订阅基金更新
const subscriptionId = wsService.subscribe('/topic/fund/000001', (data) => {
  console.log('收到基金更新:', data)
})

// 取消订阅
wsService.unsubscribe(subscriptionId)

// 断开连接
wsService.disconnect()
```

---

## 四、数据库索引优化

### 4.1 优化目标
提升查询性能，降低响应时间。

### 4.2 索引设计

**脚本位置**: `database/index_optimization.sql`

### 4.3 新增索引列表

| 表名 | 索引名 | 字段 | 用途 |
|------|--------|------|------|
| t_fund | idx_fund_type_scale | fund_type, fund_scale | 按类型和规模筛选 |
| t_fund | idx_fund_growth_ranking | day_growth, week_growth, month_growth | 排行榜查询 |
| t_fund | idx_fund_year_growth | year_growth, fund_scale | 年度收益排行 |
| t_fund | idx_fund_status_scale | status, fund_scale | 状态筛选 |
| t_fund_nav_history | idx_nav_history_fund_date | fund_code, nav_date | 净值历史查询 |
| t_fund_news | idx_news_type_time | news_type, publish_time | 新闻类型筛选 |
| t_fund_news | idx_news_sentiment_time | sentiment, publish_time | 情感筛选 |
| t_fund_news | idx_news_hot | view_count, publish_time | 热门新闻 |
| t_user_favorite | idx_favorite_user_group | user_id, group_name | 收藏分组查询 |
| t_market_data | idx_market_code_date | market_code, trade_date | 市场行情查询 |

### 4.4 性能预期

| 查询类型 | 优化前 | 优化后 | 提升幅度 |
|----------|--------|--------|----------|
| 基金搜索 | 500-1000ms | 100-200ms | 60-80% |
| 排行榜 | 800-1500ms | 150-300ms | 70-80% |
| 净值历史 | 300-600ms | 50-100ms | 70-85% |
| 新闻列表 | 400-800ms | 100-200ms | 60-75% |

### 4.5 维护建议

```sql
-- 定期分析表
ANALYZE TABLE t_fund;
ANALYZE TABLE t_fund_nav_history;

-- 定期优化表
OPTIMIZE TABLE t_fund;
OPTIMIZE TABLE t_fund_news;
```

---

## 五、个人中心功能补全

### 5.1 功能模块

**页面位置**: `frontend/src/pages/Profile.tsx`

### 5.2 功能清单

| 模块 | 功能 | 状态 |
|------|------|------|
| **基本信息** | 头像上传 | ✅ |
| | 昵称修改 | ✅ |
| | 邮箱绑定 | ✅ |
| | 手机绑定 | ✅ |
| **安全设置** | 密码修改 | ✅ |
| | 密码强度验证 | ✅ |
| | 登录历史查看 | ✅ |
| **偏好设置** | 主题切换 | ✅ |
| | 通知设置 | ✅ |
| | 邮件订阅 | ✅ |
| **账户统计** | 收藏数量 | ✅ |
| | 登录次数 | ✅ |
| | 账户天数 | ✅ |

### 5.3 API接口

```typescript
// 更新个人资料
authApi.updateProfile({ nickname, email, phone })

// 修改密码
authApi.changePassword({ oldPassword, newPassword })

// 绑定邮箱
authApi.bindEmail(email, code)

// 发送验证码
authApi.sendVerifyCode('email', email)

// 获取登录历史
authApi.getLoginHistory(pageNum, pageSize)
```

---

## 六、基金趋势图组件增强

### 6.1 功能概述
增强基金净值走势图，提供更丰富的图表类型和交互功能。

### 6.2 组件位置
`frontend/src/components/FundTrendChart.tsx`

### 6.3 新增功能

| 功能 | 说明 |
|------|------|
| **多种图表类型** | 支持折线图、面积图、柱状图切换 |
| **时间周期选择** | 日/周/月/三月/六月/年/三年 |
| **均线叠加** | 可选显示MA5/MA10/MA20/MA30均线 |
| **统计指标** | 显示期间涨幅、最高/最低净值、涨跌天数等 |
| **深色模式** | 完美支持深色主题 |
| **全屏显示** | 支持全屏查看图表 |
| **数据导出** | 支持导出图表数据 |

### 6.4 图表配置

```typescript
interface FundTrendChartProps {
  fundCode: string           // 基金代码
  fundName?: string          // 基金名称
  height?: number            // 图表高度 (默认400)
  showControls?: boolean     // 显示控制栏 (默认true)
  defaultPeriod?: string     // 默认周期 (默认'month')
  defaultChartType?: string  // 默认图表类型 (默认'area')
}
```

### 6.5 使用示例

```tsx
import FundTrendChart from '../components/FundTrendChart'

// 基本使用
<FundTrendChart 
  fundCode="000001" 
  fundName="华夏成长混合"
/>

// 自定义配置
<FundTrendChart 
  fundCode="000001"
  height={500}
  showControls={true}
  defaultPeriod="year"
  defaultChartType="area"
/>
```

---

## 七、部署说明

### 7.1 前端依赖安装

```bash
cd frontend
npm install
```

### 7.2 后端依赖

确保 `pom.xml` 包含以下依赖:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

### 7.3 数据库优化

```bash
# 执行索引优化脚本
mysql -u root -p fund_system < database/index_optimization.sql
```

### 7.4 启动服务

```bash
# 后端
cd backend
mvn spring-boot:run

# 前端
cd frontend
npm run dev
```

---

## 八、测试建议

### 8.1 功能测试

1. **骨架屏**: 测试各页面加载时骨架屏显示效果
2. **深色模式**: 测试所有页面在深色模式下的显示
3. **实时推送**: 测试WebSocket连接和数据推送
4. **个人中心**: 测试所有个人中心功能
5. **趋势图**: 测试各种图表配置和交互

### 8.2 性能测试

1. 使用 Lighthouse 测试前端性能
2. 使用 EXPLAIN 分析慢查询
3. 测试WebSocket并发连接

---

## 九、后续优化建议

1. **PWA支持**: 添加 Service Worker 支持离线访问
2. **虚拟滚动**: 对长列表实现虚拟滚动
3. **图表缓存**: 缓存已加载的图表数据
4. **推送优化**: 实现增量推送减少数据量
5. **国际化**: 添加多语言支持
