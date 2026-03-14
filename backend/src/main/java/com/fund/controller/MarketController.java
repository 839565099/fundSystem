package com.fund.controller;

import com.fund.common.Result;
import com.fund.external.FundDataApiService;
import com.fund.service.MarketDataService;
import com.fund.vo.MarketDataVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/market")
@RequiredArgsConstructor
public class MarketController {
    
    private final MarketDataService marketDataService;
    private final FundDataApiService fundDataApiService;
    
    @GetMapping("/data")
    public Result<List<MarketDataVO>> getMarketData() {
        List<MarketDataVO> marketData = marketDataService.getMarketData();
        return Result.success(marketData);
    }
    
    @GetMapping("/history/{marketCode}")
    public Result<List<Map<String, Object>>> getMarketHistory(
            @PathVariable String marketCode,
            @RequestParam(defaultValue = "month") String period) {
        List<Object[]> history = fundDataApiService.fetchMarketHistory(marketCode, period);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] item : history) {
            Map<String, Object> map = new HashMap<>();
            map.put("date", item[0]);
            map.put("open", item[1]);
            map.put("close", item[2]);
            map.put("high", item[3]);
            map.put("low", item[4]);
            map.put("volume", item[5]);
            map.put("changeRatio", item[6]);
            result.add(map);
        }
        return Result.success(result);
    }
}
