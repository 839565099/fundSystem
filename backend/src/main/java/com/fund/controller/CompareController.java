package com.fund.controller;

import com.fund.common.Result;
import com.fund.entity.Fund;
import com.fund.service.FundService;
import com.fund.service.FundNavHistoryService;
import com.fund.vo.FundCompareVO;
import com.fund.vo.FundNavHistoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/compare")
@RequiredArgsConstructor
public class CompareController {

    private final FundService fundService;
    private final FundNavHistoryService navHistoryService;

    private final com.fund.external.FundDataApiService fundDataApiService;

    @PostMapping("/funds")
    public Result<List<FundCompareVO>> compareFunds(@RequestBody List<String> fundCodes) {
        List<FundCompareVO> compareList = new ArrayList<>();

        for (String fundCode : fundCodes) {
            Fund fund = fundService.getByFundCode(fundCode);
            if (fund == null) {
                fund = fundDataApiService.fetchFundInfo(fundCode);
            }

            if (fund != null) {
                FundCompareVO vo = new FundCompareVO();
                vo.setFundCode(fund.getFundCode());
                vo.setFundName(fund.getFundName());
                vo.setFundType(fund.getFundType());
                vo.setFundCompany(fund.getFundCompany());
                vo.setFundScale(fund.getFundScale());
                vo.setNav(fund.getNav());
                vo.setDayGrowth(fund.getDayGrowth());
                vo.setWeekGrowth(fund.getWeekGrowth());
                vo.setMonthGrowth(fund.getMonthGrowth());
                vo.setThreeMonthGrowth(fund.getThreeMonthGrowth());
                vo.setSixMonthGrowth(fund.getSixMonthGrowth());
                vo.setYearGrowth(fund.getYearGrowth());
                vo.setTotalGrowth(fund.getTotalGrowth());

                // 获取净值历史数据 - 使用带缓存的服务
                try {
                    List<FundNavHistoryVO> historyList = navHistoryService.getNavHistory(fundCode, "sixMonth");
                    if (historyList != null && !historyList.isEmpty()) {
                        vo.setNavHistory(historyList);
                    }
                } catch (Exception e) {
                    // 忽略获取历史数据失败的情况
                }

                compareList.add(vo);
            }
        }

        return Result.success(compareList);
    }
}
