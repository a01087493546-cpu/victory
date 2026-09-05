package com.victory.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReadingFocusResponse {

    private long totalSeconds;

    public static ReadingFocusResponse of(long totalSeconds) {
        return new ReadingFocusResponse(totalSeconds);
    }
}
