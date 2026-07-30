package com.victory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * 개별읽기 읽기 중(책속 생각쓰기) 저장 요청
 * (PUT /api/students/me/individual-reading/{readingRecordId}/during-responses/{questionSlot}).
 * 저장 고유 기준은 studentId+readingRecordId+activityDate+questionSlot이고,
 * questionType은 그 슬롯에 붙는 유형 정보일 뿐이다(같은 유형을 여러 슬롯에서
 * 써도 서로 다른 슬롯이면 각각 별도로 보존된다).
 * activityDate는 요청에 받지 않는다 - 서버가 Asia/Seoul 기준 오늘 날짜로 결정한다.
 * currentPage는 선택값이다: 이 화면에서 "오늘 읽은 곳"을 같이 적어 넘기면
 * 이전 기록보다 늘어난 경우에만 반영하고, 없거나 줄어들면 조용히 무시한다
 * (쪽수 자체를 엄격히 검증하는 화면은 별도의 pages API).
 */
@Getter
@NoArgsConstructor
public class IndividualDuringResponseSaveRequest {

    @NotBlank
    private String questionType;

    @NotBlank
    private String question;

    @NotBlank
    private String answer;

    @Min(0)
    private Integer currentPage;
}
