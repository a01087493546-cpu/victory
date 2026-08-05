package com.victory.dto;

import java.time.LocalDateTime;

import com.victory.entity.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/*
 * 개별읽기 읽기 전 질문·답 응답. AI 판정 상태·피드백 필드는 넣지 않는다
 * (AI 결과는 DB에 저장하지 않으므로 되돌려줄 값 자체가 없음).
 *
 * rewardGranted/stats는 이 저장 요청이 "읽기 전 4단계 최초 완료" 전환을
 * 발생시켰을 때만 채워진다(그 외에는 각각 false/null) - IndividualReadingService가
 * from()으로 만든 뒤 필요할 때만 setter로 덧붙인다.
 */
@Getter
@Setter
@AllArgsConstructor
public class IndividualBeforeResponseItem {

    private Long responseId;
    private Long readingRecordId;
    private String stepType;
    private String question;
    private String answer;

    /*
     * 차례가 없는 책이라 학생이 "차례 없음"을 선택해 이 단계를 통과했는지
     * 여부. true면 question/answer는 실제 학생 작성값이 아니라 빈 값이다.
     */
    private boolean skipped;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean rewardGranted;
    private StudentStatsResponse stats;

    public static IndividualBeforeResponseItem from(Response response) {

        Object skipped = response.getExtraData() == null
            ? null
            : response.getExtraData().get("skipped");

        return new IndividualBeforeResponseItem(
            response.getId(),
            response.getReadingRecord().getId(),
            extractFromExtraData(response, "stepType"),
            extractFromExtraData(response, "question"),
            response.getContent(),
            Boolean.TRUE.equals(skipped),
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
}
