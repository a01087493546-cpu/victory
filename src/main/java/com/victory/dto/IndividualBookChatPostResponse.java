package com.victory.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.victory.entity.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/*
 * 개별읽기 책수다방 글(밸런스 게임 형태) 응답 한 건. 텍스트만 저장한다
 * (사진 첨부 기능은 제거됨).
 *
 * mine/countA/countB는 학급 전체 목록 조회 시 서버가 계산해서 채운다.
 * rewardGranted/stats는 "이 글 등록으로 오늘 처음 용기 보상을 받았을 때"만
 * 채워진다(그 외 - 목록 조회, 이미 오늘 보상을 받은 뒤 추가 등록 - 에는
 * 각각 false/null).
 *
 * 필드명 주의: 원래 isMine/aCount/bCount로 썼더니 Jackson이 getter
 * 이름에서 JSON 키를 유추할 때 "isMine()"은 "mine"으로, "getACount()"는
 * (선행 대문자가 2개 연속이면 전부 소문자로 뭉개는 규칙 때문에) "acount"로
 * 바꿔버려 프론트가 기대하는 키와 어긋났다(라이브 E2E 테스트로 확인) -
 * @JsonProperty로 강제하면 자동 감지된 getter 기반 프로퍼티와 중복
 * 출력되므로, 아예 이 문제가 생기지 않는 필드명(mine/countA/countB)을
 * 쓴다. mine의 getter는 Lombok 관례상 isMine()으로 그대로 나온다.
 */
@Getter
@Setter
@AllArgsConstructor
public class IndividualBookChatPostResponse {

    private Long id;
    private String writer;
    private String writerName;
    private Long studentId;
    private Long classId;
    private String bookTitle;
    private String title;
    private String scene;
    private String optionA;
    private String optionB;
    private LocalDate activityDate;
    private LocalDateTime createdAt;
    private boolean mine;
    private Boolean isMine;
    private String moderationStatus;
    private String rejectReason;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private long countA;
    private long countB;
    private long commentCount;
    private boolean rewardGranted;
    private StudentStatsResponse stats;

    public static IndividualBookChatPostResponse fromPost(
            Response response, boolean isMine, long countA, long countB) {
        return fromPost(response, isMine, null, countA, countB);
    }

    public static IndividualBookChatPostResponse fromPost(
            Response response, boolean isMine, Long classId, long countA, long countB) {

        String writerName = response.getStudent().getName();

        return new IndividualBookChatPostResponse(
            response.getId(),
            writerName,
            writerName,
            response.getStudent().getId(),
            classId,
            extractFromExtraData(response, "bookTitle"),
            extractFromExtraData(response, "title"),
            response.getContent(),
            extractFromExtraData(response, "optionA"),
            extractFromExtraData(response, "optionB"),
            response.getActivityDate(),
            response.getCreatedAt(),
            isMine,
            isMine,
            normalizeModerationStatus(response.getStatus()),
            response.getRejectReason(),
            response.getReviewedBy() == null ? null : response.getReviewedBy().getId(),
            response.getReviewedAt(),
            countA,
            countB,
            countA + countB,
            false,
            null
        );
    }

    private static String normalizeModerationStatus(String status) {
        if (status == null || status.isBlank()) {
            return "PENDING";
        }

        String normalized = status.trim().toUpperCase();

        if ("PENDING".equals(normalized)
                || "APPROVED".equals(normalized)
                || "REJECTED".equals(normalized)) {
            return normalized;
        }

        return "PENDING";
    }

    private static String extractFromExtraData(Response response, String key) {
        Object value = response.getExtraData() == null
            ? null
            : response.getExtraData().get(key);

        return value == null ? null : value.toString();
    }
}
