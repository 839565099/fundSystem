package com.fund.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FundDetailVO {
    
    private Long id;
    private String fundCode;
    private String fundName;
    private String fundType;
    private String fundCompany;
    private LocalDate establishDate;
    private BigDecimal fundScale;
    private BigDecimal nav;
    private BigDecimal accNav;
    private LocalDate navDate;
    private BigDecimal dayGrowth;
    private BigDecimal weekGrowth;
    private BigDecimal monthGrowth;
    private BigDecimal threeMonthGrowth;
    private BigDecimal sixMonthGrowth;
    private BigDecimal yearGrowth;
    private BigDecimal totalGrowth;
    private Integer riskLevel;
    private String riskLevelName;
    private BigDecimal minPurchase;
    private BigDecimal purchaseRate;
    private BigDecimal redemptionRate;
    private BigDecimal managementRate;
    private BigDecimal custodyRate;
    private Integer status;
}
