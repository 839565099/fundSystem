package com.fund.service;

import com.fund.vo.MarketDataVO;
import java.util.List;

public interface MarketDataService {
    
    List<MarketDataVO> getMarketData();
    
    void updateMarketData();
}
