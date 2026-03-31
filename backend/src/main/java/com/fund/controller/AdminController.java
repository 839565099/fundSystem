package com.fund.controller;

import com.fund.annotation.RequireAdmin;
import com.fund.common.Result;
import com.fund.service.AdminService;
import com.fund.service.LogService;
import com.fund.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 管理员控制器
 * 所有接口需要管理员权限
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final LogService logService;

    /**
     * 获取系统统计数据
     */
    @GetMapping("/stats")
    @RequireAdmin
    public Result<Map<String, Object>> getSystemStats() {
        return Result.success(adminService.getSystemStats());
    }

    /**
     * 获取用户列表
     */
    @GetMapping("/users")
    @RequireAdmin
    public Result<Map<String, Object>> listUsers(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer status) {
        return Result.success(adminService.listUsers(page, pageSize, keyword, role, status));
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/users/{id}")
    @RequireAdmin
    public Result<UserVO> getUserDetail(@PathVariable Long id) {
        UserVO vo = adminService.getUserDetail(id);
        if (vo == null) {
            return Result.error("用户不存在");
        }
        return Result.success(vo);
    }

    /**
     * 更新用户状态（启用/禁用）
     */
    @PutMapping("/users/{id}/status")
    @RequireAdmin
    public Result<?> updateUserStatus(@PathVariable Long id, @RequestParam Integer status, HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute("userId");
        String currentUsername = (String) request.getAttribute("username");
        String ip = request.getRemoteAddr();

        String error = adminService.updateUserStatus(id, status, currentUserId, currentUsername, ip);
        if (error != null) {
            return Result.error(error);
        }
        return Result.success(status == 1 ? "用户已启用" : "用户已禁用");
    }

    /**
     * 更新用户角色
     */
    @PutMapping("/users/{id}/role")
    @RequireAdmin
    public Result<?> updateUserRole(@PathVariable Long id, @RequestParam String role, HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute("userId");
        String currentUsername = (String) request.getAttribute("username");
        String ip = request.getRemoteAddr();

        String error = adminService.updateUserRole(id, role, currentUserId, currentUsername, ip);
        if (error != null) {
            return Result.error(error);
        }
        return Result.success("角色更新成功");
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/users/{id}")
    @RequireAdmin
    public Result<?> deleteUser(@PathVariable Long id, HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute("userId");
        String currentUsername = (String) request.getAttribute("username");
        String ip = request.getRemoteAddr();

        String error = adminService.deleteUser(id, currentUserId, currentUsername, ip);
        if (error != null) {
            return Result.error(error);
        }
        return Result.success("用户已删除");
    }

    /**
     * 获取操作日志
     */
    @GetMapping("/logs/operations")
    @RequireAdmin
    public Result<Map<String, Object>> getOperationLogs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) String username) {
        return Result.success(adminService.buildPageResult(logService.getOperationLogs(page, pageSize, operation, username), page, pageSize));
    }

    /**
     * 获取系统日志
     */
    @GetMapping("/logs/systems")
    @RequireAdmin
    public Result<Map<String, Object>> getSystemLogs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String logType) {
        return Result.success(adminService.buildPageResult(logService.getSystemLogs(page, pageSize, logType), page, pageSize));
    }
}
