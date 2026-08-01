INSERT INTO dungeons (
    name, description, difficulty, required_books, required_stat_avg,
    prerequisite_dungeon_id, time_limit_seconds, enemy_stats,
    reward_title, reward_note, reward_stat_reset_value, created_at, updated_at
)
SELECT
    '초급용',
    '처음 만나는 던전. 헤츨링을 물리치고 지식을 되찾아 보세요.',
    '초급',
    10,
    20,
    NULL,
    180,
    NULL,
    NULL,
    NULL,
    10,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM dungeons WHERE name = '초급용'
);

INSERT INTO dungeons (
    name, description, difficulty, required_books, required_stat_avg,
    prerequisite_dungeon_id, time_limit_seconds, enemy_stats,
    reward_title, reward_note, reward_stat_reset_value, created_at, updated_at
)
SELECT
    '중급용',
    '초급용을 클리어한 용사만 도전할 수 있는 던전. 용과 맞서 싸워보세요.',
    '중급',
    30,
    55,
    (SELECT id FROM dungeons WHERE name = '초급용'),
    300,
    NULL,
    NULL,
    NULL,
    15,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM dungeons WHERE name = '중급용'
);

INSERT INTO dungeons (
    name, description, difficulty, required_books, required_stat_avg,
    prerequisite_dungeon_id, time_limit_seconds, enemy_stats,
    reward_title, reward_note, reward_stat_reset_value, created_at, updated_at
)
SELECT
    '고급용',
    '마지막 관문. 나이 많은 용을 쓰러뜨리고 모험을 완성하세요.',
    '고급',
    50,
    85,
    (SELECT id FROM dungeons WHERE name = '중급용'),
    420,
    NULL,
    NULL,
    NULL,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM dungeons WHERE name = '고급용'
);
