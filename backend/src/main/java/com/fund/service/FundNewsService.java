package com.fund.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fund.entity.FundNews;
import com.fund.vo.NewsSentimentOverviewVO;

import java.util.List;

public interface FundNewsService {
    
    Page<FundNews> getNewsList(Integer pageNum, Integer pageSize);
    
    Page<FundNews> getNewsByFundCode(String fundCode, Integer pageNum, Integer pageSize);
    
    FundNews getNewsDetail(Long id);

    Page<FundNews> getRealtimeNews(String fundCode, String sentiment, Integer minutes, Integer pageNum, Integer pageSize);

    NewsSentimentOverviewVO getSentimentOverview(String fundCode, Integer minutes);

    FundNews publishRealtimeNews(FundNews news);
    
    Page<FundNews> searchNews(String keyword, String newsType, String sentiment, 
                               String sortBy, Integer pageNum, Integer pageSize);
    
    List<FundNews> getRelatedNews(Long newsId, Integer limit);
    
    List<FundNews> getHotNews(Integer limit);

    /**
     * 获取数据库中实际存在的新闻类型
     */
    List<String> getExistingNewsTypes();

    /**
     * 清空所有新闻
     */
    int clearAllNews();
}
