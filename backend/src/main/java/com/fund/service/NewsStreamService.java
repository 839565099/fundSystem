package com.fund.service;

import com.fund.entity.FundNews;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface NewsStreamService {

    SseEmitter subscribe(String fundCode);

    void publish(FundNews news);
}
