package com.fund.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_market_data")
public class MarketData implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String marketType;
    
    private String marketCode;
    
    private BigDecimal currentPoint;
    
    private BigDecimal changePoint;
    
    private BigDecimal changeRatio;
    
    private BigDecimal volume;
    
    private BigDecimal amount;
    
    private BigDecimal highPoint;
    
    private BigDecimal lowPoint;
    
    private BigDecimal openPoint;
    
    private BigDecimal prevClose;
    
    private LocalDate tradeDate;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
