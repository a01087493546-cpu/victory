package com.victory.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PracticeAchievementResponse {

    private Long classId;
    private Long classReadingBookId;

    private Integer totalStudentCount;
    private Integer todayParticipatingStudentCount;
    private Double todayParticipationRate;

    private Integer supportNeededCount;

    private List<StudentAchievementItem> students;
}
