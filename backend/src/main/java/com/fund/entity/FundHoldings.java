package com.fund.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_fund_holdings")
public class FundHoldings implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String fundCode;
    
    private LocalDate reportDate;
    
    private String stockCode;
    
    private String stockName;
    
    private BigDecimal holdingRatio;
    
    private BigDecimal holdingShares;
    
    private BigDecimal holdingValue;
    
    private String holdingType;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
