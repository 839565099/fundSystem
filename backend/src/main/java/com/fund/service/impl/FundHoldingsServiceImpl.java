package com.fund.service.impl;

import com.fund.entity.FundHoldings;
import com.fund.external.FundDataApiService;
import com.fund.mapper.FundHoldingsMapper;
import com.fund.service.FundHoldingsService;
import com.fund.vo.FundHoldingVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FundHoldingsServiceImpl implements FundHoldingsService {

    private final FundHoldingsMapper fundHoldingsMapper;
    private final FundDataApiService fundDataApiService;

    @Override
    public List<FundHoldings> getLatestHoldings(String fundCode, int limit) {
        // 1. 先从数据库获取
        List<FundHoldings> holdings = fundHoldingsMapper.getLatestHoldings(fundCode, limit);

        // 2. 数据库为空或数据不完整时，从东方财富获取
        boolean needRefresh = (holdings == null || holdings.isEmpty());
        if (!needRefresh && holdings.stream().allMatch(h -> h.getHoldingShares() == null && h.getHoldingValue() == null)) {
            needRefresh = true;
            log.info("持仓数据不完整(缺少持股数和市值)，触发刷新: {}", fundCode);
        }
        if (needRefresh) {
            log.info("数据库中无持仓数据，从东方财富获取: {}", fundCode);
            holdings = fundDataApiService.fetchFundHoldings(fundCode, limit);

            // 3. 缓存到数据库
            if (holdings != null && !holdings.isEmpty()) {
                try {
                    fundHoldingsMapper.batchInsert(holdings);
                    log.info("保存持仓数据: {}条, 基金代码: {}", holdings.size(), fundCode);
                } catch (Exception e) {
                    log.warn("保存持仓数据失败: {}", fundCode, e);
                }
            }
        }

        return holdings != null ? holdings : new ArrayList<>();
    }

    @Override
    public List<FundHoldingVO> getLatestHoldingsWithRealtime(String fundCode, int limit) {
        // 1. 获取持仓数据（包含自动从东方财富获取和缓存的逻辑）
        List<FundHoldings> holdings = getLatestHoldings(fundCode, limit);
        if (holdings == null || holdings.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 获取实时涨跌
        List<String> stockCodes = holdings.stream()
                .map(FundHoldings::getStockCode)
                .filter(code -> code != null && !code.isEmpty())
                .collect(Collectors.toList());

        Map<String, Map<String, BigDecimal>> stockDataMap = fundDataApiService.fetchStockRealtimeData(stockCodes);

        // 3. 转换为VO并填充实时数据
        List<FundHoldingVO> result = new ArrayList<>();
        for (FundHoldings holding : holdings) {
            FundHoldingVO vo = new FundHoldingVO();
            vo.setId(holding.getId());
            vo.setFundCode(holding.getFundCode());
            vo.setReportDate(holding.getReportDate());
            vo.setStockCode(holding.getStockCode());
            vo.setStockName(holding.getStockName());
            vo.setHoldingRatio(holding.getHoldingRatio());
            vo.setHoldingShares(holding.getHoldingShares());
            vo.setHoldingValue(holding.getHoldingValue());
            vo.setHoldingType(holding.getHoldingType());

            if (holding.getStockCode() != null && stockDataMap.containsKey(holding.getStockCode())) {
                Map<String, BigDecimal> stockData = stockDataMap.get(holding.getStockCode());
                vo.setDayGrowth(stockData.get("dayGrowth"));
                vo.setCurrentPrice(stockData.get("price"));
            }

            result.add(vo);
        }

        return result;
    }
}
