package com.victory.config;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

/*
 * 이 프로젝트는 Hibernate ddl-auto를 쓰지 않으므로(PracticeAchievementSchemaInitializer와
 * 같은 방식), 독서 집중 시간 누적값을 저장할 컬럼을 기존 student_stats 테이블에
 * 없으면 추가한다. 이미 있으면 아무 것도 하지 않는다(idempotent). student_stats
 * 테이블 자체는 이 프로젝트에서 이미 사용 중이므로 여기서는 컬럼만 책임진다.
 */
@Component
@RequiredArgsConstructor
public class ReadingFocusSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureTotalReadingFocusSecondsColumn() {
        if (!tableExists("student_stats")) {
            return;
        }

        if (!columnExists("student_stats", "total_reading_focus_seconds")) {
            jdbcTemplate.execute(
                "ALTER TABLE student_stats " +
                    "ADD COLUMN total_reading_focus_seconds BIGINT NOT NULL DEFAULT 0"
            );
        }
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables " +
                "WHERE table_schema = DATABASE() " +
                "AND table_name = ?",
            Integer.class,
            tableName
        );

        return count != null && count > 0;
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() " +
                "AND table_name = ? " +
                "AND column_name = ?",
            Integer.class,
            tableName,
            columnName
        );

        return count != null && count > 0;
    }
}
