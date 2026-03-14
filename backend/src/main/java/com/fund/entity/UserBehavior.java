package com.fund.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户行为记录实体
 */
@Data
@TableName("t_user_behavior")
public class UserBehavior implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID (游客为null)
     */
    private Long userId;

    /**
     * 会话ID (用于识别游客)
     */
    private String sessionId;

    /**
     * 基金代码
     */
    private String fundCode;

    /**
     * 行为类型: view/favorite/compare/share/click
     */
    private String behaviorType;

    /**
     * 行为权重分
     */
    private BigDecimal behaviorScore;

    /**
     * 停留时间(秒)
     */
    private Integer dwellTime;

    /**
     * 额外数据(JSON)
     */
    private String extraData;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    // ========== 行为类型常量 ==========
    public static final String TYPE_VIEW = "view";
    public static final String TYPE_FAVORITE = "favorite";
    public static final String TYPE_COMPARE = "compare";
    public static final String TYPE_SHARE = "share";
    public static final String TYPE_CLICK = "click";

    // ========== 行为权重 ==========
    public static final BigDecimal SCORE_VIEW = new BigDecimal("1.00");
    public static final BigDecimal SCORE_FAVORITE = new BigDecimal("3.00");
    public static final BigDecimal SCORE_COMPARE = new BigDecimal("2.00");
    public static final BigDecimal SCORE_SHARE = new BigDecimal("4.00");
    public static final BigDecimal SCORE_CLICK = new BigDecimal("0.50");
}
