package com.victory.dto;

import java.time.LocalDateTime;

import com.victory.entity.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/*
 * 개별읽기 읽기 후 간추리기 질문·답 한 세트 응답. IndividualBeforeResponseItem과
 * 달리 aiPassed/aiFeedback을 함께 내려준다 - 읽기 후 완료 조건 판정과 화면 복원
 * (통과 상태 배지) 모두에 필요하기 때문이다.
 *
 * rewardGranted/stats는 이 저장 요청이 읽기 후 완료 보상을 발생시켰을 때만
 * 채워진다(그 외에는 각각 false/null).
 */
@Getter
@Setter
@AllArgsConstructor
public class IndividualAfterResponseItem {

    private Long responseId;
    private Long readingRecordId;
    private Integer questionIndex;
    private String question;
    private String answer;
    private Boolean aiPassed;
    private String aiFeedback;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean rewardGranted;
    private StudentStatsResponse stats;

    public static IndividualAfterResponseItem from(Response response) {
        return new IndividualAfterResponseItem(
            response.getId(),
            response.getReadingRecord().getId(),
            extractQuestionIndex(response),
            extractFromExtraData(response, "question"),
            response.getContent(),
            response.getPassed(),
            extractFromExtraData(response, "aiFeedback"),
            response.getCreatedAt(),
            response.getUpdatedAt(),
            false,
            null
        );
    }

    private static Integer extractQuestionIndex(Response response) {
        Object value = response.getExtraData() == null
            ? null
            : response.getExtraData().get("questionIndex");

        if (value instanceof Number number) {
            return number.intValue();
        }

        if (value == null) {
            return null;
        }

        try {
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String extractFromExtraData(Response response, String key) {
        Object value = response.getExtraData() == null
            ? null
            : response.getExtraData().get(key);

        return value == null ? null : value.toString();
    }
}
