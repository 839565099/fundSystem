# 基金实时查看系统 - 数据库设计文档

## 1. 概述

本文档描述基金实时查看系统的数据库设计，包括表结构、字段说明、索引设计等。

## 2. 数据库信息

- 数据库名：fund_system
- 字符集：utf8mb4
- 排序规则：utf8mb4_unicode_ci

## 3. 表结构设计

### 3.1 用户表 (t_user)

存储系统用户信息。

| 字段名 | 类型 | 是否为空 | 默认值 | 说明 |
|--------|------|----------|--------|------|
| id | BIGINT | 否 | 自增 | 用户ID，主键 |
| username | VARCHAR(50) | 否 | - | 用户名，唯一 |
| password | VARCHAR(255) | 否 | - | 密码(BCrypt加密) |
| email | VARCHAR(100) | 是 | NULL | 邮箱，唯一 |
| phone | VARCHAR(20) | 是 | NULL | 手机号，唯一 |
| nickname | VARCHAR(50) | 是 | NULL | 昵称 |
| avatar | VARCHAR(255) | 是 | NULL | 头像URL |
| status | TINYINT | 否 | 1 | 状态: 0-禁用, 1-正常 |
| create_time | DATETIME | 否 | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | 否 | CURRENT_TIMESTAMP | 更新时间 |
| last_login_time | DATETIME | 是 | NULL | 最后登录时间 |

**索引**：
- PRIMARY KEY (id)
- UNIQUE KEY idx_username (username)
- UNIQUE KEY idx_email (email)
- UNIQUE KEY idx_phone (phone)

### 3.2 基金基本信息表 (t_fund)

存储基金的基本信息。

| 字段名 | 类型 | 是否为空 | 默认值 | 说明 |
|--------|------|----------|--------|------|
| id | BIGINT | 否 | 自增 | 主键ID |
| fund_code | VARCHAR(10) | 否 | - | 基金代码，唯一 |
| fund_name | VARCHAR(100) | 否 | - | 基金名称 |
| fund_type | VARCHAR(20) | 是 | NULL | 基金类型 |
| fund_company | VARCHAR(100) | 是 | NULL | 基金公司 |
| establish_date | DATE | 是 | NULL | 成立日期 |
| fund_scale | DECIMAL(18,2) | 是 | NULL | 基金规模(亿元) |
| nav | DECIMAL(10,4) | 是 | NULL | 最新净值 |
| acc_nav | DECIMAL(10,4) | 是 | NULL | 累计净值 |
| nav_date | DATE | 是 | NULL | 净值日期 |
| day_growth | DECIMAL(8,4) | 是 | NULL | 日涨跌幅(%) |
| week_growth | DECIMAL(8,4) | 是 | NULL | 近一周涨跌幅(%) |
| month_growth | DECIMAL(8,4) | 是 | NULL | 近一月涨跌幅(%) |
| three_month_growth | DECIMAL(8,4) | 是 | NULL | 近三月涨跌幅(%) |
| six_month_growth | DECIMAL(8,4) | 是 | NULL | 近六月涨跌幅(%) |
| year_growth | DECIMAL(8,4) | 是 | NULL | 近一年涨跌幅(%) |
| total_growth | DECIMAL(10,4) | 是 | NULL | 成立以来涨跌幅(%) |
| risk_level | TINYINT | 是 | NULL | 风险等级(1-5) |
| min_purchase | DECIMAL(18,2) | 是 | NULL | 最低申购金额 |
| purchase_rate | DECIMAL(5,2) | 是 | NULL | 申购费率(%) |
| redemption_rate | DECIMAL(5,2) | 是 | NULL | 赎回费率(%) |
| management_rate | DECIMAL(5,4) | 是 | NULL | 管理费率(%) |
| custody_rate | DECIMAL(5,4) | 是 | NULL | 托管费率(%) |
| status | TINYINT | 否 | 1 | 状态: 0-暂停申购, 1-正常, 2-已清算 |
| create_time | DATETIME | 否 | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | 否 | CURRENT_TIMESTAMP | 更新时间 |

**索引**：
- PRIMARY KEY (id)
- UNIQUE KEY idx_fund_code (fund_code)
- KEY idx_fund_name (fund_name)
- KEY idx_fund_type (fund_type)
- KEY idx_nav_date (nav_date)

### 3.3 基金净值历史表 (t_fund_nav_history)

存储基金净值历史数据。

| 字段名 | 类型 | 是否为空 | 默认值 | 说明 |
|--------|------|----------|--------|------|
| id | BIGINT | 否 | 自增 | 主键ID |
| fund_code | VARCHAR(10) | 否 | - | 基金代码 |
| nav_date | DATE | 否 | - | 净值日期 |
| nav | DECIMAL(10,6) | 是 | NULL | 单位净值 |
| acc_nav | DECIMAL(10,6) | 是 | NULL | 累计净值 |
| day_growth | DECIMAL(8,4) | 是 | NULL | 日涨跌幅(%) |
| subscription_status | VARCHAR(10) | 是 | NULL | 申购状态 |
| redemption_status | VARCHAR(10) | 是 | NULL | 赎回状态 |
| create_time | DATETIME | 否 | CURRENT_TIMESTAMP | 创建时间 |

**索引**：
- PRIMARY KEY (id)
- UNIQUE KEY uk_fund_date (fund_code, nav_date)
- KEY idx_fund_code (fund_code)
- KEY idx_nav_date (nav_date)

### 3.4 基金经理表 (t_fund_manager)

存储基金经理信息。

| 字段名 | 类型 | 是否为空 | 默认值 | 说明 |
|--------|------|----------|--------|------|
| id | BIGINT | 否 | 自增 | 主键ID |
| manager_id | VARCHAR(20) | 否 | - | 经理ID，唯一 |
| manager_name | VARCHAR(50) | 否 | - | 经理姓名 |
| company | VARCHAR(100) | 是 | NULL | 所属公司 |
| work_years | INT | 是 | NULL | 从业年限 |
| start_date | DATE | 是 | NULL | 任职日期 |
| total_asset | DECIMAL(18,2) | 是 | NULL | 管理规模(亿元) |
| best_return | DECIMAL(10,4) | 是 | NULL | 任期最佳回报(%) |
| education | VARCHAR(20) | 是 | NULL | 学历 |
| resume | TEXT | 是 | NULL | 个人简介 |
| photo | VARCHAR(255) | 是 | NULL | 照片URL |
| create_time | DATETIME | 否 | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | 否 | CURRENT_TIMESTAMP | 更新时间 |

### 3.5 基金经理关联表 (t_fund_manager_relation)

存储基金与经理的关联关系。

| 字段名 | 类型 | 是否为空 | 默认值 | 说明 |
|--------|------|----------|--------|------|
| id | BIGINT | 否 | 自增 | 主键ID |
| fund_code | VARCHAR(10) | 否 | - | 基金代码 |
| manager_id | VARCHAR(20) | 否 | - | 经理ID |
| start_date | DATE | 是 | NULL | 任职日期 |
| is_main | TINYINT | 否 | 0 | 是否主基金经理 |
| create_time | DATETIME | 否 | CURRENT_TIMESTAMP | 创建时间 |

### 3.6 基金持仓表 (t_fund_holdings)

存储基金持仓信息。

| 字段名 | 类型 | 是否为空 | 默认值 | 说明 |
|--------|------|----------|--------|------|
| id | BIGINT | 否 | 自增 | 主键ID |
| fund_code | VARCHAR(10) | 否 | - | 基金代码 |
| report_date | DATE | 否 | - | 报告日期 |
| stock_code | VARCHAR(10) | 是 | NULL | 股票代码 |
| stock_name | VARCHAR(50) | 是 | NULL | 股票名称 |
| holding_ratio | DECIMAL(8,4) | 是 | NULL | 持仓比例(%) |
| holding_shares | DECIMAL(18,2) | 是 | NULL | 持仓股数(万股) |
| holding_value | DECIMAL(18,2) | 是 | NULL | 持仓市值(万元) |
| holding_type | VARCHAR(20) | 是 | NULL | 持仓类型 |
| create_time | DATETIME | 否 | CURRENT_TIMESTAMP | 创建时间 |

### 3.7 用户收藏表 (t_user_favorite)

存储用户收藏的基金。

| 字段名 | 类型 | 是否为空 | 默认值 | 说明 |
|--------|------|----------|--------|------|
| id | BIGINT | 否 | 自增 | 主键ID |
| user_id | BIGINT | 否 | - | 用户ID |
| fund_code | VARCHAR(10) | 否 | - | 基金代码 |
| group_name | VARCHAR(50) | 否 | '默认分组' | 分组名称 |
| create_time | DATETIME | 否 | CURRENT_TIMESTAMP | 创建时间 |

### 3.8 基金资讯表 (t_fund_news)

存储基金相关资讯。

| 字段名 | 类型 | 是否为空 | 默认值 | 说明 |
|--------|------|----------|--------|------|
| id | BIGINT | 否 | 自增 | 主键ID |
| title | VARCHAR(200) | 否 | - | 资讯标题 |
| content | TEXT | 是 | NULL | 资讯内容 |
| source | VARCHAR(50) | 是 | NULL | 来源 |
| author | VARCHAR(50) | 是 | NULL | 作者 |
| news_type | VARCHAR(20) | 是 | NULL | 资讯类型 |
| fund_code | VARCHAR(10) | 是 | NULL | 关联基金代码 |
| publish_time | DATETIME | 是 | NULL | 发布时间 |
| view_count | INT | 否 | 0 | 浏览次数 |
| status | TINYINT | 否 | 1 | 状态 |
| create_time | DATETIME | 否 | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | 否 | CURRENT_TIMESTAMP | 更新时间 |

### 3.9 市场行情表 (t_market_data)

存储市场行情数据。

| 字段名 | 类型 | 是否为空 | 默认值 | 说明 |
|--------|------|----------|--------|------|
| id | BIGINT | 否 | 自增 | 主键ID |
| market_type | VARCHAR(20) | 否 | - | 市场类型 |
| market_code | VARCHAR(10) | 否 | - | 市场代码 |
| current_point | DECIMAL(10,2) | 是 | NULL | 当前点位 |
| change_point | DECIMAL(10,2) | 是 | NULL | 涨跌点数 |
| change_ratio | DECIMAL(8,4) | 是 | NULL | 涨跌幅(%) |
| volume | DECIMAL(18,2) | 是 | NULL | 成交量(亿手) |
| amount | DECIMAL(18,2) | 是 | NULL | 成交额(亿元) |
| high_point | DECIMAL(10,2) | 是 | NULL | 最高点 |
| low_point | DECIMAL(10,2) | 是 | NULL | 最低点 |
| open_point | DECIMAL(10,2) | 是 | NULL | 开盘点 |
| prev_close | DECIMAL(10,2) | 是 | NULL | 昨收 |
| trade_date | DATE | 是 | NULL | 交易日期 |
| update_time | DATETIME | 否 | CURRENT_TIMESTAMP | 更新时间 |

### 3.10 热门基金排行表 (t_hot_fund_rank)

存储热门基金排行数据。

| 字段名 | 类型 | 是否为空 | 默认值 | 说明 |
|--------|------|----------|--------|------|
| id | BIGINT | 否 | 自增 | 主键ID |
| fund_code | VARCHAR(10) | 否 | - | 基金代码 |
| rank_type | VARCHAR(20) | 否 | - | 排行类型 |
| rank_date | DATE | 否 | - | 排行日期 |
| rank_num | INT | 是 | NULL | 排名 |
| growth_rate | DECIMAL(10,4) | 是 | NULL | 涨跌幅(%) |
| create_time | DATETIME | 否 | CURRENT_TIMESTAMP | 创建时间 |

### 3.11 用户登录日志表 (t_user_login_log)

存储用户登录日志。

| 字段名 | 类型 | 是否为空 | 默认值 | 说明 |
|--------|------|----------|--------|------|
| id | BIGINT | 否 | 自增 | 主键ID |
| user_id | BIGINT | 是 | NULL | 用户ID |
| username | VARCHAR(50) | 是 | NULL | 用户名 |
| login_ip | VARCHAR(50) | 是 | NULL | 登录IP |
| login_location | VARCHAR(100) | 是 | NULL | 登录地点 |
| login_device | VARCHAR(100) | 是 | NULL | 登录设备 |
| login_status | TINYINT | 是 | NULL | 登录状态 |
| login_message | VARCHAR(200) | 是 | NULL | 登录信息 |
| login_time | DATETIME | 否 | CURRENT_TIMESTAMP | 登录时间 |

### 3.12 系统配置表 (t_system_config)

存储系统配置信息。

| 字段名 | 类型 | 是否为空 | 默认值 | 说明 |
|--------|------|----------|--------|------|
| id | BIGINT | 否 | 自增 | 主键ID |
| config_key | VARCHAR(50) | 否 | - | 配置键，唯一 |
| config_value | VARCHAR(500) | 是 | NULL | 配置值 |
| config_desc | VARCHAR(200) | 是 | NULL | 配置描述 |
| create_time | DATETIME | 否 | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | 否 | CURRENT_TIMESTAMP | 更新时间 |

## 4. ER图

```
+-------------+       +-------------+       +-------------+
|   t_user    |       |   t_fund    |       |t_fund_manager|
+-------------+       +-------------+       +-------------+
| id          |<----->| id          |<----->| id          |
| username    |       | fund_code   |       | manager_id  |
| password    |       | fund_name   |       | manager_name|
| email       |       | fund_type   |       | company     |
| phone       |       | ...         |       | ...         |
+-------------+       +-------------+       +-------------+
       |                     |                     |
       |                     |                     |
       v                     v                     v
+-------------+       +-------------+       +-------------+
|t_user_favorite|     |t_fund_nav   |     |t_fund_manager|
+-------------+       |_history     |       |_relation    |
| user_id     |       +-------------+       +-------------+
| fund_code   |       | fund_code   |       | fund_code   |
| group_name  |       | nav_date    |       | manager_id  |
+-------------+       | nav         |       | is_main     |
                      +-------------+       +-------------+
```

## 5. 数据字典

### 5.1 基金类型
- 股票型
- 混合型
- 债券型
- 货币型
- 指数型
- QDII
- FOF

### 5.2 风险等级
- 1: 低风险
- 2: 中低风险
- 3: 中风险
- 4: 中高风险
- 5: 高风险

### 5.3 用户状态
- 0: 禁用
- 1: 正常

### 5.4 基金状态
- 0: 暂停申购
- 1: 正常
- 2: 已清算

### 5.5 资讯类型
- 新闻
- 公告
- 研报
