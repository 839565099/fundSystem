package com.fund.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SessionLogVO {

    private Long id;

    private Long userId;

    private String username;

    private String sessionId;

    private String eventType;

    private String ipAddress;

    private LocalDateTime loginTime;

    private LocalDateTime expireTime;

    private LocalDateTime eventTime;

    private String remark;
}
