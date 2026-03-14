-- 新闻情绪字段迁移脚本
-- 执行库: fund_system

ALTER TABLE t_fund_news
    ADD COLUMN sentiment VARCHAR(20) NULL COMMENT '情感标签: BULLISH/BEARISH/NEUTRAL' AFTER fund_code,
    ADD COLUMN sentiment_score DECIMAL(5,4) NULL COMMENT '情感分数: -1~1' AFTER sentiment,
    ADD COLUMN sentiment_confidence DECIMAL(5,4) NULL COMMENT '置信度: 0~1' AFTER sentiment_score,
    ADD COLUMN impact_level TINYINT NULL COMMENT '影响等级: 1~5' AFTER sentiment_confidence;

CREATE INDEX idx_sentiment ON t_fund_news (sentiment);
CREATE INDEX idx_status_publish_time ON t_fund_news (status, publish_time);
CREATE INDEX idx_fund_publish_time ON t_fund_news (fund_code, publish_time);
