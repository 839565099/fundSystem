package com.fund.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fund.common.PageResult;
import com.fund.common.Result;
import com.fund.dto.FundSearchDTO;
import com.fund.entity.Fund;
import com.fund.service.FundNavHistoryService;
import com.fund.service.FundService;
import com.fund.vo.FundDetailVO;
import com.fund.vo.FundListVO;
import com.fund.vo.FundNavHistoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/fund")
@RequiredArgsConstructor
public class FundController {
    
    private final FundService fundService;
    private final FundNavHistoryService navHistoryService;
    
    @GetMapping("/search")
    public Result<PageResult<FundListVO>> search(FundSearchDTO searchDTO) {
        Page<FundListVO> page = fundService.searchFunds(searchDTO);
        PageResult<FundListVO> result = new PageResult<>(
                page.getRecords(), page.getTotal(), page.getSize(), page.getCurrent());
        return Result.success(result);
    }
    
    @GetMapping("/search/keyword")
    public Result<List<Fund>> searchByKeyword(@RequestParam String keyword,
                                               @RequestParam(defaultValue = "10") int limit) {
        List<Fund> funds = fundService.searchByKeyword(keyword, limit);
        return Result.success(funds);
    }
    
    @GetMapping("/detail/{fundCode}")
    public Result<FundDetailVO> getDetail(@PathVariable String fundCode) {
        FundDetailVO detail = fundService.getFundDetail(fundCode);
        if (detail == null) {
            return Result.error(404, "基金不存在");
        }
        return Result.success(detail);
    }
    
    @GetMapping("/hot")
    public Result<List<Fund>> getHotFunds(@RequestParam(defaultValue = "10") int limit) {
        List<Fund> funds = fundService.getHotFunds(limit);
        return Result.success(funds);
    }
    
    @GetMapping("/top")
    public Result<List<Fund>> getTopGrowthFunds(@RequestParam(defaultValue = "10") int limit) {
        List<Fund> funds = fundService.getTopGrowthFunds(limit);
        return Result.success(funds);
    }
    
    @GetMapping("/nav-history/{fundCode}")
    public Result<List<FundNavHistoryVO>> getNavHistory(
            @PathVariable String fundCode,
            @RequestParam(defaultValue = "month") String period) {
        List<FundNavHistoryVO> history = navHistoryService.getNavHistory(fundCode, period);
        return Result.success(history);
    }

    @GetMapping("/ranking")
    public Result<PageResult<FundListVO>> getRanking(
            @RequestParam(defaultValue = "growth") String rankingType,
            @RequestParam(defaultValue = "total") String period,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<FundListVO> page = fundService.getRankingList(rankingType, period, pageNum, pageSize);
        PageResult<FundListVO> result = new PageResult<>(
                page.getRecords(), page.getTotal(), page.getSize(), page.getCurrent());
        return Result.success(result);
    }

    @GetMapping("/types")
    public Result<List<String>> getFundTypes() {
        List<String> types = fundService.getFundTypes();
        return Result.success(types);
    }

    @GetMapping("/companies")
    public Result<List<String>> getFundCompanies() {
        List<String> companies = fundService.getFundCompanies();
        return Result.success(companies);
    }

    @GetMapping("/hot-by-type")
    public Result<List<Fund>> getHotFundsByType(
            @RequestParam String fundType,
            @RequestParam(defaultValue = "8") int limit) {
        List<Fund> funds = fundService.getHotFundsByType(fundType, limit);
        return Result.success(funds);
    }
}
