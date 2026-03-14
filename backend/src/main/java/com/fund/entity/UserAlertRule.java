package com.fund.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户预警规则实体
 */
@Data
@TableName("t_user_alert_rule")
public class UserAlertRule implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 预警类型: GROWTH/NAV_HIGH/NAV_LOW/RANKING/NEWS/MARKET
     */
    private String alertType;

    /**
     * 基金代码 (大盘预警为null)
     */
    private String fundCode;

    /**
     * 规则名称
     */
    private String alertName;

    /**
     * 条件: GT(大于)/LT(小于)/GE(>=)/LE(<=)/EQ(等于)
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

    /**
     * 状态: 0-禁用 1-启用
     */
    private Integer status;

    /**
     * 最后触发时间
     */
    private LocalDateTime lastTriggeredTime;

    /**
     * 触发次数
     */
    private Integer triggerCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ========== 预警类型常量 ==========
    public static final String TYPE_GROWTH = "GROWTH";           // 涨跌幅预警
    public static final String TYPE_NAV_HIGH = "NAV_HIGH";       // 净值新高
    public static final String TYPE_NAV_LOW = "NAV_LOW";         // 净值新低
    public static final String TYPE_RANKING = "RANKING";         // 排名变化
    public static final String TYPE_NEWS = "NEWS";               // 资讯预警
    public static final String TYPE_MARKET = "MARKET";           // 大盘预警

    // ========== 条件常量 ==========
    public static final String COND_GT = "GT";  // 大于
    public static final String COND_LT = "LT";  // 小于
    public static final String COND_GE = "GE";  // 大于等于
    public static final String COND_LE = "LE";  // 小于等于
    public static final String COND_EQ = "EQ";  // 等于

    // ========== 单位常量 ==========
    public static final String UNIT_PERCENT = "PERCENT";
    public static final String UNIT_POINT = "POINT";
    public static final String UNIT_VALUE = "VALUE";

    // ========== 通知渠道常量 ==========
    public static final String CHANNEL_WEBSOCKET = "WEBSOCKET";
    public static final String CHANNEL_EMAIL = "EMAIL";
    public static final String CHANNEL_SMS = "SMS";

}
