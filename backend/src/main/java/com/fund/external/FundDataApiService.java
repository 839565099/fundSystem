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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class FundDataApiService {

    private static final javax.net.ssl.SSLSocketFactory TRUST_ALL_SSL_FACTORY;
    private static final javax.net.ssl.HostnameVerifier TRUST_ALL_HOSTNAME_VERIFIER;

    static {
        javax.net.ssl.SSLSocketFactory sslFactory = null;
        javax.net.ssl.HostnameVerifier hostnameVerifier = null;
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
            };
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            sslFactory = sc.getSocketFactory();
            hostnameVerifier = (hostname, session) -> true;
        } catch (Exception e) {
            log.error("初始化SSL失败", e);
        }
        TRUST_ALL_SSL_FACTORY = sslFactory;
        TRUST_ALL_HOSTNAME_VERIFIER = hostnameVerifier;
    }
    
    private String httpGet(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            if (conn instanceof HttpsURLConnection) {
                HttpsURLConnection httpsConn = (HttpsURLConnection) conn;
                if (TRUST_ALL_SSL_FACTORY != null) {
                    httpsConn.setSSLSocketFactory(TRUST_ALL_SSL_FACTORY);
                }
                if (TRUST_ALL_HOSTNAME_VERIFIER != null) {
                    httpsConn.setHostnameVerifier(TRUST_ALL_HOSTNAME_VERIFIER);
                }
            }
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9");
            
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                log.warn("HTTP请求失败, 状态码: {}, URL: {}", responseCode, urlStr);
                return null;
            }
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            reader.close();
            return result.toString();
        } catch (Exception e) {
            log.error("HTTP请求失败: {}", urlStr, e);
            return null;
        }
    }
    
    private String httpPost(String urlStr, String body) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            if (conn instanceof HttpsURLConnection) {
                HttpsURLConnection httpsConn = (HttpsURLConnection) conn;
                if (TRUST_ALL_SSL_FACTORY != null) {
                    httpsConn.setSSLSocketFactory(TRUST_ALL_SSL_FACTORY);
                }
                if (TRUST_ALL_HOSTNAME_VERIFIER != null) {
                    httpsConn.setHostnameVerifier(TRUST_ALL_HOSTNAME_VERIFIER);
                }
            }
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9");

            if (body != null && !body.isEmpty()) {
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.getBytes("UTF-8"));
                    os.flush();
                }
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                log.warn("HTTP POST请求失败, 状态码: {}, URL: {}", responseCode, urlStr);
                return null;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            reader.close();
            return result.toString();
        } catch (Exception e) {
            log.error("HTTP POST请求失败: {}", urlStr, e);
            return null;
        }
    }

    private String httpGetWithReferer(String urlStr, String referer) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            conn.setRequestProperty("Referer", referer);
            conn.setRequestProperty("Cookie", "qgqp_b_id=7d5f5e0a8a8a4a8a8a8a8a8a8a8a8a8a");
            
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                log.warn("HTTP请求失败, 状态码: {}, URL: {}", responseCode, urlStr);
                return null;
            }
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            reader.close();
            return result.toString();
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
        List<MarketData> marketDataList = new ArrayList<>();
        try {
            String url = "https://push2.eastmoney.com/api/qt/ulist.np/get?fltt=2&secids=1.000001,0.399001,0.399006&fields=f2,f3,f4,f5,f6,f7,f15,f16,f17,f18";
            String response = httpGet(url);
            
            if (response != null) {
                JSONObject json = JSONUtil.parseObj(response);
                JSONObject data = json.getJSONObject("data");
                if (data != null) {
                    JSONArray diff = data.getJSONArray("diff");
                    if (diff != null) {
                        String[] names = {"上证指数", "深证成指", "创业板指"};
                        String[] codes = {"sh000001", "sz399001", "sz399006"};
                        
                        for (int i = 0; i < diff.size() && i < 3; i++) {
                            JSONObject item = diff.getJSONObject(i);
                            MarketData marketData = new MarketData();
                            marketData.setMarketType(names[i]);
                            marketData.setMarketCode(codes[i]);
                            
                            if (item.get("f2") != null) {
                                marketData.setCurrentPoint(item.getBigDecimal("f2"));
                            }
                            if (item.get("f3") != null) {
                                marketData.setChangeRatio(item.getBigDecimal("f3"));
                            }
                            if (item.get("f4") != null) {
                                marketData.setChangePoint(item.getBigDecimal("f4"));
                            }
                            if (item.get("f5") != null) {
                                marketData.setVolume(item.getBigDecimal("f5"));
                            }
                            if (item.get("f6") != null) {
                                marketData.setAmount(item.getBigDecimal("f6"));
                            }
                            if (item.get("f15") != null) {
                                marketData.setHighPoint(item.getBigDecimal("f15"));
                            }
                            if (item.get("f16") != null) {
                                marketData.setLowPoint(item.getBigDecimal("f16"));
                            }
                            if (item.get("f17") != null) {
                                marketData.setOpenPoint(item.getBigDecimal("f17"));
                            }
                            if (item.get("f18") != null) {
                                marketData.setPrevClose(item.getBigDecimal("f18"));
                            }
                            marketData.setTradeDate(LocalDate.now());
                            marketData.setUpdateTime(java.time.LocalDateTime.now());
                            
                            marketDataList.add(marketData);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取市场行情失败", e);
        }
        return marketDataList;
    }
    
    public List<Object[]> fetchMarketHistory(String marketCode, String period) {
        List<Object[]> historyList = new ArrayList<>();
        try {
            String secid = "";
            if ("sh000001".equals(marketCode)) {
                secid = "1.000001";
            } else if ("sz399001".equals(marketCode)) {
                secid = "0.399001";
            } else if ("sz399006".equals(marketCode)) {
                secid = "0.399006";
            } else {
                return historyList;
            }
            
            // 根据周期设置klt和lmt参数
            // klt: 101=日K, 102=周K, 103=月K
            int klt = 101;
            int lmt = 120;
            switch (period) {
                case "day":
                    klt = 101;  // 日K
                    lmt = 120;  // 最近120个交易日
                    break;
                case "week":
                    klt = 102;  // 周K
                    lmt = 200;  // 最近200周（约4年）
                    break;
                case "month":
                    klt = 103;  // 月K
                    lmt = 300;  // 最近300月（覆盖2000年至今）
                    break;
                default:
                    klt = 101;
                    lmt = 120;
            }

            String url = "https://push2his.eastmoney.com/api/qt/stock/kline/get?secid=" + secid +
                        "&fields1=f1,f2,f3,f4,f5,f6&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61" +
                        "&klt=" + klt + "&fqt=1&end=20500101&lmt=" + lmt;
            String response = httpGet(url);
            
            if (response != null) {
                JSONObject json = JSONUtil.parseObj(response);
                JSONObject data = json.getJSONObject("data");
                if (data != null) {
                    JSONArray klines = data.getJSONArray("klines");
                    if (klines != null) {
                        for (int i = 0; i < klines.size(); i++) {
                            String kline = klines.getStr(i);
                            String[] parts = kline.split(",");
                            if (parts.length >= 9) {
                                Object[] item = new Object[7];
                                item[0] = parts[0];
                                item[1] = new BigDecimal(parts[1]);
                                item[2] = new BigDecimal(parts[2]);
                                item[3] = new BigDecimal(parts[3]);
                                item[4] = new BigDecimal(parts[4]);
                                item[5] = new BigDecimal(parts[5]);
                                item[6] = new BigDecimal(parts[8]);
                                historyList.add(item);
                            }
                        }
                        log.info("获取大盘历史数据: {}条", historyList.size());
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取大盘历史数据失败: {}", marketCode, e);
        }
        return historyList;
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
        java.util.Map<String, java.util.Map<String, BigDecimal>> result = new java.util.HashMap<>();
        if (stockCodes == null || stockCodes.isEmpty()) {
            return result;
        }

        try {
            StringBuilder secids = new StringBuilder();
            for (String code : stockCodes) {
                if (code != null && !code.isEmpty()) {
                    if (secids.length() > 0) {
                        secids.append(",");
                    }
                    if (code.startsWith("6")) {
                        secids.append("1.").append(code);  // 沪市
                    } else if (code.length() == 5 && code.startsWith("0")) {
                        secids.append("116.").append(code); // 港股
                    } else {
                        secids.append("0.").append(code);  // 深市/创业板
                    }
                }
            }

            if (secids.length() == 0) {
                return result;
            }

            String url = "https://push2.eastmoney.com/api/qt/ulist.np/get?fltt=2&secids=" + secids.toString() +
                        "&fields=f2,f3,f12,f14";
            String response = httpGet(url);

            if (response != null) {
                JSONObject json = JSONUtil.parseObj(response);
                JSONObject data = json.getJSONObject("data");
                if (data != null) {
                    JSONArray diff = data.getJSONArray("diff");
                    if (diff != null) {
                        for (int i = 0; i < diff.size(); i++) {
                            JSONObject item = diff.getJSONObject(i);
                            String code = item.getStr("f12");
                            if (code != null) {
                                java.util.Map<String, BigDecimal> stockData = new java.util.HashMap<>();
                                if (item.get("f2") != null) {
                                    stockData.put("price", item.getBigDecimal("f2"));
                                }
                                if (item.get("f3") != null) {
                                    stockData.put("dayGrowth", item.getBigDecimal("f3"));
                                }
                                result.put(code, stockData);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取股票实时数据失败", e);
        }
        return result;
    }

    /**
     * 从东方财富获取基金经理信息
     * 优先使用结构化 JSON API，失败时降级到 pingzhongdata.js
     */
    public List<FundManager> fetchFundManagers(String fundCode) {
        List<FundManager> managers = new ArrayList<>();
        try {
            managers = fetchFundManagersFromApi(fundCode);
        } catch (Exception e) {
            log.warn("从结构化API获取基金经理失败，降级到pingzhongdata.js: {}", fundCode, e);
        }

        if (managers.isEmpty()) {
            managers = fetchFundManagersFromPingzhongdata(fundCode);
        }

        log.info("获取基金经理信息: {}条, 基金代码: {}", managers.size(), fundCode);
        return managers;
    }

    /**
     * 通过天天基金结构化 API 获取基金经理信息
     */
    private List<FundManager> fetchFundManagersFromApi(String fundCode) {
        List<FundManager> managers = new ArrayList<>();

        // 第一步：获取基金的经理列表
        String listUrl = "https://fundmobapi.eastmoney.com/FundMNewApi/FundMNMangerList?FCODE=" + fundCode + "&deviceid=wap&plat=Wap&product=EFund&version=2.0.0";
        log.info("从结构化API获取基金经理列表: {}", listUrl);
        String listResponse = httpGet(listUrl);

        if (listResponse == null || listResponse.isEmpty()) {
            return managers;
        }

        JSONObject listJson = JSONUtil.parseObj(listResponse);
        JSONArray datas = listJson.getJSONArray("Datas");
        if (datas == null || datas.isEmpty()) {
            return managers;
        }

        for (int i = 0; i < datas.size(); i++) {
            JSONObject item = datas.getJSONObject(i);
            if (item == null) continue;

            // 只获取当前在任经理（离任日期为"--"或为空）
            String lempDate = item.getStr("LEMPDATE");
            if (lempDate != null && !lempDate.equals("--") && !lempDate.isEmpty()) {
                log.debug("跳过已离任经理: {}, 离任日期: {}", item.getStr("MGRNAME"), lempDate);
                continue;
            }

            String managerId = item.getStr("MGRID");
            String managerName = item.getStr("MGRNAME");
            if (managerName == null || managerName.isEmpty()) continue;

            FundManager manager = new FundManager();
            manager.setManagerId(managerId != null ? managerId : fundCode + "_" + Math.abs(managerName.hashCode()));
            manager.setManagerName(managerName);
            manager.setPhoto(item.getStr("NEWPHOTOURL"));
            manager.setCompany(firstNonBlank(item, "JJGS", "COMPANY"));

            // 任职开始日期
            String fempDate = firstNonBlank(item, "FEMPDATE", "STARTDATE");
            if (fempDate != null && fempDate.length() >= 10) {
                try {
                    manager.setStartDate(LocalDate.parse(fempDate.substring(0, 10)));
                } catch (Exception e) {
                    log.debug("解析经理任职日期失败: {}", fempDate);
                }
            }

            // 任职回报
            if (item.get("PENAVGROWTH") != null) {
                try {
                    manager.setBestReturn(new BigDecimal(item.getStr("PENAVGROWTH")));
                } catch (Exception ignored) {
                }
            }
            if (manager.getBestReturn() == null) {
                BigDecimal fallbackReturn = parseBigDecimal(firstNonBlank(item, "YIELDSE", "YIELD", "RETRN"));
                manager.setBestReturn(fallbackReturn);
            }

            // 第二步：获取经理详细信息
            if (managerId != null && !managerId.isEmpty()) {
                try {
                    String detailUrl = "https://fundztapi.eastmoney.com/FundSpecialApiNew/FundMSNMangerInfo";
                    String detailBody = "FCODE=" + managerId + "&deviceid=wap&plat=Wap&product=EFund&version=2.0.0";
                    String detailResponse = httpPost(detailUrl, detailBody);

                    if (detailResponse != null && !detailResponse.isEmpty()) {
                        JSONObject detailJson = JSONUtil.parseObj(detailResponse);
                        JSONObject detailData = detailJson.getJSONObject("Datas");
                        if (detailData != null) {
                            String company = firstNonBlank(detailData, "JJGS", "COMPANY");
                            if (company != null && !company.isEmpty()) {
                                manager.setCompany(company);
                            }

                            // 从业年限
                            Integer totalDays = parseInteger(firstNonBlank(detailData, "TOTALDAYS", "WORKDAYS"));
                            if (totalDays != null && totalDays > 0) {
                                manager.setWorkYears(totalDays / 365);
                            }
                            if (manager.getWorkYears() == null) {
                                Integer years = parseInteger(firstNonBlank(detailData, "WORKYEAR", "WORKYEARS"));
                                if (years != null && years >= 0) {
                                    manager.setWorkYears(years);
                                }
                            }

                            // 管理规模（转亿元）
                            BigDecimal netNav = parseBigDecimal(firstNonBlank(detailData, "NETNAV"));
                            if (netNav != null) {
                                manager.setTotalAsset(netNav.divide(new BigDecimal("100000000"), 2, RoundingMode.HALF_UP));
                            } else {
                                BigDecimal totalAsset = parseBigDecimal(firstNonBlank(detailData, "TOTALSCALE", "ASSETS"));
                                if (totalAsset != null) {
                                    manager.setTotalAsset(totalAsset);
                                }
                            }

                            String resume = firstNonBlank(detailData, "RESUME", "DESC", "INTRO");
                            if (resume != null && !resume.isEmpty()) {
                                manager.setResume(resume);
                            }

                            // 投资理念
                            String investIdea = firstNonBlank(detailData, "INVESTMENTIDEAR", "INVESTMENTIDEA");
                            if (investIdea == null || investIdea.isEmpty()) {
                                investIdea = detailData.getStr("INVESTMENTMETHOD");
                            }
                            if (investIdea != null && !investIdea.isEmpty()) {
                                manager.setInvestmentIdea(investIdea);
                            }

                            // 当前管理基金数量（不持久化）
                            if (detailData.get("FCOUNT") != null) {
                                try {
                                    manager.setFundCount(detailData.getInt("FCOUNT"));
                                } catch (Exception ignored) {
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.debug("获取经理详情失败: {}", managerId, e);
                }
            }

            managers.add(manager);
            log.info("从API获取基金经理: {} ({})", managerName, manager.getCompany());
        }

        // 从 Expansion 补充在任经理的额外信息（投资理念等）
        JSONArray expansion = listJson.getJSONArray("Expansion");
        if (expansion != null) {
            for (int j = 0; j < expansion.size(); j++) {
                JSONObject exp = expansion.getJSONObject(j);
                if (exp == null) continue;
                String expMgrId = exp.getStr("MGRID");
                for (FundManager mgr : managers) {
                    if (mgr.getManagerId().equals(expMgrId)) {
                        // 补充投资理念（如果详情API没拿到）
                        if (mgr.getInvestmentIdea() == null || mgr.getInvestmentIdea().isEmpty()) {
                            String idea = exp.getStr("INVESTMENTIDEAR");
                            if (idea != null && !idea.isEmpty()) {
                                mgr.setInvestmentIdea(idea);
                            }
                        }
                    }
                }
            }
        }

        return managers;
    }

    /**
     * 降级方案：从 pingzhongdata.js 解析基金经理
     */
    private List<FundManager> fetchFundManagersFromPingzhongdata(String fundCode) {
        List<FundManager> managers = new ArrayList<>();
        try {
            String url = "https://fund.eastmoney.com/pingzhongdata/" + fundCode + ".js";
            log.info("降级: 从pingzhongdata.js获取基金经理: {}", url);
            String response = httpGet(url);

            if (response != null && response.length() > 100) {
                String managerData = extractJsArray(response, "Data_currentFundManager");

                if (managerData != null && managerData.length() > 10) {
                    JSONArray managerArray = JSONUtil.parseArray(managerData);
                    if (managerArray != null) {
                        for (int i = 0; i < managerArray.size(); i++) {
                            JSONObject item = managerArray.getJSONObject(i);
                            if (item == null) continue;
                            String managerId = item.getStr("id");
                            String managerName = item.getStr("name");
                            if (managerName == null || managerName.isEmpty()) continue;

                            FundManager manager = new FundManager();
                            manager.setManagerId(managerId != null ? managerId : fundCode + "_" + Math.abs(managerName.hashCode()));
                            manager.setManagerName(managerName);

                            String workTime = item.getStr("workTime");
                            if (workTime != null) {
                                manager.setResume("任职时间: " + workTime);
                            }

                            JSONObject power = item.getJSONObject("power");
                            if (power != null && power.get("avr") != null) {
                                try {
                                    manager.setBestReturn(new BigDecimal(power.getStr("avr")));
                                } catch (Exception ignored) {
                                }
                            }

                            managers.add(manager);
                            log.info("从pingzhongdata获取基金经理: {}", managerName);
                        }
                    }
                }

                if (managers.isEmpty()) {
                    String managerName = extractJsVar(response, "fund_manager");
                    if (managerName != null && !managerName.isEmpty()) {
                        FundManager manager = new FundManager();
                        manager.setManagerId(fundCode + "_" + Math.abs(managerName.hashCode()));
                        manager.setManagerName(managerName);
                        managers.add(manager);
                    }
                }
            }
        } catch (Exception e) {
            log.error("从pingzhongdata获取基金经理失败: {}", fundCode, e);
        }
        return managers;
    }

    /**
     * 从东方财富获取基金持仓数据
     * 优先使用 FundArchivesDatas.aspx 结构化接口，失败时降级到 pingzhongdata.js
     */
    public List<FundHoldings> fetchFundHoldings(String fundCode, int limit) {
        List<FundHoldings> holdings = new ArrayList<>();
        try {
            holdings = fetchFundHoldingsFromArchives(fundCode, limit);
        } catch (Exception e) {
            log.warn("从FundArchivesDatas获取持仓失败，降级到pingzhongdata.js: {}", fundCode, e);
        }

        if (holdings.isEmpty()) {
            holdings = fetchFundHoldingsFromPingzhongdata(fundCode, limit);
        }

        log.info("获取基金持仓数据: {}条, 基金代码: {}", holdings.size(), fundCode);
        return holdings;
    }

    /**
     * 通过 FundArchivesDatas.aspx 接口获取持仓数据
     */
    private List<FundHoldings> fetchFundHoldingsFromArchives(String fundCode, int limit) {
        List<FundHoldings> holdings = new ArrayList<>();
        String url = "https://fundf10.eastmoney.com/FundArchivesDatas.aspx?type=jjcc&code=" + fundCode
                + "&topline=" + limit + "&year=&month=&rt=" + System.currentTimeMillis();
        log.info("从FundArchivesDatas获取持仓: {}", url);
        String response = httpGetWithReferer(url, "https://fundf10.eastmoney.com/ccmx_" + fundCode + ".html");

        if (response == null || response.length() < 100) {
            return holdings;
        }
        String htmlContent = extractArchivesHtmlContent(response);
        if (htmlContent == null || htmlContent.isEmpty()) {
            return holdings;
        }

        Document doc = Jsoup.parse(htmlContent);
        Element firstBox = doc.selectFirst("div.box div.boxitem");
        if (firstBox == null) {
            firstBox = doc.body();
        }

        LocalDate reportDate = LocalDate.now();
        Element reportDateEl = firstBox.selectFirst("label.right font.px12");
        if (reportDateEl != null) {
            try {
                reportDate = LocalDate.parse(reportDateEl.text().trim());
            } catch (Exception ignored) {
            }
        }

        Elements rows = firstBox.select("tbody tr");
        for (Element row : rows) {
            if (holdings.size() >= limit) {
                break;
            }

            Elements tds = row.select("td");
            if (tds.size() < 5) {
                continue;
            }

            FundHoldings holding = new FundHoldings();
            holding.setFundCode(fundCode);
            holding.setReportDate(reportDate);
            holding.setHoldingType("股票");

            String codeText = tds.size() > 1 ? tds.get(1).text() : "";
            String nameText = tds.size() > 2 ? tds.get(2).text() : "";
            String stockCode = extractStockCode(codeText);
            String stockName = cleanCellText(nameText);

            // 兼容字段错位场景
            if (!isStockCode(stockCode) && isStockCode(nameText)) {
                stockCode = extractStockCode(nameText);
                stockName = cleanCellText(codeText);
            }

            if (!isStockCode(stockCode)) {
                continue;
            }
            holding.setStockCode(stockCode);
            holding.setStockName(stockName);

            String ratioRaw = extractRatioTextFromCells(tds);
            BigDecimal ratio = parseBigDecimal(cleanNumber(ratioRaw));
            if (ratio != null) {
                holding.setHoldingRatio(ratio);
            }

            String sharesRaw = tds.get(tds.size() - 2).text();
            BigDecimal shares = parseBigDecimal(cleanNumber(sharesRaw));
            if (shares != null) {
                holding.setHoldingShares(shares);
            }

            String valueRaw = tds.get(tds.size() - 1).text();
            BigDecimal value = parseBigDecimal(cleanNumber(valueRaw));
            if (value != null) {
                holding.setHoldingValue(value);
            }

            holdings.add(holding);
            log.debug("从Archives获取持仓: {} - {} 占比{}%", holding.getStockCode(), holding.getStockName(), holding.getHoldingRatio());
        }

        log.info("从FundArchivesDatas获取持仓: {}条", holdings.size());
        return holdings;
    }

    private String extractArchivesHtmlContent(String response) {
        Matcher matcher = Pattern.compile("content:\"(.*)\",arryear:", Pattern.DOTALL).matcher(response);
        if (!matcher.find()) {
            return null;
        }
        String content = matcher.group(1);
        return content
                .replace("\\\"", "\"")
                .replace("\\/", "/")
                .replace("\\n", "")
                .replace("\\r", "");
    }

    private String extractRatioTextFromCells(Elements tds) {
        int ratioIndex = tds.size() - 3;
        if (ratioIndex >= 0) {
            String candidate = tds.get(ratioIndex).text();
            if (candidate.contains("%")) {
                return candidate;
            }
        }
        for (Element td : tds) {
            String text = td.text();
            if (text.contains("%")) {
                return text;
            }
        }
        return "";
    }

    private String extractStockCode(String text) {
        if (text == null) return "";
        Matcher matcher = Pattern.compile("(\\d{6})").matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private boolean isStockCode(String text) {
        return text != null && text.matches("\\d{6}");
    }

    private String cleanCellText(String text) {
        if (text == null) return "";
        return text.replaceAll("\\s+", "").trim();
    }

    private String cleanNumber(String raw) {
        if (raw == null) return "";
        return raw.replace("%", "").replace(",", "").replace("，", "").trim();
    }

    private BigDecimal parseBigDecimal(String text) {
        if (text == null || text.isEmpty() || "---".equals(text) || "--".equals(text)) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Integer parseInteger(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        Matcher matcher = Pattern.compile("(-?\\d+)").matcher(text);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (Exception ignored) {
            return null;
        }
    }

    private String firstNonBlank(JSONObject json, String... keys) {
        if (json == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            String value = json.getStr(key);
            if (value != null && !value.trim().isEmpty() && !"null".equalsIgnoreCase(value.trim())) {
                return value.trim();
            }
        }
        return null;
    }

    /**
     * 降级方案：从 pingzhongdata.js 解析持仓数据
     */
    private List<FundHoldings> fetchFundHoldingsFromPingzhongdata(String fundCode, int limit) {
        List<FundHoldings> holdings = new ArrayList<>();
        try {
            String url = "https://fund.eastmoney.com/pingzhongdata/" + fundCode + ".js";
            log.info("降级: 从pingzhongdata.js获取基金持仓: {}", url);
            String response = httpGet(url);

            if (response != null && response.length() > 100) {
                String stockData = extractJsArray(response, "Data_stockHoldings");
                if (stockData != null && stockData.length() > 10) {
                    JSONArray stockArray = JSONUtil.parseArray(stockData);
                    if (stockArray != null) {
                        for (int i = 0; i < stockArray.size() && holdings.size() < limit; i++) {
                            JSONObject item = stockArray.getJSONObject(i);
                            if (item == null) continue;

                            FundHoldings holding = new FundHoldings();
                            holding.setFundCode(fundCode);
                            holding.setReportDate(LocalDate.now());
                            holding.setHoldingType("股票");

                            String stockCode = item.getStr("code");
                            if (stockCode != null && !stockCode.isEmpty()) {
                                holding.setStockCode(stockCode);
                            }

                            String stockName = item.getStr("name");
                            if (stockName != null && !stockName.isEmpty()) {
                                holding.setStockName(stockName);
                            }

                            if (item.get("ratio") != null) {
                                try {
                                    holding.setHoldingRatio(item.getBigDecimal("ratio"));
                                } catch (Exception ignored) {
                                }
                            }

                            if (holding.getStockCode() != null && !holding.getStockCode().isEmpty()) {
                                holdings.add(holding);
                            }
                        }
                    }
                }

                if (holdings.isEmpty()) {
                    holdings = fetchFundHoldingsFromDetailPage(fundCode, limit);
                }
            }
        } catch (Exception e) {
            log.error("从pingzhongdata获取持仓失败: {}", fundCode, e);
        }
        return holdings;
    }

    private List<FundHoldings> fetchFundHoldingsFromDetailPage(String fundCode, int limit) {
        List<FundHoldings> holdings = new ArrayList<>();
        try {
            String url = "https://fundf10.eastmoney.com/ccmx_" + fundCode + ".html";
            log.info("从基金详情页获取持仓: {}", url);
            String response = httpGetWithReferer(url, "https://fund.eastmoney.com/");

            if (response != null && response.length() > 1000) {
                Pattern rowPattern = Pattern.compile(
                    "<td[^>]*>\\s*(\\d+)\\s*</td>\\s*" +
                    "<td[^>]*>\\s*<a[^>]*>([^<]+)</a>\\s*</td>\\s*" +
                    "<td[^>]*>\\s*<a[^>]*>([^<]+)</a>\\s*</td>\\s*" +
                    "<td[^>]*>([^<]*)</td>\\s*" +
                    "<td[^>]*>([^<]*)</td>\\s*" +
                    "<td[^>]*>([^<]*)</td>",
                    Pattern.DOTALL
                );
                
                Matcher matcher = rowPattern.matcher(response);
                int count = 0;
                
                while (matcher.find() && count < limit) {
                    try {
                        String stockCode = matcher.group(3).trim();
                        String stockName = matcher.group(2).trim();
                        String ratioStr = matcher.group(6).trim();
                        
                        if (stockCode != null && !stockCode.isEmpty() && 
                            stockName != null && !stockName.isEmpty()) {
                            
                            FundHoldings holding = new FundHoldings();
                            holding.setFundCode(fundCode);
                            holding.setStockCode(stockCode);
                            holding.setStockName(stockName);
                            holding.setReportDate(LocalDate.now());
                            holding.setHoldingType("股票");
                            
                            if (ratioStr != null && !ratioStr.isEmpty() && !"--".equals(ratioStr)) {
                                try {
                                    ratioStr = ratioStr.replace("%", "").trim();
                                    holding.setHoldingRatio(new BigDecimal(ratioStr));
                                } catch (Exception e) {
                                }
                            }
                            
                            holdings.add(holding);
                            count++;
                            log.debug("从详情页获取持仓: {} - {}", stockCode, stockName);
                        }
                    } catch (Exception e) {
                        log.warn("解析持仓行失败", e);
                    }
                }
                
                log.info("从基金详情页获取持仓: {}条", holdings.size());
            }
        } catch (Exception e) {
            log.error("从基金详情页获取持仓失败: {}", fundCode, e);
        }
        return holdings;
    }
}
