-- ============================================================================
-- 문답책(Victory) DB 스키마 v2
-- 기준: 최종 확정 16개 테이블 (2026-07-16) + class_reading_books (기존 코드 유지, 17번째)
-- questions 테이블은 제거됨 (교사가 질문을 미리 등록하는 화면이 없어 학생이 항상
-- 직접 질문을 작성하는 구조로 확정).
--
-- 참고: reading_records.represent_response_id <-> responses.reading_record_id 는
-- 서로를 참조하는 순환 관계라, 두 테이블을 먼저 만든 뒤 마지막에 ALTER TABLE로
-- reading_records.represent_response_id의 FK를 추가한다.
-- ============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------------------------------------------------------
-- 1. users (사용자)
-- ----------------------------------------------------------------------------
CREATE TABLE users (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    login_id               VARCHAR(50)  NOT NULL,
    password               VARCHAR(255) NOT NULL,
    name                   VARCHAR(50)  NOT NULL,
    role                   VARCHAR(20)  NOT NULL,
    has_seen_story_intro   BOOLEAN      NOT NULL DEFAULT FALSE,
    has_seen_ending        BOOLEAN      NOT NULL DEFAULT FALSE,
    school                 VARCHAR(100) NULL,
    created_at             DATETIME     NOT NULL,

    UNIQUE KEY uk_users_login_id (login_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 2. classes (학급)
-- ----------------------------------------------------------------------------
CREATE TABLE classes (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    teacher_id    BIGINT       NOT NULL,
    class_name    VARCHAR(100) NOT NULL,
    class_number  INT          NULL,
    grade         INT          NULL,
    created_at    DATETIME     NOT NULL,

    CONSTRAINT fk_classes_teacher
        FOREIGN KEY (teacher_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 3. class_students (학급-학생 소속)
-- ----------------------------------------------------------------------------
CREATE TABLE class_students (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_id        BIGINT   NOT NULL,
    student_id      BIGINT   NOT NULL,
    student_number  INT      NULL,
    joined_at       DATETIME NOT NULL,

    CONSTRAINT fk_class_students_class
        FOREIGN KEY (class_id) REFERENCES classes (id),
    CONSTRAINT fk_class_students_student
        FOREIGN KEY (student_id) REFERENCES users (id),
    CONSTRAINT uk_class_students_class_student
        UNIQUE (class_id, student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 4. practice_progress (연습읽기 진행 상태)
-- ----------------------------------------------------------------------------
CREATE TABLE practice_progress (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id            BIGINT   NOT NULL,
    book_selected         BOOLEAN  NOT NULL DEFAULT FALSE,
    before_done           BOOLEAN  NOT NULL DEFAULT FALSE,
    class_read_done       BOOLEAN  NOT NULL DEFAULT FALSE,
    after_done            BOOLEAN  NOT NULL DEFAULT FALSE,
    during_type_progress  JSON     NULL,
    updated_at            DATETIME NOT NULL,

    CONSTRAINT fk_practice_progress_student
        FOREIGN KEY (student_id) REFERENCES users (id),
    CONSTRAINT uk_practice_progress_student
        UNIQUE (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 5. dungeons (던전)
-- ----------------------------------------------------------------------------
CREATE TABLE dungeons (
    id                        BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                      VARCHAR(100) NOT NULL,
    description               TEXT         NULL,
    difficulty                VARCHAR(20)  NULL,
    required_books            INT          NULL,
    required_stat_avg         INT          NULL,
    prerequisite_dungeon_id   BIGINT       NULL,
    time_limit_seconds        INT          NULL,
    enemy_stats               JSON         NULL,
    reward_title              VARCHAR(100) NULL,
    reward_note               TEXT         NULL,
    reward_stat_reset_value   INT          NULL,
    created_at                DATETIME     NOT NULL,
    updated_at                DATETIME     NOT NULL,

    CONSTRAINT fk_dungeons_prerequisite
        FOREIGN KEY (prerequisite_dungeon_id) REFERENCES dungeons (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 6. dungeon_records (던전 플레이 기록)
-- ----------------------------------------------------------------------------
CREATE TABLE dungeon_records (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id  BIGINT      NOT NULL,
    dungeon_id  BIGINT      NOT NULL,
    result      VARCHAR(20) NOT NULL,
    played_at   DATETIME    NOT NULL,

    CONSTRAINT fk_dungeon_records_student
        FOREIGN KEY (student_id) REFERENCES users (id),
    CONSTRAINT fk_dungeon_records_dungeon
        FOREIGN KEY (dungeon_id) REFERENCES dungeons (id),
    KEY idx_dungeon_records_student_dungeon (student_id, dungeon_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 7. books (책)
-- ----------------------------------------------------------------------------
CREATE TABLE books (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    title          VARCHAR(200) NOT NULL,
    author         VARCHAR(100) NULL,
    cover_image    LONGTEXT     NULL,
    book_type      VARCHAR(30)  NULL,
    description    TEXT         NULL,
    source         VARCHAR(20)  NOT NULL DEFAULT 'individual',
    class_id       BIGINT       NULL,
    registered_by  BIGINT       NULL,
    reading_range  VARCHAR(200) NULL,
    total_pages    INT          NULL,
    current_page   INT          NULL,
    created_at     DATETIME     NOT NULL,
    updated_at     DATETIME     NOT NULL,

    CONSTRAINT fk_books_class
        FOREIGN KEY (class_id) REFERENCES classes (id),
    CONSTRAINT fk_books_registered_by
        FOREIGN KEY (registered_by) REFERENCES users (id),
    KEY idx_books_class_id (class_id),
    KEY idx_books_registered_by (registered_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 8. class_reading_books (학급별 온책읽기 현재 책) -- 문서 밖 17번째 테이블,
--    기존 ClassReadingBook 엔티티/컨트롤러/서비스가 그대로 사용 중이라 유지하기로 확정.
-- ----------------------------------------------------------------------------
CREATE TABLE class_reading_books (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_id      BIGINT       NOT NULL,
    book_title    VARCHAR(200) NOT NULL,
    author        VARCHAR(100) NULL,
    cover_image   LONGTEXT     NULL,
    total_pages   INT          NULL,
    current_page  INT          NULL,
    created_at    DATETIME     NOT NULL,
    updated_at    DATETIME     NOT NULL,

    CONSTRAINT fk_class_reading_books_class
        FOREIGN KEY (class_id) REFERENCES classes (id),
    CONSTRAINT uk_class_reading_books_class
        UNIQUE (class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 9. reading_records (읽기 진행 기록)
--    represent_response_id -> responses.id FK는 순환참조 때문에 파일 끝에서 ALTER TABLE로 추가.
-- ----------------------------------------------------------------------------
CREATE TABLE reading_records (
    id                              BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id                      BIGINT      NOT NULL,
    book_id                         BIGINT      NOT NULL,
    current_stage                   VARCHAR(20) NOT NULL DEFAULT 'before',
    before_done                     BOOLEAN     NOT NULL DEFAULT FALSE,
    during_done                     BOOLEAN     NOT NULL DEFAULT FALSE,
    after_done                      BOOLEAN     NOT NULL DEFAULT FALSE,
    current_page                    INT         NULL,
    total_pages                     INT         NULL,
    rating                          INT         NULL,
    represent_response_id           BIGINT      NULL,
    final_reading_practice_score    INT         NULL,
    final_record_completion_score   INT         NULL,
    finished_at                     DATETIME    NULL,
    created_at                      DATETIME    NOT NULL,
    updated_at                      DATETIME    NOT NULL,

    CONSTRAINT fk_reading_records_student
        FOREIGN KEY (student_id) REFERENCES users (id),
    CONSTRAINT fk_reading_records_book
        FOREIGN KEY (book_id) REFERENCES books (id),
    CONSTRAINT uk_reading_records_student_book
        UNIQUE (student_id, book_id),
    KEY idx_reading_records_book_id (book_id),
    KEY idx_reading_records_finished_at (finished_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 10. responses (질문답변 / 책수다방)
-- ----------------------------------------------------------------------------
CREATE TABLE responses (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id          BIGINT      NOT NULL,
    book_id             BIGINT      NULL,
    reading_record_id   BIGINT      NULL,
    parent_id           BIGINT      NULL,
    mode                VARCHAR(20) NOT NULL,
    content_type        VARCHAR(30) NOT NULL,
    stage               VARCHAR(20) NULL,
    content              TEXT        NULL,
    passed              BOOLEAN     NULL,
    status               VARCHAR(20) NOT NULL DEFAULT 'approved',
    reject_reason        TEXT        NULL,
    reviewed_by          BIGINT      NULL,
    reviewed_at          DATETIME    NULL,
    teacher_note         TEXT        NULL,
    deleted_at           DATETIME    NULL,
    image_data           LONGTEXT    NULL,
    extra_data           JSON        NULL,
    created_at           DATETIME    NOT NULL,
    updated_at           DATETIME    NOT NULL,

    CONSTRAINT fk_responses_student
        FOREIGN KEY (student_id) REFERENCES users (id),
    CONSTRAINT fk_responses_book
        FOREIGN KEY (book_id) REFERENCES books (id),
    CONSTRAINT fk_responses_reading_record
        FOREIGN KEY (reading_record_id) REFERENCES reading_records (id),
    CONSTRAINT fk_responses_parent
        FOREIGN KEY (parent_id) REFERENCES responses (id),
    CONSTRAINT fk_responses_reviewed_by
        FOREIGN KEY (reviewed_by) REFERENCES users (id),
    KEY idx_responses_student_id (student_id),
    KEY idx_responses_book_id (book_id),
    KEY idx_responses_parent_id (parent_id),
    KEY idx_responses_status (status),
    KEY idx_responses_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 11. summaries (간추리기)
-- ----------------------------------------------------------------------------
CREATE TABLE summaries (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id         BIGINT      NOT NULL,
    book_id            BIGINT      NULL,
    reading_record_id  BIGINT      NULL,
    book_type          VARCHAR(30) NULL,
    summary_text       TEXT        NOT NULL,
    is_shared          BOOLEAN     NOT NULL DEFAULT FALSE,
    status             VARCHAR(20) NOT NULL DEFAULT 'approved',
    ai_passed          BOOLEAN     NULL,
    created_at         DATETIME    NOT NULL,
    updated_at         DATETIME    NOT NULL,

    CONSTRAINT fk_summaries_student
        FOREIGN KEY (student_id) REFERENCES users (id),
    CONSTRAINT fk_summaries_book
        FOREIGN KEY (book_id) REFERENCES books (id),
    CONSTRAINT fk_summaries_reading_record
        FOREIGN KEY (reading_record_id) REFERENCES reading_records (id),
    KEY idx_summaries_student_id (student_id),
    KEY idx_summaries_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 12. student_stats (능력치)
-- ----------------------------------------------------------------------------
CREATE TABLE student_stats (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id  BIGINT   NOT NULL,
    magic       INT      NOT NULL DEFAULT 8,
    stamina     INT      NOT NULL DEFAULT 8,
    wisdom      INT      NOT NULL DEFAULT 8,
    courage     INT      NOT NULL DEFAULT 8,
    updated_at  DATETIME NOT NULL,

    CONSTRAINT fk_student_stats_student
        FOREIGN KEY (student_id) REFERENCES users (id),
    CONSTRAINT uk_student_stats_student
        UNIQUE (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 13. student_stat_reward_log (능력치 보상 이력)
-- ----------------------------------------------------------------------------
CREATE TABLE student_stat_reward_log (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id   BIGINT       NOT NULL,
    reward_type  VARCHAR(60)  NOT NULL,
    stat_type    VARCHAR(20)  NOT NULL,
    amount       INT          NOT NULL,
    instance_id  VARCHAR(100) NOT NULL DEFAULT '',
    granted_at   DATETIME     NOT NULL,

    CONSTRAINT fk_reward_log_student
        FOREIGN KEY (student_id) REFERENCES users (id),
    CONSTRAINT uk_reward_log_student_type_instance
        UNIQUE (student_id, reward_type, instance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 14. content_likes (좋아요)
--     content_id는 content_type(summary/response/book_recommendation)에 따라
--     참조 대상이 달라지는 다형성 컬럼이라 DB 레벨 FK를 걸지 않는다.
--     무결성은 애플리케이션에서 관리한다.
-- ----------------------------------------------------------------------------
CREATE TABLE content_likes (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id    BIGINT      NOT NULL,
    content_type  VARCHAR(30) NOT NULL,
    content_id    BIGINT      NOT NULL,
    created_at    DATETIME    NOT NULL,

    CONSTRAINT fk_content_likes_student
        FOREIGN KEY (student_id) REFERENCES users (id),
    CONSTRAINT uk_content_likes_student_content
        UNIQUE (student_id, content_type, content_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 15. book_recommendations (친구 추천 책)
--     teaser_response_ids는 responses.id 배열을 담는 JSON 컬럼이라 FK를 걸 수 없다.
-- ----------------------------------------------------------------------------
CREATE TABLE book_recommendations (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id            BIGINT       NOT NULL,
    title                 VARCHAR(200) NOT NULL,
    author                VARCHAR(100) NULL,
    reason                TEXT         NOT NULL,
    teaser_response_ids   JSON         NULL,
    created_at            DATETIME     NOT NULL,

    CONSTRAINT fk_book_recommendations_student
        FOREIGN KEY (student_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 16. reading_progress_logs (개별읽기 일별 진행 기록)
-- ----------------------------------------------------------------------------
CREATE TABLE reading_progress_logs (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id          BIGINT   NOT NULL,
    reading_record_id   BIGINT   NOT NULL,
    log_date            DATE     NOT NULL,
    cumulative_page      INT      NOT NULL,
    total_pages          INT      NOT NULL,
    progress_percent     INT      NOT NULL,
    created_at           DATETIME NOT NULL,
    updated_at           DATETIME NOT NULL,

    CONSTRAINT fk_progress_logs_student
        FOREIGN KEY (student_id) REFERENCES users (id),
    CONSTRAINT fk_progress_logs_reading_record
        FOREIGN KEY (reading_record_id) REFERENCES reading_records (id),
    CONSTRAINT uk_progress_logs_student_record_date
        UNIQUE (student_id, reading_record_id, log_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 17. student_daily_metrics (교사 대시보드 일별 스냅샷)
-- ----------------------------------------------------------------------------
CREATE TABLE student_daily_metrics (
    id                     BIGINT      AUTO_INCREMENT PRIMARY KEY,
    student_id             BIGINT      NOT NULL,
    class_id               BIGINT      NULL,
    metric_type            VARCHAR(20) NOT NULL,
    metric_date            DATE        NOT NULL,
    score_a                INT         NOT NULL,
    score_b                INT         NOT NULL,
    total_score            INT         NOT NULL,
    activity_count_today   INT         NOT NULL DEFAULT 0,
    teacher_comment        TEXT        NULL,
    created_at             DATETIME    NOT NULL,
    updated_at             DATETIME    NOT NULL,

    CONSTRAINT fk_daily_metrics_student
        FOREIGN KEY (student_id) REFERENCES users (id),
    CONSTRAINT fk_daily_metrics_class
        FOREIGN KEY (class_id) REFERENCES classes (id),
    CONSTRAINT uk_daily_metrics_student_type_date
        UNIQUE (student_id, metric_type, metric_date),
    KEY idx_daily_metrics_class_id (class_id),
    KEY idx_daily_metrics_metric_date (metric_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 순환참조 FK 추가: reading_records.represent_response_id -> responses.id
-- (reading_records, responses 두 테이블이 모두 생성된 뒤에만 추가 가능)
-- ----------------------------------------------------------------------------
ALTER TABLE reading_records
    ADD CONSTRAINT fk_reading_records_represent_response
        FOREIGN KEY (represent_response_id) REFERENCES responses (id);

SET FOREIGN_KEY_CHECKS = 1;
