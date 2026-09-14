package com.victory.service;

import java.util.List;

import org.springframework.stereotype.Component;

/*
 * 개별읽기 성장 포트폴리오 전용 "독서 역량 4종"(질문 생성/독서 지속/생각
 * 다듬기/생각 나눔) 순수 계산기. DB/네트워크에 의존하지 않아 공식만
 * 독립적으로 단위 테스트할 수 있다(IndividualAchievementCalculator와
 * 같은 패턴). 학생 간 상대평가(반 평균/상위 학생 기준 정규화)는 절대
 * 하지 않고, 학생 개인이 평가기간 동안 자신에게 주어진 활동을 얼마나
 * 수행했는지 기준의 절대적 0~100 점수만 낸다.
 *
 * 게임 능력치 누적값(student_stat_reward_log 기반 Competencies)은 이
 * 계산에 전혀 관여하지 않는다 - 완전히 별도의 입력(책별 완료 단계,
 * 독서실천도/기록완성도, 나눔 활동 존재 여부)만 사용한다.
 */
@Component
public class ReadingCompetencyCalculator {

    private static final int STAGE_TOTAL = 3;

    /*
     * 질문 생성 역량 = 평가기간에 관련된 책(완독 + 현재 진행 중)마다
     * 필수 질문 단계(읽기 전/중/후) 3개 중 실제 완료한 단계 수 비율의
     * 평균. 완료 여부는 boolean 플래그(beforeDone/duringDone/afterDone)
     * 기준이라, 같은 단계에 질문을 여러 개 써도 점수가 더 오르지 않는다
     * (단순 건수 기반 만점 방지).
     */
    public double questionGenerationScore(List<Integer> completedStageCountPerBook) {
        if (completedStageCountPerBook == null || completedStageCountPerBook.isEmpty()) {
            return 0.0;
        }

        double total = completedStageCountPerBook.stream()
            .mapToDouble(count -> stageCompletionRate(count == null ? 0 : count))
            .sum();

        return clamp(total / completedStageCountPerBook.size());
    }

    private double stageCompletionRate(int completedStageCount) {
        int count = Math.max(0, Math.min(STAGE_TOTAL, completedStageCount));

        return (count / (double) STAGE_TOTAL) * 100.0;
    }

    /*
     * 독서 지속 역량 = 평가기간 내 완독한 책들의 평균 독서실천도를 그대로
     * 재사용한다(IndividualAchievementCalculator.readingPracticeScore()로
     * 이미 0~100으로 계산되어 있으므로 새 계산식을 복제하지 않는다).
     * 완독한 책이 없으면 0으로 일관되게 처리한다.
     */
    public double readingPersistenceScore(Double averageReadingPracticeScore) {
        return clamp(averageReadingPracticeScore == null ? 0.0 : averageReadingPracticeScore);
    }

    /*
     * 생각 다듬기 역량 = 평가기간 내 완독한 책들의 평균 기록완성도를
     * 그대로 재사용한다(활동완료율 + AI 첫 통과율이 이미 반영된 값 -
     * 새 계산식을 복제하지 않는다). 완독한 책이 없으면 0으로 처리한다.
     */
    public double thoughtRefinementScore(Double averageRecordCompletionScore) {
        return clamp(averageRecordCompletionScore == null ? 0.0 : averageRecordCompletionScore);
    }

    /*
     * 생각 나눔 역량 = "평가기간에 관련된 책마다 나눔 활동에 최소 1회
     * 참여했는가"의 비율. booksWithAttributedSharing은 책수다방 글/친구
     * 추천처럼 특정 책(readingRecordId)에 귀속되는 나눔 활동이 있었던
     * 책의 수다. 책수다방 댓글(chat_reply)은 구조상 어느 책에 대한
     * 것인지 알 수 없어 책 단위로 귀속시키지 않고, hasUnattributedSharing
     * 이 true면 참여 책 수가 아직 전체보다 적을 때만 최대 1권 분의
     * 보너스로만 인정한다 - raw 댓글 개수를 그대로 점수화하지 않는다.
     */
    public double thoughtSharingScore(
            int booksWithAttributedSharing, int totalBooksInScope, boolean hasUnattributedSharing) {
        if (totalBooksInScope <= 0) {
            return 0.0;
        }

        int booksWithSharing = Math.max(0, Math.min(totalBooksInScope, booksWithAttributedSharing));

        if (hasUnattributedSharing && booksWithSharing < totalBooksInScope) {
            booksWithSharing++;
        }

        return clamp((booksWithSharing / (double) totalBooksInScope) * 100.0);
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(100.0, value));
    }
}
