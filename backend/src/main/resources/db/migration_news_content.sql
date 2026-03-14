-- 新闻内容字段迁移脚本
-- 确保新闻表有内容相关字段

-- 检查并添加 content 字段（如果不存在）
-- MySQL 8.0+ 语法
ALTER TABLE t_fund_news
    ADD COLUMN IF NOT EXISTS content TEXT COMMENT '新闻正文内容' AFTER title;

-- 添加 summary 字段（如果不存在）
ALTER TABLE t_fund_news
    ADD COLUMN IF NOT EXISTS summary VARCHAR(500) COMMENT '摘要' AFTER title;

-- 添加 original_url 字段（如果不存在）
ALTER TABLE t_fund_news
    ADD COLUMN IF NOT EXISTS original_url VARCHAR(500) COMMENT '原文链接' AFTER content;

-- 添加 cover_image 字段（如果不存在）
ALTER TABLE t_fund_news
    ADD COLUMN IF NOT EXISTS cover_image VARCHAR(500) COMMENT '封面图URL' AFTER content;
