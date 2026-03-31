package com.fund.external;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fund.entity.FundHoldings;
import com.fund.entity.FundManager;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class FundDetailApiService {

    @Autowired
    private CloseableHttpClient httpClient;

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

    // --- JS 解析工具方法（从 FundDataApiService 搬移） ---

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
}
