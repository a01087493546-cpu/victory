package com.victory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AfterReadingTypePracticeRequest {

    @NotNull
    private Long classReadingBookId;

    @NotBlank
    private String bookType;

    @NotBlank
    private String question1;

    @NotBlank
    private String answer1;

    @NotBlank
    private String question2;

    @NotBlank
    private String answer2;
}
