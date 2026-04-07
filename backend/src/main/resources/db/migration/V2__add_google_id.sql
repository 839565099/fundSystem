-- V2__add_google_id.sql
ALTER TABLE t_user ADD COLUMN google_id VARCHAR(50) DEFAULT NULL UNIQUE COMMENT 'Google账号ID';
CREATE INDEX idx_user_google_id ON t_user(google_id);
CREATE INDEX idx_user_email ON t_user(email);
