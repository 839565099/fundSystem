package com.fund.service;

import com.fund.entity.FundHoldings;
import java.util.List;

public interface FundHoldingsService {
    
    List<FundHoldings> getLatestHoldings(String fundCode, int limit);
}
