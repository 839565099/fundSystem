package com.fund.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fund.annotation.RequireAdmin;
import com.fund.common.Result;
import com.fund.entity.OperationLog;
import com.fund.entity.SystemLog;
import com.fund.entity.User;
import com.fund.mapper.UserMapper;
import com.fund.service.LogService;
import com.fund.service.UserService;
import com.fund.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理员控制器
 * 所有接口需要管理员权限
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserMapper userMapper;
    private final UserService userService;
    private final LogService logService;

    /**
     * 获取系统统计数据
     */
    @GetMapping("/stats")
    @RequireAdmin
    public Result<Map<String, Object>> getSystemStats() {
        Map<String, Object> stats = new HashMap<>();

        // 用户统计
        Long totalUsers = userMapper.selectCount(null);
        Long activeUsers = userMapper.selectCount(
            new LambdaQueryWrapper<User>().eq(User::getStatus, 1)
        );
        Long adminCount = userMapper.selectCount(
            new LambdaQueryWrapper<User>().eq(User::getRole, "ADMIN")
        );

        // 今日新增用户
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        Long todayNewUsers = userMapper.selectCount(
            new LambdaQueryWrapper<User>().ge(User::getCreateTime, todayStart)
        );

        stats.put("totalUsers", totalUsers);
        stats.put("activeUsers", activeUsers);
        stats.put("adminCount", adminCount);
        stats.put("todayNewUsers", todayNewUsers);

        return Result.success(stats);
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

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        // 搜索条件
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w
                .like(User::getUsername, keyword)
                .or().like(User::getEmail, keyword)
                .or().like(User::getNickname, keyword)
            );
        }
        if (role != null && !role.isEmpty()) {
            wrapper.eq(User::getRole, role);
        }
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }

        wrapper.orderByDesc(User::getCreateTime);

        IPage<User> userPage = userMapper.selectPage(new Page<>(page, pageSize), wrapper);

        List<UserVO> userVOList = userPage.getRecords().stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("list", userVOList);
        result.put("total", userPage.getTotal());
        result.put("page", page);
        result.put("pageSize", pageSize);

        return Result.success(result);
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/users/{id}")
    @RequireAdmin
    public Result<UserVO> getUserDetail(@PathVariable Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        return Result.success(convertToVO(user));
    }

    /**
     * 更新用户状态（启用/禁用）
     */
    @PutMapping("/users/{id}/status")
    @RequireAdmin
    public Result<?> updateUserStatus(@PathVariable Long id, @RequestParam Integer status, HttpServletRequest request) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 获取当前登录用户
        Long currentUserId = (Long) request.getAttribute("userId");
        String currentUsername = (String) request.getAttribute("username");
        String ip = request.getRemoteAddr();

        // 不能禁用自己
        if (id.equals(currentUserId)) {
            return Result.error("不能禁用自己的账户");
        }

        user.setStatus(status);
        userMapper.updateById(user);

        // 记录操作日志
        String operation = status == 1 ? "ENABLE_USER" : "DISABLE_USER";
        String detail = String.format("{\"userId\":%d,\"username\":\"%s\",\"newStatus\":%d}", id, user.getUsername(), status);
        logService.logOperation(currentUserId, currentUsername, operation, "USER", id.toString(), detail, ip);

        return Result.success(status == 1 ? "用户已启用" : "用户已禁用");
    }

    /**
     * 更新用户角色
     */
    @PutMapping("/users/{id}/role")
    @RequireAdmin
    public Result<?> updateUserRole(@PathVariable Long id, @RequestParam String role, HttpServletRequest request) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }

        if (!role.equals("ADMIN") && !role.equals("USER")) {
            return Result.error("无效的角色");
        }

        String oldRole = user.getRole();

        // 获取当前登录用户
        Long currentUserId = (Long) request.getAttribute("userId");
        String currentUsername = (String) request.getAttribute("username");
        String ip = request.getRemoteAddr();

        user.setRole(role);
        userMapper.updateById(user);

        // 记录操作日志
        String detail = String.format("{\"userId\":%d,\"username\":\"%s\",\"oldRole\":\"%s\",\"newRole\":\"%s\"}",
                id, user.getUsername(), oldRole, role);
        logService.logOperation(currentUserId, currentUsername, "CHANGE_ROLE", "USER", id.toString(), detail, ip);

        return Result.success("角色更新成功");
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/users/{id}")
    @RequireAdmin
    public Result<?> deleteUser(@PathVariable Long id, HttpServletRequest request) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 不能删除管理员
        if ("ADMIN".equals(user.getRole())) {
            return Result.error("不能删除管理员账户");
        }

        // 获取当前登录用户
        Long currentUserId = (Long) request.getAttribute("userId");
        String currentUsername = (String) request.getAttribute("username");
        String ip = request.getRemoteAddr();

        // 记录操作日志
        String detail = String.format("{\"userId\":%d,\"username\":\"%s\"}", id, user.getUsername());
        logService.logOperation(currentUserId, currentUsername, "DELETE_USER", "USER", id.toString(), detail, ip);

        userMapper.deleteById(id);
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

        Page<OperationLog> logPage = logService.getOperationLogs(page, pageSize, operation, username);

        Map<String, Object> result = new HashMap<>();
        result.put("list", logPage.getRecords());
        result.put("total", logPage.getTotal());
        result.put("page", page);
        result.put("pageSize", pageSize);

        return Result.success(result);
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

        Page<SystemLog> logPage = logService.getSystemLogs(page, pageSize, logType);

        Map<String, Object> result = new HashMap<>();
        result.put("list", logPage.getRecords());
        result.put("total", logPage.getTotal());
        result.put("page", page);
        result.put("pageSize", pageSize);

        return Result.success(result);
    }

    private UserVO convertToVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        if (user.getRole() == null) {
            vo.setRole("USER");
        }
        return vo;
    }
}
