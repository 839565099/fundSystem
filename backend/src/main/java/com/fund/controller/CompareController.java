package com.fund.controller;

import com.fund.common.Result;
import com.fund.service.CompareService;
import com.fund.vo.FundCompareVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/compare")
@RequiredArgsConstructor
public class CompareController {

    private final CompareService compareService;

    @PostMapping("/funds")
    public Result<List<FundCompareVO>> compareFunds(@RequestBody List<String> fundCodes) {
        if (fundCodes == null || fundCodes.size() < 2) {
            return Result.error("请选择至少2只基金进行对比");
        }
        if (fundCodes.size() > 10) {
            return Result.error("最多对比10只基金");
        }
        return Result.success(compareService.compareFunds(fundCodes));
    }
}
