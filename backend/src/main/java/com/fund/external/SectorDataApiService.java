package com.fund.external;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fund.vo.SectorHistoryVO;
import com.fund.vo.SectorStockVO;
import com.fund.vo.SectorVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 板块数据API服务 - 从东方财富获取板块数据
 */
@Slf4j
@Service
public class SectorDataApiService {

    @Autowired
    private CloseableHttpClient httpClient;

    /**
     * 使用HttpClient连接池发送HTTP请求
     */
    private String httpGet(String urlStr) {
        try {
            log.info("板块API请求: {}", urlStr);
            HttpGet request = new HttpGet(urlStr);
            request.addHeader("Accept", "*/*");
            request.addHeader("Accept-Language", "zh-CN,zh;q=0.9");
            request.addHeader("Referer", "http://quote.eastmoney.com/center/boardlist.html");

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String result = EntityUtils.toString(response.getEntity(), "UTF-8");
                log.info("板块API响应长度: {}", result.length());
                return result;
            }
        } catch (Exception e) {
            log.error("HTTP请求失败: {}", urlStr, e);
            return null;
        }
    }

    // 板块列表API
    private static final String SECTOR_LIST_URL = "http://push2.eastmoney.com/api/qt/clist/get?" +
            "pn=1&pz=500&po=1&np=1&ut=bd1d9ddb04089700cf9c27f6f7426281&fltt=2&invt=2&fid=f3&fs=%s&fields=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f12,f13,f14,f15,f16,f17,f18,f20,f21,f23,f24,f25,f26,f22,f11,f62,f128,f136,f115,f152";

    // 板块K线API
    private static final String SECTOR_KLINE_URL = "http://push2his.eastmoney.com/api/qt/stock/kline/get?" +
            "secid=90.%s&fields1=f1,f2,f3,f4,f5,f6&fields2=f51,f52,f53,f54,f55,f56,f57&klt=%s&fqt=1&end=20500101&lmt=%s&ut=fa5fd1943c7b386f172d6893dbfba10b";

    // 板块成分股API
    private static final String SECTOR_STOCKS_URL = "http://push2.eastmoney.com/api/qt/clist/get?" +
            "pn=1&pz=100&po=1&np=1&ut=bd1d9ddb04089700cf9c27f6f7426281&fltt=2&invt=2&fid=f3&fs=b:%s&fields=f1,f2,f3,f4,f5,f6,f7,f12,f13,f14";

    // 板块类型映射
    private static final Map<String, String> SECTOR_TYPE_FS = new HashMap<>();
    static {
        SECTOR_TYPE_FS.put("industry", "m:90+t:2");    // 行业
        SECTOR_TYPE_FS.put("concept", "m:90+t:3");     // 概念
        SECTOR_TYPE_FS.put("region", "m:90+t:1");      // 地域
        SECTOR_TYPE_FS.put("all", "m:90+t:2,m:90+t:3,m:90+t:1"); // 全部
    }

    /**
     * 获取板块列表
     */
    public List<SectorVO> fetchSectors(String type) {
        List<SectorVO> result = new ArrayList<>();
        String fs = SECTOR_TYPE_FS.getOrDefault(type, SECTOR_TYPE_FS.get("all"));

        try {
            String url = String.format(SECTOR_LIST_URL, fs);
            log.info("请求板块API: {}", url);
            String response = httpGet(url);
            log.info("板块API响应: {}", response != null ? response.length() : "null");
            if (response == null) {
                log.warn("获取板块数据响应为空");
                return result;
            }
            JSONObject json = JSONUtil.parseObj(response);

            if (json == null || json.getJSONObject("data") == null) {
                log.warn("获取板块数据为空");
                return result;
            }

            JSONArray diff = json.getJSONObject("data").getJSONArray("diff");
            if (diff == null) {
                return result;
            }

            for (int i = 0; i < diff.size(); i++) {
                JSONObject item = diff.getJSONObject(i);
                SectorVO vo = parseSectorVO(item, type);
                if (vo != null) {
                    result.add(vo);
                }
            }
        } catch (Exception e) {
            log.error("获取板块数据失败", e);
        }

        return result;
    }

    /**
     * 解析板块数据
     */
    private SectorVO parseSectorVO(JSONObject item, String defaultType) {
        try {
            SectorVO vo = new SectorVO();
            vo.setCode(item.getStr("f12"));  // 板块代码
            vo.setName(item.getStr("f14"));  // 板块名称

            // 判断板块类型
            String code = vo.getCode();
            if (code != null) {
                if (code.startsWith("BK0") && code.length() == 5) {
                    // 根据代码判断类型
                    int codeNum = Integer.parseInt(code.substring(2));
                    if (codeNum >= 1 && codeNum <= 100) {
                        vo.setType("industry");
                    } else if (codeNum >= 101 && codeNum <= 900) {
                        vo.setType("concept");
                    } else {
                        vo.setType("region");
                    }
                } else {
                    vo.setType(defaultType != null ? defaultType : "concept");
                }
            }

            // 价格和涨跌幅
            vo.setPrice(item.getBigDecimal("f2"));      // 当前点位
            vo.setChange(item.getBigDecimal("f4"));     // 涨跌额
            vo.setChangePercent(item.getBigDecimal("f3")); // 今日涨跌幅

            // 成交额（转换为亿）
            BigDecimal volume = item.getBigDecimal("f6");
            if (volume != null) {
                vo.setVolume(volume.divide(new BigDecimal("100000000"), 2, RoundingMode.HALF_UP));
            }

            // 换手率
            vo.setTurnover(item.getBigDecimal("f8"));

            // 涨跌家数（API未提供准确数据，暂时设为null）
            vo.setUpCount(null);
            vo.setDownCount(null);

            // 领涨股信息
            vo.setLeadingStock(item.getStr("f128"));  // 领涨股名称
            vo.setLeadingStockCode(item.getStr("f140")); // 领涨股代码
            vo.setLeadingStockGrowth(item.getBigDecimal("f136")); // 领涨股涨幅

            // 更新时间
            vo.setUpdateTime(LocalDateTime.now());

            return vo;
        } catch (Exception e) {
            log.warn("解析板块数据失败: {}", item, e);
            return null;
        }
    }

    /**
     * 获取板块历史K线数据
     */
    public SectorHistoryVO fetchSectorHistory(String code, String period) {
        SectorHistoryVO result = new SectorHistoryVO();
        result.setCode(code);

        try {
            // K线类型: 101-日K, 102-周K, 103-月K
            String klt = "101";
            int limit = 30;

            switch (period) {
                case "day":
                    klt = "101";
                    limit = 30;
                    break;
                case "week":
                    klt = "102";
                    limit = 26;
                    break;
                case "month":
                    klt = "103";
                    limit = 24;
                    break;
                case "threeMonth":
                    klt = "101";
                    limit = 90;
                    break;
                case "sixMonth":
                    klt = "101";
                    limit = 180;
                    break;
                case "year":
                    klt = "101";
                    limit = 365;
                    break;
            }

            String url = String.format(SECTOR_KLINE_URL, code, klt, limit);
            String response = httpGet(url);
            if (response == null) {
                return result;
            }
            JSONObject json = JSONUtil.parseObj(response);

            if (json == null || json.getJSONObject("data") == null) {
                return result;
            }

            JSONObject data = json.getJSONObject("data");
            result.setName(data.getStr("name"));

            JSONArray klines = data.getJSONArray("klines");
            if (klines == null) {
                return result;
            }

            List<SectorHistoryVO.KLineItem> history = new ArrayList<>();
            BigDecimal prevClose = null;

            for (int i = 0; i < klines.size(); i++) {
                String klineStr = klines.getStr(i);
                String[] parts = klineStr.split(",");

                if (parts.length >= 7) {
                    SectorHistoryVO.KLineItem item = new SectorHistoryVO.KLineItem();
                    item.setDate(parts[0]);
                    item.setOpen(new BigDecimal(parts[1]));
                    item.setClose(new BigDecimal(parts[2]));
                    item.setHigh(new BigDecimal(parts[3]));
                    item.setLow(new BigDecimal(parts[4]));

                    // 成交额转亿
                    BigDecimal vol = new BigDecimal(parts[5]);
                    item.setVolume(vol.divide(new BigDecimal("100000000"), 2, RoundingMode.HALF_UP));

                    // 计算涨跌幅
                    if (prevClose != null && prevClose.compareTo(BigDecimal.ZERO) != 0) {
                        BigDecimal change = item.getClose().subtract(prevClose);
                        BigDecimal changePercent = change.divide(prevClose, 4, RoundingMode.HALF_UP)
                                .multiply(new BigDecimal("100"));
                        item.setChangePercent(changePercent);
                    } else {
                        item.setChangePercent(BigDecimal.ZERO);
                    }
                    prevClose = item.getClose();

                    history.add(item);
                }
            }

            result.setHistory(history);
        } catch (Exception e) {
            log.error("获取板块历史数据失败: code={}", code, e);
        }

        return result;
    }

    /**
     * 获取板块成分股
     */
    public List<SectorStockVO> fetchSectorStocks(String code) {
        List<SectorStockVO> result = new ArrayList<>();

        try {
            String url = String.format(SECTOR_STOCKS_URL, code);
            String response = httpGet(url);
            if (response == null) {
                return result;
            }
            JSONObject json = JSONUtil.parseObj(response);

            if (json == null || json.getJSONObject("data") == null) {
                return result;
            }

            JSONArray diff = json.getJSONObject("data").getJSONArray("diff");
            if (diff == null) {
                return result;
            }

            BigDecimal maxGrowth = BigDecimal.ZERO;

            for (int i = 0; i < diff.size(); i++) {
                JSONObject item = diff.getJSONObject(i);
                SectorStockVO vo = new SectorStockVO();
                vo.setCode(item.getStr("f12"));
                vo.setName(item.getStr("f14"));
                vo.setChangePercent(item.getBigDecimal("f3"));
                vo.setPrice(item.getBigDecimal("f2"));

                BigDecimal volume = item.getBigDecimal("f6");
                if (volume != null) {
                    vo.setVolume(volume.divide(new BigDecimal("100000000"), 2, RoundingMode.HALF_UP));
                }

                vo.setIsLeading(false);

                // 记录最大涨幅
                if (vo.getChangePercent() != null && vo.getChangePercent().compareTo(maxGrowth) > 0) {
                    maxGrowth = vo.getChangePercent();
                }

                result.add(vo);
            }

            // 标记领涨股
            for (SectorStockVO vo : result) {
                if (vo.getChangePercent() != null && vo.getChangePercent().compareTo(maxGrowth) == 0) {
                    vo.setIsLeading(true);
                    break;
                }
            }
        } catch (Exception e) {
            log.error("获取板块成分股失败: code={}", code, e);
        }

        return result;
    }

    /**
     * 获取板块详情
     */
    public SectorVO fetchSectorDetail(String code) {
        // 先从所有类型中查找
        for (String type : Arrays.asList("industry", "concept", "region")) {
            List<SectorVO> sectors = fetchSectors(type);
            for (SectorVO vo : sectors) {
                if (code.equals(vo.getCode())) {
                    return vo;
                }
            }
        }
        return null;
    }
}
