package com.victory.config;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

/*
 * 이 프로젝트는 Hibernate ddl-auto를 쓰지 않으므로(AfterReadingSchemaInitializer와
 * 같은 방식), 교사 연습읽기 성취도 기능에 필요한 새 테이블 2개를 애플리케이션
 * 시작 시 없으면 만든다. 이미 있으면 아무 것도 하지 않는다(idempotent).
 */
@Component
@RequiredArgsConstructor
public class PracticeAchievementSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensurePracticeAchievementTables() {
        ensureAiEvaluationAttemptsTable();
        ensureAiEvaluationAttemptsEvaluationKeyColumns();
        ensureAiEvaluationAttemptsClassReadingBookIdColumn();
        ensurePracticeAchievementSnapshotsTable();
    }

    /*
     * class_reading_book_id도 나중에 추가된 컬럼이라 같은 방식으로 안전하게
     * ALTER한다. NULL 허용이라 이 컬럼이 없던 시절에 쌓인 행이 있어도(현재는
     * 테이블이 비어 있어 해당 없음) 실패하지 않는다 - 그런 행은 책 범위를 알
     * 수 없으므로 PracticeAchievementService의 책별 조회에서 자연히
     * 제외된다(찾을 책 id로 필터링하기 때문).
     */
    private void ensureAiEvaluationAttemptsClassReadingBookIdColumn() {
        if (!tableExists("ai_evaluation_attempts")) {
            return;
        }

        if (!columnExists("ai_evaluation_attempts", "class_reading_book_id")) {
            jdbcTemplate.execute(
                "ALTER TABLE ai_evaluation_attempts " +
                    "ADD COLUMN class_reading_book_id BIGINT NULL"
            );
        }

        if (!indexExists(
                "ai_evaluation_attempts",
                "idx_ai_eval_attempts_student_book")) {
            jdbcTemplate.execute(
                "CREATE INDEX idx_ai_eval_attempts_student_book " +
                    "ON ai_evaluation_attempts (student_id, class_reading_book_id)"
            );
        }
    }

    /*
     * evaluation_key/attempt_number는 나중에 추가된 컬럼이라, 이미 만들어진
     * 환경(ai_evaluation_attempts가 이미 있고 이 컬럼들이 없는 경우)에도
     * ALTER TABLE로 안전하게 붙인다. 두 컬럼 다 NULL 허용이라 기존 레코드가
     * 있어도(이 프로젝트에서는 studentId를 보내는 프론트가 아직 없어 실제
     * 저장된 행이 없지만, 혹시 있더라도) NOT NULL 제약 위반 없이 그대로
     * ALTER가 성공한다 - 그 행들은 evaluation_key가 NULL로 남아 이해도
     * 계산에서 자연히 제외된다(PracticeAchievementService 참고).
     */
    private void ensureAiEvaluationAttemptsEvaluationKeyColumns() {
        if (!tableExists("ai_evaluation_attempts")) {
            return;
        }

        if (!columnExists("ai_evaluation_attempts", "evaluation_key")) {
            jdbcTemplate.execute(
                "ALTER TABLE ai_evaluation_attempts " +
                    "ADD COLUMN evaluation_key VARCHAR(191) NULL"
            );
        }

        if (!columnExists("ai_evaluation_attempts", "attempt_number")) {
            jdbcTemplate.execute(
                "ALTER TABLE ai_evaluation_attempts " +
                    "ADD COLUMN attempt_number INT NULL"
            );
        }

        if (!indexExists(
                "ai_evaluation_attempts",
                "idx_ai_eval_attempts_student_evaluation_key")) {
            jdbcTemplate.execute(
                "CREATE INDEX idx_ai_eval_attempts_student_evaluation_key " +
                    "ON ai_evaluation_attempts (student_id, evaluation_key)"
            );
        }

        /*
         * 동시 요청으로 같은 student_id+evaluation_key에 같은 attempt_number가
         * 두 번 저장되는 것을 DB 차원에서 막는 최후 방어선. evaluation_key/
         * attempt_number가 NULL인 행은 MySQL UNIQUE 인덱스에서 서로 다른
         * 값으로 취급되어 여러 개 있어도 위반되지 않는다.
         */
        if (!indexExists(
                "ai_evaluation_attempts",
                "uk_ai_eval_attempts_student_key_attempt")) {
            jdbcTemplate.execute(
                "CREATE UNIQUE INDEX uk_ai_eval_attempts_student_key_attempt " +
                    "ON ai_evaluation_attempts (student_id, evaluation_key, attempt_number)"
            );
        }
    }

    private void ensureAiEvaluationAttemptsTable() {
        if (tableExists("ai_evaluation_attempts")) {
            return;
        }

        jdbcTemplate.execute(
            "CREATE TABLE ai_evaluation_attempts (" +
                "id BIGINT NOT NULL AUTO_INCREMENT, " +
                "student_id BIGINT NOT NULL, " +
                "activity_type VARCHAR(60) NOT NULL, " +
                "question_type VARCHAR(30) NULL, " +
                "status VARCHAR(10) NOT NULL, " +
                "evaluated_at DATETIME NOT NULL, " +
                "PRIMARY KEY (id), " +
                "INDEX idx_ai_eval_attempts_student_activity_question " +
                    "(student_id, activity_type, question_type)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
        );
    }

    private void ensurePracticeAchievementSnapshotsTable() {
        if (tableExists("practice_achievement_snapshots")) {
            return;
        }

        jdbcTemplate.execute(
            "CREATE TABLE practice_achievement_snapshots (" +
                "id BIGINT NOT NULL AUTO_INCREMENT, " +
                "student_id BIGINT NOT NULL, " +
                "class_reading_book_id BIGINT NOT NULL, " +
                "snapshot_date DATE NOT NULL, " +
                "reading_activity_completion_rate DOUBLE NOT NULL, " +
                "question_participation_rate DOUBLE NOT NULL, " +
                "thought_sharing_participation_rate DOUBLE NOT NULL, " +
                "participation_rate DOUBLE NOT NULL, " +
                "comprehension_rate DOUBLE NOT NULL, " +
                "achievement_rate DOUBLE NOT NULL, " +
                "final_reading_progress DOUBLE NOT NULL, " +
                "has_ai_evaluation BOOLEAN NOT NULL, " +
                "created_at DATETIME NOT NULL, " +
                "PRIMARY KEY (id), " +
                "UNIQUE KEY uk_achievement_snapshot_student_book_date " +
                    "(student_id, class_reading_book_id, snapshot_date), " +
                "INDEX idx_achievement_snapshot_book_date " +
                    "(class_reading_book_id, snapshot_date)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
        );
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
