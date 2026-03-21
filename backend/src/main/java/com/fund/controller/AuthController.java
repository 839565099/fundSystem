package com.fund.controller;

import com.fund.common.Result;
import com.fund.dto.ChangePasswordDTO;
import com.fund.dto.LoginDTO;
import com.fund.dto.RegisterDTO;
import com.fund.dto.UpdateProfileDTO;
import com.fund.entity.User;
import com.fund.service.UserService;
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
    
    @PostMapping("/register")
    public Result<UserVO> register(@Valid @RequestBody RegisterDTO registerDTO) {
        UserVO user = userService.register(registerDTO);
        return Result.success("注册成功", user);
    }
    
    @PostMapping("/login")
    public Result<String> login(@Valid @RequestBody LoginDTO loginDTO) {
        String token = userService.login(loginDTO);
        return Result.success("登录成功", token);
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
}
