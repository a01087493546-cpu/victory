package com.victory.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.victory.entity.AiEvaluationAttempt;

public interface AiEvaluationAttemptRepository
        extends JpaRepository<AiEvaluationAttempt, Long> {

    List<AiEvaluationAttempt> findByStudentIdInOrderByEvaluatedAtAsc(
            List<Long> studentIds);

    /*
     * 교사용 성취도 조회는 현재 학급의 온책읽기 책 하나로 반드시 범위를
     * 좁혀야 한다(학생이 다른 학급/다른 책의 평가 기록까지 섞이지 않게).
     */
    List<AiEvaluationAttempt> findByStudentIdInAndClassReadingBookIdOrderByEvaluatedAtAsc(
            List<Long> studentIds, Long classReadingBookId);

    /*
     * attempt_number를 백엔드가 계산할 때 쓴다: 이 개수 + 1이 이번 시도의
     * attempt_number다.
     */
    long countByStudentIdAndEvaluationKey(Long studentId, String evaluationKey);
}
