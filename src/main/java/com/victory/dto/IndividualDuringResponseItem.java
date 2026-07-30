package com.victory.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.victory.entity.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/*
 * 개별읽기 읽기 중(책속 생각쓰기) 응답 한 건. AI 판정 상태·피드백 필드는
 * 넣지 않는다(AI 결과는 DB에 저장하지 않음).
 *
 * questionSlot(1/2/3)이 저장 고유 식별자다 - questionType은 그 슬롯에
 * 붙는 유형 정보일 뿐이라, 같은 유형을 여러 슬롯에서 써도 각각 별도로
 * 보존된다. rewardGranted/stats는 "이 저장으로 오늘 3개 슬롯이 모두
 * 처음 채워졌을 때"만 채워진다(그 외에는 각각 false/null).
 */
@Getter
@Setter
@AllArgsConstructor
public class IndividualDuringResponseItem {

    private Long responseId;
    private Long readingRecordId;
    private Integer questionSlot;
    private String questionType;
    private String question;
    private String answer;
    private LocalDate activityDate;
    private Integer currentPage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean rewardGranted;
    private StudentStatsResponse stats;

    public static IndividualDuringResponseItem from(Response response, Integer currentPage) {
        return new IndividualDuringResponseItem(
            response.getId(),
            response.getReadingRecord().getId(),
            extractIntFromExtraData(response, "questionSlot"),
            extractFromExtraData(response, "questionType"),
            extractFromExtraData(response, "question"),
            response.getContent(),
            response.getActivityDate(),
            currentPage,
            response.getCreatedAt(),
            response.getUpdatedAt(),
            false,
            null
        );
    }

    private static String extractFromExtraData(Response response, String key) {
        Object value = response.getExtraData() == null
            ? null
            : response.getExtraData().get(key);

        return value == null ? null : value.toString();
    }

    private static Integer extractIntFromExtraData(Response response, String key) {
        Object value = response.getExtraData() == null
            ? null
            : response.getExtraData().get(key);

        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.intValue();
        }

        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
