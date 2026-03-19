-- 添加昨日涨跌幅字段
-- 执行日期: 2024-03-18

-- 为基金表添加昨日涨跌幅字段
ALTER TABLE t_fund ADD COLUMN yesterday_growth DECIMAL(10, 2) DEFAULT NULL COMMENT '昨日涨跌幅(%)';
