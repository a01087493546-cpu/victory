-- ============================================================
-- Victory 프로젝트 DB 스키마 (docs/db-schema-final.md 기준)
-- 아직 실행되지 않은 설계용 SQL입니다. 검토/승인 후 실행하세요.
--
-- 순서:
--   1) CREATE TABLE (FK 제약 없이 컬럼/PK/UNIQUE만 생성)
--   2) ALTER TABLE ... ADD CONSTRAINT (FK 일괄 추가)
--      - reading_records <-> responses 순환 참조 때문에 FK를 분리함
--
-- updated_at 컬럼(practice_progress, student_stats)은 ON UPDATE CURRENT_TIMESTAMP로
-- DB가 자동 갱신하도록 설정했습니다.
-- ============================================================

CREATE DATABASE IF NOT EXISTS victory_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE victory_db;

-- ============================================================
-- 1. CREATE TABLE
-- ============================================================

CREATE TABLE users (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    login_id              VARCHAR(50)  NOT NULL,
    password              VARCHAR(255) NOT NULL,
    name                  VARCHAR(50)  NOT NULL,
    role                  VARCHAR(20)  NOT NULL,
    has_seen_story_intro  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_login_id (login_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE classes (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    teacher_id    BIGINT       NOT NULL,
    class_name    VARCHAR(100) NOT NULL,
    class_number  INT          NULL,
    grade         INT          NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE class_students (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    class_id    BIGINT   NOT NULL,
    student_id  BIGINT   NOT NULL,
    joined_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_class_students_class_student (class_id, student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE books (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    title          VARCHAR(200) NOT NULL,
    author         VARCHAR(100) NULL,
    cover_image    LONGTEXT     NULL,
    book_type      VARCHAR(30)  NULL,
    source         VARCHAR(20)  NOT NULL DEFAULT 'individual',
    class_id       BIGINT       NULL,
    registered_by  BIGINT       NULL,
    reading_range  VARCHAR(200) NULL,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE recommendation_books (
    id                       BIGINT       NOT NULL AUTO_INCREMENT,
    title                    VARCHAR(200) NOT NULL,
    author                   VARCHAR(100) NOT NULL,
    description              TEXT         NOT NULL,
    cover_image              LONGTEXT     NULL,
    thickness                VARCHAR(30)  NOT NULL,
    mood                     VARCHAR(50)  NOT NULL,
    genre                    VARCHAR(50)  NOT NULL,
    illustration_level       VARCHAR(30)  NOT NULL,
    difficulty               VARCHAR(30)  NOT NULL,
    purpose_tags             JSON         NOT NULL,
    recommended_grade_min    INT          NULL,
    recommended_grade_max    INT          NULL,
    is_active                TINYINT(1)   NOT NULL DEFAULT 1,
    created_at               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_recommendation_books_title_author (title, author),
    KEY idx_recommendation_books_active_title (is_active, title, id),
    KEY idx_recommendation_books_grade (recommended_grade_min, recommended_grade_max)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE questions (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    book_id          BIGINT      NULL,
    class_id         BIGINT      NULL,
    teacher_id       BIGINT      NULL,
    question_type    VARCHAR(30) NOT NULL,
    stage            VARCHAR(20) NULL,
    content          TEXT        NOT NULL,
    expected_answer  TEXT        NULL,
    created_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE practice_progress (
    id                     BIGINT   NOT NULL AUTO_INCREMENT,
    student_id             BIGINT   NOT NULL,
    book_selected          BOOLEAN  NOT NULL DEFAULT FALSE,
    before_done            BOOLEAN  NOT NULL DEFAULT FALSE,
    class_read_done        BOOLEAN  NOT NULL DEFAULT FALSE,
    after_done             BOOLEAN  NOT NULL DEFAULT FALSE,
    during_type_progress   JSON     NULL,
    updated_at             DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_practice_progress_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE reading_records (
    id                      BIGINT       NOT NULL AUTO_INCREMENT,
    student_id              BIGINT       NOT NULL,
    book_id                 BIGINT       NOT NULL,
    current_stage           VARCHAR(20)  NOT NULL DEFAULT 'before',
    before_done             BOOLEAN      NOT NULL DEFAULT FALSE,
    during_done             BOOLEAN      NOT NULL DEFAULT FALSE,
    after_done              BOOLEAN      NOT NULL DEFAULT FALSE,
    current_page            INT          NULL,
    total_pages             INT          NULL,
    rating                  INT          NULL,
    represent_response_id   BIGINT       NULL,
    finished_at             DATETIME     NULL,
    created_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_reading_records_student_book (student_id, book_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE responses (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    student_id          BIGINT      NOT NULL,
    book_id             BIGINT      NULL,
    reading_record_id   BIGINT      NULL,
    question_id         BIGINT      NULL,
    parent_id           BIGINT      NULL,
    mode                VARCHAR(20) NOT NULL,
    content_type        VARCHAR(30) NOT NULL,
    stage               VARCHAR(20) NULL,
    content              TEXT        NULL,
    passed               BOOLEAN     NULL,
    status               VARCHAR(20) NOT NULL DEFAULT 'approved',
    extra_data           JSON        NULL,
    created_at           DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE summaries (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    student_id          BIGINT      NOT NULL,
    book_id             BIGINT      NULL,
    reading_record_id   BIGINT      NULL,
    book_type           VARCHAR(30) NULL,
    summary_text        TEXT        NOT NULL,
    is_shared           BOOLEAN     NOT NULL DEFAULT FALSE,
    status               VARCHAR(20) NOT NULL DEFAULT 'approved',
    created_at           DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE student_stats (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    student_id  BIGINT   NOT NULL,
    magic       INT      NOT NULL DEFAULT 8,
    stamina     INT      NOT NULL DEFAULT 8,
    wisdom      INT      NOT NULL DEFAULT 8,
    courage     INT      NOT NULL DEFAULT 8,
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_student_stats_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE student_stat_reward_log (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    student_id   BIGINT      NOT NULL,
    reward_type  VARCHAR(60) NOT NULL,
    stat_type    VARCHAR(20) NOT NULL,
    amount       INT         NOT NULL,
    granted_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_reward_log_student_reward (student_id, reward_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE content_likes (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    student_id    BIGINT      NOT NULL,
    content_type  VARCHAR(30) NOT NULL,
    content_id    BIGINT      NOT NULL,
    created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_content_likes_student_content (student_id, content_type, content_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- content_id는 content_type(summary/response/book_recommendation)에 따라
-- 가리키는 테이블이 달라지는 다형성 참조라 DB 레벨 FK를 걸지 않음 (애플리케이션에서 무결성 관리).

CREATE TABLE book_recommendations (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    student_id            BIGINT       NOT NULL,
    title                 VARCHAR(200) NOT NULL,
    author                VARCHAR(100) NULL,
    reason                TEXT         NOT NULL,
    teaser_response_ids   JSON         NULL,
    like_count            INT          NOT NULL DEFAULT 0,
    created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE dungeons (
    id                        BIGINT       NOT NULL AUTO_INCREMENT,
    name                      VARCHAR(100) NOT NULL,
    description               TEXT         NULL,
    difficulty                VARCHAR(20)  NULL,
    required_books            INT          NULL,
    required_stat_avg         INT          NULL,
    time_limit_seconds        INT          NULL,
    enemy_stats               JSON         NULL,
    reward_title              VARCHAR(100) NULL,
    reward_note               TEXT         NULL,
    reward_stat_reset_value   INT          NULL,
    created_at                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE dungeon_records (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    student_id  BIGINT      NOT NULL,
    dungeon_id  BIGINT      NOT NULL,
    result      VARCHAR(20) NOT NULL,
    played_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 2. ALTER TABLE - FOREIGN KEY 일괄 추가
-- ============================================================

-- classes
ALTER TABLE classes
    ADD CONSTRAINT fk_classes_teacher
    FOREIGN KEY (teacher_id) REFERENCES users(id);

-- class_students
ALTER TABLE class_students
    ADD CONSTRAINT fk_class_students_class
    FOREIGN KEY (class_id) REFERENCES classes(id);
ALTER TABLE class_students
    ADD CONSTRAINT fk_class_students_student
    FOREIGN KEY (student_id) REFERENCES users(id);

-- books
ALTER TABLE books
    ADD CONSTRAINT fk_books_class
    FOREIGN KEY (class_id) REFERENCES classes(id);
ALTER TABLE books
    ADD CONSTRAINT fk_books_registered_by
    FOREIGN KEY (registered_by) REFERENCES users(id);

-- questions
ALTER TABLE questions
    ADD CONSTRAINT fk_questions_book
    FOREIGN KEY (book_id) REFERENCES books(id);
ALTER TABLE questions
    ADD CONSTRAINT fk_questions_class
    FOREIGN KEY (class_id) REFERENCES classes(id);
ALTER TABLE questions
    ADD CONSTRAINT fk_questions_teacher
    FOREIGN KEY (teacher_id) REFERENCES users(id);

-- practice_progress
ALTER TABLE practice_progress
    ADD CONSTRAINT fk_practice_progress_student
    FOREIGN KEY (student_id) REFERENCES users(id);

-- reading_records (represent_response_id -> responses.id는 아래에서 별도 추가)
ALTER TABLE reading_records
    ADD CONSTRAINT fk_reading_records_student
    FOREIGN KEY (student_id) REFERENCES users(id);
ALTER TABLE reading_records
    ADD CONSTRAINT fk_reading_records_book
    FOREIGN KEY (book_id) REFERENCES books(id);

-- responses
ALTER TABLE responses
    ADD CONSTRAINT fk_responses_student
    FOREIGN KEY (student_id) REFERENCES users(id);
ALTER TABLE responses
    ADD CONSTRAINT fk_responses_book
    FOREIGN KEY (book_id) REFERENCES books(id);
ALTER TABLE responses
    ADD CONSTRAINT fk_responses_reading_record
    FOREIGN KEY (reading_record_id) REFERENCES reading_records(id);
ALTER TABLE responses
    ADD CONSTRAINT fk_responses_question
    FOREIGN KEY (question_id) REFERENCES questions(id);
ALTER TABLE responses
    ADD CONSTRAINT fk_responses_parent
    FOREIGN KEY (parent_id) REFERENCES responses(id);

-- reading_records <-> responses 순환 참조 마무리
-- (responses가 reading_records를 참조하는 FK가 먼저 걸린 뒤에 추가)
ALTER TABLE reading_records
    ADD CONSTRAINT fk_reading_records_represent_response
    FOREIGN KEY (represent_response_id) REFERENCES responses(id);

-- summaries
ALTER TABLE summaries
    ADD CONSTRAINT fk_summaries_student
    FOREIGN KEY (student_id) REFERENCES users(id);
ALTER TABLE summaries
    ADD CONSTRAINT fk_summaries_book
    FOREIGN KEY (book_id) REFERENCES books(id);
ALTER TABLE summaries
    ADD CONSTRAINT fk_summaries_reading_record
    FOREIGN KEY (reading_record_id) REFERENCES reading_records(id);

-- student_stats
ALTER TABLE student_stats
    ADD CONSTRAINT fk_student_stats_student
    FOREIGN KEY (student_id) REFERENCES users(id);

-- student_stat_reward_log
ALTER TABLE student_stat_reward_log
    ADD CONSTRAINT fk_reward_log_student
    FOREIGN KEY (student_id) REFERENCES users(id);

-- content_likes (content_id는 다형성 참조라 FK 없음, student_id만)
ALTER TABLE content_likes
    ADD CONSTRAINT fk_content_likes_student
    FOREIGN KEY (student_id) REFERENCES users(id);

-- book_recommendations
ALTER TABLE book_recommendations
    ADD CONSTRAINT fk_book_recommendations_student
    FOREIGN KEY (student_id) REFERENCES users(id);

-- dungeon_records
ALTER TABLE dungeon_records
    ADD CONSTRAINT fk_dungeon_records_student
    FOREIGN KEY (student_id) REFERENCES users(id);
ALTER TABLE dungeon_records
    ADD CONSTRAINT fk_dungeon_records_dungeon
    FOREIGN KEY (dungeon_id) REFERENCES dungeons(id);

-- ============================================================
-- 3. MIGRATIONS - 이미 생성된 테이블에 반영할 변경 사항
-- ============================================================

-- books.cover_image: TEXT(약 64KB 한도) -> LONGTEXT(최대 4GB)
-- book-select.html에서 표지 이미지를 압축 없이 base64로 그대로 저장하기 때문에
-- TEXT로는 스마트폰 사진 한 장도 못 담을 수 있어 용량 한도를 늘림.
ALTER TABLE books
    MODIFY COLUMN cover_image LONGTEXT NULL;

-- classes.class_number 컬럼 추가 (class_name 다음, grade 이전)
-- teacher-register.html의 classSelect(<select value="1"~"10">)가 반 번호를 문자열로 넘기지만
-- 의미상 숫자(정렬/비교 대상)라 grade와 동일하게 INT로 저장, 백엔드에서 parseInt 처리.
ALTER TABLE classes
    ADD COLUMN class_number INT NULL AFTER class_name;
