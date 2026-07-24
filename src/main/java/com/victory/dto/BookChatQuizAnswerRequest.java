package com.victory.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BookChatQuizAnswerRequest {

    @NotBlank
    private String answer;
}
