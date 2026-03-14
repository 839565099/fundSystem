package com.fund.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fund.common.PageResult;
import com.fund.common.Result;
import com.fund.entity.FundNews;
import com.fund.external.NewsCrawlerService;
import com.fund.service.FundNewsService;
import com.fund.service.NewsStreamService;
import com.fund.vo.NewsSentimentOverviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/news")
@RequiredArgsConstructor
public class NewsController {
    
    private final FundNewsService fundNewsService;
    private final NewsStreamService newsStreamService;
    private final NewsCrawlerService newsCrawlerService;
    
    @GetMapping("/list")
    public Result<PageResult<FundNews>> getNewsList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<FundNews> page = fundNewsService.getNewsList(pageNum, pageSize);
        PageResult<FundNews> result = new PageResult<>(
                page.getRecords(), page.getTotal(), page.getSize(), page.getCurrent());
        return Result.success(result);
    }
    
    @GetMapping("/fund/{fundCode}")
    public Result<PageResult<FundNews>> getNewsByFundCode(
            @PathVariable String fundCode,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<FundNews> page = fundNewsService.getNewsByFundCode(fundCode, pageNum, pageSize);
        PageResult<FundNews> result = new PageResult<>(
                page.getRecords(), page.getTotal(), page.getSize(), page.getCurrent());
        return Result.success(result);
    }

    @GetMapping("/realtime")
    public Result<PageResult<FundNews>> getRealtimeNews(
            @RequestParam(required = false) String fundCode,
            @RequestParam(required = false) String sentiment,
            @RequestParam(defaultValue = "60") Integer minutes,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<FundNews> page = fundNewsService.getRealtimeNews(fundCode, sentiment, minutes, pageNum, pageSize);
        PageResult<FundNews> result = new PageResult<>(
                page.getRecords(), page.getTotal(), page.getSize(), page.getCurrent());
        return Result.success(result);
    }

    @GetMapping("/sentiment/overview")
    public Result<NewsSentimentOverviewVO> getSentimentOverview(
            @RequestParam(required = false) String fundCode,
            @RequestParam(defaultValue = "60") Integer minutes) {
        NewsSentimentOverviewVO overview = fundNewsService.getSentimentOverview(fundCode, minutes);
        return Result.success(overview);
    }

    @PostMapping("/realtime/publish")
    public Result<FundNews> publishRealtimeNews(@RequestBody FundNews news) {
        FundNews saved = fundNewsService.publishRealtimeNews(news);
        newsStreamService.publish(saved);
        return Result.success(saved);
    }

    @GetMapping("/stream")
    public SseEmitter stream(@RequestParam(required = false) String fundCode) {
        return newsStreamService.subscribe(fundCode);
    }
    
    @GetMapping("/detail/{id}")
    public Result<FundNews> getNewsDetail(@PathVariable Long id) {
        FundNews news = fundNewsService.getNewsDetail(id);
        if (news == null) {
            return Result.error(404, "资讯不存在");
        }
        return Result.success(news);
    }

    @PostMapping("/crawl")
    public Result<Integer> crawlNews() {
        int count = newsCrawlerService.crawlAndSaveNews();
        return Result.success(count);
    }

    @DeleteMapping("/all")
    public Result<Integer> clearAllNews() {
        int count = fundNewsService.clearAllNews();
        newsCrawlerService.clearCache();
        return Result.success(count);
    }
    
    @GetMapping("/search")
    public Result<PageResult<FundNews>> searchNews(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String newsType,
            @RequestParam(required = false) String sentiment,
            @RequestParam(defaultValue = "time") String sortBy,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<FundNews> page = fundNewsService.searchNews(keyword, newsType, sentiment, sortBy, pageNum, pageSize);
        PageResult<FundNews> result = new PageResult<>(
                page.getRecords(), page.getTotal(), page.getSize(), page.getCurrent());
        return Result.success(result);
    }
    
    @GetMapping("/related/{id}")
    public Result<List<FundNews>> getRelatedNews(
            @PathVariable Long id,
            @RequestParam(defaultValue = "5") Integer limit) {
        List<FundNews> relatedNews = fundNewsService.getRelatedNews(id, limit);
        return Result.success(relatedNews);
    }
    
    @GetMapping("/hot")
    public Result<List<FundNews>> getHotNews(
            @RequestParam(defaultValue = "10") Integer limit) {
        List<FundNews> hotNews = fundNewsService.getHotNews(limit);
        return Result.success(hotNews);
    }
    
    @GetMapping("/types")
    public Result<List<String>> getNewsTypes() {
        // 从数据库查询实际存在的新闻类型
        List<String> types = fundNewsService.getExistingNewsTypes();
        // 如果数据库没有类型，返回默认分类
        if (types == null || types.isEmpty()) {
            types = Arrays.asList("财经要闻", "基金动态", "市场分析", "政策法规", "行业动态", "资金流向");
        }
        return Result.success(types);
    }
}
