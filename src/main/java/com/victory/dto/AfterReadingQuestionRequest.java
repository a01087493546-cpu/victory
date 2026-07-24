package com.victory.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AfterReadingQuestionRequest {

    @NotNull
    @Min(1)
    @Max(3)
    private Integer index;

    @NotBlank
    private String question;

    @NotBlank
    private String answer;

    private Boolean aiPassed;
}
