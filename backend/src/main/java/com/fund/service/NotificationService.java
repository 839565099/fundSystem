package com.fund.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fund.entity.Notification;
import java.util.List;

/**
 * 通知服务接口
 */
public interface NotificationService {

    /**
     * 创建通知
     */
    Notification create(Notification notification);

    /**
     * 批量创建通知
     */
    void batchCreate(List<Notification> notifications);

    /**
     * 获取用户通知分页
     */
    Page<Notification> getUserNotifications(Long userId, String type, Integer pageNum, Integer pageSize);

    /**
     * 获取未读通知数量
     */
    Long getUnreadCount(Long userId);

    /**
     * 标记单条通知为已读
     */
    boolean markAsRead(Long userId, Long notificationId);

    /**
     * 标记所有通知为已读
     */
    int markAllAsRead(Long userId);

    /**
     * 删除通知
     */
    boolean delete(Long userId, Long notificationId);

    /**
     * 发送预警通知
     */
    void sendAlertNotification(Long userId, String title, String content, String link);
}
