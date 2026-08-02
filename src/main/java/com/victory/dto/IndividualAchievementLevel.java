package com.victory.dto;

/*
 * 개별읽기 종합달성도 등급. 반올림된 종합달성도(0~100 정수) 기준으로 판정한다.
 * 문자열을 코드 곳곳에 흩어 두지 않고 이 enum 하나로만 관리한다.
 */
public enum IndividualAchievementLevel {

    VERY_GOOD("매우 우수"),
    GOOD("우수"),
    NORMAL("보통"),
    NEED_SUPPORT("집중 지원");

    private final String label;

    IndividualAchievementLevel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static IndividualAchievementLevel fromRoundedScore(int roundedScore) {
        if (roundedScore >= 85) {
            return VERY_GOOD;
        }

        if (roundedScore >= 70) {
            return GOOD;
        }

        if (roundedScore >= 50) {
            return NORMAL;
        }

        return NEED_SUPPORT;
    }
}
