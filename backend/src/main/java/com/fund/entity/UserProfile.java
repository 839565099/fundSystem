package com.fund.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户画像实体
 */
@Data
@TableName("t_user_profile")
public class UserProfile implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 风险偏好: 1-保守 2-稳健 3-激进
     */
    private Integer riskPreference;

    /**
     * 投资经验: 1-新手 2-一般 3-丰富
     */
    private Integer investExperience;

    /**
     * 投资周期: 1-短期 2-中期 3-长期
     */
    private Integer investPeriod;

    /**
     * 偏好基金类型(JSON数组)
     */
    private String preferredTypes;

    /**
     * 偏好板块(JSON数组)
     */
    private String preferredSectors;

    /**
     * 平均投资金额
     */
    private BigDecimal avgInvestAmount;

    /**
     * 总行为分数
     */
    private BigDecimal totalBehaviorScore;

    /**
     * 最后活跃时间
     */
    private LocalDateTime lastActiveTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ========== 风险偏好常量 ==========
    public static final int RISK_CONSERVATIVE = 1;
    public static final int RISK_BALANCED = 2;
    public static final int RISK_AGGRESSIVE = 3;

    // ========== 投资经验常量 ==========
    public static final int EXP_BEGINNER = 1;
    public static final int EXP_NORMAL = 2;
    public static final int EXP_EXPERT = 3;
}
