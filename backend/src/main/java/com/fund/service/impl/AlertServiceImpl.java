package com.fund.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fund.dto.AlertRuleDTO;
import com.fund.entity.AlertHistory;
import com.fund.entity.Fund;
import com.fund.entity.UserAlertRule;
import com.fund.mapper.AlertHistoryMapper;
import com.fund.mapper.UserAlertRuleMapper;
import com.fund.service.AlertService;
import com.fund.service.FundService;
import com.fund.service.RealtimeDataService;
import com.fund.vo.AlertHistoryVO;
import com.fund.vo.AlertRuleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 预警服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertServiceImpl implements AlertService {

    private final UserAlertRuleMapper alertRuleMapper;
    private final AlertHistoryMapper alertHistoryMapper;
    private final FundService fundService;
    private final RealtimeDataService realtimeDataService;

    @Override
    @Transactional
    public AlertRuleVO createRule(Long userId, AlertRuleDTO dto) {
        UserAlertRule rule = new UserAlertRule();
        rule.setUserId(userId);
        rule.setAlertType(dto.getAlertType());
        rule.setFundCode(dto.getFundCode());
        rule.setAlertName(dto.getAlertName());
        rule.setAlertCondition(dto.getAlertCondition());
        rule.setThreshold(dto.getThreshold());
        rule.setUnit(dto.getUnit() != null ? dto.getUnit() : "PERCENT");
        rule.setNotifyChannel(dto.getNotifyChannel() != null ? dto.getNotifyChannel() : "WEBSOCKET");
        rule.setCooldownMinutes(dto.getCooldownMinutes() != null ? dto.getCooldownMinutes() : 60);
        rule.setStatus(1);
        rule.setTriggerCount(0);

        alertRuleMapper.insert(rule);
        return convertToVO(rule);
    }

    @Override
    @Transactional
    public AlertRuleVO updateRule(Long userId, Long id, AlertRuleDTO dto) {
        UserAlertRule rule = alertRuleMapper.selectById(id);
        if (rule == null || !rule.getUserId().equals(userId)) {
            throw new RuntimeException("预警规则不存在或无权操作");
        }

        if (dto.getAlertName() != null) {
            rule.setAlertName(dto.getAlertName());
        }
        if (dto.getAlertCondition() != null) {
            rule.setAlertCondition(dto.getAlertCondition());
        }
        if (dto.getThreshold() != null) {
            rule.setThreshold(dto.getThreshold());
        }
        if (dto.getNotifyChannel() != null) {
            rule.setNotifyChannel(dto.getNotifyChannel());
        }
        if (dto.getCooldownMinutes() != null) {
            rule.setCooldownMinutes(dto.getCooldownMinutes());
        }

        alertRuleMapper.updateById(rule);
        return convertToVO(rule);
    }

    @Override
    @Transactional
    public void toggleRule(Long userId, Long id, Integer status) {
        UserAlertRule rule = alertRuleMapper.selectById(id);
        if (rule == null || !rule.getUserId().equals(userId)) {
            throw new RuntimeException("预警规则不存在或无权操作");
        }
        rule.setStatus(status);
        alertRuleMapper.updateById(rule);
    }

    @Override
    @Transactional
    public void deleteRule(Long userId, Long ruleId) {
        UserAlertRule rule = alertRuleMapper.selectById(ruleId);
        if (rule == null || !rule.getUserId().equals(userId)) {
            throw new RuntimeException("预警规则不存在或无权操作");
        }
        alertRuleMapper.deleteById(ruleId);
    }

    @Override
    public List<AlertRuleVO> getUserRules(Long userId) {
        LambdaQueryWrapper<UserAlertRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAlertRule::getUserId, userId)
                .orderByDesc(UserAlertRule::getCreateTime);
        List<UserAlertRule> rules = alertRuleMapper.selectList(wrapper);
        return rules.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public AlertRuleVO getRuleDetail(Long userId, Long ruleId) {
        UserAlertRule rule = alertRuleMapper.selectById(ruleId);
        if (rule == null || !rule.getUserId().equals(userId)) {
            throw new RuntimeException("预警规则不存在或无权操作");
        }
        return convertToVO(rule);
    }

    @Override
    public List<AlertHistoryVO> getAlertHistory(Long userId, Integer isRead, Integer limit) {
        LambdaQueryWrapper<AlertHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlertHistory::getUserId, userId);
        if (isRead != null) {
            wrapper.eq(AlertHistory::getIsRead, isRead);
        }
        wrapper.orderByDesc(AlertHistory::getTriggeredTime);
        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + limit);
        }
        List<AlertHistory> histories = alertHistoryMapper.selectList(wrapper);
        return histories.stream().map(this::convertHistoryToVO).collect(Collectors.toList());
    }

    @Override
    public Integer getUnreadCount(Long userId) {
        LambdaQueryWrapper<AlertHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlertHistory::getUserId, userId)
                .eq(AlertHistory::getIsRead, 0);
        return Math.toIntExact(alertHistoryMapper.selectCount(wrapper));
    }

    @Override
    @Transactional
    public void markAsRead(Long userId, List<Long> alertIds) {
        if (alertIds == null || alertIds.isEmpty()) {
            return;
        }
        LambdaUpdateWrapper<AlertHistory> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(AlertHistory::getId, alertIds)
                .eq(AlertHistory::getUserId, userId)
                .set(AlertHistory::getIsRead, 1)
                .set(AlertHistory::getReadTime, LocalDateTime.now());
        alertHistoryMapper.update(null, wrapper);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        LambdaUpdateWrapper<AlertHistory> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AlertHistory::getUserId, userId)
                .eq(AlertHistory::getIsRead, 0)
                .set(AlertHistory::getIsRead, 1)
                .set(AlertHistory::getReadTime, LocalDateTime.now());
        alertHistoryMapper.update(null, wrapper);
    }

    @Override
    @Scheduled(fixedRate = 60000) // 每分钟检查一次
    public void checkAlerts() {
        log.debug("开始检查预警规则...");

        // 获取所有启用的预警规则
        LambdaQueryWrapper<UserAlertRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAlertRule::getStatus, 1);
        List<UserAlertRule> rules = alertRuleMapper.selectList(wrapper);

        for (UserAlertRule rule : rules) {
            try {
                checkRule(rule);
            } catch (Exception e) {
                log.error("检查预警规则失败: ruleId={}", rule.getId(), e);
            }
        }
    }

    @Override
    public void checkFundAlerts(String fundCode) {
        LambdaQueryWrapper<UserAlertRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAlertRule::getStatus, 1)
                .eq(UserAlertRule::getFundCode, fundCode);
        List<UserAlertRule> rules = alertRuleMapper.selectList(wrapper);

        for (UserAlertRule rule : rules) {
            try {
                checkRule(rule);
            } catch (Exception e) {
                log.error("检查基金预警失败: fundCode={}, ruleId={}", fundCode, rule.getId(), e);
            }
        }
    }

    /**
     * 检查单条规则
     */
    private void checkRule(UserAlertRule rule) {
        // 检查冷却时间
        if (rule.getLastTriggeredTime() != null && rule.getCooldownMinutes() != null) {
            LocalDateTime cooldownEndTime = rule.getLastTriggeredTime().plusMinutes(rule.getCooldownMinutes());
            if (LocalDateTime.now().isBefore(cooldownEndTime)) {
                return; // 还在冷却期
            }
        }

        Fund fund = null;
        BigDecimal currentValue = null;

        // 获取当前值
        if (rule.getFundCode() != null) {
            fund = fundService.getByFundCode(rule.getFundCode());
            if (fund == null) {
                return;
            }

            // 根据预警类型获取对应的值
            currentValue = getAlertValue(rule.getAlertType(), fund);
        } else {
            // 大盘预警
            // TODO: 实现大盘预警逻辑
            return;
        }

        if (currentValue == null) {
            return;
        }

        // 检查条件
        boolean shouldTrigger = checkCondition(rule.getAlertCondition(), currentValue, rule.getThreshold());

        if (shouldTrigger) {
            triggerAlert(rule, fund, currentValue);
        }
    }

    /**
     * 根据预警类型获取对应的值
     */
    private BigDecimal getAlertValue(String alertType, Fund fund) {
        switch (alertType) {
            case UserAlertRule.TYPE_GROWTH:
                return fund.getDayGrowth();
            case UserAlertRule.TYPE_NAV_HIGH:
            case UserAlertRule.TYPE_NAV_LOW:
                return fund.getNav();
            default:
                return null;
        }
    }

    /**
     * 检查条件
     */
    private boolean checkCondition(String condition, BigDecimal value, BigDecimal threshold) {
        int cmp = value.compareTo(threshold);
        switch (condition) {
            case UserAlertRule.COND_GT:
                return cmp > 0;
            case UserAlertRule.COND_LT:
                return cmp < 0;
            case UserAlertRule.COND_GE:
                return cmp >= 0;
            case UserAlertRule.COND_LE:
                return cmp <= 0;
            case UserAlertRule.COND_EQ:
                return cmp == 0;
            default:
                return false;
        }
    }

    /**
     * 触发预警
     */
    @Transactional
    private void triggerAlert(UserAlertRule rule, Fund fund, BigDecimal currentValue) {
        // 生成预警标题和消息
        String title = generateAlertTitle(rule, fund);
        String message = generateAlertMessage(rule, fund, currentValue);

        // 保存预警历史
        AlertHistory history = new AlertHistory();
        history.setUserId(rule.getUserId());
        history.setRuleId(rule.getId());
        history.setAlertType(rule.getAlertType());
        history.setFundCode(rule.getFundCode());
        history.setFundName(fund != null ? fund.getFundName() : null);
        history.setAlertTitle(title);
        history.setAlertMessage(message);
        history.setAlertValue(currentValue);
        history.setTriggeredTime(LocalDateTime.now());
        history.setIsRead(0);
        alertHistoryMapper.insert(history);

        // 更新规则触发信息
        rule.setLastTriggeredTime(LocalDateTime.now());
        rule.setTriggerCount(rule.getTriggerCount() + 1);
        alertRuleMapper.updateById(rule);

        // 推送通知
        String notifyChannel = rule.getNotifyChannel();
        if (UserAlertRule.CHANNEL_WEBSOCKET.equals(notifyChannel)) {
            realtimeDataService.pushNotification(
                    rule.getUserId().toString(),
                    title,
                    message,
                    "ALERT"
            );
        }

        log.info("预警触发: userId={}, ruleId={}, fundCode={}, value={}",
                rule.getUserId(), rule.getId(), rule.getFundCode(), currentValue);
    }

    /**
     * 生成预警标题
     */
    private String generateAlertTitle(UserAlertRule rule, Fund fund) {
        String fundName = fund != null ? fund.getFundName() : "大盘";
        String typeDesc = "";
        switch (rule.getAlertType()) {
            case UserAlertRule.TYPE_GROWTH:
                typeDesc = "涨跌幅";
                break;
            case UserAlertRule.TYPE_NAV_HIGH:
                typeDesc = "净值新高";
                break;
            case UserAlertRule.TYPE_NAV_LOW:
                typeDesc = "净值新低";
                break;
            case UserAlertRule.TYPE_MARKET:
                typeDesc = "大盘";
                break;
        }
        return String.format("【%s预警】%s", typeDesc, fundName);
    }

    /**
     * 生成预警消息
     */
    private String generateAlertMessage(UserAlertRule rule, Fund fund, BigDecimal currentValue) {
        String fundName = fund != null ? fund.getFundName() : "大盘";
        String conditionDesc = "";
        switch (rule.getAlertCondition()) {
            case UserAlertRule.COND_GT:
                conditionDesc = "高于";
                break;
            case UserAlertRule.COND_LT:
                conditionDesc = "低于";
                break;
            case UserAlertRule.COND_GE:
                conditionDesc = "不低于";
                break;
            case UserAlertRule.COND_LE:
                conditionDesc = "不高于";
                break;
        }

        String unitDesc = "%".equals(rule.getUnit()) || "PERCENT".equals(rule.getUnit()) ? "%" : "";
        return String.format("%s 当前值 %.2f%s，已%s设定的 %.2f%s 阈值",
                fundName,
                currentValue,
                unitDesc,
                conditionDesc,
                rule.getThreshold(),
                unitDesc);
    }

    /**
     * 转换为VO
     */
    private AlertRuleVO convertToVO(UserAlertRule rule) {
        AlertRuleVO vo = new AlertRuleVO();
        BeanUtils.copyProperties(rule, vo);

        // 获取基金名称
        if (rule.getFundCode() != null) {
            Fund fund = fundService.getByFundCode(rule.getFundCode());
            if (fund != null) {
                vo.setFundName(fund.getFundName());
            }
        }

        return vo;
    }

    /**
     * 转换历史为VO
     */
    private AlertHistoryVO convertHistoryToVO(AlertHistory history) {
        AlertHistoryVO vo = new AlertHistoryVO();
        BeanUtils.copyProperties(history, vo);
        return vo;
    }
}
