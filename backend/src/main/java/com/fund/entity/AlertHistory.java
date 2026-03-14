package com.fund.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预警历史记录实体
 */
@Data
@TableName("t_alert_history")
public class AlertHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 规则ID
     */
    private Long ruleId;

    /**
     * 预警类型
     */
    private String alertType;

    /**
     * 基金代码
     */
    private String fundCode;

    /**
     * 基金名称
     */
    private String fundName;

    /**
     * 预警标题
     */
    private String alertTitle;

    /**
     * 预警消息详情
     */
    private String alertMessage;

    /**
     * 触发时的实际值
     */
    private BigDecimal alertValue;

    /**
     * 触发时间
     */
    private LocalDateTime triggeredTime;

    /**
     * 是否已读
     */
    private Integer isRead;

    /**
     * 阅读时间
     */
    private LocalDateTime readTime;
}
