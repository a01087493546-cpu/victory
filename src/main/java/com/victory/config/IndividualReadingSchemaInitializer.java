package com.victory.config;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

/*
 * 이 프로젝트는 Hibernate ddl-auto를 쓰지 않으므로(AfterReadingSchemaInitializer/
 * PracticeAchievementSchemaInitializer와 같은 방식), 개별읽기 "같은 책 재독" 요구사항에
 * 맞게 reading_records의 기존 UNIQUE(student_id, book_id) 제약을 애플리케이션
 * 시작 시 안전하게 제거한다. 이미 제거됐으면 아무 것도 하지 않는다(idempotent).
 */
@Component
@RequiredArgsConstructor
public class IndividualReadingSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureReadingRecordsAllowsMultipleRecordsPerBook() {
        ensureResponsesHasActivityDateColumn();
        ensureResponsesHasBookChatModerationColumns();
        correctUnreviewedIndividualBookChatPosts();

        if (!tableExists("reading_records")) {
            return;
        }

        /*
         * uk_reading_records_student_book(student_id, book_id)를 지우기 전에
         * student_id 단독 인덱스를 먼저 만들어 둔다 - fk_reading_records_student
         * 외래키가 student_id를 참조하는 인덱스 없이 남으면 MySQL이
         * "Cannot drop index: needed in a foreign key constraint" 오류를 내면서
         * DROP 자체가 실패한다. 순서를 바꾸면 안 된다.
         */
        if (!indexExists("reading_records", "idx_reading_records_student_id")) {
            jdbcTemplate.execute(
                "CREATE INDEX idx_reading_records_student_id " +
                    "ON reading_records (student_id)"
            );
        }

        if (indexExists("reading_records", "uk_reading_records_student_book")) {
            jdbcTemplate.execute(
                "ALTER TABLE reading_records " +
                    "DROP INDEX uk_reading_records_student_book"
            );
        }
    }

    /*
     * 개별읽기 읽기 중(책속 생각쓰기)처럼 완독 전까지 날짜별로 반복하는 활동을
     * responses에 저장하려면 "그 활동을 한 날짜"를 DB 레벨에서 안정적으로
     * 조회·구분할 수 있어야 한다. extra_data JSON에 숨겨두는 방식 대신
     * 명시 컬럼을 추가한다(§book-chat 설계 분석에서도 같은 컬럼을 재사용할 예정).
     */
    private void ensureResponsesHasActivityDateColumn() {
        if (!tableExists("responses")) {
            return;
        }

        if (!columnExists("responses", "activity_date")) {
            jdbcTemplate.execute(
                "ALTER TABLE responses ADD COLUMN activity_date DATE NULL AFTER stage"
            );
        }
    }

    private void ensureResponsesHasBookChatModerationColumns() {
        if (!tableExists("responses")) {
            return;
        }

        if (!columnExists("responses", "status")) {
            jdbcTemplate.execute(
                "ALTER TABLE responses ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'approved' AFTER passed"
            );
        }

        if (!columnExists("responses", "reject_reason")) {
            jdbcTemplate.execute(
                "ALTER TABLE responses ADD COLUMN reject_reason TEXT NULL AFTER status"
            );
        }

        if (!columnExists("responses", "reviewed_by")) {
            jdbcTemplate.execute(
                "ALTER TABLE responses ADD COLUMN reviewed_by BIGINT NULL AFTER reject_reason"
            );
        }

        if (!columnExists("responses", "reviewed_at")) {
            jdbcTemplate.execute(
                "ALTER TABLE responses ADD COLUMN reviewed_at DATETIME NULL AFTER reviewed_by"
            );
        }

        if (!indexExists("responses", "idx_responses_status")) {
            jdbcTemplate.execute(
                "CREATE INDEX idx_responses_status ON responses (status)"
            );
        }
    }

    private void correctUnreviewedIndividualBookChatPosts() {
        if (!tableExists("responses")
                || !columnExists("responses", "status")
                || !columnExists("responses", "reviewed_by")
                || !columnExists("responses", "reviewed_at")
                || !columnExists("responses", "reject_reason")) {
            return;
        }

        jdbcTemplate.update(
            "UPDATE responses " +
                "SET status = 'pending', reject_reason = NULL " +
                "WHERE mode = 'individual' " +
                "AND content_type = 'chat_post' " +
                "AND deleted_at IS NULL " +
                "AND reviewed_by IS NULL " +
                "AND reviewed_at IS NULL " +
                "AND (status IS NULL " +
                    "OR TRIM(status) = '' " +
                    "OR LOWER(status) = 'approved' " +
                    "OR LOWER(status) NOT IN ('pending', 'approved', 'rejected'))"
        );
    }

    private boolean columnExists(String tableName, String columnName) {
        try {
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
        } catch (DataAccessException ignored) {
            return false;
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
