package com.victory.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class IndividualBookChatCommentRequest {

    private Long postId;
    private String choice;
    private String content;
}
