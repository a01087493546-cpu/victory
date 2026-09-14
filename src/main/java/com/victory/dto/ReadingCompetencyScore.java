package com.victory.dto;

/*
 * 개별읽기 성장 포트폴리오 전용 독서 역량 1종의 0~100 점수 + 4단계
 * 성장 수준. level은 프론트가 그대로 쓸 수 있도록 enum이 아니라 한국어
 * 라벨 문자열로 직렬화한다({"score":72,"level":"우수"}).
 */
public record ReadingCompetencyScore(int score, String level) {

    public static ReadingCompetencyScore of(double rawScore) {
        int clamped = (int) Math.round(Math.max(0.0, Math.min(100.0, rawScore)));
        return new ReadingCompetencyScore(clamped, ReadingCompetencyLevel.fromScore(clamped).getLabel());
    }
}
