package com.fund.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fund.entity.PasswordResetToken;
import com.fund.entity.User;
import com.fund.mapper.PasswordResetTokenMapper;
import com.fund.mapper.UserMapper;
import com.fund.service.EmailService;
import com.fund.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 密码重置服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private final PasswordResetTokenMapper tokenMapper;
    private final UserMapper userMapper;
    private final EmailService emailService;

    @Value("${password.reset.url}")
    private String resetUrl;

    @Value("${password.reset.expire-minutes:30}")
    private int expireMinutes;

    @Override
    @Transactional
    public boolean sendResetEmail(String email) {
        // 查找用户
        User user = findUserByEmail(email);
        if (user == null) {
            // 不暴露用户是否存在的信息
            log.warn("尝试重置不存在的邮箱: {}", email);
            return true;
        }

        // 使之前的令牌失效
        tokenMapper.invalidateByEmail(email);

        // 生成新令牌
        String token = generateToken();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUserId(user.getId());
        resetToken.setEmail(email);
        resetToken.setToken(token);
        resetToken.setExpireTime(LocalDateTime.now().plusMinutes(expireMinutes));
        tokenMapper.insert(resetToken);

        // 发送邮件
        try {
            emailService.sendPasswordResetEmail(email, token, resetUrl);
            return true;
        } catch (Exception e) {
            log.error("发送重置邮件失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public boolean sendVerificationCode(String email) {
        // 查找用户
        User user = findUserByEmail(email);
        if (user == null) {
            log.warn("尝试发送验证码到不存在的邮箱: {}", email);
            return true;
        }

        // 使之前的令牌失效
        tokenMapper.invalidateByEmail(email);

        // 生成验证码
        String code = generateCode();
        String token = generateToken();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUserId(user.getId());
        resetToken.setEmail(email);
        resetToken.setToken(token);
        resetToken.setCode(code);
        resetToken.setExpireTime(LocalDateTime.now().plusMinutes(expireMinutes));
        tokenMapper.insert(resetToken);

        // 发送邮件
        try {
            emailService.sendVerificationCodeEmail(email, code);
            return true;
        } catch (Exception e) {
            log.error("发送验证码失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean validateToken(String token) {
        PasswordResetToken resetToken = tokenMapper.selectOne(
                new LambdaQueryWrapper<PasswordResetToken>()
                        .eq(PasswordResetToken::getToken, token)
                        .isNull(PasswordResetToken::getUsedTime)
                        .gt(PasswordResetToken::getExpireTime, LocalDateTime.now())
        );
        return resetToken != null;
    }

    @Override
    public boolean validateCode(String email, String code) {
        PasswordResetToken resetToken = tokenMapper.selectOne(
                new LambdaQueryWrapper<PasswordResetToken>()
                        .eq(PasswordResetToken::getEmail, email)
                        .eq(PasswordResetToken::getCode, code)
                        .isNull(PasswordResetToken::getUsedTime)
                        .gt(PasswordResetToken::getExpireTime, LocalDateTime.now())
        );
        return resetToken != null;
    }

    @Override
    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenMapper.selectOne(
                new LambdaQueryWrapper<PasswordResetToken>()
                        .eq(PasswordResetToken::getToken, token)
                        .isNull(PasswordResetToken::getUsedTime)
        );

        if (resetToken == null || resetToken.getExpireTime().isBefore(LocalDateTime.now())) {
            log.warn("无效或过期的重置令牌: {}", token);
            return false;
        }

        // 更新密码
        User user = userMapper.selectById(resetToken.getUserId());
        if (user == null) {
            return false;
        }

        user.setPassword(BCrypt.hashpw(newPassword));
        userMapper.updateById(user);

        // 标记令牌已使用
        tokenMapper.markAsUsed(token);

        log.info("用户 {} 密码重置成功", user.getUsername());
        return true;
    }

    @Override
    @Transactional
    public boolean resetPasswordWithCode(String email, String code, String newPassword) {
        PasswordResetToken resetToken = tokenMapper.selectOne(
                new LambdaQueryWrapper<PasswordResetToken>()
                        .eq(PasswordResetToken::getEmail, email)
                        .eq(PasswordResetToken::getCode, code)
                        .isNull(PasswordResetToken::getUsedTime)
        );

        if (resetToken == null || resetToken.getExpireTime().isBefore(LocalDateTime.now())) {
            log.warn("无效或过期的验证码: {} for {}", code, email);
            return false;
        }

        // 更新密码
        User user = userMapper.selectById(resetToken.getUserId());
        if (user == null) {
            return false;
        }

        user.setPassword(BCrypt.hashpw(newPassword));
        userMapper.updateById(user);

        // 标记令牌已使用
        tokenMapper.markAsUsed(resetToken.getToken());

        log.info("用户 {} 密码重置成功", user.getUsername());
        return true;
    }

    private User findUserByEmail(String email) {
        return userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getEmail, email)
        );
    }

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String generateCode() {
        return String.format("%06d", (int) (Math.random() * 1000000));
    }
}
