package com.fund.service;

import com.fund.entity.FundHoldings;
import com.fund.vo.FundHoldingVO;
import java.util.List;

public interface FundHoldingsService {
    
    List<FundHoldings> getLatestHoldings(String fundCode, int limit);
    
    List<FundHoldingVO> getLatestHoldingsWithRealtime(String fundCode, int limit);
}
