package com.victory.config;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

/*
 * 이 프로젝트는 Hibernate ddl-auto를 쓰지 않으므로(PracticeAchievementSchemaInitializer와
 * 같은 방식), book_recommendations에 나중에 추가된 reading_record_id
 * 컬럼을 애플리케이션 시작 시 없으면 만든다. 이미 있으면 아무 것도 하지
 * 않는다(idempotent). book_recommendations 테이블 자체는 이 초기화 클래스가
 * 만들지 않는다(이미 다른 마이그레이션으로 존재함) - student_id처럼 이
 * 테이블은 실제 FK 제약을 쓰는 관례라 reading_record_id도 같은 방식으로
 * 맞춘다.
 */
@Component
@RequiredArgsConstructor
public class BookRecommendationSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureBookRecommendationsReadingRecordIdColumn() {
        if (!tableExists("book_recommendations")) {
            return;
        }

        if (!columnExists("book_recommendations", "reading_record_id")) {
            jdbcTemplate.execute(
                "ALTER TABLE book_recommendations " +
                    "ADD COLUMN reading_record_id BIGINT NULL"
            );
        }

        if (!indexExists("book_recommendations", "idx_book_recommendations_reading_record")) {
            jdbcTemplate.execute(
                "CREATE INDEX idx_book_recommendations_reading_record " +
                    "ON book_recommendations (reading_record_id)"
            );
        }

        if (tableExists("reading_records")
                && !constraintExists("book_recommendations", "fk_book_recommendations_reading_record")) {
            jdbcTemplate.execute(
                "ALTER TABLE book_recommendations " +
                    "ADD CONSTRAINT fk_book_recommendations_reading_record " +
                    "FOREIGN KEY (reading_record_id) REFERENCES reading_records (id)"
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

    private boolean indexExists(String tableName, String indexName) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.statistics " +
                    "WHERE table_schema = DATABASE() " +
                    "AND table_name = ? " +
                    "AND index_name = ?",
                Integer.class,
                tableName,
                indexName
            );

            return count != null && count > 0;
        } catch (DataAccessException ignored) {
            return false;
        }
    }

    private boolean constraintExists(String tableName, String constraintName) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.table_constraints " +
                "WHERE table_schema = DATABASE() " +
                "AND table_name = ? " +
                "AND constraint_name = ?",
            Integer.class,
            tableName,
            constraintName
        );

        return count != null && count > 0;
    }
}
