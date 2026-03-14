package com.fund.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预警规则返回
 */
@Data
public class AlertRuleVO {

    private Long id;

    private String alertType;

    private String fundCode;

    private String fundName;

    private String alertName;

    private String alertCondition;

    private BigDecimal threshold;

    private String unit;

    private String notifyChannel;

    private Integer cooldownMinutes;

    private Integer status;

    private LocalDateTime lastTriggeredTime;

    private Integer triggerCount;

    private LocalDateTime createTime;
}
