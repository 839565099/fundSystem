package com.fund.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fund.common.ErrorCode;
import com.fund.entity.User;
import com.fund.exception.BusinessException;
import com.fund.mapper.UserMapper;
import com.fund.service.EmailLoginService;
import com.fund.service.EmailService;
import com.fund.service.SessionService;
import com.fund.util.JwtUtil;
import com.fund.vo.LoginVO;
import com.fund.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailLoginServiceImpl implements EmailLoginService {

    private final UserMapper userMapper;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final SessionService sessionService;
    private final StringRedisTemplate redisTemplate;

    @Value("${email.login.expire-minutes:30}")
    private int expireMinutes;

    private static final String CODE_KEY_PREFIX = "email:login:code:";
    private static final String COOLDOWN_KEY_PREFIX = "email:login:cooldown:";
    private static final String FAIL_COUNT_PREFIX = "email:login:fail:";
    private static final String DAILY_COUNT_PREFIX = "email:login:daily:";
    private static final int COOLDOWN_SECONDS = 60;
    private static final int MAX_FAIL_ATTEMPTS = 5;
    private static final int MAX_DAILY_SENDS = 10;

    @Override
    public boolean sendLoginCode(String email) {
        // 检查冷却时间
        String cooldownKey = COOLDOWN_KEY_PREFIX + email;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            throw new BusinessException(ErrorCode.EMAIL_CODE_COOLDOWN);
        }

        // 检查每日发送次数上限
        String dailyKey = DAILY_COUNT_PREFIX + email;
        String dailyCountStr = redisTemplate.opsForValue().get(dailyKey);
        int dailyCount = dailyCountStr != null ? Integer.parseInt(dailyCountStr) : 0;
        if (dailyCount >= MAX_DAILY_SENDS) {
            throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED);
        }

        // 生成验证码
        String code = generateCode();

        // 存储到 Redis，30分钟有效
        String codeKey = CODE_KEY_PREFIX + email;
        redisTemplate.opsForValue().set(codeKey, code, expireMinutes, TimeUnit.MINUTES);

        // 设置冷却
        redisTemplate.opsForValue().set(cooldownKey, "1", COOLDOWN_SECONDS, TimeUnit.SECONDS);

        // 发送邮件
        try {
            emailService.sendVerificationCodeEmail(email, code);
            // 发送成功后才增加每日计数，key 到当天结束时过期
            long secondsUntilMidnight = java.time.Duration.between(
                    LocalDateTime.now(),
                    LocalDateTime.now().plusDays(1).withHour(0).withMinute(0).withSecond(0)
            ).getSeconds();
            redisTemplate.opsForValue().set(dailyKey, String.valueOf(dailyCount + 1),
                    Math.max(secondsUntilMidnight, 1), TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            log.error("发送邮箱登录验证码失败: {}", e.getMessage());
            // 发送失败，清理 Redis 状态避免不一致
            redisTemplate.delete(codeKey);
            redisTemplate.delete(cooldownKey);
            return false;
        }
    }

    @Override
    @Transactional
    public LoginVO loginWithCode(String email, String code, HttpServletRequest request) {
        // 1. 校验验证码
        String codeKey = CODE_KEY_PREFIX + email;
        String failKey = FAIL_COUNT_PREFIX + email;

        // 检查失败次数
        String failCountStr = redisTemplate.opsForValue().get(failKey);
        if (failCountStr != null && Integer.parseInt(failCountStr) >= MAX_FAIL_ATTEMPTS) {
            redisTemplate.delete(codeKey);
            redisTemplate.delete(failKey);
            throw new BusinessException(ErrorCode.EMAIL_CODE_INVALID.getCode(), "验证码已失效，请重新获取");
        }

        String storedCode = redisTemplate.opsForValue().get(codeKey);
        if (storedCode == null || !storedCode.equals(code)) {
            // 记录失败次数，30分钟过期
            Long failCount = redisTemplate.opsForValue().increment(failKey);
            if (failCount != null && failCount == 1) {
                redisTemplate.expire(failKey, expireMinutes, TimeUnit.MINUTES);
            }
            throw new BusinessException(ErrorCode.EMAIL_CODE_INVALID);
        }

        // 验证通过后删除验证码和失败计数
        redisTemplate.delete(codeKey);
        redisTemplate.delete(failKey);

        // 2. 查找或创建用户
        User user = findByEmail(email);

        if (user == null) {
            // 自动创建新用户
            user = new User();
            String baseUsername = email.split("@")[0];
            user.setUsername(generateUniqueUsername(baseUsername));
            user.setPassword(BCrypt.hashpw(UUID.randomUUID().toString()));
            user.setEmail(email);
            user.setEmailVerified(1);
            user.setNickname("用户" + String.format("%04d", (int) (Math.random() * 10000)));
            user.setStatus(1);
            user.setRole("USER");
            userMapper.insert(user);
            log.info("邮箱验证码登录自动创建用户: email={}, username={}", email, user.getUsername());
        } else {
            // 已有用户：标记邮箱已验证
            if (user.getEmailVerified() == null || user.getEmailVerified() != 1) {
                user.setEmailVerified(1);
            }
        }

        // 3. 检查用户状态
        if (user.getStatus() != null && user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }

        // 4. 更新登录时间
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        // 5. 签发 JWT + 创建会话
        String role = user.getRole() != null ? user.getRole() : "USER";
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), role);

        LoginVO.SessionInfo sessionInfo = sessionService.createSession(
                user.getId(), user.getUsername(), role, request);

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setUser(convertToVO(user));
        loginVO.setSessionInfo(sessionInfo);
        return loginVO;
    }

    private User findByEmail(String email) {
        return userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, email));
    }

    private String generateUniqueUsername(String base) {
        String username = base;
        int suffix = 1;
        while (userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username)) != null) {
            username = base + suffix;
            suffix++;
        }
        return username;
    }

    private String generateCode() {
        return String.format("%06d", (int) (Math.random() * 1000000));
    }

    private UserVO convertToVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
