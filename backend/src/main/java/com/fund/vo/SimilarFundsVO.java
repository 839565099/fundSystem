package com.fund.vo;

import lombok.Data;
import java.util.List;

/**
 * 相似基金 VO
 */
@Data
public class SimilarFundsVO {
    private String fundCode;
    private String fundName;
    private List<RecommendFundVO> similarFunds;
}
