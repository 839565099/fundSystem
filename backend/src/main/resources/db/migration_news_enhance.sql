-- 资讯模块增强字段迁移脚本
-- 请手动执行此SQL

ALTER TABLE t_fund_news
ADD COLUMN summary VARCHAR(500) COMMENT '摘要' AFTER title,
ADD COLUMN cover_image VARCHAR(500) COMMENT '封面图URL' AFTER content,
ADD COLUMN original_url VARCHAR(500) COMMENT '原文链接' AFTER cover_image;
