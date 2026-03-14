package com.fund.service.impl;

import com.fund.dto.SectorQueryDTO;
import com.fund.external.SectorDataApiService;
import com.fund.service.SectorService;
import com.fund.vo.SectorHistoryVO;
import com.fund.vo.SectorStockVO;
import com.fund.vo.SectorVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 板块服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SectorServiceImpl implements SectorService {

    private final SectorDataApiService sectorDataApiService;

    @Override
    @Cacheable(value = "sectors", key = "#query.type + ':' + #query.sortBy + ':' + #query.sortOrder + ':' + #query.keyword", unless = "#result == null || #result.isEmpty()")
    public List<SectorVO> getSectors(SectorQueryDTO query) {
        List<SectorVO> sectors = new ArrayList<>();

        // 根据类型获取板块
        if ("all".equals(query.getType())) {
            sectors.addAll(sectorDataApiService.fetchSectors("industry"));
            sectors.addAll(sectorDataApiService.fetchSectors("concept"));
        } else {
            sectors = sectorDataApiService.fetchSectors(query.getType());
        }

        // 关键词过滤
        if (query.getKeyword() != null && !query.getKeyword().isEmpty()) {
            String keyword = query.getKeyword().toLowerCase();
            sectors = sectors.stream()
                    .filter(s -> s.getName() != null && s.getName().toLowerCase().contains(keyword))
                    .collect(Collectors.toList());
        }

        // 排序
        Comparator<SectorVO> comparator = getComparator(query.getSortBy());
        if ("asc".equals(query.getSortOrder())) {
            comparator = comparator.reversed();
        }
        sectors.sort(comparator.reversed());

        // 分页
        int start = (query.getPageNum() - 1) * query.getPageSize();
        int end = Math.min(start + query.getPageSize(), sectors.size());

        if (start < sectors.size()) {
            return new ArrayList<>(sectors.subList(start, end));
        }
        return new ArrayList<>();
    }

    private Comparator<SectorVO> getComparator(String sortBy) {
        switch (sortBy) {
            case "weekGrowth":
                return Comparator.comparing(s -> s.getWeekGrowth() != null ? s.getWeekGrowth() : java.math.BigDecimal.ZERO);
            case "monthGrowth":
                return Comparator.comparing(s -> s.getMonthGrowth() != null ? s.getMonthGrowth() : java.math.BigDecimal.ZERO);
            case "volume":
                return Comparator.comparing(s -> s.getVolume() != null ? s.getVolume() : java.math.BigDecimal.ZERO);
            case "turnover":
                return Comparator.comparing(s -> s.getTurnover() != null ? s.getTurnover() : java.math.BigDecimal.ZERO);
            case "dayGrowth":
            default:
                return Comparator.comparing(s -> s.getChangePercent() != null ? s.getChangePercent() : java.math.BigDecimal.ZERO);
        }
    }

    @Override
    public SectorVO getSectorDetail(String code) {
        return sectorDataApiService.fetchSectorDetail(code);
    }

    @Override
    @Cacheable(value = "sectorHistory", key = "#code + ':' + #period", unless = "#result == null || #result.history == null || #result.history.isEmpty()")
    public SectorHistoryVO getSectorHistory(String code, String period) {
        return sectorDataApiService.fetchSectorHistory(code, period);
    }

    @Override
    @Cacheable(value = "sectorStocks", key = "#code", unless = "#result == null || #result.isEmpty()")
    public List<SectorStockVO> getSectorStocks(String code) {
        return sectorDataApiService.fetchSectorStocks(code);
    }

    @Override
    public List<String> getSectorTypes() {
        List<String> types = new ArrayList<>();
        types.add("all");
        types.add("industry");
        types.add("concept");
        types.add("region");
        return types;
    }
}
