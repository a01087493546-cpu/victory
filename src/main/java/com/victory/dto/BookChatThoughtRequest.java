package com.victory.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BookChatThoughtRequest {

    @NotBlank
    private String main;

    @NotBlank
    private String reason;
}
