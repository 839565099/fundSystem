package com.fund.controller;

import com.fund.common.Result;
import com.fund.entity.Fund;
import com.fund.service.HotFundService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/hot-fund")
@RequiredArgsConstructor
public class HotFundController {
    
    private final HotFundService hotFundService;
    private final CacheManager cacheManager;
    
    @GetMapping("/list")
    public Result<List<Fund>> getHotFunds(@RequestParam(defaultValue = "10") int limit) {
        List<Fund> funds = hotFundService.getHotFunds(limit);
        return Result.success(funds);
    }
    
    @PostMapping("/add")
    public Result<String> addHotFund(@RequestParam String fundCode,
                                     @RequestParam(required = false) Integer sortNum) {
        try {
            hotFundService.addHotFund(fundCode, sortNum);
            return Result.success("添加成功");
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }
    
    @DeleteMapping("/remove")
    public Result<String> removeHotFund(@RequestParam String fundCode) {
        hotFundService.removeHotFund(fundCode);
        cacheManager.getCache("hotFunds").clear();
        return Result.success("移除成功");
    }
    
    @PutMapping("/sort")
    public Result<String> updateSortNum(@RequestParam String fundCode,
                                        @RequestParam Integer sortNum) {
        hotFundService.updateSortNum(fundCode, sortNum);
        cacheManager.getCache("hotFunds").clear();
        return Result.success("更新成功");
    }
    
    @GetMapping("/check")
    public Result<Boolean> isHotFund(@RequestParam String fundCode) {
        boolean isHot = hotFundService.isHotFund(fundCode);
        return Result.success(isHot);
    }
    
    @DeleteMapping("/cache/clear")
    public Result<String> clearCache() {
        cacheManager.getCache("hotFunds").clear();
        return Result.success("缓存已清除");
    }
}
