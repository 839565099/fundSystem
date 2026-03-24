package com.fund.service;

/**
 * 邮件服务接口
 */
public interface EmailService {

    /**
     * 发送密码重置邮件
     * @param to 收件人邮箱
     * @param resetToken 重置令牌
     * @param resetUrl 重置URL前缀
     */
    void sendPasswordResetEmail(String to, String resetToken, String resetUrl);

    /**
     * 发送验证码邮件
     * @param to 收件人邮箱
     * @param code 验证码
     */
    void sendVerificationCodeEmail(String to, String code);

    /**
     * 发送简单邮件
     * @param to 收件人
     * @param subject 主题
     * @param content 内容
     */
    void sendSimpleEmail(String to, String subject, String content);
}
