-- ========================================
-- 智能化功能增强 - 数据库迁移脚本
-- 创建时间: 2024
-- 说明: 包含AI对话、推荐系统、预警系统、投资组合等新功能所需的表结构
-- ========================================

USE fund_system;

-- ========================================
-- 1. AI 问答助手相关表
-- ========================================

-- AI 对话会话表
CREATE TABLE IF NOT EXISTS t_ai_chat_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    session_id VARCHAR(64) NOT NULL COMMENT '会话唯一标识',
    title VARCHAR(200) COMMENT '会话标题(自动生成或用户设置)',
    model_type VARCHAR(50) DEFAULT 'qwen-turbo' COMMENT '使用的模型类型',
    total_tokens INT DEFAULT 0 COMMENT '总消耗token数',
    status TINYINT DEFAULT 1 COMMENT '状态: 0-已关闭 1-进行中',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    UNIQUE KEY uk_session_id (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话会话表';

-- AI 对话历史表
CREATE TABLE IF NOT EXISTS t_ai_chat_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(64) NOT NULL COMMENT '会话ID',
    role VARCHAR(20) NOT NULL COMMENT '角色: user/assistant/system',
    content TEXT NOT NULL COMMENT '消息内容',
    tokens_used INT DEFAULT 0 COMMENT '消耗token数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session_id (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话历史表';

-- AI 知识库表 (用于RAG检索增强)
CREATE TABLE IF NOT EXISTS t_ai_knowledge_base (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category VARCHAR(50) COMMENT '分类: fund_knowledge/market_analysis/invest_strategy',
    title VARCHAR(200) COMMENT '标题',
    content TEXT COMMENT '内容',
    keywords VARCHAR(500) COMMENT '关键词',
    source VARCHAR(100) COMMENT '来源',
    status TINYINT DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category (category),
    FULLTEXT INDEX ft_content (content, keywords) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI知识库表';

-- ========================================
-- 2. 智能推荐系统相关表
-- ========================================

-- 用户行为记录表
CREATE TABLE IF NOT EXISTS t_user_behavior (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT COMMENT '用户ID (游客为null)',
    session_id VARCHAR(64) COMMENT '会话ID (用于识别游客)',
    fund_code VARCHAR(10) COMMENT '基金代码',
    behavior_type VARCHAR(20) NOT NULL COMMENT '行为类型: view/favorite/compare/share/click',
    behavior_score DECIMAL(5,2) DEFAULT 1.00 COMMENT '行为权重分 (view=1, favorite=3, compare=2)',
    dwell_time INT DEFAULT 0 COMMENT '停留时间(秒)',
    extra_data JSON COMMENT '额外数据',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_fund_code (fund_code),
    INDEX idx_behavior_type (behavior_type),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户行为记录表';

-- 用户画像表
CREATE TABLE IF NOT EXISTS t_user_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE COMMENT '用户ID',
    risk_preference TINYINT DEFAULT 2 COMMENT '风险偏好: 1-保守 2-稳健 3-激进',
    invest_experience TINYINT DEFAULT 1 COMMENT '投资经验: 1-新手 2-一般 3-丰富',
    invest_period TINYINT DEFAULT 2 COMMENT '投资周期: 1-短期 2-中期 3-长期',
    preferred_types JSON COMMENT '偏好基金类型 ["股票型","混合型"]',
    preferred_sectors JSON COMMENT '偏好板块 ["科技","医药"]',
    avg_invest_amount DECIMAL(18,2) COMMENT '平均投资金额',
    total_behavior_score DECIMAL(10,2) DEFAULT 0 COMMENT '总行为分数',
    last_active_time DATETIME COMMENT '最后活跃时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_risk_preference (risk_preference)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户画像表';

-- 基金相似度表 (预计算)
CREATE TABLE IF NOT EXISTS t_fund_similarity (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    fund_code_a VARCHAR(10) NOT NULL COMMENT '基金A代码',
    fund_code_b VARCHAR(10) NOT NULL COMMENT '基金B代码',
    similarity_score DECIMAL(5,4) NOT NULL COMMENT '相似度分数 0-1',
    similarity_type VARCHAR(20) DEFAULT 'hybrid' COMMENT '相似度类型: holding/return/risk/hybrid',
    calc_time DATETIME COMMENT '计算时间',
    UNIQUE KEY uk_fund_pair (fund_code_a, fund_code_b),
    INDEX idx_score (similarity_score),
    INDEX idx_fund_a (fund_code_a)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='基金相似度表';

-- ========================================
-- 3. 智能预警系统相关表
-- ========================================

-- 用户预警规则表
CREATE TABLE IF NOT EXISTS t_user_alert_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    alert_type VARCHAR(20) NOT NULL COMMENT '预警类型: GROWTH/NAV_HIGH/NAV_LOW/RANKING/NEWS/MARKET',
    fund_code VARCHAR(10) COMMENT '基金代码 (大盘预警为null)',
    alert_name VARCHAR(100) COMMENT '规则名称',
    alert_condition VARCHAR(10) NOT NULL COMMENT '条件: GT(大于)/LT(小于)/GE(>=)/LE(<=)/EQ(等于)',
    threshold DECIMAL(10,4) NOT NULL COMMENT '阈值',
    unit VARCHAR(20) DEFAULT 'PERCENT' COMMENT '单位: PERCENT/POINT/VALUE',
    notify_channel VARCHAR(20) DEFAULT 'WEBSOCKET' COMMENT '通知渠道: WEBSOCKET/EMAIL/SMS',
    cooldown_minutes INT DEFAULT 60 COMMENT '冷却时间(分钟)，避免重复通知',
    status TINYINT DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    last_triggered_time DATETIME COMMENT '最后触发时间',
    trigger_count INT DEFAULT 0 COMMENT '触发次数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_alert_type (alert_type),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户预警规则表';

-- 预警历史记录表
CREATE TABLE IF NOT EXISTS t_alert_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    rule_id BIGINT NOT NULL COMMENT '规则ID',
    alert_type VARCHAR(20) NOT NULL COMMENT '预警类型',
    fund_code VARCHAR(10) COMMENT '基金代码',
    fund_name VARCHAR(100) COMMENT '基金名称',
    alert_title VARCHAR(200) COMMENT '预警标题',
    alert_message VARCHAR(500) COMMENT '预警消息详情',
    alert_value DECIMAL(10,4) COMMENT '触发时的实际值',
    triggered_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '触发时间',
    is_read TINYINT DEFAULT 0 COMMENT '是否已读',
    read_time DATETIME COMMENT '阅读时间',
    INDEX idx_user_id (user_id),
    INDEX idx_triggered_time (triggered_time),
    INDEX idx_is_read (is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预警历史记录表';

-- ========================================
-- 4. 投资组合管理相关表
-- ========================================

-- 投资组合表
CREATE TABLE IF NOT EXISTS t_portfolio (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    name VARCHAR(100) NOT NULL COMMENT '组合名称',
    description VARCHAR(500) COMMENT '组合描述',
    total_amount DECIMAL(18,2) DEFAULT 0 COMMENT '总投资金额',
    current_value DECIMAL(18,2) DEFAULT 0 COMMENT '当前市值',
    total_profit DECIMAL(18,2) DEFAULT 0 COMMENT '总盈亏金额',
    total_return DECIMAL(10,4) DEFAULT 0 COMMENT '总收益率(%)',
    day_profit DECIMAL(18,2) DEFAULT 0 COMMENT '今日盈亏',
    day_return DECIMAL(8,4) DEFAULT 0 COMMENT '今日收益率(%)',
    fund_count INT DEFAULT 0 COMMENT '基金数量',
    status TINYINT DEFAULT 1 COMMENT '状态: 0-已归档 1-活跃',
    is_default TINYINT DEFAULT 0 COMMENT '是否为默认组合',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投资组合表';

-- 组合持仓表
CREATE TABLE IF NOT EXISTS t_portfolio_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    portfolio_id BIGINT NOT NULL COMMENT '组合ID',
    fund_code VARCHAR(10) NOT NULL COMMENT '基金代码',
    fund_name VARCHAR(100) COMMENT '基金名称(冗余)',
    shares DECIMAL(18,4) COMMENT '持有份额',
    amount DECIMAL(18,2) NOT NULL COMMENT '投资金额',
    target_ratio DECIMAL(5,2) COMMENT '目标占比(%)',
    actual_ratio DECIMAL(5,2) COMMENT '实际占比(%)',
    buy_nav DECIMAL(10,4) COMMENT '买入净值',
    buy_date DATE COMMENT '买入日期',
    current_nav DECIMAL(10,4) COMMENT '当前净值',
    current_value DECIMAL(18,2) COMMENT '当前市值',
    profit DECIMAL(18,2) COMMENT '盈亏金额',
    profit_ratio DECIMAL(10,4) COMMENT '盈亏比例(%)',
    day_profit DECIMAL(18,2) COMMENT '今日盈亏',
    status TINYINT DEFAULT 1 COMMENT '状态: 0-已清仓 1-持有',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_portfolio_id (portfolio_id),
    UNIQUE KEY uk_portfolio_fund (portfolio_id, fund_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组合持仓表';

-- ========================================
-- 5. 高级分析相关表
-- ========================================

-- 基金分析结果缓存表
CREATE TABLE IF NOT EXISTS t_fund_analytics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    fund_code VARCHAR(10) NOT NULL COMMENT '基金代码',
    analytics_type VARCHAR(50) NOT NULL COMMENT '分析类型: sharpe/drawdown/volatility/correlation',
    period VARCHAR(20) COMMENT '周期: 1m/3m/6m/1y/3y/all',
    result_json JSON COMMENT '分析结果JSON',
    calc_time DATETIME COMMENT '计算时间',
    expire_time DATETIME COMMENT '过期时间',
    UNIQUE KEY uk_fund_analytics (fund_code, analytics_type, period),
    INDEX idx_calc_time (calc_time),
    INDEX idx_expire_time (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='基金分析结果缓存表';

-- ========================================
-- 6. 现有表结构扩展
-- ========================================

-- 扩展用户表
ALTER TABLE t_user
    ADD COLUMN IF NOT EXISTS risk_preference TINYINT DEFAULT 2 COMMENT '风险偏好: 1-保守 2-稳健 3-激进' AFTER phone,
    ADD COLUMN IF NOT EXISTS invest_experience TINYINT DEFAULT 1 COMMENT '投资经验: 1-新手 2-一般 3-丰富' AFTER risk_preference;

-- 扩展基金表 (添加分析指标)
ALTER TABLE t_fund
    ADD COLUMN IF NOT EXISTS sharpe_ratio DECIMAL(8,4) COMMENT '夏普比率' AFTER total_growth,
    ADD COLUMN IF NOT EXISTS max_drawdown DECIMAL(8,4) COMMENT '最大回撤(%)' AFTER sharpe_ratio,
    ADD COLUMN IF NOT EXISTS volatility DECIMAL(8,4) COMMENT '波动率(%)' AFTER max_drawdown,
    ADD COLUMN IF NOT EXISTS calmar_ratio DECIMAL(8,4) COMMENT '卡玛比率' AFTER volatility,
    ADD COLUMN IF NOT EXISTS sortino_ratio DECIMAL(8,4) COMMENT '索提诺比率' AFTER calmar_ratio;

-- 扩展新闻表 (添加更多分析字段)
ALTER TABLE t_fund_news
    ADD COLUMN IF NOT EXISTS importance_level TINYINT DEFAULT 1 COMMENT '重要性: 1-普通 2-重要 3-紧急' AFTER impact_level,
    ADD COLUMN IF NOT EXISTS related_funds JSON COMMENT '相关基金代码列表' AFTER importance_level,
    ADD COLUMN IF NOT EXISTS related_sectors JSON COMMENT '相关板块列表' AFTER related_funds;

-- ========================================
-- 7. 初始化数据
-- ========================================

-- 初始化AI知识库示例数据
INSERT INTO t_ai_knowledge_base (category, title, content, keywords, source) VALUES
('fund_knowledge', '什么是夏普比率', '夏普比率（Sharpe Ratio）是衡量基金风险调整后收益的指标。计算公式为：(投资组合收益率 - 无风险收益率) / 投资组合标准差。夏普比率越高，说明单位风险所获得的超额回报越高。一般来说，夏普比率大于1为良好，大于2为优秀。', '夏普比率,风险调整收益,基金指标', 'system'),
('fund_knowledge', '什么是最大回撤', '最大回撤（Maximum Drawdown）是指基金净值在选定周期内任一时间点往后推，净值走到最低点时的收益率回撤幅度的最大值。它衡量了投资者可能面临的最大亏损。最大回撤越小，说明基金的风控能力越强。', '最大回撤,风险指标,亏损幅度', 'system'),
('fund_knowledge', '基金定投策略', '基金定投是指定期定额投资基金。优点包括：1.平均成本法，降低择时风险；2.纪律性投资，避免情绪化操作；3.积少成多，适合长期投资。建议选择波动较大的股票型或混合型基金进行定投。', '定投,定期定额,投资策略', 'system'),
('invest_strategy', '如何选择基金', '选择基金建议从以下维度考虑：1.基金类型与风险偏好匹配；2.基金经理任职年限和历史业绩；3.基金规模（一般20-100亿适中）；4.历史业绩的稳定性和持续性；5.费率水平；6.投资风格和持仓结构。', '选基策略,基金选择,投资建议', 'system'),
('market_analysis', '市场情绪指标', '市场情绪指标包括：1.成交量变化；2.涨跌停家数比；3.北向资金流向；4.VIX恐慌指数；5.两市融资余额。这些指标可帮助判断市场短期走向。', '市场情绪,技术指标,资金流向', 'system');

-- ========================================
-- 8. 创建视图 (便于查询)
-- ========================================

-- 用户预警概览视图
CREATE OR REPLACE VIEW v_user_alert_overview AS
SELECT
    h.id,
    h.user_id,
    h.rule_id,
    h.alert_type,
    h.fund_code,
    h.fund_name,
    h.alert_title,
    h.alert_message,
    h.alert_value,
    h.triggered_time,
    h.is_read,
    r.alert_name,
    r.threshold,
    r.alert_condition
FROM t_alert_history h
LEFT JOIN t_user_alert_rule r ON h.rule_id = r.id;

-- 组合持仓概览视图
CREATE OR REPLACE VIEW v_portfolio_overview AS
SELECT
    p.id as portfolio_id,
    p.user_id,
    p.name as portfolio_name,
    p.total_amount,
    p.current_value,
    p.total_profit,
    p.total_return,
    p.day_profit,
    p.day_return,
    p.fund_count,
    COUNT(pi.id) as actual_fund_count
FROM t_portfolio p
LEFT JOIN t_portfolio_item pi ON p.id = pi.portfolio_id AND pi.status = 1
GROUP BY p.id;

-- ========================================
-- 完成
-- ========================================
SELECT 'Migration completed successfully!' as status;
