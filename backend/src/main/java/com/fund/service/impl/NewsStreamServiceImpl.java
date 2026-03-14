package com.fund.service.impl;

import com.fund.entity.FundNews;
import com.fund.service.NewsStreamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class NewsStreamServiceImpl implements NewsStreamService {

    private final Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();

    @Override
    public SseEmitter subscribe(String fundCode) {
        String key = buildKey(fundCode);
        SseEmitter emitter = new SseEmitter(0L);
        emitterMap.put(key, emitter);

        emitter.onCompletion(() -> emitterMap.remove(key));
        emitter.onTimeout(() -> emitterMap.remove(key));
        emitter.onError((ex) -> emitterMap.remove(key));

        try {
            emitter.send(SseEmitter.event().name("connected").data("news-stream-connected"));
        } catch (IOException e) {
            log.warn("SSE连接初始化失败: {}", e.getMessage());
            emitter.complete();
        }
        return emitter;
    }

    @Override
    public void publish(FundNews news) {
        if (news == null) {
            return;
        }

        Iterator<Map.Entry<String, SseEmitter>> iterator = emitterMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, SseEmitter> entry = iterator.next();
            String key = entry.getKey();
            SseEmitter emitter = entry.getValue();

            if (!matches(news, key)) {
                continue;
            }

            try {
                emitter.send(SseEmitter.event()
                        .name("news")
                        .data(news));
            } catch (Exception e) {
                log.debug("推送失败，移除连接: {}", e.getMessage());
                emitter.complete();
                iterator.remove();
            }
        }
    }

    private boolean matches(FundNews news, String key) {
        if (Objects.equals(key, "ALL")) {
            return true;
        }
        return StringUtils.hasText(news.getFundCode()) && Objects.equals(key, news.getFundCode());
    }

    private String buildKey(String fundCode) {
        return StringUtils.hasText(fundCode) ? fundCode : "ALL";
    }
}
