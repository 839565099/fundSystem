package com.fund.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("t_hot_fund_config")
public class HotFundConfig implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String fundCode;
    
    private Integer sortNum;
    
    private Integer status;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
