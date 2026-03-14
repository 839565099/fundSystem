package com.fund.service.impl;

import com.fund.dto.CorrelationRequest;
import com.fund.dto.DIPSimRequest;
import com.fund.entity.Fund;
import com.fund.entity.FundNavHistory;
import com.fund.mapper.FundMapper;
import com.fund.mapper.FundNavHistoryMapper;
import com.fund.service.AnalyticsService;
import com.fund.service.FundService;
import com.fund.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 基金分析服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final FundService fundService;
    private final FundMapper fundMapper;
    private final FundNavHistoryMapper navHistoryMapper;

    // 无风险利率（年化）
    private static final BigDecimal RISK_FREE_RATE = new BigDecimal("0.03");

    @Override
    public SharpeRatioVO calculateSharpeRatio(String fundCode, String period) {
        List<FundNavHistory> history = getNavHistory(fundCode, period);
        if (history.size() < 2) {
            return null;
        }

        // 计算日收益率
        List<BigDecimal> returns = calculateDailyReturns(history);

        // 计算平均收益率
        BigDecimal avgReturn = returns.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(returns.size()), 10, RoundingMode.HALF_UP);

        // 计算标准差（波动率）
        BigDecimal stdDev = calculateStandardDeviation(returns, avgReturn);

        // 日无风险利率
        BigDecimal dailyRiskFreeRate = RISK_FREE_RATE.divide(BigDecimal.valueOf(252), 10, RoundingMode.HALF_UP);

        // 夏普比率 = (平均收益 - 无风险利率) / 标准差
        BigDecimal sharpeRatio = BigDecimal.ZERO;
        if (stdDev.compareTo(BigDecimal.ZERO) > 0) {
            sharpeRatio = avgReturn.subtract(dailyRiskFreeRate)
                    .divide(stdDev, 10, RoundingMode.HALF_UP);
        }

        // 年化夏普比率
        BigDecimal annualizedSharpe = sharpeRatio.multiply(BigDecimal.valueOf(Math.sqrt(252)));

        Fund fund = fundService.getByFundCode(fundCode);
        SharpeRatioVO vo = new SharpeRatioVO();
        vo.setFundCode(fundCode);
        vo.setFundName(fund != null ? fund.getFundName() : null);
        vo.setPeriod(period);
        vo.setSharpeRatio(annualizedSharpe.setScale(4, RoundingMode.HALF_UP));
        vo.setAvgReturn(avgReturn.multiply(BigDecimal.valueOf(252)).setScale(4, RoundingMode.HALF_UP));
        vo.setVolatility(stdDev.multiply(BigDecimal.valueOf(Math.sqrt(252))).setScale(4, RoundingMode.HALF_UP));
        vo.setRiskFreeRate(RISK_FREE_RATE);

        return vo;
    }

    @Override
    public MaxDrawdownVO calculateMaxDrawdown(String fundCode, String period) {
        List<FundNavHistory> history = getNavHistory(fundCode, period);
        if (history.isEmpty()) {
            return null;
        }

        // 按日期排序
        history.sort(Comparator.comparing(FundNavHistory::getNavDate));

        BigDecimal maxNav = BigDecimal.ZERO;
        BigDecimal maxDrawdown = BigDecimal.ZERO;
        LocalDate peakDate = null;
        LocalDate troughDate = null;
        LocalDate currentPeakDate = null;

        for (FundNavHistory nav : history) {
                BigDecimal currentNav = nav.getNav();
                if (currentNav == null) continue;

                if (currentNav.compareTo(maxNav) > 0) {
                    maxNav = currentNav;
                    currentPeakDate = nav.getNavDate();
                } else if (maxNav.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal drawdown = maxNav.subtract(currentNav)
                            .divide(maxNav, 4, RoundingMode.HALF_UP);
                    if (drawdown.compareTo(maxDrawdown) > 0) {
                        maxDrawdown = drawdown;
                        peakDate = currentPeakDate;
                        troughDate = nav.getNavDate();
                    }
                }
            }

        // 计算恢复天数
        Long recoveryDays = null;
        if (peakDate != null && troughDate != null) {
                recoveryDays = ChronoUnit.DAYS.between(peakDate, troughDate);
            }

        Fund fund = fundService.getByFundCode(fundCode);
        MaxDrawdownVO vo = new MaxDrawdownVO();
        vo.setFundCode(fundCode);
        vo.setFundName(fund != null ? fund.getFundName() : null);
        vo.setPeriod(period);
        vo.setMaxDrawdown(maxDrawdown.multiply(new BigDecimal("100")).setScale(4, RoundingMode.HALF_UP));
        vo.setPeakDate(peakDate);
        vo.setTroughDate(troughDate);
        vo.setRecoveryDays(recoveryDays);

        return vo;
    }

    @Override
    public VolatilityVO calculateVolatility(String fundCode, String period) {
        List<FundNavHistory> history = getNavHistory(fundCode, period);
        if (history.size() < 2) {
            return null;
        }

        List<BigDecimal> returns = calculateDailyReturns(history);

        BigDecimal avgReturn = returns.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(returns.size()), 10, RoundingMode.HALF_UP);

        BigDecimal dailyVolatility = calculateStandardDeviation(returns, avgReturn);

        // 年化波动率
        BigDecimal annualizedVolatility = dailyVolatility.multiply(BigDecimal.valueOf(Math.sqrt(252)));

        // 下行波动率（只计算负收益）
        List<BigDecimal> negativeReturns = returns.stream()
                .filter(r -> r.compareTo(BigDecimal.ZERO) < 0)
                .collect(Collectors.toList());

        BigDecimal downsideVolatility = BigDecimal.ZERO;
        if (!negativeReturns.isEmpty()) {
            BigDecimal avgNegReturn = negativeReturns.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(negativeReturns.size()), 10, RoundingMode.HALF_UP);
            downsideVolatility = calculateStandardDeviation(negativeReturns, avgNegReturn);
        }

        Fund fund = fundService.getByFundCode(fundCode);
        VolatilityVO vo = new VolatilityVO();
        vo.setFundCode(fundCode);
        vo.setFundName(fund != null ? fund.getFundName() : null);
        vo.setPeriod(period);
        vo.setDailyVolatility(dailyVolatility.setScale(4, RoundingMode.HALF_UP));
        vo.setAnnualizedVolatility(annualizedVolatility.setScale(4, RoundingMode.HALF_UP));
        vo.setDownsideVolatility(downsideVolatility.setScale(4, RoundingMode.HALF_UP));

        return vo;
    }

    @Override
    public CorrelationMatrixVO calculateCorrelationMatrix(CorrelationRequest request) {
        List<String> fundCodes = request.getFundCodes();
        if (fundCodes == null || fundCodes.size() < 2) {
            return null;
        }

        // 获取每只基金的收益率序列
        Map<String, List<BigDecimal>> returnsMap = new HashMap<>();
        int minLength = Integer.MAX_VALUE;

        for (String code : fundCodes) {
                List<FundNavHistory> history = getNavHistory(code, request.getPeriod());
                if (!history.isEmpty()) {
                    List<BigDecimal> returns = calculateDailyReturns(history);
                    returnsMap.put(code, returns);
                    minLength = Math.min(minLength, returns.size());
                }
            }

        if (returnsMap.size() < 2) {
            return null;
        }

        // 截断到相同长度
        for (String code : fundCodes) {
                List<BigDecimal> returns = returnsMap.get(code);
                if (returns != null && returns.size() > minLength) {
                    returnsMap.put(code, returns.subList(returns.size() - minLength, returns.size()));
                }
            }

        // 计算相关性矩阵
        int n = fundCodes.size();
        List<List<BigDecimal>> matrix = new ArrayList<>();
        List<String> fundNames = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            List<BigDecimal> row = new ArrayList<>();
            String codeA = fundCodes.get(i);
            Fund fundA = fundService.getByFundCode(codeA);
            fundNames.add(fundA != null ? fundA.getFundName() : codeA);

            for (int j = 0; j < n; j++) {
                String codeB = fundCodes.get(j);
                BigDecimal correlation = calculateCorrelation(
                        returnsMap.get(codeA), returnsMap.get(codeB));
                row.add(correlation);
            }
            matrix.add(row);
        }

        CorrelationMatrixVO vo = new CorrelationMatrixVO();
        vo.setFundCodes(fundCodes);
        vo.setFundNames(fundNames);
        vo.setCorrelationMatrix(matrix);

        return vo;
    }

    @Override
    public DIPSimResultVO simulateDIP(DIPSimRequest request) {
        String fundCode = request.getFundCode();
        List<FundNavHistory> history = getNavHistory(fundCode, "all");

        if (history.isEmpty()) {
            return null;
        }

        // 按日期排序
        history.sort(Comparator.comparing(FundNavHistory::getNavDate));

        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate() != null ? request.getEndDate() : LocalDate.now();
        BigDecimal amount = request.getAmount();
        String frequency = request.getFrequency();

        BigDecimal totalInvested = BigDecimal.ZERO;
        BigDecimal totalShares = BigDecimal.ZERO;
        int investmentCount = 0;
        List<DIPSimResultVO.MonthlyRecord> records = new ArrayList<>();

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
                // 找到最近一个交易日的净值
                FundNavHistory nav = findNearestNav(history, currentDate);
                if (nav != null && nav.getNav() != null && nav.getNav().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal shares = amount.divide(nav.getNav(), 4, RoundingMode.HALF_UP);
                    totalInvested = totalInvested.add(amount);
                    totalShares = totalShares.add(shares);
                    investmentCount++;

                    DIPSimResultVO.MonthlyRecord record = new DIPSimResultVO.MonthlyRecord();
                    record.setMonth(currentDate.toString());
                    record.setInvested(amount);
                    record.setShares(shares);
                    record.setNav(nav.getNav());
                    records.add(record);
                }

                // 根据频率计算下一个投资日期
                switch (frequency) {
                    case "weekly":
                        currentDate = currentDate.plusWeeks(1);
                        break;
                    case "biweekly":
                        currentDate = currentDate.plusWeeks(2);
                        break;
                    default: // monthly
                        currentDate = currentDate.plusMonths(1);
                }
            }

        // 计算当前市值
        BigDecimal currentNav = history.get(history.size() - 1).getNav();
        BigDecimal currentValue = totalShares.multiply(currentNav);
        BigDecimal totalProfit = currentValue.subtract(totalInvested);

        BigDecimal totalReturn = BigDecimal.ZERO;
        if (totalInvested.compareTo(BigDecimal.ZERO) > 0) {
            totalReturn = totalProfit.divide(totalInvested, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        Fund fund = fundService.getByFundCode(fundCode);
        DIPSimResultVO vo = new DIPSimResultVO();
        vo.setFundCode(fundCode);
        vo.setFundName(fund != null ? fund.getFundName() : null);
        vo.setTotalInvested(totalInvested);
        vo.setCurrentValue(currentValue);
        vo.setTotalProfit(totalProfit);
        vo.setTotalReturn(totalReturn);
        vo.setTotalShares(investmentCount);
        vo.setAvgCost(totalInvested.divide(totalShares, 4, RoundingMode.HALF_UP));
        vo.setMonthlyRecords(records);

        return vo;
    }

    @Override
    public FundAnalyticsReportVO getAnalyticsReport(String fundCode) {
        Fund fund = fundService.getByFundCode(fundCode);
        if (fund == null) {
            return null;
        }

        FundAnalyticsReportVO report = new FundAnalyticsReportVO();
        report.setFundCode(fundCode);
        report.setFundName(fund.getFundName());
        report.setFundType(fund.getFundType());

        // 基础指标
        report.setCurrentNav(fund.getNav());
        report.setDayGrowth(fund.getDayGrowth());
        report.setMonthGrowth(fund.getMonthGrowth());
        report.setYearGrowth(fund.getYearGrowth());
        report.setTotalGrowth(fund.getTotalGrowth());
        report.setRiskLevel(fund.getRiskLevel());

        // 计算风险指标
        try {
            SharpeRatioVO sharpe = calculateSharpeRatio(fundCode, "year");
            if (sharpe != null) {
                report.setSharpeRatio(sharpe.getSharpeRatio());
            }
        } catch (Exception e) {
            log.warn("计算夏普比率失败: {}", e.getMessage());
        }

        try {
            MaxDrawdownVO drawdown = calculateMaxDrawdown(fundCode, "year");
            if (drawdown != null) {
                report.setMaxDrawdown(drawdown.getMaxDrawdown());
            }
        } catch (Exception e) {
            log.warn("计算最大回撤失败: {}", e.getMessage());
        }

        try {
            VolatilityVO volatility = calculateVolatility(fundCode, "year");
            if (volatility != null) {
                report.setVolatility(volatility.getAnnualizedVolatility());
            }
        } catch (Exception e) {
            log.warn("计算波动率失败: {}", e.getMessage());
        }

        // 评级
        report.setPerformanceRating(calculatePerformanceRating(report));
        report.setRiskRating(calculateRiskRating(report));
        report.setOverallRating(calculateOverallRating(report));

        // 投资建议
        report.setInvestmentAdvice(generateInvestmentAdvice(report));

        return report;
    }

    @Override
    public void batchCalculateAnalytics(List<String> fundCodes) {
        for (String code : fundCodes) {
                try {
                    FundAnalyticsReportVO report = getAnalyticsReport(code);
                    // 可以缓存到数据库
                    log.info("完成分析: {} - 夏普: {}, 最大回撤: {}",
                            code, report.getSharpeRatio(), report.getMaxDrawdown());
                } catch (Exception e) {
                    log.error("分析失败: {}", code, e);
                }
            }
    }

    // ========== 私有方法 ==========

    private List<FundNavHistory> getNavHistory(String fundCode, String period) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate;

        switch (period) {
            case "week":
                startDate = endDate.minusWeeks(1);
                break;
            case "month":
                startDate = endDate.minusMonths(1);
                break;
            case "threeMonth":
                startDate = endDate.minusMonths(3);
                break;
            case "sixMonth":
                startDate = endDate.minusMonths(6);
                break;
            case "year":
                startDate = endDate.minusYears(1);
                break;
            case "threeYear":
                startDate = endDate.minusYears(3);
                break;
            default:
                startDate = endDate.minusYears(1);
        }

        return navHistoryMapper.findByFundCodeAndNavDateBetween(fundCode, startDate, endDate);
    }

    private List<BigDecimal> calculateDailyReturns(List<FundNavHistory> history) {
        List<BigDecimal> returns = new ArrayList<>();
        for (int i = 1; i < history.size(); i++) {
            BigDecimal prevNav = history.get(i - 1).getNav();
            BigDecimal currentNav = history.get(i).getNav();
            if (prevNav != null && currentNav != null && prevNav.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal dailyReturn = currentNav.subtract(prevNav).divide(prevNav, 10, RoundingMode.HALF_UP);
                returns.add(dailyReturn);
            }
        }
        return returns;
    }

    private BigDecimal calculateStandardDeviation(List<BigDecimal> values, BigDecimal mean) {
        BigDecimal variance = values.stream()
                .map(v -> v.subtract(mean).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 10, RoundingMode.HALF_UP);
        return new BigDecimal(Math.sqrt(variance.doubleValue()));
    }

    private BigDecimal calculateCorrelation(List<BigDecimal> x, List<BigDecimal> y) {
        if (x == null || y == null || x.size() != y.size() || x.isEmpty()) {
            return BigDecimal.ZERO;
        }

        int n = x.size();
        BigDecimal sumX = x.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sumY = y.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal meanX = sumX.divide(BigDecimal.valueOf(n), 10, RoundingMode.HALF_UP);
        BigDecimal meanY = sumY.divide(BigDecimal.valueOf(n), 10, RoundingMode.HALF_UP);

        BigDecimal numerator = BigDecimal.ZERO;
        BigDecimal denomX = BigDecimal.ZERO;
        BigDecimal denomY = BigDecimal.ZERO;

        for (int i = 0; i < n; i++) {
            BigDecimal dx = x.get(i).subtract(meanX);
            BigDecimal dy = y.get(i).subtract(meanY);
            numerator = numerator.add(dx.multiply(dy));
            denomX = denomX.add(dx.pow(2));
            denomY = denomY.add(dy.pow(2));
        }

        BigDecimal denom = new BigDecimal(Math.sqrt(denomX.multiply(denomY).doubleValue()));
        if (denom.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return numerator.divide(denom, 4, RoundingMode.HALF_UP);
    }

    private FundNavHistory findNearestNav(List<FundNavHistory> history, LocalDate date) {
        return history.stream()
                .filter(h -> !h.getNavDate().isAfter(date))
                .max(Comparator.comparing(FundNavHistory::getNavDate))
                .orElse(null);
    }

    private String calculatePerformanceRating(FundAnalyticsReportVO report) {
        BigDecimal yearGrowth = report.getYearGrowth();
        if (yearGrowth == null) return "N/A";
        if (yearGrowth.compareTo(new BigDecimal("20")) > 0) return "优秀";
        if (yearGrowth.compareTo(new BigDecimal("10")) > 0) return "良好";
        if (yearGrowth.compareTo(BigDecimal.ZERO) > 0) return "一般";
        return "较差";
    }

    private String calculateRiskRating(FundAnalyticsReportVO report) {
        BigDecimal maxDrawdown = report.getMaxDrawdown();
        if (maxDrawdown == null) return "N/A";
        BigDecimal absDrawdown = maxDrawdown.abs();
        if (absDrawdown.compareTo(new BigDecimal("5")) < 0) return "低风险";
        if (absDrawdown.compareTo(new BigDecimal("15")) < 0) return "中风险";
        return "高风险";
    }

    private String calculateOverallRating(FundAnalyticsReportVO report) {
        String perfRating = report.getPerformanceRating();
        String riskRating = report.getRiskRating();

        if ("优秀".equals(perfRating) && "低风险".equals(riskRating)) return "★★★★★";
        if ("优秀".equals(perfRating) || "良好".equals(perfRating)) return "★★★★";
        if ("一般".equals(perfRating)) return "★★★";
        if ("较差".equals(perfRating)) return "★★";
        return "★★★★";
    }

    private String generateInvestmentAdvice(FundAnalyticsReportVO report) {
        StringBuilder advice = new StringBuilder();

        BigDecimal sharpe = report.getSharpeRatio();
        if (sharpe != null && sharpe.compareTo(BigDecimal.ONE) > 0) {
            advice.append("夏普比率较高，风险调整后收益良好。");
        } else if (sharpe != null && sharpe.compareTo(BigDecimal.ZERO) < 0) {
            advice.append("夏普比率为负，建议谨慎投资。");
        }

        BigDecimal drawdown = report.getMaxDrawdown();
        if (drawdown != null && drawdown.abs().compareTo(new BigDecimal("20")) > 0) {
            advice.append("最大回撤较大，波动剧烈，适合风险承受能力强的投资者。");
        } else if (drawdown != null && drawdown.abs().compareTo(new BigDecimal("10")) < 0) {
            advice.append("最大回撤较小，走势相对稳健。");
        }

        Integer riskLevel = report.getRiskLevel();
        if (riskLevel != null && riskLevel >= 4) {
            advice.append("该基金风险等级较高，建议组合投资分散风险。");
        }

        return advice.length() > 0 ? advice.toString() : "建议结合个人风险偏好和投资周期综合考虑。";
    }
}
