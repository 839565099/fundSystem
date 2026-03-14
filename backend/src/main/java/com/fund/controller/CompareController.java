package com.fund.controller;

import com.fund.common.Result;
import com.fund.entity.Fund;
import com.fund.entity.FundNavHistory;
import com.fund.external.FundDataApiService;
import com.fund.service.FundService;
import com.fund.vo.FundCompareVO;
import com.fund.vo.FundNavHistoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/compare")
@RequiredArgsConstructor
public class CompareController {

    private final FundService fundService;
    private final FundDataApiService fundDataApiService;

    @PostMapping("/funds")
    public Result<List<FundCompareVO>> compareFunds(@RequestBody List<String> fundCodes) {
        List<FundCompareVO> compareList = new ArrayList<>();
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(6); // 获取近6个月净值历史用于对比

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

                // 获取净值历史数据用于图表展示
                try {
                    List<FundNavHistory> historyList = fundDataApiService.fetchNavHistory(fundCode, startDate, endDate);
                    if (historyList != null && !historyList.isEmpty()) {
                        List<FundNavHistoryVO> navHistoryVOList = new ArrayList<>();
                        for (FundNavHistory history : historyList) {
                            FundNavHistoryVO historyVO = new FundNavHistoryVO();
                            historyVO.setFundCode(history.getFundCode());
                            historyVO.setNavDate(history.getNavDate());
                            historyVO.setNav(history.getNav());
                            historyVO.setAccNav(history.getAccNav());
                            historyVO.setDayGrowth(history.getDayGrowth());
                            navHistoryVOList.add(historyVO);
                        }
                        vo.setNavHistory(navHistoryVOList);
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
