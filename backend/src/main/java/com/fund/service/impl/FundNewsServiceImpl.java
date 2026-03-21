package com.fund.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fund.entity.FundNews;
import com.fund.mapper.FundNewsMapper;
import com.fund.service.FundNewsService;
import com.fund.service.NewsSentimentAnalyzeService;
import com.fund.vo.NewsSentimentOverviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class FundNewsServiceImpl implements FundNewsService {
    
    private final FundNewsMapper fundNewsMapper;
    private final NewsSentimentAnalyzeService newsSentimentAnalyzeService;
    
    @Override
    public Page<FundNews> getNewsList(Integer pageNum, Integer pageSize) {
        Page<FundNews> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<FundNews> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FundNews::getStatus, 1)
                .orderByDesc(FundNews::getPublishTime);
        return fundNewsMapper.selectPage(page, wrapper);
    }
    
    @Override
    public Page<FundNews> getNewsByFundCode(String fundCode, Integer pageNum, Integer pageSize) {
        Page<FundNews> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<FundNews> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FundNews::getStatus, 1)
                .eq(FundNews::getFundCode, fundCode)
                .orderByDesc(FundNews::getPublishTime);
        return fundNewsMapper.selectPage(page, wrapper);
    }
    
    @Override
    public FundNews getNewsDetail(Long id) {
        FundNews news = fundNewsMapper.selectById(id);
        if (news != null) {
            int currentViewCount = news.getViewCount() == null ? 0 : news.getViewCount();
            news.setViewCount(currentViewCount + 1);
            fundNewsMapper.updateById(news);
        }
        return news;
    }

    @Override
    public Page<FundNews> getRealtimeNews(String fundCode, String sentiment, Integer minutes, Integer pageNum, Integer pageSize) {
        int safeMinutes = (minutes == null || minutes <= 0) ? 60 : minutes;
        Page<FundNews> page = new Page<>(pageNum, pageSize);
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(safeMinutes);

        LambdaQueryWrapper<FundNews> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FundNews::getStatus, 1)
                .ge(FundNews::getPublishTime, threshold)
                .orderByDesc(FundNews::getPublishTime);

        if (StringUtils.hasText(fundCode)) {
            wrapper.eq(FundNews::getFundCode, fundCode);
        }
        if (StringUtils.hasText(sentiment)) {
            wrapper.eq(FundNews::getSentiment, normalizeSentiment(sentiment));
        }

        return fundNewsMapper.selectPage(page, wrapper);
    }

    @Override
    public NewsSentimentOverviewVO getSentimentOverview(String fundCode, Integer minutes) {
        int safeMinutes = (minutes == null || minutes <= 0) ? 60 : minutes;
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(safeMinutes);

        long bullishCount = countBySentiment(fundCode, threshold, "BULLISH");
        long bearishCount = countBySentiment(fundCode, threshold, "BEARISH");
        long neutralCount = countBySentiment(fundCode, threshold, "NEUTRAL");

        long total = bullishCount + bearishCount + neutralCount;
        double sentimentIndex = total == 0 ? 0D : (double) (bullishCount - bearishCount) / total;

        // 计算百分比
        double bullishPercent = total == 0 ? 0D : (double) bullishCount * 100 / total;
        double bearishPercent = total == 0 ? 0D : (double) bearishCount * 100 / total;
        double neutralPercent = total == 0 ? 0D : (double) neutralCount * 100 / total;

        NewsSentimentOverviewVO overviewVO = new NewsSentimentOverviewVO();
        overviewVO.setBullishCount(bullishCount);
        overviewVO.setBearishCount(bearishCount);
        overviewVO.setNeutralCount(neutralCount);
        overviewVO.setBullishPercent(bullishPercent);
        overviewVO.setBearishPercent(bearishPercent);
        overviewVO.setNeutralPercent(neutralPercent);
        overviewVO.setSentimentIndex(sentimentIndex);
        overviewVO.setMinutes(safeMinutes);
        overviewVO.setFundCode(fundCode);
        return overviewVO;
    }

    @Override
    public FundNews publishRealtimeNews(FundNews news) {
        if (news == null || !StringUtils.hasText(news.getTitle())) {
            throw new IllegalArgumentException("新闻标题不能为空");
        }

        if (news.getPublishTime() == null) {
            news.setPublishTime(LocalDateTime.now());
        }
        if (news.getStatus() == null) {
            news.setStatus(1);
        }
        if (news.getViewCount() == null) {
            news.setViewCount(0);
        }

        newsSentimentAnalyzeService.analyzeAndFill(news);
        fundNewsMapper.insert(news);
        return news;
    }

    private long countBySentiment(String fundCode, LocalDateTime threshold, String sentiment) {
        LambdaQueryWrapper<FundNews> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FundNews::getStatus, 1)
                .ge(FundNews::getPublishTime, threshold)
                .eq(FundNews::getSentiment, sentiment);

        if (StringUtils.hasText(fundCode)) {
            wrapper.eq(FundNews::getFundCode, fundCode);
        }
        return fundNewsMapper.selectCount(wrapper);
    }

    private String normalizeSentiment(String sentiment) {
        if (!StringUtils.hasText(sentiment)) {
            return sentiment;
        }
        return sentiment.trim().toUpperCase(Locale.ROOT);
    }

    @Override
    public Page<FundNews> searchNews(String keyword, String newsType, String sentiment,
                                      String sortBy, Integer pageNum, Integer pageSize) {
        Page<FundNews> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<FundNews> wrapper = new LambdaQueryWrapper<>();
        
        wrapper.eq(FundNews::getStatus, 1);
        
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(FundNews::getTitle, keyword)
                    .or()
                    .like(FundNews::getContent, keyword));
        }
        
        if (StringUtils.hasText(newsType)) {
            wrapper.eq(FundNews::getNewsType, newsType);
        }
        
        if (StringUtils.hasText(sentiment)) {
            wrapper.eq(FundNews::getSentiment, normalizeSentiment(sentiment));
        }
        
        if ("hot".equals(sortBy)) {
            wrapper.orderByDesc(FundNews::getViewCount);
        } else if ("time".equals(sortBy)) {
            wrapper.orderByDesc(FundNews::getPublishTime);
        } else {
            wrapper.orderByDesc(FundNews::getPublishTime);
        }
        
        return fundNewsMapper.selectPage(page, wrapper);
    }

    @Override
    public List<FundNews> getRelatedNews(Long newsId, Integer limit) {
        FundNews currentNews = fundNewsMapper.selectById(newsId);
        if (currentNews == null) {
            return new ArrayList<>();
        }
        
        LambdaQueryWrapper<FundNews> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FundNews::getStatus, 1)
                .ne(FundNews::getId, newsId);
        
        boolean hasCondition = false;
        
        if (StringUtils.hasText(currentNews.getFundCode())) {
            wrapper.eq(FundNews::getFundCode, currentNews.getFundCode());
            hasCondition = true;
        }
        
        if (!hasCondition && StringUtils.hasText(currentNews.getNewsType())) {
            wrapper.eq(FundNews::getNewsType, currentNews.getNewsType());
            hasCondition = true;
        }
        
        if (!hasCondition && StringUtils.hasText(currentNews.getSentiment())) {
            wrapper.eq(FundNews::getSentiment, currentNews.getSentiment());
        }
        
        wrapper.orderByDesc(FundNews::getPublishTime);
        wrapper.last("LIMIT " + Math.min(limit != null ? limit : 5, 50));
        
        return fundNewsMapper.selectList(wrapper);
    }

    @Override
    public List<FundNews> getHotNews(Integer limit) {
        LambdaQueryWrapper<FundNews> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FundNews::getStatus, 1)
                .orderByDesc(FundNews::getViewCount)
                .last("LIMIT " + Math.min(limit != null ? limit : 10, 50));

        return fundNewsMapper.selectList(wrapper);
    }

    @Override
    public List<String> getExistingNewsTypes() {
        // 查询数据库中实际存在的新闻类型
        return fundNewsMapper.selectObjs(
            new LambdaQueryWrapper<FundNews>()
                .select(FundNews::getNewsType)
                .eq(FundNews::getStatus, 1)
                .isNotNull(FundNews::getNewsType)
                .groupBy(FundNews::getNewsType)
        ).stream().map(obj -> (String) obj).collect(java.util.stream.Collectors.toList());
    }

    @Override
    public int clearAllNews() {
        Long count = fundNewsMapper.selectCount(null);
        fundNewsMapper.delete(null);
        return count.intValue();
    }
}
