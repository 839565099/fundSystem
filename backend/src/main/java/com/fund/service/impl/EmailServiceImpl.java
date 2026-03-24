package com.fund.service.impl;

import com.fund.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

/**
 * 邮件服务实现
 */
@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String from;

    @Override
    @Async
    public void sendPasswordResetEmail(String to, String resetToken, String resetUrl) {
        if (mailSender == null) {
            log.warn("邮件服务未配置，跳过发送密码重置邮件到: {}", to);
            log.info("密码重置链接: {}/reset-password?token={}", resetUrl, resetToken);
            return;
        }

        String resetLink = resetUrl + "?token=" + resetToken;
        String subject = "【基金系统】重置密码";
        String content = buildResetEmailContent(resetLink);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);
            mailSender.send(message);
            log.info("密码重置邮件发送成功: {}", to);
        } catch (MessagingException e) {
            log.error("密码重置邮件发送失败: {}", e.getMessage());
            throw new RuntimeException("邮件发送失败，请稍后重试");
        }
    }

    @Override
    @Async
    public void sendVerificationCodeEmail(String to, String code) {
        if (mailSender == null) {
            log.warn("邮件服务未配置，跳过发送验证码邮件到: {}", to);
            log.info("验证码: {}", code);
            return;
        }

        String subject = "【基金系统】验证码";
        String content = buildVerificationCodeContent(code);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);
            mailSender.send(message);
            log.info("验证码邮件发送成功: {}", to);
        } catch (MessagingException e) {
            log.error("验证码邮件发送失败: {}", e.getMessage());
            throw new RuntimeException("邮件发送失败，请稍后重试");
        }
    }

    @Override
    @Async
    public void sendSimpleEmail(String to, String subject, String content) {
        if (mailSender == null) {
            log.warn("邮件服务未配置，跳过发送邮件到: {}", to);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);
        mailSender.send(message);
        log.info("邮件发送成功: {}", to);
    }

    private String buildResetEmailContent(String resetLink) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif; padding: 20px; background-color: #f5f5f5;'>" +
                "<div style='max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);'>" +
                "<h2 style='color: #18a058; margin-bottom: 20px;'>密码重置</h2>" +
                "<p style='color: #333; line-height: 1.6;'>您好，</p>" +
                "<p style='color: #333; line-height: 1.6;'>您正在进行密码重置操作，请点击以下按钮重置您的密码：</p>" +
                "<div style='text-align: center; margin: 30px 0;'>" +
                "<a href='" + resetLink + "' style='display: inline-block; padding: 12px 30px; background-color: #18a058; color: white; text-decoration: none; border-radius: 5px; font-weight: bold;'>重置密码</a>" +
                "</div>" +
                "<p style='color: #666; font-size: 14px;'>如果按钮无法点击，请复制以下链接到浏览器打开：</p>" +
                "<p style='color: #18a058; word-break: break-all; font-size: 14px;'>" + resetLink + "</p>" +
                "<p style='color: #999; font-size: 12px; margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee;'>此链接有效期为30分钟，如非本人操作请忽略此邮件。</p>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    private String buildVerificationCodeContent(String code) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif; padding: 20px; background-color: #f5f5f5;'>" +
                "<div style='max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);'>" +
                "<h2 style='color: #18a058; margin-bottom: 20px;'>验证码</h2>" +
                "<p style='color: #333; line-height: 1.6;'>您好，</p>" +
                "<p style='color: #333; line-height: 1.6;'>您的验证码是：</p>" +
                "<div style='text-align: center; margin: 30px 0;'>" +
                "<span style='font-size: 32px; font-weight: bold; color: #18a058; letter-spacing: 10px;'>" + code + "</span>" +
                "</div>" +
                "<p style='color: #999; font-size: 12px; margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee;'>验证码有效期为30分钟，如非本人操作请忽略此邮件。</p>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
}
