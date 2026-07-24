package com.victory.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AfterReadingSaveRequest {

    @NotNull
    private Long classReadingBookId;

    @NotBlank
    private String bookType;

    @Valid
    @NotEmpty
    private List<AfterReadingQuestionRequest> questions;

    @NotBlank
    private String summary;

    private Boolean summaryAiPassed;
}
