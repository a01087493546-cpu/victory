package com.victory.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

/*
 * 교사용 개별읽기 대시보드 전체 응답. todayParticipationRate는
 * individualReadingStudentCount(개별읽기를 한 번이라도 시작한 학생 수)가
 * 0이면 null이 되고, 그 경우 todayParticipationMessage에 안내 문구가
 * 채워진다(퍼센트 대신 상태를 표현).
 */
@Getter
@AllArgsConstructor
public class TeacherIndividualReadingDashboardResponse {

    private Long classId;
    private String className;
    private LocalDate todayDate;

    private Double todayParticipationRate;
    private String todayParticipationMessage;
    private int todayActiveStudentCount;

    /*
     * 오늘 참여율의 분모 - ReadingRecord가 1건 이상 있는 학생 수(진행 중
     * 여부와 무관, 완독만 한 학생도 포함). 예전 이름 activeReadingStudentCount는
     * "진행 중"으로 오해하기 쉬워 이름을 바꿨다.
     */
    private int individualReadingStudentCount;

    private int supportNeededStudentCount;

    private List<TeacherIndividualReadingStudentResponse> students;
}
