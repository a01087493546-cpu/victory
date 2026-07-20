package com.victory.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TeacherClassResponse {

    private Long teacherId;
    private String teacherName;
    private String school;

    private Long classId;
    private String className;
    private Integer grade;
    private Integer classNumber;
}