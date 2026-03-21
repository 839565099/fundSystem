package com.fund.config;

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
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/login",
                        "/auth/register",
                        "/fund/search",
                        "/fund/search/**",
                        "/fund/detail/**",
                        "/fund/hot",
                        "/fund/top",
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
                        "/druid/**",
                        "/actuator/health",
                        "/error"
                );
    }
}
