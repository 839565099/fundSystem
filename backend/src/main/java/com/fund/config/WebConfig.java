package com.fund.config;

import com.fund.interceptor.AdminInterceptor;
import com.fund.interceptor.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;
    private final AdminInterceptor adminInterceptor;
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // JWT 拦截器
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/login",
                        "/auth/register",
                        "/auth/forgot-password",
                        "/auth/validate-reset-token",
                        "/auth/validate-reset-code",
                        "/auth/reset-password",
                        "/auth/email-login-code",
                        "/auth/email-login",
                        "/auth/google",
                        "/auth/google/callback",
                        "/auth/google/config",
                        "/fund/search",
                        "/fund/search/**",
                        "/fund/detail/**",
                        "/fund/hot",
                        "/fund/top",
                        "/fund/hot-by-type",
                        "/fund/nav-history/**",
                        "/fund/ranking",
                        "/fund/types",
                        "/fund/companies",
                        "/fund-info/**",
                        "/hot-fund/list",
                        "/hot-fund/top",
                        "/market/**",
                        "/news/list",
                        "/news/detail/**",
                        "/news/hot",
                        "/news/related/**",
                        "/sector/**",
                        "/compare/**",
                        "/recommend/hot",
                        "/recommend/personalized",
                        "/recommend/risk-based",
                        "/druid/**",
                        "/actuator/health",
                        "/error"
                );

        // 管理员权限拦截器
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/admin/**");
    }
}
