package com.fund.controller;

import com.fund.common.Result;
import com.fund.dto.*;
import com.fund.entity.User;
import com.fund.service.EmailLoginService;
import com.fund.service.GoogleOAuthService;
import com.fund.service.PasswordResetService;
import com.fund.service.SessionService;
import com.fund.service.UserService;
import com.fund.vo.LoginVO;
import com.fund.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final EmailLoginService emailLoginService;
    private final PasswordResetService passwordResetService;
    private final SessionService sessionService;
    private final GoogleOAuthService googleOAuthService;
    private final StringRedisTemplate redisTemplate;

    @Value("${google.oauth.frontend-url:http://localhost:3002}")
    private String googleFrontendUrl;

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

    /**
     * Google OAuth 登录 — 重定向到 Google 授权页
     */
    @GetMapping("/google")
    public void googleAuth(HttpServletResponse response, HttpServletRequest request) throws IOException {
        if (!googleOAuthService.isConfigured()) {
            String frontendUrl = getFrontendUrl(request) + "/auth/callback?error=not_configured";
            response.sendRedirect(frontendUrl);
            return;
        }

        String state = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set("google:state:" + state, "1", 10, TimeUnit.MINUTES);

        String authUrl = googleOAuthService.getAuthorizationUrl(state);
        response.sendRedirect(authUrl);
    }

    /**
     * Google OAuth 回调 — 处理授权结果
     */
    @GetMapping("/google/callback")
    public void googleCallback(@RequestParam String code,
                               @RequestParam(required = false) String state,
                               @RequestParam(required = false) String error,
                               HttpServletRequest request,
                               HttpServletResponse response) throws IOException {
        String frontendUrl = googleFrontendUrl;

        if (error != null) {
            response.sendRedirect(frontendUrl + "/auth/callback?error=access_denied");
            return;
        }

        if (state == null || !Boolean.TRUE.equals(redisTemplate.hasKey("google:state:" + state))) {
            response.sendRedirect(frontendUrl + "/auth/callback?error=invalid_state");
            return;
        }
        redisTemplate.delete("google:state:" + state);

        try {
            String accessToken = googleOAuthService.exchangeCodeForToken(code);
            String userInfoJson = googleOAuthService.getUserInfo(accessToken);

            com.alibaba.fastjson2.JSONObject userInfo = com.alibaba.fastjson2.JSON.parseObject(userInfoJson);
            String googleId = userInfo.getString("sub");
            String email = userInfo.getString("email");
            String name = userInfo.getString("name");
            String picture = userInfo.getString("picture");

            LoginVO loginVO = userService.loginWithGoogle(googleId, email, name, picture, request);

            response.sendRedirect(frontendUrl + "/auth/callback?token=" + loginVO.getToken());
        } catch (Exception e) {
            log.error("Google OAuth callback error", e);
            response.sendRedirect(frontendUrl + "/auth/callback?error=google_api_error");
        }
    }

    /**
     * 检查 Google OAuth 是否已配置
     */
    @GetMapping("/google/config")
    public Result<Map<String, Object>> googleConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("enabled", googleOAuthService.isConfigured());
        return Result.success(config);
    }

    /**
     * 发送邮箱登录验证码
     */
    @PostMapping("/email-login-code")
    public Result<?> sendEmailLoginCode(@Valid @RequestBody SendLoginCodeDTO dto) {
        boolean success = emailLoginService.sendLoginCode(dto.getEmail());
        if (success) {
            return Result.success("验证码已发送，请查收邮箱");
        }
        return Result.error("发送失败，请稍后重试");
    }

    /**
     * 邮箱验证码登录
     */
    @PostMapping("/email-login")
    public Result<LoginVO> emailLogin(@Valid @RequestBody EmailLoginDTO dto, HttpServletRequest request) {
        LoginVO loginVO = emailLoginService.loginWithCode(dto.getEmail(), dto.getCode(), request);
        return Result.success("登录成功", loginVO);
    }

    /**
     * 修改用户名
     */
    @PutMapping("/username")
    public Result<UserVO> updateUsername(@RequestAttribute Long userId, @RequestBody Map<String, String> body) {
        String newUsername = body.get("username");
        if (newUsername == null || newUsername.trim().isEmpty()) {
            return Result.error("用户名不能为空");
        }
        UserVO updatedUser = userService.updateUsername(userId, newUsername.trim());
        return Result.success("用户名修改成功", updatedUser);
    }

    /**
     * 设置密码（首次设置，无需旧密码）
     */
    @PostMapping("/set-password")
    public Result<?> setPassword(@RequestAttribute Long userId, @Valid @RequestBody SetPasswordDTO dto) {
        userService.setPassword(userId, dto.getPassword());
        return Result.success("密码设置成功");
    }

    /**
     * 检查是否已设置密码
     */
    @GetMapping("/has-password")
    public Result<Map<String, Object>> hasPassword(@RequestAttribute Long userId) {
        boolean has = userService.hasPassword(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("hasPassword", has);
        return Result.success(result);
    }

    private String getFrontendUrl(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin != null && !origin.isEmpty()) {
            return origin;
        }
        String referer = request.getHeader("Referer");
        if (referer != null) {
            try {
                URL url = new URL(referer);
                return url.getProtocol() + "://" + url.getHost() + (url.getPort() > 0 ? ":" + url.getPort() : "");
            } catch (Exception ignored) {}
        }
        return "http://localhost:3000";
    }
}
