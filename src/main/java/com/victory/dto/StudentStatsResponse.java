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

    public static StudentStatsResponse zero() {
        return new StudentStatsResponse(0, 0, 0, 0);
    }

    public static StudentStatsResponse from(StudentStats stats) {
        if (stats == null) {
            return zero();
        }

        return new StudentStatsResponse(
            stats.getMagic(),
            stats.getStamina(),
            stats.getWisdom(),
            stats.getCourage()
        );
    }
}
