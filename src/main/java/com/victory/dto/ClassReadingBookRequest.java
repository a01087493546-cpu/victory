package com.victory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ClassReadingBookRequest {

    @NotBlank
    private String bookTitle;

    private String author;

    private String coverImage;

    @NotNull
    @Min(1)
    private Integer totalPages;

    @NotNull
    @Min(0)
    private Integer currentPage;
}