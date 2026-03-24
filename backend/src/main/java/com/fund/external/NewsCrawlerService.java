package com.fund.external;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fund.entity.FundNews;
import com.fund.mapper.FundNewsMapper;
import com.fund.service.FundNewsService;
import com.fund.service.NewsSentimentAnalyzeService;
import com.fund.service.NewsStreamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsCrawlerService {

    private final FundNewsService fundNewsService;
    private final NewsSentimentAnalyzeService newsSentimentAnalyzeService;
    private final NewsStreamService newsStreamService;
    private final FundNewsMapper fundNewsMapper;
    
    private final Set<String> existedTitles = new HashSet<>();
    private static final int MAX_CACHE_SIZE = 1000;
    private static final int NEWS_EXPIRE_DAYS = 30;

    // 分类关键词映射
    private static final Map<String, String[]> CATEGORY_KEYWORDS = new LinkedHashMap<>();

    static {
        CATEGORY_KEYWORDS.put("基金动态", new String[]{"基金", "净值", "申购", "赎回", "基金经理", "ETF", "LOF", "QDII"});
        CATEGORY_KEYWORDS.put("政策法规", new String[]{"政策", "监管", "证监会", "央行", "银保监", "国务院", "法规", "新规"});
        CATEGORY_KEYWORDS.put("市场分析", new String[]{"市场", "行情", "大盘", "指数", "A股", "沪指", "深成指", "创业板"});
        CATEGORY_KEYWORDS.put("行业动态", new String[]{"行业", "板块", "概念", "龙头", "产业链", "新能源", "半导体", "医药"});
        CATEGORY_KEYWORDS.put("资金流向", new String[]{"资金", "流入", "流出", "主力", "北向", "南向", "外资", "机构"});
    }

    /**
     * 智能分类新闻
     */
    private String classifyNews(String title, String content) {
        String text = (title != null ? title : "") + " " + (content != null ? content : "");

        for (Map.Entry<String, String[]> entry : CATEGORY_KEYWORDS.entrySet()) {
            String category = entry.getKey();
            String[] keywords = entry.getValue();
            for (String keyword : keywords) {
                if (text.contains(keyword)) {
                    return category;
                }
            }
        }
        return "财经要闻";
    }

    @PostConstruct
    public void init() {
        cleanExpiredNews();      // 启动时清理过期新闻
        loadExistingTitles();
    }

    public void clearCache() {
        existedTitles.clear();
        log.info("已清空新闻标题缓存");
    }

    private void loadExistingTitles() {
        try {
            List<FundNews> recentNews = fundNewsService.getNewsList(1, 100).getRecords();
            for (FundNews news : recentNews) {
                existedTitles.add(news.getTitle());
            }
            log.info("已加载 {} 条已存在新闻标题", existedTitles.size());
        } catch (Exception e) {
            log.warn("加载已存在新闻标题失败", e);
        }
    }

    @Scheduled(fixedRate = 300000)
    public void crawlNewsScheduled() {
        log.info("开始定时抓取财经新闻...");
        crawlAndSaveNews();
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanExpiredNewsScheduled() {
        log.info("开始定时清理过期新闻...");
        cleanExpiredNews();
    }

    public int crawlAndSaveNews() {
        int savedCount = 0;
        
        List<FundNews> sinaNews = crawlSinaFinanceNews();
        for (FundNews news : sinaNews) {
            if (saveNewsIfNotExists(news)) {
                savedCount++;
            }
        }

        List<FundNews> eastmoneyNews = crawlEastmoneyNews();
        for (FundNews news : eastmoneyNews) {
            if (saveNewsIfNotExists(news)) {
                savedCount++;
            }
        }

        log.info("本次抓取共保存 {} 条新闻", savedCount);
        return savedCount;
    }

    public int cleanExpiredNews() {
        try {
            LocalDateTime expireTime = LocalDateTime.now().minusDays(NEWS_EXPIRE_DAYS);
            LambdaQueryWrapper<FundNews> wrapper = new LambdaQueryWrapper<>();
            wrapper.lt(FundNews::getPublishTime, expireTime);
            
            int deletedCount = fundNewsMapper.delete(wrapper);
            log.info("已清理 {} 条过期新闻（超过 {} 天）", deletedCount, NEWS_EXPIRE_DAYS);
            return deletedCount;
        } catch (Exception e) {
            log.error("清理过期新闻失败", e);
            return 0;
        }
    }

    private boolean saveNewsIfNotExists(FundNews news) {
        if (existedTitles.contains(news.getTitle())) {
            return false;
        }

        try {
            newsSentimentAnalyzeService.analyzeAndFill(news);
            FundNews saved = fundNewsService.publishRealtimeNews(news);
            newsStreamService.publish(saved);
            
            existedTitles.add(news.getTitle());
            if (existedTitles.size() > MAX_CACHE_SIZE) {
                existedTitles.clear();
                loadExistingTitles();
            }
            
            log.info("保存新闻: {} [{}]", news.getTitle(), news.getSentiment());
            return true;
        } catch (Exception e) {
            log.error("保存新闻失败: {}", news.getTitle(), e);
            return false;
        }
    }

    private List<FundNews> crawlSinaFinanceNews() {
        List<FundNews> newsList = new ArrayList<>();
        try {
            String url = "https://feed.mix.sina.com.cn/api/roll/get?pageid=153&lid=2516&k=&num=20&page=1&r=" + System.currentTimeMillis();
            String response = HttpUtil.get(url, 10000);
            
            if (response != null) {
                JSONObject json = JSONUtil.parseObj(response);
                JSONObject result = json.getJSONObject("result");
                if (result != null) {
                    JSONArray data = result.getJSONArray("data");
                    if (data != null) {
                        for (int i = 0; i < data.size(); i++) {
                            try {
                                JSONObject item = data.getJSONObject(i);
                                FundNews news = new FundNews();
                                news.setTitle(item.getStr("title"));
                                news.setSource("新浪财经");
                                news.setAuthor(item.getStr("media_name"));
                                news.setNewsType(classifyNews(news.getTitle(), news.getSummary()));
                                // 获取原文链接
                                String newsUrl = item.getStr("url");
                                if (newsUrl != null && !newsUrl.isEmpty()) {
                                    news.setOriginalUrl(newsUrl);
                                }
                                // 获取摘要作为内容（新浪API字段是intro不是digest）
                                String intro = item.getStr("intro");
                                if (intro != null && intro.length() > 10) {
                                    news.setSummary(intro);
                                    news.setContent(intro);
                                }
                                // 也尝试获取digest字段（兼容）
                                String digest = item.getStr("digest");
                                if ((intro == null || intro.length() <= 10) && digest != null && digest.length() > 10) {
                                    news.setSummary(digest);
                                    news.setContent(digest);
                                }
                                
                                String ctime = item.getStr("ctime");
                                if (ctime != null && ctime.length() >= 10) {
                                    try {
                                        news.setPublishTime(LocalDateTime.parse(ctime + " 00:00:00", 
                                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                                    } catch (Exception e) {
                                        news.setPublishTime(LocalDateTime.now());
                                    }
                                } else {
                                    news.setPublishTime(LocalDateTime.now());
                                }
                                
                                if (news.getTitle() != null && !news.getTitle().isEmpty()) {
                                    newsList.add(news);
                                }
                            } catch (Exception e) {
                                log.warn("解析新浪财经新闻失败", e);
                            }
                        }
                    }
                }
            }
            log.info("从新浪财经获取 {} 条新闻", newsList.size());
        } catch (Exception e) {
            log.error("抓取新浪财经新闻失败", e);
        }
        return newsList;
    }

    private List<FundNews> crawlEastmoneyNews() {
        List<FundNews> newsList = new ArrayList<>();
        try {
            String url = "https://newsapi.eastmoney.com/kuaixun/v1/getlist_102_ajaxResult_50_1_.html?r=" + System.currentTimeMillis();
            String response = HttpUtil.get(url, 10000);

            if (response != null) {
                // 处理 JSONP 格式: var ajaxResult={...}
                int startIdx = response.indexOf("{");
                int endIdx = response.lastIndexOf("}");
                if (startIdx > 0 && endIdx > startIdx) {
                    String jsonStr = response.substring(startIdx, endIdx + 1);
                    JSONObject json = JSONUtil.parseObj(jsonStr);
                    // 实际字段名是 LivesList 不是 Data
                    JSONArray data = json.getJSONArray("LivesList");
                    if (data != null) {
                        for (int i = 0; i < data.size(); i++) {
                            try {
                                JSONObject item = data.getJSONObject(i);
                                FundNews news = new FundNews();
                                // 实际字段名是小写
                                news.setTitle(item.getStr("title"));
                                news.setSource("东方财富");
                                news.setAuthor(item.getStr("editor_name"));
                                news.setNewsType(classifyNews(news.getTitle(), news.getSummary()));

                                // 原文链接是 url_w 或 url_unique
                                String artUrl = item.getStr("url_w");
                                if (artUrl == null || artUrl.isEmpty()) {
                                    artUrl = item.getStr("url_unique");
                                }
                                if (artUrl != null && !artUrl.isEmpty()) {
                                    news.setOriginalUrl(artUrl);
                                }

                                // 发布时间字段是 showtime
                                String showTime = item.getStr("showtime");
                                if (showTime != null) {
                                    try {
                                        news.setPublishTime(LocalDateTime.parse(showTime,
                                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                                    } catch (Exception e) {
                                        news.setPublishTime(LocalDateTime.now());
                                    }
                                } else {
                                    news.setPublishTime(LocalDateTime.now());
                                }

                                // 摘要字段是 digest（小写）
                                String digest = item.getStr("digest");
                                if (digest != null && digest.length() > 10) {
                                    news.setSummary(digest);
                                    news.setContent(digest);
                                }

                                if (news.getTitle() != null && !news.getTitle().isEmpty()) {
                                    newsList.add(news);
                                }
                            } catch (Exception e) {
                                log.warn("解析东方财富新闻失败", e);
                            }
                        }
                    }
                }
            }
            log.info("从东方财富获取 {} 条新闻", newsList.size());
        } catch (Exception e) {
            log.error("抓取东方财富新闻失败", e);
        }
        return newsList;
    }
}
