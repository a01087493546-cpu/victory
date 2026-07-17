package com.victory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class StudentRegisterRequest {

    @NotBlank
    @Size(max = 50)
    private String loginId;

    @NotBlank
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
        message = "비밀번호는 영문과 숫자를 모두 포함해야 합니다."
    )
    private String password;

    @NotBlank
    @Size(max = 50)
    private String name;

    @NotNull
    private Integer studentNumber;
}