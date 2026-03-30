package com.fund.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fund.dto.FundSearchDTO;
import com.fund.entity.Fund;
import com.fund.external.FundDataApiService;
import com.fund.mapper.FundMapper;
import com.fund.service.FundNavHistoryService;
import com.fund.service.FundService;
import com.fund.vo.FundDetailVO;
import com.fund.vo.FundListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FundServiceImpl implements FundService {
    
    private final FundMapper fundMapper;
    private final FundNavHistoryService navHistoryService;
    private final FundDataApiService fundDataApiService;
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String FUND_CACHE_PREFIX = "fund:";
    private static final String FUND_DETAIL_CACHE_PREFIX = "fund:detail:";
    private static final long CACHE_EXPIRE = 300;
    
    @Override
    public Page<FundListVO> searchFunds(FundSearchDTO searchDTO) {
        Page<Fund> page = new Page<>(searchDTO.getPageNum(), searchDTO.getPageSize());
        LambdaQueryWrapper<Fund> wrapper = new LambdaQueryWrapper<>();
        
        wrapper.eq(Fund::getStatus, 1);
        
        if (StrUtil.isNotBlank(searchDTO.getKeyword())) {
            wrapper.and(w -> w.like(Fund::getFundCode, searchDTO.getKeyword())
                    .or().like(Fund::getFundName, searchDTO.getKeyword()));
        }
        
        if (StrUtil.isNotBlank(searchDTO.getFundType())) {
            wrapper.eq(Fund::getFundType, searchDTO.getFundType());
        }
        
        if (searchDTO.getRiskLevel() != null) {
            wrapper.eq(Fund::getRiskLevel, searchDTO.getRiskLevel());
        }
        
        if (searchDTO.getMinScale() != null) {
            wrapper.ge(Fund::getFundScale, searchDTO.getMinScale());
        }
        
        if (searchDTO.getMaxScale() != null) {
            wrapper.le(Fund::getFundScale, searchDTO.getMaxScale());
        }

        if (searchDTO.getMinDayGrowth() != null) {
            wrapper.ge(Fund::getDayGrowth, searchDTO.getMinDayGrowth());
        }
        if (searchDTO.getMaxDayGrowth() != null) {
            wrapper.le(Fund::getDayGrowth, searchDTO.getMaxDayGrowth());
        }

        if (searchDTO.getMinWeekGrowth() != null) {
            wrapper.ge(Fund::getWeekGrowth, searchDTO.getMinWeekGrowth());
        }
        if (searchDTO.getMaxWeekGrowth() != null) {
            wrapper.le(Fund::getWeekGrowth, searchDTO.getMaxWeekGrowth());
        }

        if (searchDTO.getMinMonthGrowth() != null) {
            wrapper.ge(Fund::getMonthGrowth, searchDTO.getMinMonthGrowth());
        }
        if (searchDTO.getMaxMonthGrowth() != null) {
            wrapper.le(Fund::getMonthGrowth, searchDTO.getMaxMonthGrowth());
        }

        if (searchDTO.getMinYearGrowth() != null) {
            wrapper.ge(Fund::getYearGrowth, searchDTO.getMinYearGrowth());
        }
        if (searchDTO.getMaxYearGrowth() != null) {
            wrapper.le(Fund::getYearGrowth, searchDTO.getMaxYearGrowth());
        }

        if (searchDTO.getMinTotalGrowth() != null) {
            wrapper.ge(Fund::getTotalGrowth, searchDTO.getMinTotalGrowth());
        }
        if (searchDTO.getMaxTotalGrowth() != null) {
            wrapper.le(Fund::getTotalGrowth, searchDTO.getMaxTotalGrowth());
        }

        if (StrUtil.isNotBlank(searchDTO.getFundCompany())) {
            wrapper.like(Fund::getFundCompany, searchDTO.getFundCompany());
        }

        if (searchDTO.getMinEstablishYears() != null) {
            LocalDate maxEstablishDate = LocalDate.now().minusYears(searchDTO.getMinEstablishYears());
            wrapper.le(Fund::getEstablishDate, maxEstablishDate);
        }
        if (searchDTO.getMaxEstablishYears() != null) {
            LocalDate minEstablishDate = LocalDate.now().minusYears(searchDTO.getMaxEstablishYears());
            wrapper.ge(Fund::getEstablishDate, minEstablishDate);
        }

        if (searchDTO.getEstablishDateStart() != null) {
            wrapper.ge(Fund::getEstablishDate, searchDTO.getEstablishDateStart());
        }
        if (searchDTO.getEstablishDateEnd() != null) {
            wrapper.le(Fund::getEstablishDate, searchDTO.getEstablishDateEnd());
        }
        
        if (StrUtil.isNotBlank(searchDTO.getSortBy())) {
            boolean isAsc = "asc".equalsIgnoreCase(searchDTO.getSortOrder());
            switch (searchDTO.getSortBy()) {
                case "dayGrowth":
                    wrapper.orderBy(true, isAsc, Fund::getDayGrowth);
                    break;
                case "weekGrowth":
                    wrapper.orderBy(true, isAsc, Fund::getWeekGrowth);
                    break;
                case "monthGrowth":
                    wrapper.orderBy(true, isAsc, Fund::getMonthGrowth);
                    break;
                case "threeMonthGrowth":
                    wrapper.orderBy(true, isAsc, Fund::getThreeMonthGrowth);
                    break;
                case "sixMonthGrowth":
                    wrapper.orderBy(true, isAsc, Fund::getSixMonthGrowth);
                    break;
                case "yearGrowth":
                    wrapper.orderBy(true, isAsc, Fund::getYearGrowth);
                    break;
                case "totalGrowth":
                    wrapper.orderBy(true, isAsc, Fund::getTotalGrowth);
                    break;
                case "fundScale":
                    wrapper.orderBy(true, isAsc, Fund::getFundScale);
                    break;
                case "establishDate":
                    wrapper.orderBy(true, isAsc, Fund::getEstablishDate);
                    break;
                default:
                    wrapper.orderByDesc(Fund::getFundScale);
            }
        } else {
            wrapper.orderByDesc(Fund::getFundScale);
        }
        
        Page<Fund> fundPage = fundMapper.selectPage(page, wrapper);
        
        Page<FundListVO> resultPage = new Page<>();
        resultPage.setCurrent(fundPage.getCurrent());
        resultPage.setSize(fundPage.getSize());
        resultPage.setTotal(fundPage.getTotal());
        resultPage.setPages(fundPage.getPages());
        resultPage.setRecords(fundPage.getRecords().stream()
                .map(this::convertToListVO)
                .collect(Collectors.toList()));
        
        return resultPage;
    }
    
    @Override
    public List<Fund> searchByKeyword(String keyword, int limit) {
        String cacheKey = FUND_CACHE_PREFIX + "search:" + keyword;
        
        @SuppressWarnings("unchecked")
        List<Fund> cached = (List<Fund>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        List<Fund> funds = fundDataApiService.searchFunds(keyword);
        if (funds.size() > limit) {
            funds = funds.subList(0, limit);
        }
        redisTemplate.opsForValue().set(cacheKey, funds, CACHE_EXPIRE, TimeUnit.SECONDS);
        return funds;
    }
    
    @Override
    @Cacheable(value = "fundDetail", key = "#fundCode", unless = "#result == null")
    public FundDetailVO getFundDetail(String fundCode) {
        Fund fund = getByFundCode(fundCode);
        if (fund == null) {
            fund = fundDataApiService.fetchFundInfo(fundCode);
            if (fund != null) {
                fundMapper.insert(fund);
            }
        }
        
        if (fund == null) {
            return null;
        }
        
        return convertToDetailVO(fund);
    }
    
    @Override
    @Cacheable(value = "hotFunds", key = "#limit")
    public List<Fund> getHotFunds(int limit) {
        return fundMapper.getHotFunds(limit);
    }
    
    @Override
    @Cacheable(value = "topGrowthFunds", key = "#limit")
    public List<Fund> getTopGrowthFunds(int limit) {
        return fundMapper.getTopGrowth(limit);
    }
    
    @Override
    public Fund getByFundCode(String fundCode) {
        LambdaQueryWrapper<Fund> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Fund::getFundCode, fundCode);
        return fundMapper.selectOne(wrapper);
    }
    
    @Override
    @CacheEvict(value = {"fundDetail", "hotFunds", "topGrowthFunds"}, allEntries = true)
    public void updateFundData(String fundCode) {
        try {
            Fund fund = fundDataApiService.fetchFundInfo(fundCode);
            if (fund != null) {
                Fund existingFund = getByFundCode(fundCode);
                if (existingFund != null) {
                    fund.setId(existingFund.getId());
                    fundMapper.updateById(fund);
                } else {
                    fundMapper.insert(fund);
                }
            }
        } catch (Exception e) {
            log.error("更新基金数据失败: {}", fundCode, e);
        }
    }

    @Override
    @CacheEvict(value = {"fundDetail", "hotFunds", "topGrowthFunds"}, allEntries = true)
    public Fund fetchAndSaveFund(String fundCode) {
        try {
            // 先检查本地是否已存在
            Fund existingFund = getByFundCode(fundCode);
            if (existingFund != null) {
                return existingFund;
            }

            // 从东方财富 API 获取基金信息
            Fund fund = fundDataApiService.fetchFundInfo(fundCode);
            if (fund != null) {
                fund.setStatus(1);
                fundMapper.insert(fund);
                log.info("从外部 API 获取基金信息成功: {}", fundCode);
                return fund;
            }
        } catch (Exception e) {
            log.error("获取基金信息失败: {}", fundCode, e);
        }
        return null;
    }
    
    @Override
    @Scheduled(fixedRate = 30000)
    @CacheEvict(value = {"fundDetail", "hotFunds", "topGrowthFunds"}, allEntries = true)
    public void batchUpdateFundData() {
        log.info("开始批量更新基金数据...");
        try {
            List<Fund> trackedFunds = fundMapper.selectList(new LambdaQueryWrapper<Fund>()
                    .eq(Fund::getStatus, 1)
                    .select(Fund::getFundCode));

            if (trackedFunds == null || trackedFunds.isEmpty()) {
                trackedFunds = fundMapper.getHotFunds(50);
            }

            for (Fund fund : trackedFunds) {
                if (fund.getFundCode() != null) {
                    updateFundData(fund.getFundCode());
                }
            }
            log.info("批量更新基金数据完成, 共更新{}只基金", trackedFunds.size());
        } catch (Exception e) {
            log.error("批量更新基金数据失败", e);
        }
    }
    
    private FundListVO convertToListVO(Fund fund) {
        FundListVO vo = new FundListVO();
        BeanUtils.copyProperties(fund, vo);
        return vo;
    }
    
    private FundDetailVO convertToDetailVO(Fund fund) {
        FundDetailVO vo = new FundDetailVO();
        BeanUtils.copyProperties(fund, vo);
        vo.setRiskLevelName(getRiskLevelName(fund.getRiskLevel()));
        return vo;
    }
    
    private String getRiskLevelName(Integer riskLevel) {
        if (riskLevel == null) return "未知";
        switch (riskLevel) {
            case 1: return "低风险";
            case 2: return "中低风险";
            case 3: return "中风险";
            case 4: return "中高风险";
            case 5: return "高风险";
            default: return "未知";
        }
    }

    @Override
    public Page<FundListVO> getRankingList(String rankingType, String period, Integer pageNum, Integer pageSize) {
        String cacheKey = "fund:ranking:" + rankingType + ":" + period + ":" + pageNum + ":" + pageSize;
        
        @SuppressWarnings("unchecked")
        Page<FundListVO> cached = (Page<FundListVO>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        List<Fund> funds = fundDataApiService.fetchFundRanking(rankingType, period, pageNum, pageSize);
        int total = fundDataApiService.fetchFundRankingTotal(rankingType, period);
        
        Page<FundListVO> resultPage = new Page<>();
        resultPage.setCurrent(pageNum);
        resultPage.setSize(pageSize);
        resultPage.setTotal(total);
        resultPage.setPages((long) Math.ceil((double) total / pageSize));
        resultPage.setRecords(funds.stream()
                .map(this::convertToListVO)
                .collect(Collectors.toList()));
        
        redisTemplate.opsForValue().set(cacheKey, resultPage, 5, TimeUnit.MINUTES);
        
        return resultPage;
    }

    @Override
    public List<String> getFundTypes() {
        return fundMapper.selectList(new LambdaQueryWrapper<Fund>()
                .select(Fund::getFundType)
                .isNotNull(Fund::getFundType)
                .groupBy(Fund::getFundType))
                .stream()
                .map(Fund::getFundType)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getFundCompanies() {
        return fundMapper.selectList(new LambdaQueryWrapper<Fund>()
                .select(Fund::getFundCompany)
                .isNotNull(Fund::getFundCompany)
                .groupBy(Fund::getFundCompany))
                .stream()
                .map(Fund::getFundCompany)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
    }

    @Override
    public List<Fund> getHotFundsByType(String fundType, int limit) {
        String cacheKey = "fund:hotType:" + fundType + ":" + limit;
        @SuppressWarnings("unchecked")
        List<Fund> cached = (List<Fund>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<Fund> funds = fundDataApiService.fetchHotFundsByType(fundType, limit);
        if (!funds.isEmpty()) {
            redisTemplate.opsForValue().set(cacheKey, funds, CACHE_EXPIRE, TimeUnit.SECONDS);
        }
        return funds;
    }
}
