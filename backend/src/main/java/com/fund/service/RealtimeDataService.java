package com.fund.service;

import com.fund.entity.Fund;
import com.fund.vo.FundNavHistoryVO;
import com.fund.vo.MarketDataVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeDataService {

    private final SimpMessagingTemplate messagingTemplate;
    private final FundService fundService;
    private final MarketDataService marketDataService;
    private final FundNavHistoryService navHistoryService;

    private final Map<String, Object> lastPushedData = new ConcurrentHashMap<>();

    public void pushFundUpdate(String fundCode) {
        try {
            Fund fund = fundService.getByFundCode(fundCode);
            if (fund != null) {
                Map<String, Object> update = new HashMap<>();
                update.put("fundCode", fund.getFundCode());
                update.put("fundName", fund.getFundName());
                update.put("nav", fund.getNav());
                update.put("accNav", fund.getAccNav());
                update.put("navDate", fund.getNavDate());
                update.put("dayGrowth", fund.getDayGrowth());
                update.put("weekGrowth", fund.getWeekGrowth());
                update.put("monthGrowth", fund.getMonthGrowth());
                update.put("updateTime", System.currentTimeMillis());

                messagingTemplate.convertAndSend("/topic/fund/" + fundCode, update);
                log.debug("推送基金更新: {}", fundCode);
            }
        } catch (Exception e) {
            log.error("推送基金更新失败: {}", fundCode, e);
        }
    }

    public void pushMarketUpdate() {
        try {
            List<MarketDataVO> marketData = marketDataService.getMarketData();
            if (marketData != null && !marketData.isEmpty()) {
                Map<String, Object> update = new HashMap<>();
                update.put("data", marketData);
                update.put("updateTime", System.currentTimeMillis());

                messagingTemplate.convertAndSend("/topic/market", update);
                log.debug("推送市场行情更新");
            }
        } catch (Exception e) {
            log.error("推送市场行情更新失败", e);
        }
    }

    public void pushNavHistoryUpdate(String fundCode, String period) {
        try {
            List<FundNavHistoryVO> history = navHistoryService.getNavHistory(fundCode, period);
            if (history != null && !history.isEmpty()) {
                Map<String, Object> update = new HashMap<>();
                update.put("fundCode", fundCode);
                update.put("period", period);
                update.put("data", history);
                update.put("updateTime", System.currentTimeMillis());

                messagingTemplate.convertAndSend("/topic/nav-history/" + fundCode, update);
                log.debug("推送净值历史更新: {}", fundCode);
            }
        } catch (Exception e) {
            log.error("推送净值历史更新失败: {}", fundCode, e);
        }
    }

    public void pushBatchFundUpdate(List<String> fundCodes) {
        try {
            for (String fundCode : fundCodes) {
                pushFundUpdate(fundCode);
            }
        } catch (Exception e) {
            log.error("批量推送基金更新失败", e);
        }
    }

    public void pushNotification(String userId, String title, String message, String type) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("title", title);
            notification.put("message", message);
            notification.put("type", type);
            notification.put("timestamp", System.currentTimeMillis());

            messagingTemplate.convertAndSendToUser(userId, "/queue/notification", notification);
            log.debug("推送通知给用户: {}", userId);
        } catch (Exception e) {
            log.error("推送通知失败: {}", userId, e);
        }
    }

    public void broadcastAlert(String title, String message, String level) {
        try {
            Map<String, Object> alert = new HashMap<>();
            alert.put("title", title);
            alert.put("message", message);
            alert.put("level", level);
            alert.put("timestamp", System.currentTimeMillis());

            messagingTemplate.convertAndSend("/topic/alert", alert);
            log.info("广播告警: {} - {}", title, message);
        } catch (Exception e) {
            log.error("广播告警失败", e);
        }
    }

    @Scheduled(fixedRate = 30000)
    public void scheduledMarketPush() {
        pushMarketUpdate();
    }
}
