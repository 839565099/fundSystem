-- =============================================
-- 基金实时查看系统数据库设计
-- 数据库: fund_system
-- 字符集: utf8mb4
-- =============================================

CREATE DATABASE IF NOT EXISTS fund_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE fund_system;

-- =============================================
-- 1. 用户表
-- =============================================
DROP TABLE IF EXISTS t_user;
CREATE TABLE t_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码(加密存储)',
    email VARCHAR(100) UNIQUE COMMENT '邮箱',
    phone VARCHAR(20) UNIQUE COMMENT '手机号',
    nickname VARCHAR(50) COMMENT '昵称',
    avatar VARCHAR(255) COMMENT '头像URL',
    status TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-正常',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    last_login_time DATETIME COMMENT '最后登录时间',
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- =============================================
-- 2. 基金基本信息表
-- =============================================
DROP TABLE IF EXISTS t_fund;
CREATE TABLE t_fund (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    fund_code VARCHAR(10) NOT NULL UNIQUE COMMENT '基金代码',
    fund_name VARCHAR(100) NOT NULL COMMENT '基金名称',
    fund_type VARCHAR(20) COMMENT '基金类型: 股票型/混合型/债券型/货币型/指数型/QDII/FOF',
    fund_company VARCHAR(100) COMMENT '基金公司',
    establish_date DATE COMMENT '成立日期',
    fund_scale DECIMAL(18,2) COMMENT '基金规模(亿元)',
    nav DECIMAL(10,4) COMMENT '最新净值',
    acc_nav DECIMAL(10,4) COMMENT '累计净值',
    nav_date DATE COMMENT '净值日期',
    day_growth DECIMAL(8,4) COMMENT '日涨跌幅(%)',
    week_growth DECIMAL(8,4) COMMENT '近一周涨跌幅(%)',
    month_growth DECIMAL(8,4) COMMENT '近一月涨跌幅(%)',
    three_month_growth DECIMAL(8,4) COMMENT '近三月涨跌幅(%)',
    six_month_growth DECIMAL(8,4) COMMENT '近六月涨跌幅(%)',
    year_growth DECIMAL(8,4) COMMENT '近一年涨跌幅(%)',
    total_growth DECIMAL(10,4) COMMENT '成立以来涨跌幅(%)',
    risk_level TINYINT COMMENT '风险等级: 1-低风险, 2-中低风险, 3-中风险, 4-中高风险, 5-高风险',
    min_purchase DECIMAL(18,2) COMMENT '最低申购金额',
    purchase_rate DECIMAL(5,2) COMMENT '申购费率(%)',
    redemption_rate DECIMAL(5,2) COMMENT '赎回费率(%)',
    management_rate DECIMAL(5,4) COMMENT '管理费率(%)',
    custody_rate DECIMAL(5,4) COMMENT '托管费率(%)',
    status TINYINT DEFAULT 1 COMMENT '状态: 0-暂停申购, 1-正常, 2-已清算',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_fund_code (fund_code),
    INDEX idx_fund_name (fund_name),
    INDEX idx_fund_type (fund_type),
    INDEX idx_nav_date (nav_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='基金基本信息表';

-- =============================================
-- 3. 基金净值历史表
-- =============================================
DROP TABLE IF EXISTS t_fund_nav_history;
CREATE TABLE t_fund_nav_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    fund_code VARCHAR(10) NOT NULL COMMENT '基金代码',
    nav_date DATE NOT NULL COMMENT '净值日期',
    nav DECIMAL(10,6) COMMENT '单位净值',
    acc_nav DECIMAL(10,6) COMMENT '累计净值',
    day_growth DECIMAL(8,4) COMMENT '日涨跌幅(%)',
    subscription_status VARCHAR(10) COMMENT '申购状态',
    redemption_status VARCHAR(10) COMMENT '赎回状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_fund_date (fund_code, nav_date),
    INDEX idx_fund_code (fund_code),
    INDEX idx_nav_date (nav_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='基金净值历史表';

-- =============================================
-- 4. 基金经理表
-- =============================================
DROP TABLE IF EXISTS t_fund_manager;
CREATE TABLE t_fund_manager (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    manager_id VARCHAR(20) UNIQUE COMMENT '经理ID',
    manager_name VARCHAR(50) NOT NULL COMMENT '经理姓名',
    company VARCHAR(100) COMMENT '所属公司',
    work_years INT COMMENT '从业年限',
    start_date DATE COMMENT '任职日期',
    total_asset DECIMAL(18,2) COMMENT '管理规模(亿元)',
    best_return DECIMAL(10,4) COMMENT '任期最佳回报(%)',
    education VARCHAR(20) COMMENT '学历',
    resume TEXT COMMENT '个人简介',
    photo VARCHAR(255) COMMENT '照片URL',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_manager_id (manager_id),
    INDEX idx_manager_name (manager_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='基金经理表';

-- =============================================
-- 5. 基金经理关联表
-- =============================================
DROP TABLE IF EXISTS t_fund_manager_relation;
CREATE TABLE t_fund_manager_relation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    fund_code VARCHAR(10) NOT NULL COMMENT '基金代码',
    manager_id VARCHAR(20) NOT NULL COMMENT '经理ID',
    start_date DATE COMMENT '任职日期',
    is_main TINYINT DEFAULT 0 COMMENT '是否主基金经理: 0-否, 1-是',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_fund_manager (fund_code, manager_id),
    INDEX idx_fund_code (fund_code),
    INDEX idx_manager_id (manager_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='基金经理关联表';

-- =============================================
-- 6. 基金持仓表
-- =============================================
DROP TABLE IF EXISTS t_fund_holdings;
CREATE TABLE t_fund_holdings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    fund_code VARCHAR(10) NOT NULL COMMENT '基金代码',
    report_date DATE NOT NULL COMMENT '报告日期',
    stock_code VARCHAR(10) COMMENT '股票代码',
    stock_name VARCHAR(50) COMMENT '股票名称',
    holding_ratio DECIMAL(8,4) COMMENT '持仓比例(%)',
    holding_shares DECIMAL(18,2) COMMENT '持仓股数(万股)',
    holding_value DECIMAL(18,2) COMMENT '持仓市值(万元)',
    holding_type VARCHAR(20) COMMENT '持仓类型: 股票/债券/现金/其他',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_fund_code (fund_code),
    INDEX idx_report_date (report_date),
    INDEX idx_stock_code (stock_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='基金持仓表';

-- =============================================
-- 7. 用户收藏表
-- =============================================
DROP TABLE IF EXISTS t_user_favorite;
CREATE TABLE t_user_favorite (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    fund_code VARCHAR(10) NOT NULL COMMENT '基金代码',
    group_name VARCHAR(50) DEFAULT '默认分组' COMMENT '分组名称',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_fund (user_id, fund_code),
    INDEX idx_user_id (user_id),
    INDEX idx_fund_code (fund_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收藏表';

-- =============================================
-- 8. 基金资讯表
-- =============================================
DROP TABLE IF EXISTS t_fund_news;
CREATE TABLE t_fund_news (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    title VARCHAR(200) NOT NULL COMMENT '资讯标题',
    content TEXT COMMENT '资讯内容',
    source VARCHAR(50) COMMENT '来源',
    author VARCHAR(50) COMMENT '作者',
    news_type VARCHAR(20) COMMENT '资讯类型: 新闻/公告/研报',
    fund_code VARCHAR(10) COMMENT '关联基金代码',
    sentiment VARCHAR(20) COMMENT '情感标签: BULLISH/BEARISH/NEUTRAL',
    sentiment_score DECIMAL(5,4) COMMENT '情感分数: -1~1',
    sentiment_confidence DECIMAL(5,4) COMMENT '置信度: 0~1',
    impact_level TINYINT COMMENT '影响等级: 1~5',
    publish_time DATETIME COMMENT '发布时间',
    view_count INT DEFAULT 0 COMMENT '浏览次数',
    status TINYINT DEFAULT 1 COMMENT '状态: 0-下架, 1-上架',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_fund_code (fund_code),
    INDEX idx_news_type (news_type),
    INDEX idx_publish_time (publish_time),
    INDEX idx_sentiment (sentiment),
    INDEX idx_status_publish_time (status, publish_time),
    INDEX idx_fund_publish_time (fund_code, publish_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='基金资讯表';

-- =============================================
-- 9. 市场行情表
-- =============================================
DROP TABLE IF EXISTS t_market_data;
CREATE TABLE t_market_data (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    market_type VARCHAR(20) NOT NULL COMMENT '市场类型: 上证指数/深证成指/创业板指',
    market_code VARCHAR(10) NOT NULL COMMENT '市场代码',
    current_point DECIMAL(10,2) COMMENT '当前点位',
    change_point DECIMAL(10,2) COMMENT '涨跌点数',
    change_ratio DECIMAL(8,4) COMMENT '涨跌幅(%)',
    volume DECIMAL(18,2) COMMENT '成交量(亿手)',
    amount DECIMAL(18,2) COMMENT '成交额(亿元)',
    high_point DECIMAL(10,2) COMMENT '最高点',
    low_point DECIMAL(10,2) COMMENT '最低点',
    open_point DECIMAL(10,2) COMMENT '开盘点',
    prev_close DECIMAL(10,2) COMMENT '昨收',
    trade_date DATE COMMENT '交易日期',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_market_date (market_code, trade_date),
    INDEX idx_market_type (market_type),
    INDEX idx_trade_date (trade_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='市场行情表';

-- =============================================
-- 10. 热门基金排行表
-- =============================================
DROP TABLE IF EXISTS t_hot_fund_rank;
CREATE TABLE t_hot_fund_rank (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    fund_code VARCHAR(10) NOT NULL COMMENT '基金代码',
    rank_type VARCHAR(20) NOT NULL COMMENT '排行类型: 日/周/月/年/热门',
    rank_date DATE NOT NULL COMMENT '排行日期',
    rank_num INT COMMENT '排名',
    growth_rate DECIMAL(10,4) COMMENT '涨跌幅(%)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_fund_rank (fund_code, rank_type, rank_date),
    INDEX idx_rank_type (rank_type),
    INDEX idx_rank_date (rank_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='热门基金排行表';

-- =============================================
-- 11. 用户登录日志表
-- =============================================
DROP TABLE IF EXISTS t_user_login_log;
CREATE TABLE t_user_login_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT COMMENT '用户ID',
    username VARCHAR(50) COMMENT '用户名',
    login_ip VARCHAR(50) COMMENT '登录IP',
    login_location VARCHAR(100) COMMENT '登录地点',
    login_device VARCHAR(100) COMMENT '登录设备',
    login_status TINYINT COMMENT '登录状态: 0-失败, 1-成功',
    login_message VARCHAR(200) COMMENT '登录信息',
    login_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    INDEX idx_user_id (user_id),
    INDEX idx_login_time (login_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户登录日志表';

-- =============================================
-- 12. 系统配置表
-- =============================================
DROP TABLE IF EXISTS t_system_config;
CREATE TABLE t_system_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    config_key VARCHAR(50) NOT NULL UNIQUE COMMENT '配置键',
    config_value VARCHAR(500) COMMENT '配置值',
    config_desc VARCHAR(200) COMMENT '配置描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- =============================================
-- 初始化数据
-- =============================================

-- 初始化系统配置
INSERT INTO t_system_config (config_key, config_value, config_desc) VALUES
('data_update_interval', '30', '数据更新间隔(秒)'),
('cache_expire_time', '300', '缓存过期时间(秒)'),
('max_favorite_count', '50', '最大收藏数量'),
('api_request_limit', '100', 'API请求限制(次/分钟)');

-- 热门基金配置表
DROP TABLE IF EXISTS t_hot_fund_config;
CREATE TABLE t_hot_fund_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    fund_code VARCHAR(10) NOT NULL COMMENT '基金代码',
    sort_num INT DEFAULT 999 COMMENT '排序号',
    status TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_fund_code (fund_code),
    INDEX idx_sort_num (sort_num),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='热门基金配置表';

-- 初始化市场数据
INSERT INTO t_market_data (market_type, market_code, trade_date) VALUES
('上证指数', 'sh000001', CURDATE()),
('深证成指', 'sz399001', CURDATE()),
('创业板指', 'sz399006', CURDATE());
