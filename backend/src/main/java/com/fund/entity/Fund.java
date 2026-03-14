package com.fund.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_fund")
public class Fund implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String fundCode;
    
    private String fundName;
    
    private String fundType;
    
    @TableField(exist = false)
    private String sector;
    
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
    
    private BigDecimal minPurchase;
    
    private BigDecimal purchaseRate;
    
    private BigDecimal redemptionRate;
    
    private BigDecimal managementRate;
    
    private BigDecimal custodyRate;
    
    private Integer status;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
