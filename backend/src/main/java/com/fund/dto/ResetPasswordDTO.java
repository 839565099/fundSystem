package com.fund.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 重置密码请求DTO
 */
@Data
public class ResetPasswordDTO {

    /**
     * 重置令牌（通过链接重置时使用）
     */
    private String token;

    /**
     * 邮箱（通过验证码重置时使用）
     */
    private String email;

    /**
     * 验证码（通过验证码重置时使用）
     */
    private String code;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度为6-20位")
    private String newPassword;
}
