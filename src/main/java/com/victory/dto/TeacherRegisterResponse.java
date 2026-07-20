package com.victory.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TeacherRegisterResponse {

    private Long teacherId;
    private Long classId;
    private String teacherName;
    private String school;
    private String className;
    private Integer grade;
    private Integer classNumber;
    private List<StudentResponse> students;

    @Getter
    @AllArgsConstructor
    public static class StudentResponse {

        private Long studentId;
        private String loginId;
        private String name;
        private Integer studentNumber;
    }
}