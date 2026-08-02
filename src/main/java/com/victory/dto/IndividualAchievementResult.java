package com.victory.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;

/*
 * 개별읽기 지표(교사용 평가) 계산 결과. 최종 점수뿐 아니라 중간 계산값도
 * 함께 담아서, 테스트와 향후 교사 화면에서 "왜 이 점수가 나왔는지" 근거를
 * 바로 확인할 수 있게 한다.
 */
@Getter
@AllArgsConstructor
public class IndividualAchievementResult {

    private Long studentId;
    private Long readingRecordId;

    // 독서실천도 근거
    private int readingDays;
    private double readingDaysScore;
    private int activityTypeCount;
    private double activityTypeScore;
    private double readingPracticeScore;

    // 기록완성도 근거
    private int completedStageCount;
    private double stageCompletionRate;
    private int inspectedItemCount;
    private int passedWithinThreeCount;
    private double contentSuitabilityScore;
    private double recordCompletionScore;

    // 종합
    private double overallAchievementScore;
    private int roundedOverallAchievementScore;
    private IndividualAchievementLevel achievementLevel;

    // 학생 전체 누적 완독 권수
    private long totalCompletedBookCount;

    /*
     * 교사용 대시보드(지원 필요 사유·최근 활동일)가 재사용하는 값들.
     * calculate() 내부에서 이미 조회한 데이터로 계산하므로 이 필드들
     * 때문에 별도 쿼리가 추가되지 않는다.
     */
    private boolean wroteQuestionActivity;
    private int bookChatPostCount;
    private LocalDate latestActivityDate;
}
