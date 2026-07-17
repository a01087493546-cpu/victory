package com.victory.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TeacherRegisterRequest {

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

    @NotBlank
    @Size(max = 100)
    private String school;

    @NotBlank
    @Size(max = 100)
    private String className;

    @NotNull
    @Min(1)
    private Integer grade;

    @NotNull
    @Min(1)
    private Integer classNumber;

    @Valid
    @NotEmpty
    private List<StudentRegisterRequest> students;
}