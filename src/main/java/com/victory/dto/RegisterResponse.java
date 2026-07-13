package com.victory.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class RegisterResponse {

    private Long id;
    private String loginId;
    private String name;
    private String role;
    private LocalDateTime createdAt;
}
