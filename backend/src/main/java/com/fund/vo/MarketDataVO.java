package com.fund.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class MarketDataVO {
    
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
}
