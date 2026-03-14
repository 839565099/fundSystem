package com.fund.controller;

import com.fund.common.Result;
import com.fund.entity.FundHoldings;
import com.fund.service.FundHoldingsService;
import com.fund.service.FundManagerService;
import com.fund.vo.FundManagerVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/fund-info")
@RequiredArgsConstructor
public class FundInfoController {
    
    private final FundManagerService fundManagerService;
    private final FundHoldingsService fundHoldingsService;
    
    @GetMapping("/managers/{fundCode}")
    public Result<List<FundManagerVO>> getManagers(@PathVariable String fundCode) {
        List<FundManagerVO> managers = fundManagerService.getManagersByFundCode(fundCode);
        return Result.success(managers);
    }
    
    @GetMapping("/holdings/{fundCode}")
    public Result<List<FundHoldings>> getHoldings(
            @PathVariable String fundCode,
            @RequestParam(defaultValue = "10") int limit) {
        List<FundHoldings> holdings = fundHoldingsService.getLatestHoldings(fundCode, limit);
        return Result.success(holdings);
    }
}
