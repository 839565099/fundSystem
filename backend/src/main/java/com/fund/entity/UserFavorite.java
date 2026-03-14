package com.fund.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_user_favorite")
public class UserFavorite implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private String fundCode;
    
    private String groupName;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(exist = false)
    private String fundName;
    
    @TableField(exist = false)
    private BigDecimal nav;

    @TableField(exist = false)
    private java.time.LocalDate navDate;
    
    @TableField(exist = false)
    private BigDecimal dayGrowth;
    
    @TableField(exist = false)
    private BigDecimal weekGrowth;
    
    @TableField(exist = false)
    private BigDecimal monthGrowth;
    
    @TableField(exist = false)
    private BigDecimal threeMonthGrowth;
    
    @TableField(exist = false)
    private BigDecimal yearGrowth;
}
