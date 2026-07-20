package com.victory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ClassReadingProgressRequest {

    @NotNull
    @Min(1)
    private Integer totalPages;

    @NotNull
    @Min(0)
    private Integer currentPage;
}
