package com.victory.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * 개별읽기 읽기 전 한 단계(title/contents/picture/skim)의 질문·답 저장
 * 요청. AI가 good으로 판정한 직후에만 프론트에서 이 요청을 보낸다.
 * AI 판정 결과(good/need)·피드백 문구·evaluationKey는 이 DTO에도,
 * DB에도 저장하지 않는다.
 *
 * skipped=true(차례 없음)일 때는 question/answer가 실제 학생 작성값이
 * 아니므로 비어 있을 수 있다 - 그래서 @NotBlank를 빼고
 * IndividualReadingService.saveBeforeResponse()에서 skipped 여부에
 * 따라 다르게 검증한다(일반 저장은 기존과 동일하게 필수).
 */
@Getter
@NoArgsConstructor
public class IndividualBeforeResponseSaveRequest {

    private String question;

    private String answer;

    private boolean skipped;
}
