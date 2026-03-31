package com.fund.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fund.entity.User;
import com.fund.mapper.UserMapper;
import com.fund.service.AdminService;
import com.fund.service.LogService;
import com.fund.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理员服务实现
 */
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserMapper userMapper;
    private final LogService logService;

    @Override
    public Map<String, Object> getSystemStats() {
        Map<String, Object> stats = new HashMap<>();

        Long totalUsers = userMapper.selectCount(null);
        Long activeUsers = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getStatus, 1)
        );
        Long adminCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getRole, "ADMIN")
        );

        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        Long todayNewUsers = userMapper.selectCount(
                new LambdaQueryWrapper<User>().ge(User::getCreateTime, todayStart)
        );

        stats.put("totalUsers", totalUsers);
        stats.put("activeUsers", activeUsers);
        stats.put("adminCount", adminCount);
        stats.put("todayNewUsers", todayNewUsers);

        return stats;
    }

    @Override
    public Map<String, Object> listUsers(Integer page, Integer pageSize, String keyword, String role, Integer status) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

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

        Page<User> userPage = userMapper.selectPage(new Page<>(page, pageSize), wrapper);

        List<UserVO> userVOList = userPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("list", userVOList);
        result.put("total", userPage.getTotal());
        result.put("page", page);
        result.put("pageSize", pageSize);

        return result;
    }

    @Override
    public UserVO getUserDetail(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return null;
        }
        return convertToVO(user);
    }

    @Override
    public String updateUserStatus(Long id, Integer status, Long currentUserId, String currentUsername, String ip) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return "用户不存在";
        }

        if (id.equals(currentUserId)) {
            return "不能禁用自己的账户";
        }

        user.setStatus(status);
        userMapper.updateById(user);

        String operation = status == 1 ? "ENABLE_USER" : "DISABLE_USER";
        String detail = String.format("{\"userId\":%d,\"username\":\"%s\",\"newStatus\":%d}", id, user.getUsername(), status);
        logService.logOperation(currentUserId, currentUsername, operation, "USER", id.toString(), detail, ip);

        return null;
    }

    @Override
    public String updateUserRole(Long id, String role, Long currentUserId, String currentUsername, String ip) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return "用户不存在";
        }

        if (!role.equals("ADMIN") && !role.equals("USER")) {
            return "无效的角色";
        }

        String oldRole = user.getRole();
        user.setRole(role);
        userMapper.updateById(user);

        String detail = String.format("{\"userId\":%d,\"username\":\"%s\",\"oldRole\":\"%s\",\"newRole\":\"%s\"}",
                id, user.getUsername(), oldRole, role);
        logService.logOperation(currentUserId, currentUsername, "CHANGE_ROLE", "USER", id.toString(), detail, ip);

        return null;
    }

    @Override
    public String deleteUser(Long id, Long currentUserId, String currentUsername, String ip) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return "用户不存在";
        }

        if ("ADMIN".equals(user.getRole())) {
            return "不能删除管理员账户";
        }

        String detail = String.format("{\"userId\":%d,\"username\":\"%s\"}", id, user.getUsername());
        logService.logOperation(currentUserId, currentUsername, "DELETE_USER", "USER", id.toString(), detail, ip);

        userMapper.deleteById(id);
        return null;
    }

    private UserVO convertToVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        if (user.getRole() == null) {
            vo.setRole("USER");
        }
        return vo;
    }

    @Override
    public <T> Map<String, Object> buildPageResult(IPage<T> page, Integer pageNum, Integer pageSize) {
        Map<String, Object> result = new HashMap<>();
        result.put("list", page.getRecords());
        result.put("total", page.getTotal());
        result.put("page", pageNum);
        result.put("pageSize", pageSize);
        return result;
    }
}
