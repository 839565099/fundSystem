package com.fund.controller;

import com.fund.common.PageResult;
import com.fund.common.Result;
import com.fund.entity.Notification;
import com.fund.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通知控制器
 */
@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/list")
    public Result<PageResult<Notification>> getNotifications(
            @RequestAttribute Long userId,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(PageResult.from(notificationService.getUserNotifications(userId, type, pageNum, pageSize)));
    }

    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount(@RequestAttribute Long userId) {
        return Result.success(notificationService.getUnreadCount(userId));
    }

    @PostMapping("/read/{id}")
    public Result<Boolean> markAsRead(@RequestAttribute Long userId, @PathVariable Long id) {
        return Result.success(notificationService.markAsRead(userId, id));
    }

    @PostMapping("/read")
    public Result<Void> batchMarkAsRead(@RequestAttribute Long userId, @RequestBody List<Long> ids) {
        notificationService.batchMarkAsRead(userId, ids);
        return Result.success();
    }

    @PostMapping("/read-all")
    public Result<Integer> markAllAsRead(@RequestAttribute Long userId) {
        return Result.success(notificationService.markAllAsRead(userId));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@RequestAttribute Long userId, @PathVariable Long id) {
        return Result.success(notificationService.delete(userId, id));
    }
}
