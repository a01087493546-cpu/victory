package com.victory.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TeacherManagedAccountsResponse {

    private AccountItem teacher;
    private List<AccountItem> students;

    @Getter
    @AllArgsConstructor
    public static class AccountItem {
        private Long id;
        private String name;
        private String loginId;
        private String role;
        private String school;
        private String className;
        private Integer grade;
        private Integer classNumber;
        private Integer studentNumber;
        private LocalDateTime createdAt;
        private boolean self;
    }
}
