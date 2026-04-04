package com.fund.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fund.common.PageResult;
import com.fund.dto.SessionConfigUpdateDTO;
import com.fund.entity.SessionConfig;
import com.fund.entity.SessionLog;
import com.fund.mapper.SessionConfigMapper;
import com.fund.mapper.SessionLogMapper;
import com.fund.service.SessionService;
import com.fund.vo.LoginVO;
import com.fund.vo.SessionInfoVO;
import com.fund.vo.SessionLogVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private static final String SESSION_KEY_PREFIX = "session:";
    private static final String SESSION_ACTIVE_KEY = "session:active";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SessionConfigMapper sessionConfigMapper;
    private final SessionLogMapper sessionLogMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public LoginVO.SessionInfo createSession(Long userId, String username, String role, HttpServletRequest request) {
        SessionConfig config = getConfigByRole(role);
        int maxDuration = config != null && config.getIsEnabled() ? config.getMaxDurationMinutes() : 480;
        int warningMinutes = config != null ? config.getWarningMinutes() : 5;

        String sessionId = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireTime = now.plusMinutes(maxDuration);

        String ipAddress = getClientIpAddress(request);

        // 写入 Redis Hash
        String redisKey = SESSION_KEY_PREFIX + userId;
        Map<String, String> sessionData = new HashMap<>();
        sessionData.put("sessionId", sessionId);
        sessionData.put("username", username);
        sessionData.put("role", role);
        sessionData.put("loginTime", String.valueOf(System.currentTimeMillis()));
        sessionData.put("expireTime", String.valueOf(expireTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()));
        sessionData.put("maxDuration", String.valueOf(maxDuration));
        sessionData.put("warningMinutes", String.valueOf(warningMinutes));
        sessionData.put("ipAddress", ipAddress);
        sessionData.put("warned", "false");

        redisTemplate.opsForHash().putAll(redisKey, sessionData);
        redisTemplate.expire(redisKey, maxDuration, TimeUnit.MINUTES);

        // 添加到活跃会话集合
        stringRedisTemplate.opsForSet().add(SESSION_ACTIVE_KEY, String.valueOf(userId));

        // 记录审计日志
        saveSessionLog(userId, username, sessionId, "LOGIN", ipAddress, request.getHeader("User-Agent"), now, expireTime, "用户登录");

        // 构建 SessionInfo
        LoginVO.SessionInfo sessionInfo = new LoginVO.SessionInfo();
        sessionInfo.setSessionId(sessionId);
        sessionInfo.setLoginTime(now);
        sessionInfo.setExpireTime(expireTime);
        sessionInfo.setMaxDurationMinutes(maxDuration);
        sessionInfo.setWarningMinutes(warningMinutes);

        return sessionInfo;
    }

    @Override
    public void destroySession(Long userId) {
        String redisKey = SESSION_KEY_PREFIX + userId;
        Map<Object, Object> data = redisTemplate.opsForHash().entries(redisKey);
        if (!data.isEmpty()) {
            String sessionId = (String) data.get("sessionId");
            String username = (String) data.get("username");
            saveSessionLog(userId, username, sessionId, "LOGOUT", (String) data.get("ipAddress"), null, null, null, "用户主动登出");
        }
        redisTemplate.delete(redisKey);
        stringRedisTemplate.opsForSet().remove(SESSION_ACTIVE_KEY, String.valueOf(userId));
    }

    @Override
    public void kickUser(Long userId) {
        String redisKey = SESSION_KEY_PREFIX + userId;
        Map<Object, Object> data = redisTemplate.opsForHash().entries(redisKey);
        if (!data.isEmpty()) {
            String sessionId = (String) data.get("sessionId");
            String username = (String) data.get("username");

            // WebSocket 推送踢人通知
            Map<String, Object> message = new HashMap<>();
            message.put("type", "FORCE_LOGOUT");
            Map<String, String> msgData = new HashMap<>();
            msgData.put("reason", "ADMIN_KICKED");
            message.put("data", msgData);
            messagingTemplate.convertAndSendToUser(String.valueOf(userId), "/queue/session", message);

            saveSessionLog(userId, username, sessionId, "KICKED", (String) data.get("ipAddress"), null, null, null, "管理员强制下线");
        }
        redisTemplate.delete(redisKey);
        stringRedisTemplate.opsForSet().remove(SESSION_ACTIVE_KEY, String.valueOf(userId));
    }

    @Override
    public boolean validateSession(Long userId) {
        String redisKey = SESSION_KEY_PREFIX + userId;
        Map<Object, Object> data = redisTemplate.opsForHash().entries(redisKey);
        if (data.isEmpty()) {
            return false;
        }
        String expireTimeStr = (String) data.get("expireTime");
        if (expireTimeStr != null) {
            long expireTimeMs = Long.parseLong(expireTimeStr);
            if (System.currentTimeMillis() > expireTimeMs) {
                // 会话已过期，清理
                String sessionId = (String) data.get("sessionId");
                String username = (String) data.get("username");
                saveSessionLog(userId, username, sessionId, "EXPIRED", (String) data.get("ipAddress"), null, null, null, "会话到期自动下线");
                redisTemplate.delete(redisKey);
                stringRedisTemplate.opsForSet().remove(SESSION_ACTIVE_KEY, String.valueOf(userId));
                return false;
            }
        }
        return true;
    }

    @Override
    public List<SessionInfoVO> getActiveSessions() {
        Set<String> userIds = stringRedisTemplate.opsForSet().members(SESSION_ACTIVE_KEY);
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<SessionInfoVO> sessions = new ArrayList<>();
        for (String userIdStr : userIds) {
            try {
                Long userId = Long.parseLong(userIdStr);
                String redisKey = SESSION_KEY_PREFIX + userId;
                Map<Object, Object> data = redisTemplate.opsForHash().entries(redisKey);
                if (data.isEmpty()) {
                    stringRedisTemplate.opsForSet().remove(SESSION_ACTIVE_KEY, userIdStr);
                    continue;
                }

                SessionInfoVO vo = new SessionInfoVO();
                vo.setUserId(userId);
                vo.setUsername((String) data.get("username"));
                vo.setRole((String) data.get("role"));
                vo.setSessionId((String) data.get("sessionId"));
                vo.setIpAddress((String) data.get("ipAddress"));

                String loginTimeMs = (String) data.get("loginTime");
                String expireTimeMs = (String) data.get("expireTime");
                if (loginTimeMs != null) {
                    vo.setLoginTime(LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(Long.parseLong(loginTimeMs)),
                            java.time.ZoneId.systemDefault()));
                }
                if (expireTimeMs != null) {
                    long expireMs = Long.parseLong(expireTimeMs);
                    vo.setExpireTime(LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(expireMs),
                            java.time.ZoneId.systemDefault()));
                    long remainingMs = expireMs - System.currentTimeMillis();
                    vo.setRemainingMinutes(remainingMs > 0 ? remainingMs / 60000.0 : 0);
                }

                sessions.add(vo);
            } catch (NumberFormatException e) {
                log.warn("无效的用户ID: {}", userIdStr);
            }
        }

        // 按剩余时间排序
        sessions.sort(Comparator.comparingDouble(SessionInfoVO::getRemainingMinutes));
        return sessions;
    }

    @Override
    public List<SessionConfig> getSessionConfigs() {
        return sessionConfigMapper.selectList(null);
    }

    @Override
    public void updateSessionConfig(String roleName, SessionConfigUpdateDTO dto) {
        LambdaQueryWrapper<SessionConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SessionConfig::getRoleName, roleName);
        SessionConfig config = sessionConfigMapper.selectOne(wrapper);
        if (config == null) {
            config = new SessionConfig();
            config.setRoleName(roleName);
            config.setMaxDurationMinutes(dto.getMaxDurationMinutes());
            config.setWarningMinutes(dto.getWarningMinutes());
            config.setIsEnabled(dto.getIsEnabled());
            sessionConfigMapper.insert(config);
        } else {
            config.setMaxDurationMinutes(dto.getMaxDurationMinutes());
            config.setWarningMinutes(dto.getWarningMinutes());
            config.setIsEnabled(dto.getIsEnabled());
            sessionConfigMapper.updateById(config);
        }
    }

    @Override
    public PageResult<SessionLogVO> getSessionLogs(int page, int pageSize, String eventType, String username, String startTime, String endTime) {
        LambdaQueryWrapper<SessionLog> wrapper = new LambdaQueryWrapper<>();
        if (eventType != null && !eventType.isEmpty()) {
            wrapper.eq(SessionLog::getEventType, eventType);
        }
        if (username != null && !username.isEmpty()) {
            wrapper.like(SessionLog::getUsername, username);
        }
        if (startTime != null && !startTime.isEmpty()) {
            wrapper.ge(SessionLog::getEventTime, LocalDateTime.parse(startTime, FORMATTER));
        }
        if (endTime != null && !endTime.isEmpty()) {
            wrapper.le(SessionLog::getEventTime, LocalDateTime.parse(endTime, FORMATTER));
        }
        wrapper.orderByDesc(SessionLog::getEventTime);

        Page<SessionLog> pageResult = sessionLogMapper.selectPage(new Page<>(page, pageSize), wrapper);

        List<SessionLogVO> voList = pageResult.getRecords().stream().map(sessionLog -> {
            SessionLogVO vo = new SessionLogVO();
            BeanUtils.copyProperties(sessionLog, vo);
            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(voList, pageResult.getTotal(), pageResult.getSize(), pageResult.getCurrent());
    }

    @Scheduled(fixedRate = 30000)
    public void sessionMonitorTask() {
        Set<String> userIds = stringRedisTemplate.opsForSet().members(SESSION_ACTIVE_KEY);
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        for (String userIdStr : userIds) {
            try {
                Long userId = Long.parseLong(userIdStr);
                String redisKey = SESSION_KEY_PREFIX + userId;
                Map<Object, Object> data = redisTemplate.opsForHash().entries(redisKey);

                if (data.isEmpty()) {
                    stringRedisTemplate.opsForSet().remove(SESSION_ACTIVE_KEY, userIdStr);
                    continue;
                }

                String expireTimeStr = (String) data.get("expireTime");
                String warned = (String) data.get("warned");
                String sessionId = (String) data.get("sessionId");
                String username = (String) data.get("username");
                String ipAddress = (String) data.get("ipAddress");
                String warningMinutesStr = (String) data.get("warningMinutes");

                if (expireTimeStr == null) continue;

                long expireTimeMs = Long.parseLong(expireTimeStr);
                long remainingMs = expireTimeMs - now;

                if (remainingMs <= 0) {
                    // 会话已过期，强制下线
                    Map<String, Object> message = new HashMap<>();
                    message.put("type", "FORCE_LOGOUT");
                    Map<String, String> msgData = new HashMap<>();
                    msgData.put("reason", "SESSION_EXPIRED");
                    message.put("data", msgData);
                    messagingTemplate.convertAndSendToUser(String.valueOf(userId), "/queue/session", message);

                    redisTemplate.delete(redisKey);
                    stringRedisTemplate.opsForSet().remove(SESSION_ACTIVE_KEY, userIdStr);

                    LocalDateTime loginTime = data.get("loginTime") != null ?
                            LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(Long.parseLong((String) data.get("loginTime"))), java.time.ZoneId.systemDefault()) : null;
                    LocalDateTime expireTime = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(expireTimeMs), java.time.ZoneId.systemDefault());

                    saveSessionLog(userId, username, sessionId, "EXPIRED", ipAddress, null, loginTime, expireTime, "会话到期自动下线");
                    log.info("会话过期，用户 {} ({}) 已自动下线", username, userId);
                } else if (warningMinutesStr != null && "false".equals(warned)) {
                    // 检查是否需要提醒
                    int warningMinutes = Integer.parseInt(warningMinutesStr);
                    long warningMs = (long) warningMinutes * 60 * 1000;
                    if (remainingMs <= warningMs) {
                        // 发送即将到期提醒
                        double remainingMinutes = remainingMs / 60000.0;
                        Map<String, Object> message = new HashMap<>();
                        message.put("type", "SESSION_WARNING");
                        Map<String, Object> msgData = new HashMap<>();
                        msgData.put("remainingMinutes", Math.round(remainingMinutes * 10) / 10.0);
                        msgData.put("expireTime", LocalDateTime.ofInstant(
                                java.time.Instant.ofEpochMilli(expireTimeMs), java.time.ZoneId.systemDefault()).toString());
                        message.put("data", msgData);
                        messagingTemplate.convertAndSendToUser(String.valueOf(userId), "/queue/session", message);

                        // 标记已提醒
                        redisTemplate.opsForHash().put(redisKey, "warned", "true");

                        saveSessionLog(userId, username, sessionId, "WARNING", ipAddress, null, null, null, "会话即将到期，剩余" + Math.round(remainingMinutes) + "分钟");
                        log.info("会话即将到期，用户 {} ({}) 剩余 {} 分钟", username, userId, Math.round(remainingMinutes));
                    }
                }
            } catch (Exception e) {
                log.error("监控会话异常，用户ID: {}", userIdStr, e);
            }
        }
    }

    private SessionConfig getConfigByRole(String role) {
        LambdaQueryWrapper<SessionConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SessionConfig::getRoleName, role);
        return sessionConfigMapper.selectOne(wrapper);
    }

    private void saveSessionLog(Long userId, String username, String sessionId, String eventType,
                                String ipAddress, String userAgent,
                                LocalDateTime loginTime, LocalDateTime expireTime, String remark) {
        try {
            SessionLog sessionLog = new SessionLog();
            sessionLog.setUserId(userId);
            sessionLog.setUsername(username);
            sessionLog.setSessionId(sessionId);
            sessionLog.setEventType(eventType);
            sessionLog.setIpAddress(ipAddress);
            sessionLog.setUserAgent(userAgent);
            sessionLog.setLoginTime(loginTime);
            sessionLog.setExpireTime(expireTime);
            sessionLog.setEventTime(LocalDateTime.now());
            sessionLog.setRemark(remark);
            sessionLogMapper.insert(sessionLog);
        } catch (Exception e) {
            log.error("保存会话日志失败", e);
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
