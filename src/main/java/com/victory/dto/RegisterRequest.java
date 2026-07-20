package com.victory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RegisterRequest {

    @NotBlank
    @Size(max = 50)
    private String loginId;

    @NotBlank
    private String password;

    @NotBlank
    @Size(max = 50)
    private String name;

    @NotBlank
    @Size(max = 20)
    private String role;
}
