package com.fund.service;

import com.fund.dto.AlertRuleDTO;
import com.fund.entity.UserAlertRule;
import com.fund.vo.AlertHistoryVO;
import com.fund.vo.AlertRuleVO;

import java.util.List;

/**
 * 预警服务接口
 */
public interface AlertService {

    /**
     * 创建预警规则
     */
    AlertRuleVO createRule(Long userId, AlertRuleDTO dto);

    /**
     * 更新预警规则
     */
    AlertRuleVO updateRule(Long userId, Long id, AlertRuleDTO dto);

    /**
     * 删除预警规则
     */
    void deleteRule(Long userId, Long ruleId);

    /**
     * 获取用户的预警规则列表
     */
    List<AlertRuleVO> getUserRules(Long userId);

    /**
     * 获取预警规则详情
     */
    AlertRuleVO getRuleDetail(Long userId, Long ruleId);

    /**
     * 启用/禁用预警规则
     */
    void toggleRule(Long userId, Long ruleId, Integer status);

    /**
     * 获取预警历史
     */
    List<AlertHistoryVO> getAlertHistory(Long userId, Integer isRead, Integer limit);

    /**
     * 获取未读预警数量
     */
    Integer getUnreadCount(Long userId);

    /**
     * 标记预警为已读
     */
    void markAsRead(Long userId, List<Long> alertIds);

    /**
     * 标记所有预警为已读
     */
    void markAllAsRead(Long userId);

    /**
     * 检查预警（定时任务调用）
     */
    void checkAlerts();

    /**
     * 检查单个基金的预警
     */
    void checkFundAlerts(String fundCode);
}
