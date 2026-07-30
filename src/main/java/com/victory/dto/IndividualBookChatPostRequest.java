package com.victory.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class IndividualBookChatPostRequest {

    private String bookTitle;
    private String title;
    private String scene;
    private String optionA;
    private String optionB;
}
