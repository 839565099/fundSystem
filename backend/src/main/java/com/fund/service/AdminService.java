package com.fund.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fund.dto.UpdateProfileDTO;
import com.fund.vo.UserVO;

import java.util.Map;

/**
 * 管理员服务接口
 */
public interface AdminService {

    /**
     * 获取系统统计数据
     */
    Map<String, Object> getSystemStats();

    /**
     * 获取用户列表
     */
    Map<String, Object> listUsers(Integer page, Integer pageSize, String keyword, String role, Integer status);

    /**
     * 获取用户详情
     */
    UserVO getUserDetail(Long id);

    /**
     * 更新用户状态
     */
    String updateUserStatus(Long id, Integer status, Long currentUserId, String currentUsername, String ip);

    /**
     * 更新用户角色
     */
    String updateUserRole(Long id, String role, Long currentUserId, String currentUsername, String ip);

    /**
     * 删除用户
     */
    String deleteUser(Long id, Long currentUserId, String currentUsername, String ip);

    /**
     * 更新用户基本信息
     */
    String updateUserInfo(Long id, UpdateProfileDTO dto, Long currentUserId, String currentUsername, String ip);

    /**
     * 构建分页结果
     */
    <T> Map<String, Object> buildPageResult(IPage<T> page, Integer pageNum, Integer pageSize);
}
