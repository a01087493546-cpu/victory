START TRANSACTION;

INSERT INTO recommendation_books (
    title, author, description, cover_image,
    thickness, mood, genre, illustration_level, difficulty, purpose_tags,
    recommended_grade_min, recommended_grade_max, is_active, created_at, updated_at
)
SELECT
    '만복이네 떡집',
    '김리리',
    '친구에게 거친 말을 하던 만복이가 신비한 떡을 먹으며 자신의 말과 행동을 돌아보는 이야기입니다.',
    NULL,
    'thin',
    'funny',
    'fantasy',
    'many',
    'easy',
    JSON_ARRAY('fun', 'imagination', 'comfort'),
    3,
    4,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM recommendation_books WHERE title = '만복이네 떡집' AND author = '김리리'
);

INSERT INTO recommendation_books (
    title, author, description, cover_image,
    thickness, mood, genre, illustration_level, difficulty, purpose_tags,
    recommended_grade_min, recommended_grade_max, is_active, created_at, updated_at
)
SELECT
    '긴긴밤',
    '루리',
    '세상에 마지막으로 남은 흰바위코뿔소와 어린 펭귄이 서로를 의지하며 바다를 찾아가는 이야기입니다.',
    NULL,
    'thin',
    'touching',
    'friendship',
    'many',
    'easy',
    JSON_ARRAY('comfort', 'challenge', 'imagination'),
    3,
    5,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM recommendation_books WHERE title = '긴긴밤' AND author = '루리'
);

INSERT INTO recommendation_books (
    title, author, description, cover_image,
    thickness, mood, genre, illustration_level, difficulty, purpose_tags,
    recommended_grade_min, recommended_grade_max, is_active, created_at, updated_at
)
SELECT
    '나쁜 어린이표',
    '황선미',
    '선생님에게 나쁜 어린이표를 받은 건우가 자신의 마음과 학교생활을 돌아보는 이야기입니다.',
    NULL,
    'thin',
    'touching',
    'daily_life',
    'many',
    'easy',
    JSON_ARRAY('comfort', 'fun'),
    3,
    4,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM recommendation_books WHERE title = '나쁜 어린이표' AND author = '황선미'
);

INSERT INTO recommendation_books (
    title, author, description, cover_image,
    thickness, mood, genre, illustration_level, difficulty, purpose_tags,
    recommended_grade_min, recommended_grade_max, is_active, created_at, updated_at
)
SELECT
    '가방 들어 주는 아이',
    '고정욱',
    '다리가 불편한 친구의 가방을 들어 주게 된 아이가 우정과 배려를 배워 가는 이야기입니다.',
    NULL,
    'thin',
    'touching',
    'friendship',
    'medium',
    'easy',
    JSON_ARRAY('comfort', 'challenge'),
    3,
    4,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM recommendation_books WHERE title = '가방 들어 주는 아이' AND author = '고정욱'
);

INSERT INTO recommendation_books (
    title, author, description, cover_image,
    thickness, mood, genre, illustration_level, difficulty, purpose_tags,
    recommended_grade_min, recommended_grade_max, is_active, created_at, updated_at
)
SELECT
    '아홉 살 마음 사전',
    '박성우',
    '어린이가 생활 속에서 느끼는 여러 감정의 뜻과 상황을 쉽고 재미있게 알려 주는 책입니다.',
    NULL,
    'thin',
    'calm',
    'daily_life',
    'many',
    'easy',
    JSON_ARRAY('comfort', 'knowledge'),
    3,
    4,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM recommendation_books WHERE title = '아홉 살 마음 사전' AND author = '박성우'
);

INSERT INTO recommendation_books (
    title, author, description, cover_image,
    thickness, mood, genre, illustration_level, difficulty, purpose_tags,
    recommended_grade_min, recommended_grade_max, is_active, created_at, updated_at
)
SELECT
    '강아지똥',
    '권정생',
    '아무 쓸모가 없다고 생각했던 강아지똥이 민들레꽃을 피우며 자신의 가치를 발견하는 이야기입니다.',
    NULL,
    'thin',
    'touching',
    'nature',
    'many',
    'easy',
    JSON_ARRAY('comfort', 'imagination'),
    2,
    4,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM recommendation_books WHERE title = '강아지똥' AND author = '권정생'
);

INSERT INTO recommendation_books (
    title, author, description, cover_image,
    thickness, mood, genre, illustration_level, difficulty, purpose_tags,
    recommended_grade_min, recommended_grade_max, is_active, created_at, updated_at
)
SELECT
    '한밤중 달빛 식당',
    '이분희',
    '힘든 기억을 가진 아이가 신비한 달빛 식당에서 위로와 용기를 얻는 이야기입니다.',
    NULL,
    'thin',
    'mysterious',
    'fantasy',
    'many',
    'easy',
    JSON_ARRAY('comfort', 'imagination', 'fun'),
    3,
    4,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM recommendation_books WHERE title = '한밤중 달빛 식당' AND author = '이분희'
);

INSERT INTO recommendation_books (
    title, author, description, cover_image,
    thickness, mood, genre, illustration_level, difficulty, purpose_tags,
    recommended_grade_min, recommended_grade_max, is_active, created_at, updated_at
)
SELECT
    '까막눈 삼디기',
    '원유순',
    '글을 잘 읽지 못하던 삼디기가 친구의 도움을 받으며 자신감을 찾아가는 이야기입니다.',
    NULL,
    'thin',
    'touching',
    'friendship',
    'many',
    'easy',
    JSON_ARRAY('comfort', 'challenge'),
    3,
    4,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM recommendation_books WHERE title = '까막눈 삼디기' AND author = '원유순'
);

INSERT INTO recommendation_books (
    title, author, description, cover_image,
    thickness, mood, genre, illustration_level, difficulty, purpose_tags,
    recommended_grade_min, recommended_grade_max, is_active, created_at, updated_at
)
SELECT
    '책 먹는 여우',
    '프란치스카 비어만',
    '책을 너무 좋아해 읽은 뒤 소금과 후추를 뿌려 먹어 버리는 여우의 유쾌한 이야기입니다.',
    NULL,
    'thin',
    'funny',
    'fantasy',
    'many',
    'easy',
    JSON_ARRAY('fun', 'imagination'),
    2,
    4,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM recommendation_books WHERE title = '책 먹는 여우' AND author = '프란치스카 비어만'
);

INSERT INTO recommendation_books (
    title, author, description, cover_image,
    thickness, mood, genre, illustration_level, difficulty, purpose_tags,
    recommended_grade_min, recommended_grade_max, is_active, created_at, updated_at
)
SELECT
    '내 짝꿍 최영대',
    '채인선',
    '말수가 적고 놀림을 받던 영대와 반 친구들의 관계가 조금씩 변화하는 이야기입니다.',
    NULL,
    'thin',
    'touching',
    'friendship',
    'many',
    'easy',
    JSON_ARRAY('comfort', 'challenge'),
    3,
    4,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM recommendation_books WHERE title = '내 짝꿍 최영대' AND author = '채인선'
);

INSERT INTO recommendation_books (
    title, author, description, cover_image,
    thickness, mood, genre, illustration_level, difficulty, purpose_tags,
    recommended_grade_min, recommended_grade_max, is_active, created_at, updated_at
)
SELECT
    '시간 가게',
    '이나영',
    '시간을 사고파는 신비한 가게를 만난 아이가 시간과 행복의 의미를 깨닫는 이야기입니다.',
    NULL,
    'medium',
    'mysterious',
    'fantasy',
    'medium',
    'medium',
    JSON_ARRAY('imagination', 'challenge', 'fun'),
    4,
    5,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM recommendation_books WHERE title = '시간 가게' AND author = '이나영'
);

INSERT INTO recommendation_books (
    title, author, description, cover_image,
    thickness, mood, genre, illustration_level, difficulty, purpose_tags,
    recommended_grade_min, recommended_grade_max, is_active, created_at, updated_at
)
SELECT
    '이상한 과자 가게 전천당 1',
    '히로시마 레이코',
    '손님의 고민에 맞는 신비한 과자를 파는 전천당에서 벌어지는 여러 사건을 담은 이야기입니다.',
    NULL,
    'medium',
    'mysterious',
    'fantasy',
    'many',
    'medium',
    JSON_ARRAY('fun', 'imagination'),
    3,
    5,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM recommendation_books WHERE title = '이상한 과자 가게 전천당 1' AND author = '히로시마 레이코'
);

INSERT INTO recommendation_books (
    title, author, description, cover_image,
    thickness, mood, genre, illustration_level, difficulty, purpose_tags,
    recommended_grade_min, recommended_grade_max, is_active, created_at, updated_at
)
SELECT
    '푸른 사자 와니니 1',
    '이현',
    '무리에서 쫓겨난 어린 사자 와니니가 초원을 여행하며 자신만의 힘을 찾아가는 이야기입니다.',
    NULL,
    'medium',
    'exciting',
    'adventure',
    'medium',
    'medium',
    JSON_ARRAY('challenge', 'imagination', 'fun'),
    4,
    6,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM recommendation_books WHERE title = '푸른 사자 와니니 1' AND author = '이현'
);

INSERT INTO recommendation_books (
    title, author, description, cover_image,
    thickness, mood, genre, illustration_level, difficulty, purpose_tags,
    recommended_grade_min, recommended_grade_max, is_active, created_at, updated_at
)
SELECT
    '불량한 자전거 여행',
    '김남중',
    '가족에게 화가 난 아이가 삼촌과 자전거 여행을 하며 가족과 자신을 이해해 가는 이야기입니다.',
    NULL,
    'medium',
    'exciting',
    'growth',
    'few',
    'medium',
    JSON_ARRAY('challenge', 'fun'),
    4,
    6,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM recommendation_books WHERE title = '불량한 자전거 여행' AND author = '김남중'
);

INSERT INTO recommendation_books (
    title, author, description, cover_image,
    thickness, mood, genre, illustration_level, difficulty, purpose_tags,
    recommended_grade_min, recommended_grade_max, is_active, created_at, updated_at
)
SELECT
    '5번 레인',
    '은소홀',
    '수영 선수인 나루가 경쟁과 갈등을 겪으며 자신의 마음과 진짜 목표를 찾아가는 이야기입니다.',
    NULL,
    'medium',
    'exciting',
    'growth',
    'few',
    'medium',
    JSON_ARRAY('challenge', 'comfort'),
    4,
    6,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM recommendation_books WHERE title = '5번 레인' AND author = '은소홀'
);

INSERT INTO recommendation_books (
    title, author, description, cover_image,
    thickness, mood, genre, illustration_level, difficulty, purpose_tags,
    recommended_grade_min, recommended_grade_max, is_active, created_at, updated_at
)
SELECT
    '복제인간 윤봉구',
    '임은하',
    '자신이 복제인간이라는 사실을 알게 된 봉구가 가족과 자신의 존재에 대해 고민하는 이야기입니다.',
    NULL,
    'medium',
    'funny',
    'science',
    'medium',
    'medium',
    JSON_ARRAY('imagination', 'challenge', 'fun'),
    4,
    6,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM recommendation_books WHERE title = '복제인간 윤봉구' AND author = '임은하'
);

INSERT INTO recommendation_books (
    title, author, description, cover_image,
    thickness, mood, genre, illustration_level, difficulty, purpose_tags,
    recommended_grade_min, recommended_grade_max, is_active, created_at, updated_at
)
SELECT
    '마당을 나온 암탉',
    '황선미',
    '양계장을 벗어난 암탉 잎싹이 자신의 꿈을 이루고 생명을 돌보는 이야기입니다.',
    NULL,
    'medium',
    'touching',
    'growth',
    'medium',
    'medium',
    JSON_ARRAY('comfort', 'challenge'),
    4,
    6,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM recommendation_books WHERE title = '마당을 나온 암탉' AND author = '황선미'
);

INSERT INTO recommendation_books (
    title, author, description, cover_image,
    thickness, mood, genre, illustration_level, difficulty, purpose_tags,
    recommended_grade_min, recommended_grade_max, is_active, created_at, updated_at
)
SELECT
    '건방이의 건방진 수련기 1',
    '천효정',
    '평범한 아이 건방이가 무술 스승을 만나 수련하며 성장하는 유쾌한 모험 이야기입니다.',
    NULL,
    'medium',
    'funny',
    'adventure',
    'medium',
    'medium',
    JSON_ARRAY('fun', 'challenge', 'imagination'),
    3,
    5,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM recommendation_books WHERE title = '건방이의 건방진 수련기 1' AND author = '천효정'
);

INSERT INTO recommendation_books (
    title, author, description, cover_image,
    thickness, mood, genre, illustration_level, difficulty, purpose_tags,
    recommended_grade_min, recommended_grade_max, is_active, created_at, updated_at
)
SELECT
    '우주로 가는 계단',
    '전수경',
    '우주와 가족에 관한 비밀을 간직한 아이가 특별한 만남을 통해 상처를 마주하는 이야기입니다.',
    NULL,
    'medium',
    'mysterious',
    'science',
    'medium',
    'medium',
    JSON_ARRAY('imagination', 'comfort', 'knowledge'),
    4,
    6,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM recommendation_books WHERE title = '우주로 가는 계단' AND author = '전수경'
);

INSERT INTO recommendation_books (
    title, author, description, cover_image,
    thickness, mood, genre, illustration_level, difficulty, purpose_tags,
    recommended_grade_min, recommended_grade_max, is_active, created_at, updated_at
)
SELECT
    '악플 전쟁',
    '이규희',
    '인터넷에 남긴 악성 댓글로 갈등을 겪는 아이들이 말과 책임의 중요성을 배우는 이야기입니다.',
    NULL,
    'medium',
    'exciting',
    'daily_life',
    'medium',
    'medium',
    JSON_ARRAY('knowledge', 'challenge'),
    4,
    6,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM recommendation_books WHERE title = '악플 전쟁' AND author = '이규희'
);

INSERT INTO recommendation_books (
    title, author, description, cover_image,
    thickness, mood, genre, illustration_level, difficulty, purpose_tags,
    recommended_grade_min, recommended_grade_max, is_active, created_at, updated_at
)
SELECT
    '샬롯의 거미줄',
    'E. B. 화이트',
    '돼지 윌버와 거미 샬롯이 서로를 지키며 특별한 우정을 나누는 이야기입니다.',
    NULL,
    'medium',
    'touching',
    'friendship',
    'medium',
    'medium',
    JSON_ARRAY('comfort', 'challenge'),
    4,
    6,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM recommendation_books WHERE title = '샬롯의 거미줄' AND author = 'E. B. 화이트'
);

INSERT INTO recommendation_books (
    title, author, description, cover_image,
    thickness, mood, genre, illustration_level, difficulty, purpose_tags,
    recommended_grade_min, recommended_grade_max, is_active, created_at, updated_at
)
SELECT
    '찰리와 초콜릿 공장',
    '로알드 달',
    '찰리가 신비한 초콜릿 공장을 견학하며 놀라운 사건을 경험하는 이야기입니다.',
    NULL,
    'medium',
    'funny',
    'fantasy',
    'medium',
    'medium',
    JSON_ARRAY('fun', 'imagination'),
    4,
    6,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM recommendation_books WHERE title = '찰리와 초콜릿 공장' AND author = '로알드 달'
);

INSERT INTO recommendation_books (
    title, author, description, cover_image,
    thickness, mood, genre, illustration_level, difficulty, purpose_tags,
    recommended_grade_min, recommended_grade_max, is_active, created_at, updated_at
)
SELECT
    '내 이름은 삐삐 롱스타킹',
    '아스트리드 린드그렌',
    '힘이 세고 자유로운 삐삐가 친구들과 벌이는 엉뚱하고 신나는 모험 이야기입니다.',
    NULL,
    'medium',
    'funny',
    'adventure',
    'medium',
    'medium',
    JSON_ARRAY('fun', 'imagination', 'challenge'),
    4,
    6,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM recommendation_books WHERE title = '내 이름은 삐삐 롱스타킹' AND author = '아스트리드 린드그렌'
);

INSERT INTO recommendation_books (
    title, author, description, cover_image,
    thickness, mood, genre, illustration_level, difficulty, purpose_tags,
    recommended_grade_min, recommended_grade_max, is_active, created_at, updated_at
)
SELECT
    '아낌없이 주는 나무',
    '셸 실버스타인',
    '한 나무가 사랑하는 소년에게 자신의 모든 것을 내어 주는 이야기입니다.',
    NULL,
    'thin',
    'calm',
    'growth',
    'many',
    'medium',
    JSON_ARRAY('comfort', 'imagination'),
    3,
    6,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM recommendation_books WHERE title = '아낌없이 주는 나무' AND author = '셸 실버스타인'
);

INSERT INTO recommendation_books (
    title, author, description, cover_image,
    thickness, mood, genre, illustration_level, difficulty, purpose_tags,
    recommended_grade_min, recommended_grade_max, is_active, created_at, updated_at
)
SELECT
    '초정리 편지',
    '배유안',
    '조선 시대 아이 장운이 글을 배우고 편지를 쓰며 자신의 삶을 넓혀 가는 역사 이야기입니다.',
    NULL,
    'medium',
    'touching',
    'history',
    'few',
    'medium',
    JSON_ARRAY('knowledge', 'challenge'),
    4,
    6,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM recommendation_books WHERE title = '초정리 편지' AND author = '배유안'
);

INSERT INTO recommendation_books (
    title, author, description, cover_image,
    thickness, mood, genre, illustration_level, difficulty, purpose_tags,
    recommended_grade_min, recommended_grade_max, is_active, created_at, updated_at
)
SELECT
    '담을 넘은 아이',
    '김정민',
    '조선 시대 여자아이가 자신을 가로막는 차별과 한계를 넘어서는 이야기입니다.',
    NULL,
    'medium',
    'touching',
    'history',
    'few',
    'hard',
    JSON_ARRAY('challenge', 'knowledge'),
    4,
    6,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM recommendation_books WHERE title = '담을 넘은 아이' AND author = '김정민'
);

INSERT INTO recommendation_books (
    title, author, description, cover_image,
    thickness, mood, genre, illustration_level, difficulty, purpose_tags,
    recommended_grade_min, recommended_grade_max, is_active, created_at, updated_at
)
SELECT
    '책과 노니는 집',
    '이영서',
    '조선 시대 책을 둘러싼 사건 속에서 한 아이가 책과 사람의 소중함을 알아 가는 이야기입니다.',
    NULL,
    'thick',
    'touching',
    'history',
    'few',
    'hard',
    JSON_ARRAY('knowledge', 'challenge'),
    4,
    6,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM recommendation_books WHERE title = '책과 노니는 집' AND author = '이영서'
);

INSERT INTO recommendation_books (
    title, author, description, cover_image,
    thickness, mood, genre, illustration_level, difficulty, purpose_tags,
    recommended_grade_min, recommended_grade_max, is_active, created_at, updated_at
)
SELECT
    '마틸다',
    '로알드 달',
    '책을 사랑하고 특별한 능력을 가진 마틸다가 부당한 어른들에게 맞서는 이야기입니다.',
    NULL,
    'thick',
    'funny',
    'growth',
    'medium',
    'hard',
    JSON_ARRAY('challenge', 'fun', 'imagination'),
    4,
    6,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM recommendation_books WHERE title = '마틸다' AND author = '로알드 달'
);

INSERT INTO recommendation_books (
    title, author, description, cover_image,
    thickness, mood, genre, illustration_level, difficulty, purpose_tags,
    recommended_grade_min, recommended_grade_max, is_active, created_at, updated_at
)
SELECT
    '몽실 언니',
    '권정생',
    '어려운 시대를 살아가는 몽실이가 가족을 돌보며 꿋꿋하게 성장하는 이야기입니다.',
    NULL,
    'thick',
    'touching',
    'history',
    'few',
    'hard',
    JSON_ARRAY('challenge', 'knowledge', 'comfort'),
    5,
    6,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM recommendation_books WHERE title = '몽실 언니' AND author = '권정생'
);

INSERT INTO recommendation_books (
    title, author, description, cover_image,
    thickness, mood, genre, illustration_level, difficulty, purpose_tags,
    recommended_grade_min, recommended_grade_max, is_active, created_at, updated_at
)
SELECT
    '어린 왕자',
    '앙투안 드 생텍쥐페리',
    '여러 별을 여행한 어린 왕자가 관계와 사랑, 책임의 의미를 들려주는 이야기입니다.',
    NULL,
    'medium',
    'calm',
    'fantasy',
    'medium',
    'hard',
    JSON_ARRAY('imagination', 'challenge', 'comfort'),
    5,
    6,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM recommendation_books WHERE title = '어린 왕자' AND author = '앙투안 드 생텍쥐페리'
);

COMMIT;
