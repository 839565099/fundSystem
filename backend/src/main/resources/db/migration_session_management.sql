CREATE TABLE IF NOT EXISTS t_session_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    role_name VARCHAR(50) NOT NULL COMMENT '角色名',
    max_duration_minutes INT NOT NULL COMMENT '最大在线时长(分钟)',
    warning_minutes INT NOT NULL DEFAULT 5 COMMENT '到期前提醒时间(分钟)',
    is_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用: 0-禁用, 1-启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_role_name (role_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话时长配置表';

CREATE TABLE IF NOT EXISTS t_session_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    session_id VARCHAR(64) NOT NULL COMMENT '会话ID',
    event_type VARCHAR(20) NOT NULL COMMENT '事件类型: LOGIN/WARNING/EXPIRED/LOGOUT/KICKED',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    user_agent VARCHAR(500) COMMENT '浏览器UA',
    login_time DATETIME COMMENT '登录时间',
    expire_time DATETIME COMMENT '过期时间',
    event_time DATETIME NOT NULL COMMENT '事件时间',
    remark VARCHAR(255) COMMENT '备注',
    INDEX idx_session_log_user_id (user_id),
    INDEX idx_session_log_event_type (event_type),
    INDEX idx_session_log_event_time (event_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话审计日志表';

INSERT IGNORE INTO t_session_config (role_name, max_duration_minutes, warning_minutes, is_enabled)
VALUES ('USER', 480, 5, 1);

INSERT IGNORE INTO t_session_config (role_name, max_duration_minutes, warning_minutes, is_enabled)
VALUES ('ADMIN', 720, 5, 1);
