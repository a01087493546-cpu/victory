package com.victory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * saveMyAfterReadingData(최종 완료)와 달리, 간추리기가 루미 피드백을
 * 통과한 시점에 그 간추리기만 자동저장하기 위한 요청이다 - 질문
 * 3개가 아직 다 안 갖춰졌어도 저장할 수 있고, isShared/afterDone은
 * 건드리지 않는다(공유·완료는 여전히 최종 "다음으로" 버튼에서만 일어난다).
 */
@Getter
@NoArgsConstructor
public class AfterReadingSummarySaveRequest {

    @NotNull
    private Long classReadingBookId;

    @NotBlank
    private String bookType;

    @NotBlank
    private String summary;
}
