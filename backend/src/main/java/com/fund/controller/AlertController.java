package com.fund.controller;

import com.fund.common.Result;
import com.fund.dto.AlertRuleDTO;
import com.fund.service.AlertService;
import com.fund.vo.AlertHistoryVO;
import com.fund.vo.AlertRuleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 预警管理控制器
 */
@RestController
@RequestMapping("/alert")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @PostMapping("/rule")
    public Result<AlertRuleVO> createRule(@RequestAttribute Long userId, @RequestBody AlertRuleDTO dto) {
        AlertRuleVO vo = alertService.createRule(userId, dto);
        return Result.success(vo);
    }

    @PutMapping("/rule/{id}")
    public Result<AlertRuleVO> updateRule(@RequestAttribute Long userId,
                                          @PathVariable Long id,
                                          @RequestBody AlertRuleDTO dto) {
        AlertRuleVO vo = alertService.updateRule(userId, id, dto);
        return Result.success(vo);
    }

    @DeleteMapping("/rule/{id}")
    public Result<Void> deleteRule(@RequestAttribute Long userId, @PathVariable Long id) {
        alertService.deleteRule(userId, id);
        return Result.success();
    }

    @GetMapping("/rules")
    public Result<List<AlertRuleVO>> getUserRules(@RequestAttribute Long userId) {
        List<AlertRuleVO> rules = alertService.getUserRules(userId);
        return Result.success(rules);
    }

    @GetMapping("/rule/{id}")
    public Result<AlertRuleVO> getRuleDetail(@RequestAttribute Long userId, @PathVariable Long id) {
        AlertRuleVO vo = alertService.getRuleDetail(userId, id);
        return Result.success(vo);
    }

    @PutMapping("/rule/{id}/toggle")
    public Result<Void> toggleRule(@RequestAttribute Long userId,
                                   @PathVariable Long id,
                                   @RequestParam Integer status) {
        alertService.toggleRule(userId, id, status);
        return Result.success();
    }

    @GetMapping("/history")
    public Result<List<AlertHistoryVO>> getAlertHistory(
            @RequestAttribute Long userId,
            @RequestParam(required = false) Integer isRead,
            @RequestParam(defaultValue = "50") Integer limit) {
        List<AlertHistoryVO> history = alertService.getAlertHistory(userId, isRead, limit);
        return Result.success(history);
    }

    @GetMapping("/unread-count")
    public Result<Integer> getUnreadCount(@RequestAttribute Long userId) {
        Integer count = alertService.getUnreadCount(userId);
        return Result.success(count);
    }

    @PostMapping("/mark-read")
    public Result<Void> markAsRead(@RequestAttribute Long userId, @RequestBody List<Long> alertIds) {
        alertService.markAsRead(userId, alertIds);
        return Result.success();
    }

    @PostMapping("/mark-all-read")
    public Result<Void> markAllAsRead(@RequestAttribute Long userId) {
        alertService.markAllAsRead(userId);
        return Result.success();
    }
}
