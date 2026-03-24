package com.fund.service;

/**
 * 密码重置服务接口
 */
public interface PasswordResetService {

    /**
     * 发送密码重置邮件
     * @param email 用户邮箱
     * @return 是否发送成功
     */
    boolean sendResetEmail(String email);

    /**
     * 发送验证码
     * @param email 用户邮箱
     * @return 是否发送成功
     */
    boolean sendVerificationCode(String email);

    /**
     * 验证重置令牌
     * @param token 重置令牌
     * @return 是否有效
     */
    boolean validateToken(String token);

    /**
     * 验证验证码
     * @param email 邮箱
     * @param code 验证码
     * @return 是否有效
     */
    boolean validateCode(String email, String code);

    /**
     * 重置密码
     * @param token 重置令牌
     * @param newPassword 新密码
     * @return 是否重置成功
     */
    boolean resetPassword(String token, String newPassword);

    /**
     * 使用验证码重置密码
     * @param email 邮箱
     * @param code 验证码
     * @param newPassword 新密码
     * @return 是否重置成功
     */
    boolean resetPasswordWithCode(String email, String code, String newPassword);
}
