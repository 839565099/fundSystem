package com.fund.external;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fund.entity.Fund;
import com.fund.entity.FundManager;
import com.fund.entity.FundNavHistory;
import com.fund.entity.FundHoldings;
import com.fund.entity.MarketData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class FundDataApiService {

    @Autowired
    private CloseableHttpClient httpClient;

    @Autowired
    private MarketDataApiService marketDataApiService;

    @Autowired
    private FundDetailApiService fundDetailApiService;

    private String httpGet(String urlStr) {
        try {
            HttpGet request = new HttpGet(urlStr);
            request.addHeader("Accept", "*/*");
            request.addHeader("Accept-Language", "zh-CN,zh;q=0.9");
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    log.warn("HTTP请求失败, 状态码: {}, URL: {}", response.getStatusLine().getStatusCode(), urlStr);
                    return null;
                }
                return EntityUtils.toString(response.getEntity(), "UTF-8");
            }
        } catch (Exception e) {
            log.error("HTTP请求失败: {}", urlStr, e);
            return null;
        }
    }
    
    private String httpPost(String urlStr, String body) {
        try {
            HttpPost request = new HttpPost(urlStr);
            request.addHeader("Content-Type", "application/x-www-form-urlencoded");
            request.addHeader("Accept", "*/*");
            request.addHeader("Accept-Language", "zh-CN,zh;q=0.9");
            if (body != null && !body.isEmpty()) {
                request.setEntity(new StringEntity(body, "UTF-8"));
            }
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    log.warn("HTTP POST请求失败, 状态码: {}, URL: {}", response.getStatusLine().getStatusCode(), urlStr);
                    return null;
                }
                return EntityUtils.toString(response.getEntity(), "UTF-8");
            }
        } catch (Exception e) {
            log.error("HTTP POST请求失败: {}", urlStr, e);
            return null;
        }
    }

    private String httpGetWithReferer(String urlStr, String referer) {
        try {
            HttpGet request = new HttpGet(urlStr);
            request.addHeader("Accept", "*/*");
            request.addHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            request.addHeader("Referer", referer);
            request.addHeader("Cookie", "qgqp_b_id=7d5f5e0a8a8a4a8a8a8a8a8a8a8a8a8a");
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    log.warn("HTTP请求失败, 状态码: {}, URL: {}", response.getStatusLine().getStatusCode(), urlStr);
                    return null;
                }
                return EntityUtils.toString(response.getEntity(), "UTF-8");
            }
        } catch (Exception e) {
            log.error("HTTP请求失败: {}", urlStr, e);
            return null;
        }
    }
    
    public Fund fetchFundInfo(String fundCode) {
        try {
            Fund fund = new Fund();
            fund.setFundCode(fundCode);
            
            String detailUrl = "https://fund.eastmoney.com/pingzhongdata/" + fundCode + ".js";
            String response = httpGet(detailUrl);
            
            if (response != null && response.length() > 100) {
                parseFundDetail(response, fund);
            }
            
            if (fund.getFundName() == null) {
                List<Fund> searchResults = searchFunds(fundCode);
                for (Fund f : searchResults) {
                    if (fundCode.equals(f.getFundCode())) {
                        fund.setFundName(f.getFundName());
                        fund.setFundType(f.getFundType());
                        fund.setNav(f.getNav());
                        break;
                    }
                }
            }
            
            if (fund.getFundName() != null) {
                return fund;
            }
            
            return null;
        } catch (Exception e) {
            log.error("获取基金信息失败: {}", fundCode, e);
            return null;
        }
    }
    
    private void parseFundDetail(String response, Fund fund) {
        try {
            fund.setFundName(extractJsVar(response, "fS_name"));
            fund.setFundType(extractJsVar(response, "fund_jjlx"));
            
            String scale = extractJsVar(response, "fund_scale");
            if (scale != null && !scale.isEmpty()) {
                try {
                    fund.setFundScale(new BigDecimal(scale));
                } catch (Exception e) {
                    log.warn("解析基金规模失败: {}", scale);
                }
            }
            
            String establishDate = extractJsVar(response, "fund_establishment");
            if (establishDate != null && !establishDate.isEmpty()) {
                try {
                    fund.setEstablishDate(LocalDate.parse(establishDate, DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                } catch (Exception e) {
                    log.warn("解析成立日期失败: {}", establishDate);
                }
            }
            
            String nav = extractJsVar(response, "fund_nav");
            if (nav != null && !nav.isEmpty()) {
                try {
                    fund.setNav(new BigDecimal(nav));
                } catch (Exception e) {
                    log.warn("解析净值失败: {}", nav);
                }
            }
            
            String accNav = extractJsVar(response, "fund_accNav");
            if (accNav != null && !accNav.isEmpty()) {
                try {
                    fund.setAccNav(new BigDecimal(accNav));
                } catch (Exception e) {
                    log.warn("解析累计净值失败: {}", accNav);
                }
            }

            String navDate = extractJsVar(response, "fund_navDate");
            if (navDate != null && !navDate.isEmpty()) {
                try {
                    String normalized = navDate.trim();
                    if (normalized.length() >= 10) {
                        normalized = normalized.substring(0, 10);
                    }
                    fund.setNavDate(LocalDate.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                } catch (Exception e) {
                    log.warn("解析净值日期失败: {}", navDate);
                }
            }
            
            String dayGrowth = extractJsVar(response, "fund_dayGrowth");
            if (dayGrowth != null && !dayGrowth.isEmpty()) {
                try {
                    fund.setDayGrowth(new BigDecimal(dayGrowth));
                } catch (Exception e) {
                    log.warn("解析日涨跌幅失败: {}", dayGrowth);
                }
            }
            
            parsePerformanceData(response, fund);
            
        } catch (Exception e) {
            log.warn("解析基金详情失败: {}", fund.getFundCode(), e);
        }
    }
    
    private void parsePerformanceData(String response, Fund fund) {
        try {
            String syl1y = extractJsVar(response, "syl_1y");
            if (syl1y != null && !syl1y.isEmpty()) {
                try {
                    fund.setMonthGrowth(new BigDecimal(syl1y));
                    log.info("设置近一月涨跌幅: {}", fund.getMonthGrowth());
                } catch (Exception e) {
                    log.warn("解析近一月涨跌幅失败: {}", syl1y);
                }
            }
            
            String syl3y = extractJsVar(response, "syl_3y");
            if (syl3y != null && !syl3y.isEmpty()) {
                try {
                    fund.setThreeMonthGrowth(new BigDecimal(syl3y));
                    log.info("设置近三月涨跌幅: {}", fund.getThreeMonthGrowth());
                } catch (Exception e) {
                    log.warn("解析近三月涨跌幅失败: {}", syl3y);
                }
            }
            
            String syl6y = extractJsVar(response, "syl_6y");
            if (syl6y != null && !syl6y.isEmpty()) {
                try {
                    fund.setSixMonthGrowth(new BigDecimal(syl6y));
                    log.info("设置近六月涨跌幅: {}", fund.getSixMonthGrowth());
                } catch (Exception e) {
                    log.warn("解析近六月涨跌幅失败: {}", syl6y);
                }
            }
            
            String syl1n = extractJsVar(response, "syl_1n");
            if (syl1n != null && !syl1n.isEmpty()) {
                try {
                    fund.setYearGrowth(new BigDecimal(syl1n));
                    log.info("设置近一年涨跌幅: {}", fund.getYearGrowth());
                } catch (Exception e) {
                    log.warn("解析近一年涨跌幅失败: {}", syl1n);
                }
            }
            
            String dataNet = extractJsArray(response, "Data_netWorthTrend");
            if (dataNet != null) {
                JSONArray netArray = JSONUtil.parseArray(dataNet);
                if (netArray != null && netArray.size() > 0) {
                    JSONObject latest = netArray.getJSONObject(netArray.size() - 1);
                    if (latest != null) {
                        if (fund.getNav() == null && latest.get("y") != null) {
                            fund.setNav(latest.getBigDecimal("y"));
                        }
                        if (fund.getDayGrowth() == null && latest.get("equityReturn") != null) {
                            fund.setDayGrowth(latest.getBigDecimal("equityReturn"));
                        }
                        if (fund.getAccNav() == null && latest.get("accNav") != null) {
                            fund.setAccNav(latest.getBigDecimal("accNav"));
                        }
                        if (fund.getNavDate() == null && latest.get("x") != null) {
                            try {
                                Long timestamp = latest.getLong("x");
                                if (timestamp != null) {
                                    LocalDate latestNavDate = Instant.ofEpochMilli(timestamp)
                                            .atZone(ZoneId.systemDefault())
                                            .toLocalDate();
                                    fund.setNavDate(latestNavDate);
                                }
                            } catch (Exception e) {
                                log.warn("从净值趋势解析净值日期失败: {}", fund.getFundCode());
                            }
                        }
                    }

                    // 获取昨日涨跌幅（倒数第二条记录）
                    if (netArray.size() >= 2) {
                        JSONObject yesterday = netArray.getJSONObject(netArray.size() - 2);
                        if (yesterday != null && yesterday.get("equityReturn") != null) {
                            try {
                                fund.setYesterdayGrowth(yesterday.getBigDecimal("equityReturn"));
                                log.info("设置昨日涨跌幅: {}%", fund.getYesterdayGrowth());
                            } catch (Exception e) {
                                log.warn("解析昨日涨跌幅失败: {}", fund.getFundCode());
                            }
                        }
                    }
                    
                    if (netArray.size() >= 5) {
                        JSONObject weekAgo = netArray.getJSONObject(netArray.size() - 5);
                        if (weekAgo != null && weekAgo.get("y") != null && latest != null && latest.get("y") != null) {
                            BigDecimal weekAgoNav = weekAgo.getBigDecimal("y");
                            BigDecimal currentNav = latest.getBigDecimal("y");
                            if (weekAgoNav != null && currentNav != null && weekAgoNav.compareTo(BigDecimal.ZERO) != 0) {
                                BigDecimal weekGrowth = currentNav.subtract(weekAgoNav).divide(weekAgoNav, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal(100));
                                fund.setWeekGrowth(weekGrowth);
                                log.info("计算近一周涨跌幅: {}", fund.getWeekGrowth());
                            }
                        }
                    }
                    
                    if (netArray.size() > 0) {
                        JSONObject first = netArray.getJSONObject(0);
                        if (first != null && first.get("y") != null && latest != null && latest.get("y") != null) {
                            BigDecimal firstNav = first.getBigDecimal("y");
                            BigDecimal currentNav = latest.getBigDecimal("y");
                            if (firstNav != null && currentNav != null && firstNav.compareTo(BigDecimal.ZERO) != 0) {
                                BigDecimal totalGrowth = currentNav.subtract(firstNav).divide(firstNav, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal(100));
                                fund.setTotalGrowth(totalGrowth);
                                log.info("计算成立以来涨跌幅: {}", fund.getTotalGrowth());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析业绩数据失败: {}", fund.getFundCode(), e);
        }
    }
    
    private String extractJsVar(String jsContent, String varName) {
        try {
            String pattern = "var " + varName + "=\"";
            int start = jsContent.indexOf(pattern);
            if (start < 0) {
                pattern = "var " + varName + "='";
                start = jsContent.indexOf(pattern);
            }
            if (start < 0) return null;
            start += pattern.length();
            int end = jsContent.indexOf("\"", start);
            if (end < 0) {
                end = jsContent.indexOf("'", start);
            }
            if (end < 0) return null;
            return jsContent.substring(start, end);
        } catch (Exception e) {
            return null;
        }
    }
    
    private String extractJsArray(String jsContent, String varName) {
        try {
            String pattern = "var " + varName + " = ";
            int start = jsContent.indexOf(pattern);
            log.info("查找变量 {} 的位置: {}", varName, start);
            
            if (start < 0) {
                pattern = "var " + varName + " =";
                start = jsContent.indexOf(pattern);
                log.info("尝试等号后无空格模式查找变量 {} 的位置: {}", varName, start);
            }
            
            if (start < 0) {
                pattern = "var " + varName + "=";
                start = jsContent.indexOf(pattern);
                log.info("尝试无空格模式查找变量 {} 的位置: {}", varName, start);
            }
            
            if (start < 0) {
                pattern = varName + " = ";
                start = jsContent.indexOf(pattern);
                log.info("尝试无var前缀查找变量 {} 的位置: {}", varName, start);
            }
            
            if (start < 0) {
                pattern = varName + "=";
                start = jsContent.indexOf(pattern);
                log.info("尝试无var无空格查找变量 {} 的位置: {}", varName, start);
            }
            
            if (start < 0) return null;
            start += pattern.length();
            
            while (start < jsContent.length() && Character.isWhitespace(jsContent.charAt(start))) {
                start++;
            }
            
            log.info("数组开始位置: {}, 字符: {}", start, start < jsContent.length() ? jsContent.charAt(start) : "EOF");
            
            int braceCount = 0;
            int end = start;
            boolean started = false;
            
            for (int i = start; i < jsContent.length(); i++) {
                char c = jsContent.charAt(i);
                if (c == '{' || c == '[') {
                    braceCount++;
                    started = true;
                } else if (c == '}' || c == ']') {
                    braceCount--;
                }
                if (started && braceCount == 0) {
                    end = i + 1;
                    break;
                }
            }
            
            String result = jsContent.substring(start, end);
            log.info("提取结果长度: {}", result.length());
            return result;
        } catch (Exception e) {
            log.error("提取JS数组失败: {}", varName, e);
            return null;
        }
    }
    
    public List<FundNavHistory> fetchNavHistory(String fundCode, LocalDate startDate, LocalDate endDate) {
        List<FundNavHistory> historyList = new ArrayList<>();
        try {
            String url = "https://fund.eastmoney.com/pingzhongdata/" + fundCode + ".js";
            String response = httpGet(url);
            
            log.info("获取净值历史响应长度: {}", response != null ? response.length() : 0);
            if (response != null && response.length() > 100) {
                log.info("响应前200字符: {}", response.substring(0, Math.min(200, response.length())));
                log.info("包含Data_netWorthTrend: {}", response.contains("Data_netWorthTrend"));
            }
            
            if (response != null && response.length() > 100) {
                String dataNet = extractJsArray(response, "Data_netWorthTrend");
                log.info("提取Data_netWorthTrend: {}", dataNet != null ? dataNet.length() : 0);
                
                if (dataNet != null) {
                    JSONArray netArray = JSONUtil.parseArray(dataNet);
                    log.info("解析数组大小: {}", netArray != null ? netArray.size() : 0);
                    
                    if (netArray != null) {
                        for (int i = 0; i < netArray.size(); i++) {
                            JSONObject item = netArray.getJSONObject(i);
                            if (item != null) {
                                Long timestamp = item.getLong("x");
                                if (timestamp != null) {
                                    try {
                                        LocalDate navDate = java.time.Instant.ofEpochMilli(timestamp)
                                                .atZone(java.time.ZoneId.systemDefault())
                                                .toLocalDate();
                                        if (navDate.isBefore(startDate) || navDate.isAfter(endDate)) {
                                            continue;
                                        }
                                        
                                        FundNavHistory history = new FundNavHistory();
                                        history.setFundCode(fundCode);
                                        history.setNavDate(navDate);
                                        
                                        if (item.get("y") != null) {
                                            history.setNav(item.getBigDecimal("y"));
                                        }
                                        if (item.get("equityReturn") != null) {
                                            try {
                                                history.setDayGrowth(item.getBigDecimal("equityReturn"));
                                            } catch (Exception e) {
                                            }
                                        }
                                        
                                        historyList.add(history);
                                    } catch (Exception e) {
                                        log.warn("解析净值历史日期失败: {}", timestamp, e);
                                    }
                                }
                            }
                        }
                        log.info("获取到净值历史数据: {}条", historyList.size());
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取基金净值历史失败: {}", fundCode, e);
        }
        return historyList;
    }
    
    public List<Fund> searchFunds(String keyword) {
        List<Fund> funds = new ArrayList<>();
        try {
            String url = "https://fundsuggest.eastmoney.com/FundSearch/api/FundSearchAPI.ashx?m=1&key=" + keyword;
            String response = httpGet(url);
            
            if (response != null) {
                JSONObject json = JSONUtil.parseObj(response);
                JSONArray datas = json.getJSONArray("Datas");
                if (datas != null) {
                    for (int i = 0; i < datas.size() && i < 20; i++) {
                        JSONObject item = datas.getJSONObject(i);
                        JSONObject fundBaseInfo = item.getJSONObject("FundBaseInfo");
                        if (fundBaseInfo != null) {
                            Fund fund = new Fund();
                            fund.setFundCode(fundBaseInfo.getStr("FCODE"));
                            fund.setFundName(fundBaseInfo.getStr("SHORTNAME"));
                            fund.setFundType(fundBaseInfo.getStr("FTYPE"));
                            if (fundBaseInfo.get("DWJZ") != null) {
                                fund.setNav(fundBaseInfo.getBigDecimal("DWJZ"));
                            }
                            funds.add(fund);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("搜索基金失败: {}", keyword, e);
        }
        return funds;
    }
    
    public List<MarketData> fetchMarketData() {
        return marketDataApiService.fetchMarketData();
    }
    
    public List<Object[]> fetchMarketHistory(String marketCode, String period) {
        return marketDataApiService.fetchMarketHistory(marketCode, period);
    }
    
    public List<Fund> fetchFundRanking(String rankingType, String period, int pageNum, int pageSize) {
        List<Fund> funds = new ArrayList<>();
        try {
            String sortField = getSortField(rankingType, period);
            String sortType = "decline".equals(rankingType) ? "asc" : "desc";
            
            LocalDate now = LocalDate.now();
            LocalDate startDate = getStartDate(period);
            String sd = startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String ed = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            
            String url = String.format(
                "https://fund.eastmoney.com/data/rankhandler.aspx?op=ph&dt=kf&ft=all&rs=&gs=0&sc=%s&st=%s&sd=%s&ed=%s&qdii=&tabSubtype=,,,,,&pi=%d&pn=%d&dx=1",
                sortField, sortType, sd, ed, pageNum, pageSize
            );
            
            log.info("获取基金排行榜URL: {}", url);
            String response = httpGetWithReferer(url, "https://fund.eastmoney.com/data/fundranking.html");
            
            if (response != null) {
                funds = parseRankingResponse(response);
                log.info("获取基金排行榜数据: {}条", funds.size());
            }
        } catch (Exception e) {
            log.error("获取基金排行榜失败", e);
        }
        return funds;
    }

    /**
     * 按基金类型获取热门基金（从东方财富API获取）
     */
    public List<Fund> fetchHotFundsByType(String fundType, int limit) {
        List<Fund> funds = new ArrayList<>();
        try {
            String ftCode = getFundTypeCode(fundType);
            LocalDate now = LocalDate.now();
            String sd = now.minusYears(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String ed = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            String url = String.format(
                "https://fund.eastmoney.com/data/rankhandler.aspx?op=ph&dt=kf&ft=%s&rs=&gs=0&sc=1nzf&st=desc&sd=%s&ed=%s&qdii=&tabSubtype=,,,,,&pi=1&pn=%d&dx=1",
                ftCode, sd, ed, limit
            );

            log.info("按类型获取热门基金URL: {}", url);
            String response = httpGetWithReferer(url, "https://fund.eastmoney.com/data/fundranking.html");

            if (response != null) {
                funds = parseRankingResponse(response);
                log.info("获取{}类型热门基金: {}条", fundType, funds.size());
            }
        } catch (Exception e) {
            log.error("按类型获取热门基金失败: {}", fundType, e);
        }
        return funds;
    }

    /**
     * 将中文基金类型映射为东方财富API的ft参数
     */
    private String getFundTypeCode(String fundType) {
        if (fundType == null) return "all";
        switch (fundType) {
            case "股票型": return "gp";
            case "混合型": return "hh";
            case "债券型": return "zq";
            case "指数型": return "zs";
            case "货币型": return "hb";
            case "QDII": return "qdii";
            default: return "all";
        }
    }

    private String getSortField(String rankingType, String period) {
        if ("scale".equals(rankingType)) {
            return "gm";
        }
        switch (period) {
            case "day": return "rzdf";
            case "week": return "zzf";
            case "month": return "1yzf";
            case "threeMonth": return "3yzf";
            case "sixMonth": return "6yzf";
            case "year": return "1nzf";
            case "total":
            default: return "zzf";
        }
    }
    
    private LocalDate getStartDate(String period) {
        LocalDate now = LocalDate.now();
        switch (period) {
            case "day": return now.minusDays(1);
            case "week": return now.minusWeeks(1);
            case "month": return now.minusMonths(1);
            case "threeMonth": return now.minusMonths(3);
            case "sixMonth": return now.minusMonths(6);
            case "year": return now.minusYears(1);
            case "total":
            default: return now.minusYears(10);
        }
    }
    
    private List<Fund> parseRankingResponse(String response) {
        List<Fund> funds = new ArrayList<>();
        try {
            int datasIndex = response.indexOf("datas:");
            if (datasIndex < 0) {
                log.warn("响应中没有找到datas字段");
                return funds;
            }
            
            int startIndex = response.indexOf("[", datasIndex);
            int endIndex = response.lastIndexOf("]");
            if (startIndex < 0 || endIndex < 0) {
                log.warn("响应格式不正确，无法找到数组边界");
                return funds;
            }
            
            String datasStr = response.substring(startIndex, endIndex + 1);
            JSONArray dataArray = JSONUtil.parseArray(datasStr);
            
            if (dataArray != null) {
                for (int i = 0; i < dataArray.size(); i++) {
                    String item = dataArray.getStr(i);
                    if (item != null) {
                        Fund fund = parseRankingItem(item);
                        if (fund != null) {
                            funds.add(fund);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析排行榜响应失败", e);
        }
        return funds;
    }
    
    private Fund parseRankingItem(String item) {
        try {
            String[] parts = item.split(",");
            if (parts.length < 10) {
                return null;
            }
            
            Fund fund = new Fund();
            fund.setFundCode(parts[0]);
            fund.setFundName(parts[1]);
            
            if (parts.length > 4 && !parts[4].isEmpty()) {
                try {
                    fund.setNav(new BigDecimal(parts[4]));
                } catch (Exception e) {}
            }
            
            if (parts.length > 5 && !parts[5].isEmpty()) {
                try {
                    fund.setAccNav(new BigDecimal(parts[5]));
                } catch (Exception e) {}
            }
            
            if (parts.length > 6 && !parts[6].isEmpty()) {
                try {
                    fund.setDayGrowth(new BigDecimal(parts[6]));
                } catch (Exception e) {}
            }
            
            if (parts.length > 7 && !parts[7].isEmpty()) {
                try {
                    fund.setWeekGrowth(new BigDecimal(parts[7]));
                } catch (Exception e) {}
            }
            
            if (parts.length > 8 && !parts[8].isEmpty()) {
                try {
                    fund.setMonthGrowth(new BigDecimal(parts[8]));
                } catch (Exception e) {}
            }
            
            if (parts.length > 9 && !parts[9].isEmpty()) {
                try {
                    fund.setThreeMonthGrowth(new BigDecimal(parts[9]));
                } catch (Exception e) {}
            }
            
            if (parts.length > 10 && !parts[10].isEmpty()) {
                try {
                    fund.setSixMonthGrowth(new BigDecimal(parts[10]));
                } catch (Exception e) {}
            }
            
            if (parts.length > 11 && !parts[11].isEmpty()) {
                try {
                    fund.setYearGrowth(new BigDecimal(parts[11]));
                } catch (Exception e) {}
            }
            
            if (parts.length > 12 && !parts[12].isEmpty()) {
                try {
                    fund.setTotalGrowth(new BigDecimal(parts[12]));
                } catch (Exception e) {}
            }
            
            if (parts.length > 14 && !parts[14].isEmpty()) {
                try {
                    fund.setFundScale(new BigDecimal(parts[14]));
                } catch (Exception e) {}
            }
            
            fund.setNavDate(LocalDate.now());
            fund.setStatus(1);
            fund.setSector(identifySector(parts[1]));
            
            return fund;
        } catch (Exception e) {
            log.warn("解析排行榜条目失败: {}", item, e);
            return null;
        }
    }
    
    private String identifySector(String fundName) {
        if (fundName == null || fundName.isEmpty()) {
            return "其他";
        }
        
        String[][] sectorKeywords = {
            {"科技", "科技"}, {"创新", "科技"}, {"信息", "科技"}, {"互联网", "科技"}, {"数字", "科技"}, {"智能", "科技"}, {"人工智能", "科技"}, {"AI", "科技"}, {"芯片", "科技"}, {"半导体", "科技"}, {"电子", "科技"}, {"计算机", "科技"}, {"软件", "科技"}, {"通信", "科技"}, {"5G", "科技"},
            {"医药", "医药"}, {"医疗", "医药"}, {"生物", "医药"}, {"健康", "医药"}, {"中药", "医药"}, {"创新药", "医药"}, {"疫苗", "医药"}, {"CXO", "医药"},
            {"消费", "消费"}, {"白酒", "消费"}, {"食品", "消费"}, {"饮料", "消费"}, {"家电", "消费"}, {"家居", "消费"}, {"零售", "消费"}, {"品牌", "消费"},
            {"新能源", "新能源"}, {"光伏", "新能源"}, {"风电", "新能源"}, {"锂电", "新能源"}, {"储能", "新能源"}, {"电池", "新能源"}, {"电动车", "新能源"}, {"碳中和", "新能源"}, {"环保", "新能源"},
            {"金融", "金融"}, {"银行", "金融"}, {"证券", "金融"}, {"保险", "金融"}, {"券商", "金融"},
            {"军工", "军工"}, {"国防", "军工"}, {"航天", "军工"}, {"航空", "军工"},
            {"地产", "地产"}, {"房地产", "地产"}, {"物业", "地产"},
            {"基建", "基建"}, {"建筑", "基建"}, {"建材", "基建"}, {"工程", "基建"},
            {"有色", "有色"}, {"黄金", "有色"}, {"金属", "有色"}, {"稀土", "有色"}, {"煤炭", "有色"}, {"钢铁", "有色"}, {"矿业", "有色"},
            {"汽车", "汽车"}, {"新能源车", "汽车"}, {"智能汽车", "汽车"},
            {"农业", "农业"}, {"农机", "农业"}, {"种业", "农业"},
            {"传媒", "传媒"}, {"影视", "传媒"}, {"游戏", "传媒"}, {"教育", "传媒"}, {"文化", "传媒"},
            {"港股", "港股"}, {"恒生", "港股"}, {"沪港深", "港股"},
            {"美股", "美股"}, {"纳斯达克", "美股"}, {"标普", "美股"},
            {"债券", "债券"}, {"债", "债券"}, {"信用", "债券"},
            {"货币", "货币"}, {"现金", "货币"},
            {"沪深300", "沪深300"}, {"中证500", "中证500"}, {"上证50", "上证50"}, {"创业板", "创业板"}, {"科创", "科创板"},
            {"价值", "价值"}, {"蓝筹", "价值"}, {"红利", "红利"}, {"成长", "成长"}
        };
        
        for (String[] pair : sectorKeywords) {
            if (fundName.contains(pair[0])) {
                return pair[1];
            }
        }
        
        if (fundName.contains("混合")) {
            return "混合型";
        }
        if (fundName.contains("股票")) {
            return "股票型";
        }
        if (fundName.contains("指数")) {
            return "指数型";
        }
        
        return "其他";
    }
    
    public int fetchFundRankingTotal(String rankingType, String period) {
        try {
            String sortField = getSortField(rankingType, period);
            String sortType = "decline".equals(rankingType) ? "asc" : "desc";
            
            LocalDate now = LocalDate.now();
            LocalDate startDate = getStartDate(period);
            String sd = startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String ed = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            
            String url = String.format(
                "https://fund.eastmoney.com/data/rankhandler.aspx?op=ph&dt=kf&ft=all&rs=&gs=0&sc=%s&st=%s&sd=%s&ed=%s&qdii=&tabSubtype=,,,,,&pi=1&pn=1&dx=1",
                sortField, sortType, sd, ed
            );
            
            String response = httpGetWithReferer(url, "https://fund.eastmoney.com/data/fundranking.html");
            
            if (response != null) {
                int allRecordsIndex = response.indexOf("allRecords:");
                if (allRecordsIndex > 0) {
                    int startIndex = allRecordsIndex + "allRecords:".length();
                    int endIndex = response.indexOf(",", startIndex);
                    if (endIndex > startIndex) {
                        String totalStr = response.substring(startIndex, endIndex).trim();
                        return Integer.parseInt(totalStr);
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取基金排行榜总数失败", e);
        }
        return 10000;
    }
    
    public java.util.Map<String, java.util.Map<String, BigDecimal>> fetchStockRealtimeData(List<String> stockCodes) {
        return fundDetailApiService.fetchStockRealtimeData(stockCodes);
    }

    public List<FundManager> fetchFundManagers(String fundCode) {
        return fundDetailApiService.fetchFundManagers(fundCode);
    }

    public List<FundHoldings> fetchFundHoldings(String fundCode, int limit) {
        return fundDetailApiService.fetchFundHoldings(fundCode, limit);
    }
}
