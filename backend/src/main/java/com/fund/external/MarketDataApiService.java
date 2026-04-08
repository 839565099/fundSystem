package com.fund.external;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fund.entity.MarketData;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class MarketDataApiService {

    @Autowired
    private CloseableHttpClient httpClient;

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
            String secid = convertToSecid(marketCode);
            if (secid == null) return historyList;

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

    /**
     * 获取大盘分时走势数据（当日分钟级别）
     * 使用东方财富 trends2/get 接口
     */
    public List<Object[]> fetchMarketTrends(String marketCode) {
        List<Object[]> trendsList = new ArrayList<>();
        try {
            String secid = convertToSecid(marketCode);
            if (secid == null) return trendsList;

            String url = "https://push2his.eastmoney.com/api/qt/stock/trends2/get?secid=" + secid +
                    "&fields1=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f12,f13" +
                    "&fields2=f51,f52,f53,f54,f55,f56,f57,f58&iscr=0&ndays=1";
            String response = httpGet(url);

            if (response != null) {
                JSONObject json = JSONUtil.parseObj(response);
                JSONObject data = json.getJSONObject("data");
                if (data != null) {
                    JSONArray trends = data.getJSONArray("trends");
                    if (trends != null) {
                        for (int i = 0; i < trends.size(); i++) {
                            String trend = trends.getStr(i);
                            String[] parts = trend.split(",");
                            if (parts.length >= 6) {
                                // time, price, avgPrice, volume, amount
                                Object[] item = new Object[5];
                                item[0] = parts[0];                               // 时间 HH:mm
                                item[1] = new BigDecimal(parts[1]);                // 现价
                                item[2] = new BigDecimal(parts[2]);                // 均价
                                item[3] = new BigDecimal(parts[4]);                // 成交量
                                item[4] = new BigDecimal(parts[5]);                // 成交额
                                trendsList.add(item);
                            }
                        }
                        log.info("获取大盘分时数据: {}条 ({})", trendsList.size(), marketCode);
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取大盘分时数据失败: {}", marketCode, e);
        }
        return trendsList;
    }

    /**
     * 获取基金分时走势数据
     * ETF/LOF基金使用股票分时API，普通开放式基金使用估值数据
     */
    public Map<String, Object> fetchFundTrends(String fundCode) {
        Map<String, Object> result = new HashMap<>();
        result.put("fundCode", fundCode);

        // ETF/LOF 基金：通过股票分时API获取真实分钟级数据
        String secid = getFundSecid(fundCode);
        if (secid != null) {
            Map<String, Object> stockResult = fetchStockTrendsForFund(secid);
            List<Map<String, Object>> trends = (List<Map<String, Object>>) stockResult.get("trends");
            if (trends != null && !trends.isEmpty()) {
                result.put("isEtf", true);
                result.put("prevNav", stockResult.get("prevNav"));
                result.put("trends", trends);
                return result;
            }
        }

        // 普通开放式基金：通过fundgz获取实时估值，生成模拟分时曲线
        Map<String, Object> estimate = fetchFundEstimate(fundCode);
        result.put("isEtf", false);
        result.put("prevNav", estimate.get("prevNav"));
        result.put("trends", estimate.get("trends"));
        return result;
    }

    /**
     * 根据基金代码判断是否为ETF/LOF，返回对应的secid
     */
    private String getFundSecid(String fundCode) {
        if (fundCode == null || fundCode.length() < 6) return null;
        // 上海ETF: 51xxxx, 52xxxx, 56xxxx, 58xxxx, 59xxxx
        if (fundCode.startsWith("51") || fundCode.startsWith("52") ||
            fundCode.startsWith("56") || fundCode.startsWith("58") ||
            fundCode.startsWith("59")) {
            return "1." + fundCode;
        }
        // 深圳ETF/LOF: 15xxxx, 16xxxx, 18xxxx, 19xxxx
        if (fundCode.startsWith("15") || fundCode.startsWith("16") ||
            fundCode.startsWith("18") || fundCode.startsWith("19")) {
            return "0." + fundCode;
        }
        return null;
    }

    /**
     * 通过股票分时API获取ETF/LOF基金的分钟级数据
     */
    private Map<String, Object> fetchStockTrendsForFund(String secid) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> trends = new ArrayList<>();
        result.put("trends", trends);

        try {
            String url = "https://push2his.eastmoney.com/api/qt/stock/trends2/get?secid=" + secid +
                    "&fields1=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f12,f13" +
                    "&fields2=f51,f52,f53,f54,f55,f56,f57,f58&iscr=0&ndays=1";
            String response = httpGet(url);

            if (response != null) {
                JSONObject json = JSONUtil.parseObj(response);
                JSONObject data = json.getJSONObject("data");
                if (data != null) {
                    BigDecimal preClose = data.getBigDecimal("preClose");
                    result.put("prevNav", preClose);

                    JSONArray trendsArr = data.getJSONArray("trends");
                    if (trendsArr != null) {
                        for (int i = 0; i < trendsArr.size(); i++) {
                            String trend = trendsArr.getStr(i);
                            String[] parts = trend.split(",");
                            if (parts.length >= 3) {
                                Map<String, Object> point = new HashMap<>();
                                String fullTime = parts[0];
                                // 提取 HH:mm 部分
                                String time = fullTime.length() > 10 ? fullTime.substring(11) : fullTime;
                                BigDecimal price = new BigDecimal(parts[1]);
                                point.put("time", time);
                                point.put("price", price);
                                if (preClose != null && preClose.compareTo(BigDecimal.ZERO) > 0) {
                                    BigDecimal changeRatio = price.subtract(preClose)
                                            .divide(preClose, 6, RoundingMode.HALF_UP)
                                            .multiply(new BigDecimal("100"));
                                    point.put("changeRatio", changeRatio);
                                }
                                trends.add(point);
                            }
                        }
                        log.info("获取ETF分时数据: {}条 (secid={})", trends.size(), secid);
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取ETF分时数据失败: secid={}", secid, e);
        }
        return result;
    }

    /**
     * 通过fundgz获取普通基金的实时估值，并生成模拟分时曲线
     */
    private Map<String, Object> fetchFundEstimate(String fundCode) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> trends = new ArrayList<>();
        result.put("prevNav", BigDecimal.ZERO);
        result.put("trends", trends);

        try {
            String url = "http://fundgz.1234567.com.cn/js/" + fundCode + ".js";
            String response = httpGet(url);

            if (response != null && response.contains("jsonpgz(")) {
                // 解析 JSONP: jsonpgz({...});
                String jsonStr = response.substring(response.indexOf("(") + 1, response.lastIndexOf(")"));
                JSONObject json = JSONUtil.parseObj(jsonStr);

                BigDecimal dwjz = json.getBigDecimal("dwjz");     // 昨日净值
                BigDecimal gsz = json.getBigDecimal("gsz");       // 估算净值
                BigDecimal gszzl = json.getBigDecimal("gszzl");   // 估算涨跌幅(%)
                String gztime = json.getStr("gztime");            // 估算时间

                if (dwjz != null && gsz != null && dwjz.compareTo(BigDecimal.ZERO) > 0) {
                    result.put("prevNav", dwjz);

                    // 解析估算时间，生成从9:30到当前时间的模拟分时数据
                    LocalDateTime estimateTime = null;
                    if (gztime != null && gztime.length() >= 16) {
                        try {
                            estimateTime = LocalDateTime.parse(gztime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                        } catch (Exception ignored) {}
                    }
                    if (estimateTime == null) {
                        estimateTime = LocalDateTime.now();
                    }

                    // 生成模拟分时数据点
                    trends.addAll(generateSimulatedTrends(dwjz, gsz, gszzl, estimateTime));
                    log.info("生成基金估值分时数据: {}条 ({})", trends.size(), fundCode);
                }
            }
        } catch (Exception e) {
            log.warn("获取基金估值数据失败: {}", fundCode, e);
        }
        return result;
    }

    /**
     * 根据估值生成模拟分时走势数据
     * 从开盘到当前估算时间，线性插值+微小随机波动
     */
    private List<Map<String, Object>> generateSimulatedTrends(
            BigDecimal prevNav, BigDecimal estimatedNav, BigDecimal changeRatio, LocalDateTime estimateTime) {
        List<Map<String, Object>> trends = new ArrayList<>();

        // 交易时间：9:30-11:30, 13:00-15:00
        int totalMinutes = 240; // 上午120分钟 + 下午120分钟
        int currentMinute = calculateTradingMinutes(estimateTime);
        if (currentMinute <= 0) currentMinute = 1;
        if (currentMinute > totalMinutes) currentMinute = totalMinutes;

        BigDecimal totalChange = estimatedNav.subtract(prevNav);
        java.util.Random random = new java.util.Random(42); // 固定种子保证稳定性

        for (int i = 0; i <= currentMinute; i++) {
            String time = tradingMinuteToTime(i);
            if (time == null) continue;

            // 线性插值 + 小幅随机波动
            double progress = (double) i / currentMinute;
            BigDecimal basePrice = prevNav.add(totalChange.multiply(new BigDecimal(progress)));

            // 添加微小波动（不超过总变化量的20%）
            double noiseScale = Math.abs(totalChange.doubleValue()) * 0.2;
            if (noiseScale < prevNav.doubleValue() * 0.0001) {
                noiseScale = prevNav.doubleValue() * 0.0001;
            }
            double noise = (random.nextDouble() - 0.5) * 2 * noiseScale;
            BigDecimal price = basePrice.add(BigDecimal.valueOf(noise)).setScale(4, RoundingMode.HALF_UP);

            BigDecimal ratio = price.subtract(prevNav)
                    .divide(prevNav, 6, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));

            Map<String, Object> point = new HashMap<>();
            point.put("time", time);
            point.put("price", price);
            point.put("changeRatio", ratio);
            trends.add(point);
        }

        // 确保最后一个点是实际估值
        if (!trends.isEmpty()) {
            Map<String, Object> lastPoint = trends.get(trends.size() - 1);
            lastPoint.put("price", estimatedNav);
            lastPoint.put("changeRatio", changeRatio);
        }

        return trends;
    }

    /**
     * 计算当前时间对应的交易分钟数（0-240）
     * 上午：9:30(0) - 11:30(120)
     * 下午：13:00(120) - 15:00(240)
     */
    private int calculateTradingMinutes(LocalDateTime time) {
        int hour = time.getHour();
        int minute = time.getMinute();

        if (hour < 9 || (hour == 9 && minute < 30)) return 0;
        if (hour > 15 || (hour == 15 && minute > 0)) return 240;

        if (hour < 12) {
            // 上午：9:30 - 11:30
            int minutesSinceOpen = (hour - 9) * 60 + minute - 30;
            return Math.min(minutesSinceOpen, 120);
        } else {
            // 下午：13:00 - 15:00
            if (hour < 13) return 120; // 午休时间算120
            int minutesSinceAfternoon = (hour - 13) * 60 + minute;
            return 120 + Math.min(minutesSinceAfternoon, 120);
        }
    }

    /**
     * 将交易分钟数转换为时间字符串 (HH:mm)
     */
    private String tradingMinuteToTime(int minute) {
        if (minute < 0 || minute > 240) return null;
        if (minute <= 120) {
            // 上午：9:30 + minute
            int totalMinutes = 9 * 60 + 30 + minute;
            int h = totalMinutes / 60;
            int m = totalMinutes % 60;
            return String.format("%02d:%02d", h, m);
        } else {
            // 下午：13:00 + (minute - 120)
            int totalMinutes = 13 * 60 + (minute - 120);
            int h = totalMinutes / 60;
            int m = totalMinutes % 60;
            return String.format("%02d:%02d", h, m);
        }
    }

    private String convertToSecid(String marketCode) {
        switch (marketCode) {
            case "sh000001": return "1.000001";
            case "sz399001": return "0.399001";
            case "sz399006": return "0.399006";
            case "sh000688": return "1.000688";
            default: return null;
        }
    }

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
}
