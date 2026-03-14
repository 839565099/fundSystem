package com.fund.service.impl;

import com.fund.entity.FundHoldings;
import com.fund.mapper.FundHoldingsMapper;
import com.fund.service.FundHoldingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FundHoldingsServiceImpl implements FundHoldingsService {
    
    private final FundHoldingsMapper fundHoldingsMapper;
    
    @Override
    public List<FundHoldings> getLatestHoldings(String fundCode, int limit) {
        return fundHoldingsMapper.getLatestHoldings(fundCode, limit);
    }
}
