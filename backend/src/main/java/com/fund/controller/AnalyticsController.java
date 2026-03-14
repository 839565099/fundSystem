package com.fund.controller;

import com.fund.common.Result;
import com.fund.dto.CorrelationRequest;
import com.fund.dto.DIPSimRequest;
import com.fund.service.AnalyticsService;
import com.fund.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 高级数据分析控制器
 */
@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/sharpe/{fundCode}")
    public Result<SharpeRatioVO> getSharpeRatio(
            @PathVariable String fundCode,
            @RequestParam(defaultValue = "year") String period) {
        SharpeRatioVO vo = analyticsService.calculateSharpeRatio(fundCode, period);
        return Result.success(vo);
    }

    @GetMapping("/drawdown/{fundCode}")
    public Result<MaxDrawdownVO> getMaxDrawdown(
            @PathVariable String fundCode,
            @RequestParam(defaultValue = "year") String period) {
        MaxDrawdownVO vo = analyticsService.calculateMaxDrawdown(fundCode, period);
        return Result.success(vo);
    }

    @GetMapping("/volatility/{fundCode}")
    public Result<VolatilityVO> getVolatility(
            @PathVariable String fundCode,
            @RequestParam(defaultValue = "year") String period) {
        VolatilityVO vo = analyticsService.calculateVolatility(fundCode, period);
        return Result.success(vo);
    }

    @PostMapping("/correlation")
    public Result<CorrelationMatrixVO> getCorrelationMatrix(@RequestBody CorrelationRequest request) {
        CorrelationMatrixVO vo = analyticsService.calculateCorrelationMatrix(request);
        return Result.success(vo);
    }

    @PostMapping("/dip-simulate")
    public Result<DIPSimResultVO> simulateDIP(@RequestBody DIPSimRequest request) {
        DIPSimResultVO vo = analyticsService.simulateDIP(request);
        return Result.success(vo);
    }

    @GetMapping("/report/{fundCode}")
    public Result<FundAnalyticsReportVO> getAnalyticsReport(@PathVariable String fundCode) {
        FundAnalyticsReportVO report = analyticsService.getAnalyticsReport(fundCode);
        return Result.success(report);
    }
}
