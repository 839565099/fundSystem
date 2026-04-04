package com.fund.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class DatabaseInitializer {
    
    private final JdbcTemplate jdbcTemplate;
    
    public DatabaseInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        executeSqlScript("db/init.sql");
        executeSqlScript("db/migration_news_sentiment.sql");
        executeSqlScript("db/migration_session_management.sql");
        log.info("数据库初始化完成");
    }

    private void executeSqlScript(String classpathLocation) {
        try {
            ClassPathResource resource = new ClassPathResource(classpathLocation);
            String sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

            String[] statements = sql.split(";");
            for (String statement : statements) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty()) {
                    try {
                        jdbcTemplate.execute(trimmed);
                    } catch (Exception e) {
                        log.debug("SQL执行警告 [{}]: {}", classpathLocation, e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("脚本执行失败 [{}]: {}", classpathLocation, e.getMessage());
        }
    }
}
