package com.victory.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BookPreferenceRequest {

    @NotBlank
    private String thickness;

    @NotBlank
    private String mood;

    @NotBlank
    private String genre;

    @NotBlank
    private String illustrationLevel;

    @NotBlank
    private String difficulty;

    @NotBlank
    private String purpose;
}
