package com.victory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * 연습읽기 읽기 전(제목/차례/그림/글) 질문·답 저장 요청.
 * AI가 good으로 판정한 직후에만 프론트에서 이 요청을 보낸다.
 *
 * skipped=true(차례 없음)일 때는 question/answer가 실제 학생 작성값이
 * 아니므로 비어 있을 수 있다 - 그래서 두 필드에서 @NotBlank를 빼고
 * ResponseService.savePreReadingResponse()에서 skipped 여부에 따라
 * 다르게 검증한다(일반 저장은 기존과 동일하게 필수).
 */
@Getter
@NoArgsConstructor
public class PreReadingResponseRequest {

    @NotBlank
    private String stepType;

    private String question;

    private String answer;

    @NotNull
    private Long classReadingBookId;

    private boolean skipped;
}
