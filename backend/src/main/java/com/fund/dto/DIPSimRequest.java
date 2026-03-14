package com.fund.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 定投模拟请求
 */
@Data
public class DIPSimRequest {
    private String fundCode;
    private BigDecimal amount;
    private String frequency;
    private LocalDate startDate;
    private LocalDate endDate;
}
