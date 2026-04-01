package com.fund.service.impl;

import com.fund.entity.Fund;
import com.fund.external.FundDataApiService;
import com.fund.service.CompareService;
import com.fund.service.FundNavHistoryService;
import com.fund.service.FundService;
import com.fund.vo.FundCompareVO;
import com.fund.vo.FundNavHistoryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 基金对比服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompareServiceImpl implements CompareService {

    private final FundService fundService;
    private final FundNavHistoryService navHistoryService;
    private final FundDataApiService fundDataApiService;

    @Override
    public List<FundCompareVO> compareFunds(List<String> fundCodes) {
        List<FundCompareVO> result = new ArrayList<>();

        for (String fundCode : fundCodes) {
            Fund fund = fundService.getByFundCode(fundCode);
            if (fund == null) {
                fund = fundDataApiService.fetchFundInfo(fundCode);
            }

            if (fund != null) {
                result.add(buildCompareVO(fund));
            }
        }

        return result;
    }

    private FundCompareVO buildCompareVO(Fund fund) {
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

        try {
            List<FundNavHistoryVO> historyList = navHistoryService.getNavHistory(fund.getFundCode(), "sixMonth");
            if (historyList != null && !historyList.isEmpty()) {
                vo.setNavHistory(historyList);
            }
        } catch (Exception e) {
            log.warn("获取基金 {} 净值历史失败: {}", fund.getFundCode(), e.getMessage());
        }

        return vo;
    }
}
