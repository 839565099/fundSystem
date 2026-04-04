package com.fund.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("t_session_config")
public class SessionConfig implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String roleName;

    private Integer maxDurationMinutes;

    private Integer warningMinutes;

    private Boolean isEnabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
