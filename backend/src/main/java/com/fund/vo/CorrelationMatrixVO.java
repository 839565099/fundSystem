package com.fund.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 相关性矩阵结果
 */
@Data
public class CorrelationMatrixVO {
    private List<String> fundCodes;
    private List<String> fundNames;
    private List<List<BigDecimal>> correlationMatrix;
}
