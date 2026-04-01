package com.fund.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fund.entity.Notification;
import com.fund.mapper.NotificationMapper;
import com.fund.service.NotificationService;
import com.fund.service.RealtimeDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final RealtimeDataService realtimeDataService;

    @Override
    public Notification create(Notification notification) {
        notification.setIsRead(0);
        notificationMapper.insert(notification);
        pushNotification(notification);
        return notification;
    }

    @Override
    public void batchCreate(List<Notification> notifications) {
        if (notifications == null || notifications.isEmpty()) {
            return;
        }
        // 先完成所有插入
        for (Notification notification : notifications) {
            notification.setIsRead(0);
            notificationMapper.insert(notification);
        }
        // 再批量推送
        for (Notification notification : notifications) {
            pushNotification(notification);
        }
    }

    @Override
    public Page<Notification> getUserNotifications(Long userId, String type, Integer pageNum, Integer pageSize) {
        Page<Notification> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId);
        if (StringUtils.hasText(type)) {
            wrapper.eq(Notification::getType, type);
        }
        wrapper.orderByDesc(Notification::getCreateTime);
        return notificationMapper.selectPage(page, wrapper);
    }

    @Override
    public Long getUnreadCount(Long userId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0);
        return notificationMapper.selectCount(wrapper);
    }

    @Override
    public boolean markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification != null && notification.getUserId().equals(userId)) {
            notification.setIsRead(1);
            notification.setReadTime(LocalDateTime.now());
            return notificationMapper.updateById(notification) > 0;
        }
        return false;
    }

    @Override
    public void batchMarkAsRead(Long userId, List<Long> notificationIds) {
        LocalDateTime now = LocalDateTime.now();
        for (Long id : notificationIds) {
            Notification notification = notificationMapper.selectById(id);
            if (notification != null && notification.getUserId().equals(userId)) {
                notification.setIsRead(1);
                notification.setReadTime(now);
                notificationMapper.updateById(notification);
            }
        }
    }

    @Override
    public int markAllAsRead(Long userId) {
        return notificationMapper.markAllAsRead(userId);
    }

    @Override
    public boolean delete(Long userId, Long notificationId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification != null && notification.getUserId().equals(userId)) {
            return notificationMapper.deleteById(notificationId) > 0;
        }
        return false;
    }

    @Override
    @Async
    public void sendAlertNotification(Long userId, String title, String content, String link) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType("ALERT");
        notification.setTitle(title);
        notification.setContent(content);
        notification.setLink(link);
        create(notification);
    }

    /**
     * 实时推送通知到前端
     */
    private void pushNotification(Notification notification) {
        try {
            realtimeDataService.pushNotification(
                    String.valueOf(notification.getUserId()),
                    notification.getTitle(),
                    notification.getContent(),
                    notification.getType()
            );
        } catch (Exception e) {
            log.error("推送通知失败: {}", e.getMessage());
        }
    }
}
