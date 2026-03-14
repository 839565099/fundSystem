package com.fund.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class FundCompareVO {

    private String fundCode;
    private String fundName;
    private String fundType;
    private String fundCompany;
    private BigDecimal fundScale;
    private BigDecimal nav;
    private BigDecimal dayGrowth;
    private BigDecimal weekGrowth;
    private BigDecimal monthGrowth;
    private BigDecimal threeMonthGrowth;
    private BigDecimal sixMonthGrowth;
    private BigDecimal yearGrowth;
    private BigDecimal totalGrowth;
    private BigDecimal maxDrawdown;
    private BigDecimal sharpeRatio;
    private List<FundNavHistoryVO> navHistory;
}
