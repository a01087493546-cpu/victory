package com.victory.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BookThoughtRejectRequest {

    @NotBlank
    private String reason;
}
