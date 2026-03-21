package com.fund.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_fund_manager")
public class FundManager implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String managerId;
    
    private String managerName;
    
    private String company;
    
    private Integer workYears;
    
    private LocalDate startDate;
    
    private BigDecimal totalAsset;
    
    private BigDecimal bestReturn;
    
    private String education;
    
    private String resume;

    private String investmentIdea;

    private String photo;

    @TableField(exist = false)
    private Integer fundCount;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
