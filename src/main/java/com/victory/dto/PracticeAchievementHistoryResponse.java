package com.victory.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PracticeAchievementHistoryResponse {

    private Long studentId;
    private String studentName;
    private List<PracticeAchievementHistoryItem> history;
}
