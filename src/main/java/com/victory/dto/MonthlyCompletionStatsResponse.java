package com.victory.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

/*
 * 학생 메인 화면 "월별 완독 기록" 그래프용 (GET .../monthly-completion-stats).
 * 완독(finished_at IS NOT NULL)한 reading_records를 현재 연도 기준으로
 * 1~12월 집계한다. monthlyCounts는 항상 12개 원소이며 index 0이 1월,
 * index 11이 12월이다.
 */
@Getter
@AllArgsConstructor
public class MonthlyCompletionStatsResponse {

    private int year;
    private List<Integer> monthlyCounts;
}
