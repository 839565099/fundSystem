package com.fund.controller;

import com.fund.annotation.RequireAdmin;
import com.fund.common.Result;
import com.fund.dto.UpdateProfileDTO;
import com.fund.service.AdminService;
import com.fund.service.LogService;
import com.fund.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 管理员控制器
 */
@RestController
@RequestMapping("/admin")
@RequireAdmin
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final LogService logService;

    @GetMapping("/stats")
    public Result<Map<String, Object>> getSystemStats() {
        return Result.success(adminService.getSystemStats());
    }

    @GetMapping("/users")
    public Result<Map<String, Object>> listUsers(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer status) {
        return Result.success(adminService.listUsers(page, pageSize, keyword, role, status));
    }

    @GetMapping("/users/{id}")
    public Result<UserVO> getUserDetail(@PathVariable Long id) {
        UserVO vo = adminService.getUserDetail(id);
        if (vo == null) {
            return Result.error("用户不存在");
        }
        return Result.success(vo);
    }

    @PutMapping("/users/{id}/status")
    public Result<?> updateUserStatus(@PathVariable Long id, @RequestParam Integer status, HttpServletRequest request) {
        String error = adminService.updateUserStatus(id, status,
                (Long) request.getAttribute("userId"),
                (String) request.getAttribute("username"),
                request.getRemoteAddr());
        if (error != null) {
            return Result.error(error);
        }
        return Result.success(status == 1 ? "用户已启用" : "用户已禁用");
    }

    @PutMapping("/users/{id}")
    public Result<?> updateUserInfo(@PathVariable Long id, @RequestBody @Valid UpdateProfileDTO dto, HttpServletRequest request) {
        String error = adminService.updateUserInfo(id, dto,
                (Long) request.getAttribute("userId"),
                (String) request.getAttribute("username"),
                request.getRemoteAddr());
        if (error != null) {
            return Result.error(error);
        }
        return Result.success("用户信息更新成功");
    }

    @PutMapping("/users/{id}/role")
    public Result<?> updateUserRole(@PathVariable Long id, @RequestParam String role, HttpServletRequest request) {
        String error = adminService.updateUserRole(id, role,
                (Long) request.getAttribute("userId"),
                (String) request.getAttribute("username"),
                request.getRemoteAddr());
        if (error != null) {
            return Result.error(error);
        }
        return Result.success("角色更新成功");
    }

    @DeleteMapping("/users/{id}")
    public Result<?> deleteUser(@PathVariable Long id, HttpServletRequest request) {
        String error = adminService.deleteUser(id,
                (Long) request.getAttribute("userId"),
                (String) request.getAttribute("username"),
                request.getRemoteAddr());
        if (error != null) {
            return Result.error(error);
        }
        return Result.success("用户已删除");
    }

    @GetMapping("/logs/operations")
    public Result<Map<String, Object>> getOperationLogs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) String username) {
        return Result.success(adminService.buildPageResult(logService.getOperationLogs(page, pageSize, operation, username), page, pageSize));
    }

    @GetMapping("/logs/systems")
    public Result<Map<String, Object>> getSystemLogs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String logType) {
        return Result.success(adminService.buildPageResult(logService.getSystemLogs(page, pageSize, logType), page, pageSize));
    }
}
