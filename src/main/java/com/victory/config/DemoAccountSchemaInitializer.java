package com.victory.config;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

/** 심사 체험 여부와 학생 인트로 상태 컬럼을 기존 users 테이블에 멱등적으로 추가한다. */
@Component
@RequiredArgsConstructor
public class DemoAccountSchemaInitializer {
    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureDemoAccountColumn() {
        if (!tableExists("users")) return;

        if (!columnExists("users", "is_demo_account")) {
            jdbcTemplate.execute(
                "ALTER TABLE users ADD COLUMN is_demo_account TINYINT(1) NOT NULL DEFAULT 0 AFTER school");
        }

        if (!columnExists("users", "has_seen_power_intro")) {
            jdbcTemplate.execute(
                "ALTER TABLE users ADD COLUMN has_seen_power_intro TINYINT(1) NOT NULL DEFAULT 0 "
                    + "AFTER has_seen_story_intro");
            /* 기존 스토리 완료 학생에게 새 안내가 다시 뜨지 않도록 완료값을 승계한다. */
            jdbcTemplate.update(
                "UPDATE users SET has_seen_power_intro = has_seen_story_intro");
        }
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
            Integer.class, tableName);
        return count != null && count > 0;
    }

    private boolean columnExists(String tableName, String columnName) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                Integer.class, tableName, columnName);
            return count != null && count > 0;
        } catch (DataAccessException ignored) {
            return false;
        }
    }
}
