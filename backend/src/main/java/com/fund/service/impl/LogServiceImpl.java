package com.fund.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fund.entity.OperationLog;
import com.fund.entity.SystemLog;
import com.fund.mapper.OperationLogMapper;
import com.fund.mapper.SystemLogMapper;
import com.fund.service.LogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 日志服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogServiceImpl implements LogService {

    private final OperationLogMapper operationLogMapper;
    private final SystemLogMapper systemLogMapper;

    @Override
    public void logOperation(Long userId, String username, String operation, String targetType, String targetId, String detail, String ip) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setOperation(operation);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(detail);
        log.setIp(ip);
        operationLogMapper.insert(log);
    }

    @Override
    public Page<OperationLog> getOperationLogs(Integer page, Integer pageSize, String operation, String username) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();

        if (operation != null && !operation.isEmpty()) {
            wrapper.eq(OperationLog::getOperation, operation);
        }
        if (username != null && !username.isEmpty()) {
            wrapper.like(OperationLog::getUsername, username);
        }

        wrapper.orderByDesc(OperationLog::getCreateTime);

        return operationLogMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    @Override
    public void logSystem(String logType, String content, String status, String errorMsg, Integer durationMs) {
        SystemLog log = new SystemLog();
        log.setLogType(logType);
        log.setContent(content);
        log.setStatus(status);
        log.setErrorMsg(errorMsg);
        log.setDurationMs(durationMs);
        systemLogMapper.insert(log);
    }

    @Override
    public Page<SystemLog> getSystemLogs(Integer page, Integer pageSize, String logType) {
        LambdaQueryWrapper<SystemLog> wrapper = new LambdaQueryWrapper<>();

        if (logType != null && !logType.isEmpty()) {
            wrapper.eq(SystemLog::getLogType, logType);
        }

        wrapper.orderByDesc(SystemLog::getCreateTime);

        return systemLogMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }
}
