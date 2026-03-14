package com.fund.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_fund_news")
public class FundNews implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String summary;

    private String content;

    private String coverImage;

    private String originalUrl;

    private String source;
    
    private String author;
    
    private String newsType;
    
    private String fundCode;

    /**
     * 情感标签：BULLISH / BEARISH / NEUTRAL
     */
    private String sentiment;

    /**
     * 情感分数：-1 ~ 1
     */
    private BigDecimal sentimentScore;

    /**
     * 置信度：0 ~ 1
     */
    private BigDecimal sentimentConfidence;

    /**
     * 影响等级：1 ~ 5
     */
    private Integer impactLevel;
    
    private LocalDateTime publishTime;
    
    private Integer viewCount;
    
    private Integer status;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
