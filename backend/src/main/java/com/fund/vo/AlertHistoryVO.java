package com.fund.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预警历史返回
 */
@Data
public class AlertHistoryVO {

    private Long id;

    private Long ruleId;

    private String alertType;

    private String fundCode;

    private String fundName;

    private String alertTitle;

    private String alertMessage;

    private BigDecimal alertValue;

    private LocalDateTime triggeredTime;

    private Integer isRead;
}
