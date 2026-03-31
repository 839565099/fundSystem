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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
