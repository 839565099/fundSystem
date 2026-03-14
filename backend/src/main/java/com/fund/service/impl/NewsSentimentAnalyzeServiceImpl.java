package com.fund.service.impl;

import com.fund.entity.FundNews;
import com.fund.service.NewsSentimentAnalyzeService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;

@Service
public class NewsSentimentAnalyzeServiceImpl implements NewsSentimentAnalyzeService {

    private static final List<String> BULLISH_KEYWORDS = Arrays.asList(
            // 涨跌类
            "上涨", "大涨", "暴涨", "飙升", "拉升", "走高", "上扬", "反弹", "回暖", "回升",
            "涨", "红盘", "收红", "翻红", "逆转", "反转",
            // 利好类
            "利好", "利好消息", "利好政策", "超预期", "好于预期", "强劲", "乐观",
            // 资金类
            "增持", "加仓", "买入", "抄底", "布局", "加码", "流入", "净流入",
            "资金涌入", "大单买入", "主力买入", "北上资金", "外资买入",
            // 政策类
            "政策支持", "政策利好", "降息", "降准", "减税", "刺激", "扶持", "宽松",
            // 业绩类
            "创新高", "新高", "突破", "盈利", "翻倍", "增长", "大增", "暴增",
            "业绩优异", "超预期", "超赚",
            // 市场情绪
            "牛市", "看多", "看涨", "多头", "强势", "活跃"
    );

    private static final List<String> BEARISH_KEYWORDS = Arrays.asList(
            // 涨跌类
            "下跌", "大跌", "暴跌", "跳水", "重挫", "走低", "下挫", "回落", "下滑",
            "跌", "绿盘", "收绿", "翻绿", "崩盘", "熔断",
            // 利空类
            "利空", "利空消息", "不及预期", "低于预期", "疲软", "悲观", "担忧",
            // 资金类
            "减持", "清仓", "卖出", "抛售", "撤离", "流出", "净流出", "赎回",
            "资金出逃", "大单卖出", "主力出货", "外资减持",
            // 风险类
            "暴雷", "违约", "亏损", "回撤", "风险", "危机", "警告", "警示",
            "调查", "处罚", "罚款", "诉讼", "涉嫌",
            // 市场情绪
            "熊市", "看空", "看跌", "空头", "弱势", "低迷", "恐慌", "震荡"
    );

    @Override
    public void analyzeAndFill(FundNews news) {
        if (news == null) {
            return;
        }

        String text = (safe(news.getTitle()) + " " + safe(news.getContent())).toLowerCase();
        int bullishHits = countHits(text, BULLISH_KEYWORDS);
        int bearishHits = countHits(text, BEARISH_KEYWORDS);

        int totalHits = bullishHits + bearishHits;
        if (totalHits == 0) {
            news.setSentiment("NEUTRAL");
            news.setSentimentScore(BigDecimal.ZERO);
            news.setSentimentConfidence(BigDecimal.valueOf(0.3));
            news.setImpactLevel(2);
            return;
        }

        double rawScore = (double) (bullishHits - bearishHits) / totalHits;
        BigDecimal score = BigDecimal.valueOf(rawScore).setScale(4, RoundingMode.HALF_UP);
        BigDecimal confidence = BigDecimal.valueOf(Math.min(0.95, 0.5 + totalHits * 0.1)).setScale(4, RoundingMode.HALF_UP);

        String sentiment;
        if (rawScore > 0.2) {
            sentiment = "BULLISH";
        } else if (rawScore < -0.2) {
            sentiment = "BEARISH";
        } else {
            sentiment = "NEUTRAL";
        }

        int impact = Math.min(5, Math.max(1, totalHits >= 8 ? 5 : totalHits / 2 + 1));

        news.setSentiment(sentiment);
        news.setSentimentScore(score);
        news.setSentimentConfidence(confidence);
        news.setImpactLevel(impact);
    }

    private int countHits(String text, List<String> keywords) {
        int count = 0;
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase())) {
                count++;
            }
        }
        return count;
    }

    private String safe(String value) {
        return StringUtils.hasText(value) ? value : "";
    }
}
