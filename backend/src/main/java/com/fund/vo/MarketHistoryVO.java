package com.fund.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class MarketHistoryVO {
    
    private String marketCode;
    private String marketType;
    private LocalDate tradeDate;
    private BigDecimal openPoint;
    private BigDecimal closePoint;
    private BigDecimal highPoint;
    private BigDecimal lowPoint;
    private BigDecimal volume;
    private BigDecimal amount;
    private BigDecimal changeRatio;
}
