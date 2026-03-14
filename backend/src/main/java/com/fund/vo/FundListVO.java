package com.fund.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FundListVO {
    
    private String fundCode;
    private String fundName;
    private String fundType;
    private String sector;
    private BigDecimal nav;
    private BigDecimal accNav;
    private LocalDate navDate;
    private BigDecimal dayGrowth;
    private BigDecimal weekGrowth;
    private BigDecimal monthGrowth;
    private BigDecimal yearGrowth;
    private BigDecimal fundScale;
    private Integer riskLevel;
}
