package com.fund.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fund.entity.MarketData;
import com.fund.external.FundDataApiService;
import com.fund.mapper.MarketDataMapper;
import com.fund.service.MarketDataService;
import com.fund.vo.MarketDataVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketDataServiceImpl implements MarketDataService {
    
    private final MarketDataMapper marketDataMapper;
    private final FundDataApiService fundDataApiService;
    
    @Override
    public List<MarketDataVO> getMarketData() {
        LambdaQueryWrapper<MarketData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MarketData::getTradeDate, LocalDate.now());
        List<MarketData> marketDataList = marketDataMapper.selectList(wrapper);
        
        if (marketDataList == null || marketDataList.isEmpty()) {
            updateMarketData();
            marketDataList = marketDataMapper.selectList(wrapper);
        }
        
        return marketDataList.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Map<String, Object>> getMarketHistory(String marketCode, String period) {
        List<Object[]> history = fundDataApiService.fetchMarketHistory(marketCode, period);
        List<Map<String, Object>> result = new ArrayList<>();
        String[] keys = {"date", "open", "close", "high", "low", "volume", "changeRatio"};
        for (Object[] item : history) {
            Map<String, Object> map = new HashMap<>();
            for (int i = 0; i < keys.length && i < item.length; i++) {
                map.put(keys[i], item[i]);
            }
            result.add(map);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getMarketTrends(String marketCode) {
        List<Object[]> trends = fundDataApiService.fetchMarketTrends(marketCode);
        List<Map<String, Object>> result = new ArrayList<>();
        String[] keys = {"time", "price", "avgPrice", "volume", "amount"};
        for (Object[] item : trends) {
            Map<String, Object> map = new HashMap<>();
            for (int i = 0; i < keys.length && i < item.length; i++) {
                map.put(keys[i], item[i]);
            }
            result.add(map);
        }
        return result;
    }

    @Override
    @Scheduled(fixedRate = 30000)
    public void updateMarketData() {
        log.info("开始更新市场数据...");
        try {
            List<MarketData> marketDataList = fundDataApiService.fetchMarketData();
            for (MarketData marketData : marketDataList) {
                LambdaQueryWrapper<MarketData> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(MarketData::getMarketCode, marketData.getMarketCode())
                       .eq(MarketData::getTradeDate, LocalDate.now());
                MarketData existing = marketDataMapper.selectOne(wrapper);
                if (existing != null) {
                    marketData.setId(existing.getId());
                    marketDataMapper.updateById(marketData);
                } else {
                    marketDataMapper.insert(marketData);
                }
            }
            log.info("市场数据更新完成，共{}条", marketDataList.size());
        } catch (Exception e) {
            log.error("更新市场数据失败", e);
        }
    }
    
    private MarketDataVO convertToVO(MarketData marketData) {
        MarketDataVO vo = new MarketDataVO();
        BeanUtils.copyProperties(marketData, vo);
        return vo;
    }
}
