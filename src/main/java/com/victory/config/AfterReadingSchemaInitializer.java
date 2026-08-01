package com.victory.config;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AfterReadingSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureAfterReadingColumns() {
        if (!columnExists("summaries", "class_reading_book_id")) {
            jdbcTemplate.execute(
                "ALTER TABLE summaries ADD COLUMN class_reading_book_id BIGINT NULL"
            );
        }

        if (!indexExists("summaries", "idx_summaries_class_reading_book_id")) {
            jdbcTemplate.execute(
                "CREATE INDEX idx_summaries_class_reading_book_id " +
                "ON summaries (class_reading_book_id)"
            );
        }

        ensureRewardLogInstanceColumn();
    }

    private void ensureRewardLogInstanceColumn() {
        if (!tableExists("student_stat_reward_log")) {
            return;
        }

        if (!columnExists("student_stat_reward_log", "instance_id")) {
            jdbcTemplate.execute(
                "ALTER TABLE student_stat_reward_log " +
                    "ADD COLUMN instance_id VARCHAR(100) NOT NULL DEFAULT ''"
            );
        }

        if (!indexExists(
                "student_stat_reward_log",
                "uk_reward_log_student_type_instance")) {
            jdbcTemplate.execute(
                "CREATE UNIQUE INDEX uk_reward_log_student_type_instance " +
                    "ON student_stat_reward_log (student_id, reward_type, instance_id)"
            );
        }

        if (indexExists("student_stat_reward_log", "uk_reward_log_student_reward")) {
            jdbcTemplate.execute(
                "ALTER TABLE student_stat_reward_log " +
                    "DROP INDEX uk_reward_log_student_reward"
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
}
