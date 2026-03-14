package com.fund.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fund.entity.Fund;
import com.fund.entity.HotFundConfig;
import com.fund.external.FundDataApiService;
import com.fund.mapper.FundMapper;
import com.fund.mapper.HotFundConfigMapper;
import com.fund.service.HotFundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotFundServiceImpl implements HotFundService {
    
    private final HotFundConfigMapper hotFundConfigMapper;
    private final FundMapper fundMapper;
    private final FundDataApiService fundDataApiService;
    
    @Override
    @Cacheable(value = "hotFunds", key = "#limit")
    public List<Fund> getHotFunds(int limit) {
        LambdaQueryWrapper<HotFundConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HotFundConfig::getStatus, 1)
               .orderByAsc(HotFundConfig::getSortNum);
        List<HotFundConfig> configs = hotFundConfigMapper.selectList(wrapper);
        
        if (configs.isEmpty()) {
            log.info("热门基金配置为空，返回空列表");
            return new ArrayList<>();
        }
        
        List<Fund> funds = new ArrayList<>();
        for (HotFundConfig config : configs) {
            if (funds.size() >= limit) break;
            
            Fund fund = fundMapper.getByFundCode(config.getFundCode());
            if (fund == null) {
                fund = fundDataApiService.fetchFundInfo(config.getFundCode());
                if (fund != null) {
                    fundMapper.insert(fund);
                }
            }
            if (fund != null) {
                funds.add(fund);
            }
        }
        
        return funds;
    }
    
    @Override
    @CacheEvict(value = "hotFunds", allEntries = true)
    public void addHotFund(String fundCode, Integer sortNum) {
        Fund fund = fundMapper.getByFundCode(fundCode);
        if (fund == null) {
            fund = fundDataApiService.fetchFundInfo(fundCode);
            if (fund != null) {
                fundMapper.insert(fund);
            } else {
                throw new RuntimeException("基金不存在: " + fundCode);
            }
        }
        
        LambdaQueryWrapper<HotFundConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HotFundConfig::getFundCode, fundCode);
        HotFundConfig existing = hotFundConfigMapper.selectOne(wrapper);
        
        if (existing != null) {
            existing.setStatus(1);
            if (sortNum != null) {
                existing.setSortNum(sortNum);
            }
            hotFundConfigMapper.updateById(existing);
        } else {
            HotFundConfig config = new HotFundConfig();
            config.setFundCode(fundCode);
            config.setSortNum(sortNum != null ? sortNum : 999);
            config.setStatus(1);
            hotFundConfigMapper.insert(config);
        }
    }
    
    @Override
    @CacheEvict(value = "hotFunds", allEntries = true)
    public void removeHotFund(String fundCode) {
        LambdaQueryWrapper<HotFundConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HotFundConfig::getFundCode, fundCode);
        hotFundConfigMapper.delete(wrapper);
        log.info("已从热门基金移除: {}", fundCode);
    }
    
    @Override
    @CacheEvict(value = "hotFunds", allEntries = true)
    public void updateSortNum(String fundCode, Integer sortNum) {
        LambdaQueryWrapper<HotFundConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HotFundConfig::getFundCode, fundCode);
        HotFundConfig config = hotFundConfigMapper.selectOne(wrapper);
        
        if (config != null) {
            config.setSortNum(sortNum);
            hotFundConfigMapper.updateById(config);
        }
    }
    
    @Override
    public boolean isHotFund(String fundCode) {
        LambdaQueryWrapper<HotFundConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HotFundConfig::getFundCode, fundCode)
               .eq(HotFundConfig::getStatus, 1);
        return hotFundConfigMapper.selectCount(wrapper) > 0;
    }
}
