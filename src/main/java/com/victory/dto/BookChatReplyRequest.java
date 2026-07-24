package com.victory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BookChatReplyRequest {

    @NotBlank
    @Pattern(
        regexp = "SIMILAR|DIFFERENT|similar|different",
        message = "replyType은 SIMILAR 또는 DIFFERENT여야 합니다."
    )
    private String replyType;

    @NotBlank
    private String text;
}
