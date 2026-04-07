-- V3: 新增邮箱验证字段
ALTER TABLE t_user ADD COLUMN email_verified TINYINT(1) DEFAULT 0 COMMENT '邮箱是否已验证 0-未验证 1-已验证';
