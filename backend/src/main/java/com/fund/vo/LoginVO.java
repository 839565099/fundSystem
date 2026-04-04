package com.fund.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LoginVO {

    private String token;

    private UserVO user;

    private SessionInfo sessionInfo;

    @Data
    public static class SessionInfo {

        private String sessionId;

        private LocalDateTime loginTime;

        private LocalDateTime expireTime;

        private Integer maxDurationMinutes;

        private Integer warningMinutes;
    }
}
