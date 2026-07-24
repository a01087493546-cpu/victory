package com.victory.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PracticeAchievementHistoryItem {

    private LocalDate date;
    private Double participationRate;
    private Double comprehensionRate;
    private Double achievementRate;
    private Double finalReadingProgress;
}
