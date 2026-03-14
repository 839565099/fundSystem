package com.fund.service.impl;

import com.fund.entity.FundNavHistory;
import com.fund.external.FundDataApiService;
import com.fund.mapper.FundNavHistoryMapper;
import com.fund.service.FundNavHistoryService;
import com.fund.vo.FundNavHistoryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FundNavHistoryServiceImpl implements FundNavHistoryService {
    
    private final FundNavHistoryMapper navHistoryMapper;
    private final FundDataApiService fundDataApiService;
    
    @Override
    @Cacheable(value = "navHistory", key = "#fundCode + ':' + #period", unless = "#result == null || #result.isEmpty()")
    public List<FundNavHistoryVO> getNavHistory(String fundCode, String period) {
        LocalDate startDate = calculateStartDate(period);
        LocalDate endDate = LocalDate.now();
        
        List<FundNavHistory> history = getHistoryByDateRange(fundCode, startDate);
        
        boolean needFetch = history == null || history.isEmpty();
        
        if (!needFetch) {
            long daysInRange = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
            long actualDays = history.size();
            needFetch = actualDays < daysInRange * 0.8;

            if (!needFetch) {
                FundNavHistory latest = history.get(history.size() - 1);
                LocalDate latestDate = latest.getNavDate();
                if (latestDate != null && latestDate.isBefore(endDate.minusDays(1))) {
                    needFetch = true;
                    log.info("净值历史最新日期过旧，需要从API刷新: {} latest={}, today={}", fundCode, latestDate, endDate);
                }
            }
            
            if (needFetch) {
                log.info("数据不完整，需要从API获取: {} 预期{}天, 实际{}条", fundCode, daysInRange, actualDays);
            }
        }
        
        if (needFetch) {
            log.info("从API获取净值历史数据: {} 日期范围: {} 到 {}", fundCode, startDate, endDate);
            List<FundNavHistory> apiHistory = fundDataApiService.fetchNavHistory(fundCode, startDate, endDate);
            if (apiHistory != null && !apiHistory.isEmpty()) {
                for (FundNavHistory nav : apiHistory) {
                    try {
                        navHistoryMapper.insert(nav);
                    } catch (Exception e) {
                    }
                }
                history = getHistoryByDateRange(fundCode, startDate);
            }
        }
        
        if (history == null) {
            history = new java.util.ArrayList<>();
        }
        
        return history.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<FundNavHistory> getHistoryByDateRange(String fundCode, LocalDate startDate) {
        return navHistoryMapper.getHistoryByDateRange(fundCode, startDate);
    }
    
    @Override
    public List<FundNavHistory> getRecentHistory(String fundCode, int limit) {
        return navHistoryMapper.getRecentHistory(fundCode, limit);
    }
    
    @Override
    public void saveNavHistory(FundNavHistory navHistory) {
        navHistoryMapper.insert(navHistory);
    }
    
    @Override
    public void batchSaveNavHistory(List<FundNavHistory> navHistoryList) {
        for (FundNavHistory navHistory : navHistoryList) {
            saveNavHistory(navHistory);
        }
    }
    
    private LocalDate calculateStartDate(String period) {
        LocalDate now = LocalDate.now();
        switch (period) {
            case "day":
                return now.minusDays(1);
            case "week":
                return now.minusWeeks(1);
            case "month":
                return now.minusMonths(1);
            case "threeMonth":
                return now.minusMonths(3);
            case "sixMonth":
                return now.minusMonths(6);
            case "year":
                return now.minusYears(1);
            case "threeYear":
                return now.minusYears(3);
            case "fiveYear":
                return now.minusYears(5);
            default:
                return now.minusMonths(1);
        }
    }
    
    private FundNavHistoryVO convertToVO(FundNavHistory history) {
        FundNavHistoryVO vo = new FundNavHistoryVO();
        BeanUtils.copyProperties(history, vo);
        return vo;
    }
}
