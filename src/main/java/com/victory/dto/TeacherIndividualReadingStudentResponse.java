package com.victory.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

/*
 * 교사용 개별읽기 대시보드의 학생 1명분 현황. 대표 ReadingRecord가 없는
 * 학생(개별읽기를 아직 시작하지 않음)도 목록에서 빠지지 않고 0점/집중
 * 지원 상태로 함께 내려간다.
 */
@Getter
@AllArgsConstructor
public class TeacherIndividualReadingStudentResponse {

    private Long studentId;
    private String studentName;
    private Integer studentNumber;

    private Long representativeReadingRecordId;
    private String currentBookTitle;

    /*
     * 학생당 진행 중(finished_at IS NULL) 기록은 항상 0개 또는 1개만
     * 허용되는 현재 구조를 그대로 반영한 값이다(IndividualReadingService
     * 참고). representativeReadingRecordId가 "최근 완독" 기록일 수도
     * 있어서 그것만으로는 진행 중 여부를 프론트가 정확히 구분할 수
     * 없으므로 이 필드를 따로 내려준다.
     */
    private int activeReadingBookCount;

    private long totalCompletedBookCount;
    private int readingDays;
    private double readingPracticeScore;
    private int completedStageCount;
    private double recordCompletionScore;
    private int inspectedItemCount;
    private double contentSuitabilityScore;
    private double overallAchievementScore;
    private int roundedOverallAchievementScore;
    private String achievementLevel;

    private LocalDate latestActivityDate;
    private boolean activeToday;

    private List<String> supportReasons;
}
