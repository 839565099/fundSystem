package com.fund.controller;

import com.fund.common.Result;
import com.fund.entity.Fund;
import com.fund.service.HotFundService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/hot-fund")
@RequiredArgsConstructor
public class HotFundController {

    private final HotFundService hotFundService;

    @GetMapping("/list")
    public Result<List<Fund>> getHotFunds(@RequestParam(defaultValue = "10") int limit) {
        return Result.success(hotFundService.getHotFunds(limit));
    }

    @PostMapping("/add")
    public Result<String> addHotFund(@RequestParam String fundCode,
                                     @RequestParam(required = false) Integer sortNum) {
        hotFundService.addHotFund(fundCode, sortNum);
        return Result.success("添加成功");
    }

    @DeleteMapping("/remove")
    public Result<String> removeHotFund(@RequestParam String fundCode) {
        hotFundService.removeHotFund(fundCode);
        return Result.success("移除成功");
    }

    @PutMapping("/sort")
    public Result<String> updateSortNum(@RequestParam String fundCode,
                                        @RequestParam Integer sortNum) {
        hotFundService.updateSortNum(fundCode, sortNum);
        return Result.success("更新成功");
    }

    @GetMapping("/check")
    public Result<Boolean> isHotFund(@RequestParam String fundCode) {
        return Result.success(hotFundService.isHotFund(fundCode));
    }
}
