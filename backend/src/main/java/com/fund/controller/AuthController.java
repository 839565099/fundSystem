package com.fund.controller;

import com.fund.common.Result;
import com.fund.dto.*;
import com.fund.entity.User;
import com.fund.service.PasswordResetService;
import com.fund.service.SessionService;
import com.fund.service.UserService;
import com.fund.vo.LoginVO;
import com.fund.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final PasswordResetService passwordResetService;
    private final SessionService sessionService;

    @PostMapping("/register")
    public Result<UserVO> register(@Valid @RequestBody RegisterDTO registerDTO) {
        UserVO user = userService.register(registerDTO);
        return Result.success("注册成功", user);
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        LoginVO loginVO = userService.login(loginDTO);
        return Result.success("登录成功", loginVO);
    }

    @PostMapping("/logout")
    public Result<?> logout(@RequestAttribute Long userId) {
        sessionService.destroySession(userId);
        return Result.success("已退出登录");
    }
    
    @GetMapping("/info")
    public Result<UserVO> getUserInfo(@RequestAttribute Long userId) {
        UserVO user = userService.getUserInfo(userId);
        return Result.success(user);
    }
    
    @PutMapping("/profile")
    public Result<UserVO> updateProfile(@RequestAttribute Long userId, @Valid @RequestBody UpdateProfileDTO dto) {
        User user = new User();
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setAvatar(dto.getAvatar());
        UserVO updatedUser = userService.updateUserInfo(userId, user);
        return Result.success("更新成功", updatedUser);
    }
    
    @PutMapping("/password")
    public Result<?> changePassword(@RequestAttribute Long userId, @Valid @RequestBody ChangePasswordDTO dto) {
        userService.updatePassword(userId, dto.getOldPassword(), dto.getNewPassword());
        return Result.success("密码修改成功", null);
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> getUserStats(@RequestAttribute Long userId) {
        Map<String, Object> stats = userService.getUserStats(userId);
        return Result.success(stats);
    }

    /**
     * 忘记密码 - 发送重置邮件
     */
    @PostMapping("/forgot-password")
    public Result<?> forgotPassword(@Valid @RequestBody ForgotPasswordDTO dto) {
        boolean success;
        if ("code".equals(dto.getType())) {
            success = passwordResetService.sendVerificationCode(dto.getEmail());
        } else {
            success = passwordResetService.sendResetEmail(dto.getEmail());
        }
        if (success) {
            return Result.success("重置邮件已发送，请查收邮箱");
        }
        return Result.error("发送失败，请稍后重试");
    }

    /**
     * 验证重置令牌
     */
    @GetMapping("/validate-reset-token")
    public Result<Boolean> validateResetToken(@RequestParam String token) {
        boolean valid = passwordResetService.validateToken(token);
        return Result.success(valid);
    }

    /**
     * 验证验证码
     */
    @PostMapping("/validate-reset-code")
    public Result<Boolean> validateResetCode(@RequestParam String email, @RequestParam String code) {
        boolean valid = passwordResetService.validateCode(email, code);
        return Result.success(valid);
    }

    /**
     * 重置密码
     */
    @PostMapping("/reset-password")
    public Result<?> resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
        boolean success;
        if (dto.getToken() != null && !dto.getToken().isEmpty()) {
            // 通过令牌重置
            success = passwordResetService.resetPassword(dto.getToken(), dto.getNewPassword());
        } else if (dto.getEmail() != null && dto.getCode() != null) {
            // 通过验证码重置
            success = passwordResetService.resetPasswordWithCode(dto.getEmail(), dto.getCode(), dto.getNewPassword());
        } else {
            return Result.error("参数不完整");
        }

        if (success) {
            return Result.success("密码重置成功，请使用新密码登录");
        }
        return Result.error("密码重置失败，请检查链接或验证码是否有效");
    }
}
