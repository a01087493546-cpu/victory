package com.victory.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TemporaryPasswordResponse {
    private Long studentId;
    private String loginId;
    private String name;
    private String temporaryPassword;
}
