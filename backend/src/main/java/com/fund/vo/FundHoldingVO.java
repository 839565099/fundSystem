package com.fund.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FundHoldingVO {
    
    private Long id;
    
    private String fundCode;
    
    private LocalDate reportDate;
    
    private String stockCode;
    
    private String stockName;
    
    private BigDecimal holdingRatio;
    
    private BigDecimal holdingShares;
    
    private BigDecimal holdingValue;
    
    private String holdingType;
    
    private BigDecimal dayGrowth;
    
    private BigDecimal currentPrice;
}
