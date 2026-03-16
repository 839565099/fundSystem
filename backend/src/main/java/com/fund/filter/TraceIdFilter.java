package com.fund.filter;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * Trace ID 过滤器
 * 为每个请求生成唯一的追踪ID，便于日志追踪和问题排查
 */
@Slf4j
@Component
@Order(1)
public class TraceIdFilter implements Filter {

    /**
     * Trace ID 请求头名称
     */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    /**
     * MDC 中存储 Trace ID 的键名
     */
    public static final String TRACE_ID_MDC_KEY = "traceId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            // 1. 尝试从请求头获取上游传递的 Trace ID（支持分布式链路追踪）
            String traceId = httpRequest.getHeader(TRACE_ID_HEADER);

            // 2. 如果没有，生成新的 Trace ID（32位UUID，无横线）
            if (traceId == null || traceId.isEmpty()) {
                traceId = UUID.randomUUID().toString().replace("-", "");
            }

            // 3. 设置到 MDC 中，供日志框架自动输出
            MDC.put(TRACE_ID_MDC_KEY, traceId);

            // 4. 设置响应头，返回给前端（便于前端关联问题）
            httpResponse.setHeader(TRACE_ID_HEADER, traceId);

            // 5. 记录请求开始日志
            if (log.isDebugEnabled()) {
                log.debug("请求开始 - {} {} from {}",
                    httpRequest.getMethod(),
                    httpRequest.getRequestURI(),
                    getClientIp(httpRequest));
            }

            chain.doFilter(request, response);

        } finally {
            // 6. 请求结束后清理 MDC，防止内存泄漏（线程池复用）
            MDC.remove(TRACE_ID_MDC_KEY);
        }
    }

    /**
     * 获取客户端真实IP地址
     * 支持代理服务器场景
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理时取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    @Override
    public void init(FilterConfig filterConfig) {
        log.info("TraceIdFilter 初始化完成");
    }

    @Override
    public void destroy() {
        log.info("TraceIdFilter 销毁");
    }
}
