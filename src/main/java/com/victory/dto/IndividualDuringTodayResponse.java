package com.victory.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

/*
 * 개별읽기 읽기 중(책속 생각쓰기) "오늘 기록" 조회 응답
 * (GET .../during-responses/today). serverDate는 브라우저 날짜가 아니라
 * 서버가 Asia/Seoul 기준으로 판단한 오늘 날짜다 - 프론트가 화면에 쓸 "오늘"은
 * 항상 이 값을 기준으로 삼아야 한다.
 */
@Getter
@AllArgsConstructor
public class IndividualDuringTodayResponse {

    private LocalDate serverDate;
    private List<IndividualDuringResponseItem> responses;
}
