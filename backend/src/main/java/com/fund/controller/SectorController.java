package com.fund.controller;

import com.fund.common.Result;
import com.fund.dto.SectorQueryDTO;
import com.fund.service.SectorService;
import com.fund.vo.SectorHistoryVO;
import com.fund.vo.SectorStockVO;
import com.fund.vo.SectorVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 板块控制器
 */
@RestController
@RequestMapping("/sector")
@RequiredArgsConstructor
public class SectorController {

    private final SectorService sectorService;

    /**
     * 获取板块列表
     */
    @GetMapping("/list")
    public Result<List<SectorVO>> getSectors(SectorQueryDTO query) {
        List<SectorVO> sectors = sectorService.getSectors(query);
        return Result.success(sectors);
    }

    /**
     * 获取板块类型列表
     */
    @GetMapping("/types")
    public Result<List<String>> getSectorTypes() {
        List<String> types = sectorService.getSectorTypes();
        return Result.success(types);
    }

    /**
     * 获取板块详情
     */
    @GetMapping("/{code}")
    public Result<SectorVO> getSectorDetail(@PathVariable String code) {
        SectorVO sector = sectorService.getSectorDetail(code);
        return Result.success(sector);
    }

    /**
     * 获取板块历史K线
     */
    @GetMapping("/{code}/history")
    public Result<SectorHistoryVO> getSectorHistory(
            @PathVariable String code,
            @RequestParam(defaultValue = "month") String period) {
        SectorHistoryVO history = sectorService.getSectorHistory(code, period);
        return Result.success(history);
    }

    /**
     * 获取板块成分股
     */
    @GetMapping("/{code}/stocks")
    public Result<List<SectorStockVO>> getSectorStocks(@PathVariable String code) {
        List<SectorStockVO> stocks = sectorService.getSectorStocks(code);
        return Result.success(stocks);
    }
}
