package com.fund.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("t_session_log")
public class SessionLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String username;

    private String sessionId;

    private String eventType;

    private String ipAddress;

    private String userAgent;

    private LocalDateTime loginTime;

    private LocalDateTime expireTime;

    private LocalDateTime eventTime;

    private String remark;
}
