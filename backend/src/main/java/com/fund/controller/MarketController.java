package com.fund.controller;

import com.fund.common.Result;
import com.fund.service.MarketDataService;
import com.fund.vo.MarketDataVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/market")
@RequiredArgsConstructor
public class MarketController {

    private final MarketDataService marketDataService;

    @GetMapping("/data")
    public Result<List<MarketDataVO>> getMarketData() {
        return Result.success(marketDataService.getMarketData());
    }

    @GetMapping("/history/{marketCode}")
    public Result<List<Map<String, Object>>> getMarketHistory(
            @PathVariable String marketCode,
            @RequestParam(defaultValue = "month") String period) {
        return Result.success(marketDataService.getMarketHistory(marketCode, period));
    }

    @GetMapping("/trends/{marketCode}")
    public Result<List<Map<String, Object>>> getMarketTrends(@PathVariable String marketCode) {
        return Result.success(marketDataService.getMarketTrends(marketCode));
    }
}
