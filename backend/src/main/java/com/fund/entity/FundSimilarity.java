package com.fund.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 基金相似度实体
 */
@Data
@TableName("t_fund_similarity")
public class FundSimilarity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 基金代码A
     */
    private String fundCodeA;

    /**
     * 基金代码B
     */
    private String fundCodeB;

    /**
     * 相似度分数 0-1
     */
    private BigDecimal similarityScore;

    /**
     * 相似度类型
     */
    private String similarityType;

    /**
     * 计算时间
     */
    private LocalDateTime calcTime;
}
