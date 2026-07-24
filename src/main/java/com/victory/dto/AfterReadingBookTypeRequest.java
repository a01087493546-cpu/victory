package com.victory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AfterReadingBookTypeRequest {

    @NotNull
    private Long classReadingBookId;

    @NotBlank
    private String bookType;
}
