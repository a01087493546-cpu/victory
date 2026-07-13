package com.victory.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private Long id;
    private String loginId;
    private String name;
    private String role;
}
