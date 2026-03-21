package com.fund.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FundManagerVO {
    
    private String managerId;
    private String managerName;
    private String company;
    private Integer workYears;
    private LocalDate startDate;
    private BigDecimal totalAsset;
    private BigDecimal bestReturn;
    private String education;
    private String resume;
    private String photo;
    private String investmentIdea;
    private Integer fundCount;
}
