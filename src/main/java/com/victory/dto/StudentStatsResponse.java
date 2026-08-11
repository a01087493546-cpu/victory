package com.victory.dto;

import com.victory.entity.StudentStats;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StudentStatsResponse {

    private Integer magic;
    private Integer stamina;
    private Integer wisdom;
    private Integer courage;

    /*
     * 고급 던전 엔딩을 끝까지 본 학생인지 여부. "나의 힘" 화면이 이 값으로
     * 완료 안내 배지를 보여준다. 보상 결과(RewardResult) 응답 등 이 값이
     * 필요 없는 기존 호출부는 from(stats) 1-인자 오버로드를 그대로 쓰면
     * false로 채워져 기존 동작과 동일하다.
     */
    private Boolean hasSeenEnding;

    public static StudentStatsResponse zero() {
        return new StudentStatsResponse(0, 0, 0, 0, false);
    }

    public static StudentStatsResponse from(StudentStats stats) {
        return from(stats, false);
    }

    public static StudentStatsResponse from(StudentStats stats, boolean hasSeenEnding) {
        if (stats == null) {
            return new StudentStatsResponse(0, 0, 0, 0, hasSeenEnding);
        }

        return new StudentStatsResponse(
            stats.getMagic(),
            stats.getStamina(),
            stats.getWisdom(),
            stats.getCourage(),
            hasSeenEnding
        );
    }
}
