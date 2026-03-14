package com.fund.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 预警规则创建/更新请求
 */
@Data
public class AlertRuleDTO {

    /**
     * 预警类型: GROWTH/NAV_HIGH/NAV_LOW/RANKING/NEWS/MARKET
     */
    private String alertType;

    /**
     * 基金代码
     */
    private String fundCode;

    /**
     * 规则名称
     */
    private String alertName;

    /**
     * 条件: GT/LT/GE/LE/EQ
     */
    private String alertCondition;

    /**
     * 阈值
     */
    private BigDecimal threshold;

    /**
     * 单位: PERCENT/POINT/VALUE
     */
    private String unit;

    /**
     * 通知渠道: WEBSOCKET/EMAIL/SMS
     */
    private String notifyChannel;

    /**
     * 冷却时间(分钟)
     */
    private Integer cooldownMinutes;
}
