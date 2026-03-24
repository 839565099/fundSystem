package com.fund.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fund.common.Result;
import com.fund.entity.Notification;
import com.fund.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知控制器
 */
@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 获取通知列表
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> getNotifications(
            @RequestAttribute Long userId,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {

        Page<Notification> page = notificationService.getUserNotifications(userId, type, pageNum, pageSize);

        Map<String, Object> result = new HashMap<>();
        result.put("records", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pages", page.getPages());
        result.put("current", page.getCurrent());

        return Result.success(result);
    }

    /**
     * 获取未读通知数量
     */
    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount(@RequestAttribute Long userId) {
        Long count = notificationService.getUnreadCount(userId);
        return Result.success(count);
    }

    /**
     * 标记通知为已读
     */
    @PostMapping("/read/{id}")
    public Result<Boolean> markAsRead(@RequestAttribute Long userId, @PathVariable Long id) {
        boolean success = notificationService.markAsRead(userId, id);
        return Result.success(success);
    }

    /**
     * 批量标记通知为已读
     */
    @PostMapping("/read")
    public Result<Boolean> batchMarkAsRead(@RequestAttribute Long userId, @RequestBody List<Long> ids) {
        for (Long id : ids) {
            notificationService.markAsRead(userId, id);
        }
        return Result.success(true);
    }

    /**
     * 标记所有通知为已读
     */
    @PostMapping("/read-all")
    public Result<Integer> markAllAsRead(@RequestAttribute Long userId) {
        int count = notificationService.markAllAsRead(userId);
        return Result.success(count);
    }

    /**
     * 删除通知
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@RequestAttribute Long userId, @PathVariable Long id) {
        boolean success = notificationService.delete(userId, id);
        return Result.success(success);
    }
}
