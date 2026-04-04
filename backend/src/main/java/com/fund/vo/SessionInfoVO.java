package com.fund.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SessionInfoVO {

    private Long userId;

    private String username;

    private String role;

    private String sessionId;

    private LocalDateTime loginTime;

    private LocalDateTime expireTime;

    private Double remainingMinutes;

    private String ipAddress;
}
