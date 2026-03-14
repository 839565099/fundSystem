package com.fund.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FundSearchDTO {
    
    private String keyword;
    
    private String fundType;
    
    private Integer riskLevel;
    
    private BigDecimal minScale;
    
    private BigDecimal maxScale;
    
    private String sortBy;
    
    private String sortOrder;
    
    private Integer pageNum = 1;
    
    private Integer pageSize = 10;

    private BigDecimal minDayGrowth;

    private BigDecimal maxDayGrowth;

    private BigDecimal minWeekGrowth;

    private BigDecimal maxWeekGrowth;

    private BigDecimal minMonthGrowth;

    private BigDecimal maxMonthGrowth;

    private BigDecimal minYearGrowth;

    private BigDecimal maxYearGrowth;

    private BigDecimal minTotalGrowth;

    private BigDecimal maxTotalGrowth;

    private Integer minEstablishYears;

    private Integer maxEstablishYears;

    private String fundCompany;

    private LocalDate establishDateStart;

    private LocalDate establishDateEnd;
}
