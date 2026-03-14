package com.fund.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FundNavHistoryVO {
    
    private String fundCode;
    private LocalDate navDate;
    private BigDecimal nav;
    private BigDecimal accNav;
    private BigDecimal dayGrowth;
}
