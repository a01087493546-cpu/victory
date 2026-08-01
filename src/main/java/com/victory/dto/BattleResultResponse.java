package com.victory.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BattleResultResponse {

    private String result;
    private boolean rewardApplied;
    private int attemptsLeftToday;
    private boolean showEnding;
    private StudentStatsResponse updatedStats;
}
