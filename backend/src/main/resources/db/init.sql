-- 热门基金配置表
CREATE TABLE IF NOT EXISTS t_hot_fund_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    fund_code VARCHAR(10) NOT NULL COMMENT '基金代码',
    sort_num INT DEFAULT 999 COMMENT '排序号',
    status TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_fund_code (fund_code),
    INDEX idx_sort_num (sort_num),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='热门基金配置表';
