package com.fund.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_fund_nav_history")
public class FundNavHistory implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String fundCode;
    
    private LocalDate navDate;
    
    private BigDecimal nav;
    
    private BigDecimal accNav;
    
    private BigDecimal dayGrowth;
    
    private String subscriptionStatus;
    
    private String redemptionStatus;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
