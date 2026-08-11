package com.victory.config;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

/*
 * 읽기 후(연습읽기) 3종류(이야기/정보/주장) 간추리기 질문 연습 완료 상태를
 * 저장할 practice_progress.after_type_progress 컬럼을 멱등하게 추가한다.
 * during_type_progress와 같은 JSON 컬럼이며, AfterReadingSchemaInitializer와
 * 동일한 컬럼 존재 확인 후 ALTER 패턴을 쓴다.
 */
@Component
@RequiredArgsConstructor
public class PracticeProgressSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureAfterTypeProgressColumn() {
        if (!columnExists("practice_progress", "after_type_progress")) {
            jdbcTemplate.execute(
                "ALTER TABLE practice_progress ADD COLUMN after_type_progress JSON NULL"
            );
        }
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
