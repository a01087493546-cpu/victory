package com.victory.dto;

/*
 * 개별읽기 "독서 역량 4종"과 온책읽기 핵심지표(참여도/질문 이해도)가
 * 공유하는 4단계 성장 수준. 0~100 절대 점수(반 평균/상위 학생 기준
 * 정규화 없음) 기준으로 판정한다. IndividualAchievementLevel(종합달성도
 * 등급, 85/70/50 경계)과는 별개 개념이라 문자열/경계값을 공유하지 않고
 * 이 enum으로 독립 관리한다.
 */
public enum ReadingCompetencyLevel {

    NEEDS_EFFORT("노력 필요"),
    AVERAGE("보통"),
    PROFICIENT("우수"),
    EXCELLENT("매우 우수");

    private final String label;

    ReadingCompetencyLevel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /*
     * 0~39 노력 필요, 40~59 보통, 60~79 우수, 80~100 매우 우수.
     * score는 호출 전에 0~100으로 clamp되어 있어야 한다(ReadingCompetencyScore.of 참고).
     */
    public static ReadingCompetencyLevel fromScore(int score) {
        if (score >= 80) {
            return EXCELLENT;
        }

        if (score >= 60) {
            return PROFICIENT;
        }

        if (score >= 40) {
            return AVERAGE;
        }

        return NEEDS_EFFORT;
    }
}
