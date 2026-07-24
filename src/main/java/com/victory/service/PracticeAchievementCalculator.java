package com.victory.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

/*
 * 연습읽기 교사용 성취도 계산 공식을 모아 둔 순수 계산기.
 * DB/네트워크에 의존하지 않아 단위 테스트로 공식만 독립적으로 검증할 수
 * 있다. 계산 과정은 모두 double로 하고, 화면에 보여줄 자릿수 반올림은
 * 이 클래스의 round2()에서만 한다(중간 계산에서 미리 반올림하지 않는다).
 */
@Component
public class PracticeAchievementCalculator {

    private static final int ATTEMPT_SUCCESS_LIMIT = 3;

    /*
     * 쪽수진도 = MIN(100, 누적읽은쪽수 / 전체쪽수 × 100)
     * 전체쪽수가 null이거나 0이면 나눗셈이 불가능하므로(또는 진도 개념이
     * 성립하지 않으므로) 0으로 처리한다.
     */
    public double pageProgress(Integer currentPage, Integer totalPages) {
        if (totalPages == null || totalPages <= 0) {
            return 0.0;
        }

        int current = currentPage == null ? 0 : currentPage;
        double raw = (current / (double) totalPages) * 100.0;

        return Math.min(100.0, Math.max(0.0, raw));
    }

    /*
     * 총복습점수 = duringTypeProgress.review가 true면 100, 아니면(null 포함) 0
     */
    public double reviewScore(Map<String, Boolean> duringTypeProgress) {
        if (duringTypeProgress == null) {
            return 0.0;
        }

        return Boolean.TRUE.equals(duringTypeProgress.get("review")) ? 100.0 : 0.0;
    }

    /*
     * 최종 진행률 = 총복습점수 × 0.2 + 쪽수진도 × 0.8
     */
    public double finalReadingProgress(double reviewScore, double pageProgress) {
        return reviewScore * 0.2 + pageProgress * 0.8;
    }

    /*
     * 질문만들기참여율 = 질문을 한 번이라도 작성한 단계 수(0~3) / 3 × 100
     * 같은 단계에서 여러 개 써도 그 단계는 1로만 센다 - 호출하는 쪽에서
     * "단계별로 작성했는지"만 boolean 3개로 넘겨주는 것을 전제로 한다.
     */
    public double questionParticipationRate(
            boolean wroteBeforeReadingQuestion,
            boolean wroteBookThoughtQuestion,
            boolean wroteAfterReadingQuestion) {

        int completedStageCount =
            (wroteBeforeReadingQuestion ? 1 : 0)
                + (wroteBookThoughtQuestion ? 1 : 0)
                + (wroteAfterReadingQuestion ? 1 : 0);

        return (completedStageCount / 3.0) * 100.0;
    }

    /*
     * 생각나누기참여율 = 책수다방 본문 글 개수 구간표(0/20/60/100)
     */
    public double thoughtSharingParticipationRate(int bookChatPostCount) {
        if (bookChatPostCount <= 0) {
            return 0.0;
        }

        if (bookChatPostCount <= 2) {
            return 20.0;
        }

        if (bookChatPostCount <= 4) {
            return 60.0;
        }

        return 100.0;
    }

    /*
     * 참여도 = (읽기활동완료율 + 질문만들기참여율 + 생각나누기참여율) / 3
     * 읽기활동완료율은 finalReadingProgress와 같은 값을 그대로 쓴다(1-4).
     */
    public double participationRate(
            double readingActivityCompletionRate,
            double questionParticipationRate,
            double thoughtSharingParticipationRate) {

        return (readingActivityCompletionRate
            + questionParticipationRate
            + thoughtSharingParticipationRate) / 3.0;
    }

    /*
     * 이해도 = 3회 이내 good을 받은 질문 수 / AI 검사를 한 번 이상 정상적으로
     * 받은 전체 질문 수 × 100. 평가 기록이 0건이면 0(호출하는 쪽에서
     * hasAiEvaluation=false를 별도로 함께 반환해야 한다).
     */
    public double comprehensionRate(int passedWithinThreeAttempts, int aiEvaluatedQuestionCount) {
        if (aiEvaluatedQuestionCount <= 0) {
            return 0.0;
        }

        return (passedWithinThreeAttempts / (double) aiEvaluatedQuestionCount) * 100.0;
    }

    /*
     * 최종 달성도 = 참여도 × 0.5 + 이해도 × 0.5
     * AI 평가 기록이 없을 때도 이해도는 0으로 계산해서 이 공식에 그대로
     * 넣되(참여도만으로 반쪽 점수가 되는 것을 그대로 반영), 화면에는
     * hasAiEvaluation=false를 반드시 함께 내려줘야 한다.
     */
    public double achievementRate(double participationRate, double comprehensionRate) {
        return participationRate * 0.5 + comprehensionRate * 0.5;
    }

    /*
     * 한 질문(같은 activityType+questionType 슬롯)의 AI 검사 시도 상태를
     * 오래된 순서대로 받아, "3회 이내에 good을 받았는지"를 판정한다.
     * - 목록이 비어 있으면(시도 자체가 없으면) 이 질문은 애초에 분모에도
     *   포함되지 않아야 하므로 호출하는 쪽에서 빈 목록은 걸러야 한다.
     * - 4회 이후에 good을 받아도 "성공"으로 세지 않는다(활동 완료로는
     *   인정하지만 이해도 분자에는 넣지 않는다는 요구사항).
     */
    public boolean passedWithinAttemptLimit(List<String> orderedStatuses) {
        if (orderedStatuses == null || orderedStatuses.isEmpty()) {
            return false;
        }

        int limit = Math.min(orderedStatuses.size(), ATTEMPT_SUCCESS_LIMIT);

        for (int i = 0; i < limit; i++) {
            if ("good".equals(orderedStatuses.get(i))) {
                return true;
            }
        }

        return false;
    }

    /*
     * 화면에 보여줄 자릿수(소수 둘째 자리)로만 반올림한다. 계산 과정에서는
     * 절대 이 메서드를 쓰지 않고, 최종 API 응답 필드를 채울 때만 쓴다.
     */
    public double round2(double value) {
        return BigDecimal.valueOf(value)
            .setScale(2, RoundingMode.HALF_UP)
            .doubleValue();
    }
}
