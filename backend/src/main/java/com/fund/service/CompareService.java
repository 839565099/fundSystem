package com.fund.service;

import com.fund.vo.FundCompareVO;

import java.util.List;

/**
 * 基金对比服务接口
 */
public interface CompareService {

    /**
     * 对比多只基金
     */
    List<FundCompareVO> compareFunds(List<String> fundCodes);
}
