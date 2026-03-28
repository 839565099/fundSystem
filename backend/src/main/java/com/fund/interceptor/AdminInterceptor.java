package com.fund.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fund.annotation.RequireAdmin;
import com.fund.common.Result;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 管理员权限拦截器
 */
@Component
public class AdminInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;

        // 检查方法上的注解
        RequireAdmin requireAdmin = handlerMethod.getMethodAnnotation(RequireAdmin.class);
        if (requireAdmin == null) {
            // 检查类上的注解
            requireAdmin = handlerMethod.getBeanType().getAnnotation(RequireAdmin.class);
        }

        if (requireAdmin != null) {
            // 从 request 中获取角色（由 JwtInterceptor 设置）
            String role = (String) request.getAttribute("role");

            if (!"ADMIN".equals(role)) {
                response.setContentType("application/json;charset=UTF-8");
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write(objectMapper.writeValueAsString(
                    Result.error("权限不足，需要管理员权限")
                ));
                return false;
            }
        }

        return true;
    }
}
