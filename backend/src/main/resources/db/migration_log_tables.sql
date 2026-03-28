-- 操作日志表
CREATE TABLE IF NOT EXISTS t_operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '操作用户ID',
    username VARCHAR(50) COMMENT '操作用户名',
    operation VARCHAR(50) NOT NULL COMMENT '操作类型: ENABLE_USER, DISABLE_USER, CHANGE_ROLE, DELETE_USER',
    target_type VARCHAR(50) COMMENT '目标类型: USER, FUND 等',
    target_id VARCHAR(100) COMMENT '目标ID',
    detail TEXT COMMENT '操作详情(JSON格式)',
    ip VARCHAR(50) COMMENT 'IP地址',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user (user_id),
    INDEX idx_operation (operation),
    INDEX idx_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 系统日志表
CREATE TABLE IF NOT EXISTS t_system_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    log_type VARCHAR(50) NOT NULL COMMENT '日志类型: DATA_SYNC, DATA_CLEANUP, SCHEDULED_TASK 等',
    content TEXT NOT NULL COMMENT '日志内容',
    status VARCHAR(20) DEFAULT 'SUCCESS' COMMENT '状态: SUCCESS, FAILED',
    error_msg TEXT COMMENT '错误信息',
    duration_ms INT COMMENT '执行耗时(毫秒)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_type (log_type),
    INDEX idx_status (status),
    INDEX idx_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统日志表';
