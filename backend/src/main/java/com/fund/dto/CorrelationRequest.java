package com.fund.dto;

import lombok.Data;
import java.util.List;

/**
 * 相关性分析请求
 */
@Data
public class CorrelationRequest {
    private List<String> fundCodes;
    private String period;
}
