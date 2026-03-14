-- =============================================
-- 基金系统数据库索引优化脚本
-- 执行方式: 在MySQL中执行
-- 创建时间: 2024
-- =============================================

USE fund_system;

-- =============================================
-- 1. 埥询分析 - 识别慢查询
-- =============================================

-- 查看当前索引情况
SELECT 
    TABLE_NAME,
    INDEX_NAME,
    COLUMN_NAME,
    SEQ_IN_INDEX
FROM 
    INFORMATION_SCHEMA.STATISTICS 
WHERE 
    TABLE_SCHEMA = 'fund_system'
ORDER BY 
    TABLE_NAME, INDEX_NAME;

-- =============================================
-- 2. 巻加复合索引
-- =============================================

-- 基金表索引优化
-- 用于基金搜索和筛选
CREATE INDEX IF NOT EXISTS idx_fund_type_scale ON t_fund(fund_type, fund_scale);
CREATE INDEX IF NOT EXISTS idx_fund_type_nav ON t_fund(fund_type, nav_date);
CREATE INDEX IF NOT EXISTS idx_fund_scale_growth ON t_fund(fund_scale DESC, day_growth DESC);
CREATE INDEX IF NOT EXISTS idx_fund_growth_ranking ON t_fund(day_growth DESC, week_growth DESC, month_growth DESC);
CREATE INDEX IF NOT EXISTS idx_fund_year_growth ON t_fund(year_growth DESC, fund_scale DESC);
CREATE INDEX IF NOT EXISTS idx_fund_total_growth ON t_fund(total_growth DESC, fund_scale DESC);
CREATE INDEX IF NOT EXISTS idx_fund_status_scale ON t_fund(status, fund_scale DESC);
CREATE INDEX IF NOT EXISTS idx_fund_company_type ON t_fund(fund_company, fund_type);
CREATE INDEX IF NOT EXISTS idx_fund_establish_date ON t_fund(establish_date);
CREATE INDEX IF NOT EXISTS idx_fund_nav_date ON t_fund(nav_date);

-- 基金净值历史表索引优化
-- 用于净值历史查询
CREATE INDEX IF NOT EXISTS idx_nav_history_fund_date ON t_fund_nav_history(fund_code, nav_date DESC);
CREATE INDEX IF NOT EXISTS idx_nav_history_date ON t_fund_nav_history(nav_date DESC);

-- 用户收藏表索引优化
-- 用于收藏列表查询
CREATE INDEX IF NOT EXISTS idx_favorite_user_group ON t_user_favorite(user_id, group_name);
CREATE INDEX IF NOT EXISTS idx_favorite_fund_user ON t_user_favorite(fund_code, user_id);

-- 基金新闻表索引优化
-- 用于新闻搜索和筛选
CREATE INDEX IF NOT EXISTS idx_news_type_time ON t_fund_news(news_type, publish_time DESC);
CREATE INDEX IF NOT EXISTS idx_news_sentiment_time ON t_fund_news(sentiment, publish_time DESC);
CREATE INDEX IF NOT EXISTS idx_news_fund_time ON t_fund_news(fund_code, publish_time DESC);
CREATE INDEX IF NOT EXISTS idx_news_status_time ON t_fund_news(status, publish_time DESC);
CREATE INDEX IF NOT EXISTS idx_news_hot ON t_fund_news(view_count DESC, publish_time DESC);
CREATE INDEX IF NOT EXISTS idx_news_type_sentiment ON t_fund_news(news_type, sentiment);

-- 市场数据表索引优化
CREATE INDEX IF NOT EXISTS idx_market_code_date ON t_market_data(market_code, trade_date DESC);
CREATE INDEX IF NOT EXISTS idx_market_date ON t_market_data(trade_date DESC);

-- 基金经理表索引优化
CREATE INDEX IF NOT EXISTS idx_manager_name ON t_fund_manager(manager_name);

-- 基金持仓表索引优化
CREATE INDEX IF NOT EXISTS idx_holdings_fund_date ON t_fund_holdings(fund_code, report_date DESC);
CREATE INDEX IF NOT EXISTS idx_holdings_stock ON t_fund_holdings(stock_code);

-- 用户登录日志表索引优化
CREATE INDEX IF NOT EXISTS idx_login_user_time ON t_user_login_log(user_id, login_time DESC);
CREATE INDEX IF NOT EXISTS idx_login_time ON t_user_login_log(login_time DESC);

-- =============================================
-- 3. 添加全文索引 (用于搜索)
-- =============================================

-- 基金名称全文搜索索引
ALTER TABLE t_fund ADD FULLTEXT INDEX ft_fund_name (fund_name);

-- 新闻标题和内容全文搜索索引
ALTER TABLE t_fund_news ADD FULLTEXT INDEX ft_news_content (title, content);

-- =============================================
-- 4. 性能测试查询
-- =============================================

-- 测试基金搜索性能
EXPLAIN SELECT * FROM t_fund WHERE fund_type = '股票型' AND fund_scale > 10 ORDER BY day_growth DESC LIMIT 20;

-- 测试基金排行性能
EXPLAIN SELECT * FROM t_fund WHERE status = 1 ORDER BY year_growth DESC LIMIT 50;

-- 测试净值历史查询性能
EXPLAIN SELECT * FROM t_fund_nav_history WHERE fund_code = '000001' ORDER BY nav_date DESC LIMIT 100;

-- 测试新闻搜索性能
EXPLAIN SELECT * FROM t_fund_news WHERE news_type = '新闻' AND sentiment = 'BULLISH' ORDER BY publish_time DESC LIMIT 20;

-- =============================================
-- 5. 知名使用建议
-- =============================================

-- 查看索引使用情况
SELECT 
    TABLE_NAME,
    INDEX_NAME,
    CARDINALITY,
    SEQ_IN_INDEX,
    IF NULL AS 'Nulls',
    INDEX_TYPE
FROM 
    INFORMATION_SCHEMA.STATISTICS 
WHERE 
    TABLE_SCHEMA = 'fund_system'
ORDER BY 
    TABLE_NAME, INDEX_NAME;

-- 查看慢查询日志 (需要先开启慢查询日志)
-- SET GLOBAL slow_query_log = 'ON';
-- SET GLOBAL long_query_time = 2;
-- SELECT * FROM mysql.slow_log ORDER BY query_time DESC LIMIT 10;

-- =============================================
-- 6. 定期维护建议
-- =============================================

-- 定期分析表
ANALYZE TABLE t_fund;
ANALYZE TABLE t_fund_nav_history;
ANALYZE TABLE t_fund_news;
ANALYZE TABLE t_user_favorite;

-- 定期优化表
OPTIMIZE TABLE t_fund;
OPTIMIZE TABLE t_fund_nav_history;
OPTIMIZE TABLE t_fund_news;
OPTIMIZE TABLE t_user_favorite;

-- =============================================
-- 7. 知名效果预期
-- =============================================

-- 基金搜索查询: 预计提升 50-70%
-- 掌上:
-- idx_fund_type_scale: 用于按类型和规模筛选
-- idx_fund_growth_ranking: 用于排行榜查询
-- idx_fund_status_scale: 用于状态筛选

-- 净值历史查询: 预计提升 60-80%
-- 掌上:
-- idx_nav_history_fund_date: 用于按基金代码查询净值历史

-- 新闻查询: 预计提升 40-60%
-- 掌上:
-- idx_news_type_time: 用于按类型查询新闻
-- idx_news_sentiment_time: 用于按情感查询新闻
-- idx_news_hot: 用于热门新闻查询

-- =============================================
-- 8. 索引维护脚本
-- =============================================

-- 创建存储过程定期重建索引
DELIMITER //

CREATE PROCEDURE IF NOT EXISTS rebuild_indexes()
BEGIN
    -- 重建基金表索引
    ALTER TABLE t_fund ENGINE=InnoDB;
    
    -- 重建净值历史表索引
    ALTER TABLE t_fund_nav_history ENGINE=InnoDB;
    
    -- 重建新闻表索引
    ALTER TABLE t_fund_news ENGINE=InnoDB;
    
    SELECT 'Index rebuild completed' AS result;
END //

DELIMITER ;

-- 执行索引重建
-- CALL rebuild_indexes();
