package com.fund.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * 数据库表结构初始化器
 * 自动检查并添加缺失的字段
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        ensureNewsTableFields();
    }

    private void ensureNewsTableFields() {
        try {
            // 获取当前表的所有列名
            List<String> columns = jdbcTemplate.queryForList(
                    "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_fund_news'",
                    String.class
            );

            log.info("t_fund_news 表现有字段: {}", columns);

            // 检查并添加 content 字段
            if (!columns.contains("content")) {
                log.info("添加 content 字段...");
                jdbcTemplate.execute("ALTER TABLE t_fund_news ADD COLUMN content TEXT COMMENT '新闻正文内容'");
                log.info("content 字段添加成功");
            }

            // 检查并添加 summary 字段
            if (!columns.contains("summary")) {
                log.info("添加 summary 字段...");
                jdbcTemplate.execute("ALTER TABLE t_fund_news ADD COLUMN summary VARCHAR(500) COMMENT '摘要'");
                log.info("summary 字段添加成功");
            }

            // 检查并添加 original_url 字段
            if (!columns.contains("original_url")) {
                log.info("添加 original_url 字段...");
                jdbcTemplate.execute("ALTER TABLE t_fund_news ADD COLUMN original_url VARCHAR(500) COMMENT '原文链接'");
                log.info("original_url 字段添加成功");
            }

            // 检查并添加 cover_image 字段
            if (!columns.contains("cover_image")) {
                log.info("添加 cover_image 字段...");
                jdbcTemplate.execute("ALTER TABLE t_fund_news ADD COLUMN cover_image VARCHAR(500) COMMENT '封面图URL'");
                log.info("cover_image 字段添加成功");
            }

            log.info("数据库表结构检查完成");

        } catch (Exception e) {
            log.error("检查/更新数据库表结构失败", e);
        }
    }
}
