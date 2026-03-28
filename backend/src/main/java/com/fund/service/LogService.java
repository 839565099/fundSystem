package com.fund.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fund.entity.OperationLog;
import com.fund.entity.SystemLog;

/**
 * 日志服务接口
 */
public interface LogService {

    /**
     * 记录操作日志
     */
    void logOperation(Long userId, String username, String operation, String targetType, String targetId, String detail, String ip);

    /**
     * 分页查询操作日志
     */
    Page<OperationLog> getOperationLogs(Integer page, Integer pageSize, String operation, String username);

    /**
     * 记录系统日志
     */
    void logSystem(String logType, String content, String status, String errorMsg, Integer durationMs);

    /**
     * 分页查询系统日志
     */
    Page<SystemLog> getSystemLogs(Integer page, Integer pageSize, String logType);
}
