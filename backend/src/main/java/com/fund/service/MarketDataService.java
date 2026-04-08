package com.fund.service;

import com.fund.vo.MarketDataVO;
import java.util.List;
import java.util.Map;

public interface MarketDataService {

    List<MarketDataVO> getMarketData();

    List<Map<String, Object>> getMarketHistory(String marketCode, String period);

    List<Map<String, Object>> getMarketTrends(String marketCode);

    void updateMarketData();
}
