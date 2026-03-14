package com.fund.controller;

import com.fund.common.Result;
import com.fund.dto.RecommendDTO;
import com.fund.service.RecommendService;
import com.fund.vo.RecommendFundVO;
import com.fund.vo.RecommendOverviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 智能推荐控制器
 */
@RestController
@RequestMapping("/recommend")
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendService recommendService;

    @GetMapping("/overview")
    public Result<RecommendOverviewVO> getOverview(@RequestAttribute(required = false) Long userId) {
        RecommendOverviewVO overview = recommendService.getRecommendOverview(userId);
        return Result.success(overview);
    }

    @GetMapping("/personalized")
    public Result<List<RecommendFundVO>> getPersonalized(
            @RequestAttribute(required = false) Long userId,
            @RequestParam(defaultValue = "10") Integer limit) {
        List<RecommendFundVO> funds = recommendService.getPersonalizedRecommend(userId, limit);
        return Result.success(funds);
    }

    @GetMapping("/similar/{fundCode}")
    public Result<List<RecommendFundVO>> getSimilarFunds(
            @PathVariable String fundCode,
            @RequestParam(defaultValue = "10") Integer limit) {
        List<RecommendFundVO> funds = recommendService.getSimilarFunds(fundCode, limit);
        return Result.success(funds);
    }

    @GetMapping("/hot")
    public Result<List<RecommendFundVO>> getHotRecommend(
            @RequestParam(defaultValue = "10") Integer limit) {
        List<RecommendFundVO> funds = recommendService.getHotRecommend(limit);
        return Result.success(funds);
    }

    @PostMapping("/risk-based")
    public Result<List<RecommendFundVO>> getRiskBasedRecommend(
            @RequestBody RecommendDTO dto,
            @RequestParam(defaultValue = "10") Integer limit) {
        List<RecommendFundVO> funds = recommendService.getRiskBasedRecommend(dto, limit);
        return Result.success(funds);
    }

    @PostMapping("/behavior")
    public Result<Void> recordBehavior(
            @RequestAttribute(required = false) Long userId,
            @RequestParam String fundCode,
            @RequestParam String behaviorType,
            @RequestParam(required = false) Integer dwellTime) {
        if (userId != null) {
            recommendService.recordBehavior(userId, fundCode, behaviorType, dwellTime);
        }
        return Result.success();
    }
}
