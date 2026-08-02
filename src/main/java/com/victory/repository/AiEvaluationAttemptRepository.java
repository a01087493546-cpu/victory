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

    /*
     * 개별읽기 지표 계산(기록내용적합성)용. readingRecordId는 개별읽기
     * 요청에서만 채워지므로(classReadingBookId 기반 온책읽기 평가는
     * 이 컬럼이 항상 NULL) 이 조회만으로 온책읽기 기록과 섞이지 않지만,
     * activityType까지 명시적으로 개별읽기 질문·간추리기 유형으로
     * 한정해 이중으로 안전하게 걸러낸다.
     */
    List<AiEvaluationAttempt> findByReadingRecordIdAndActivityTypeIn(
            Long readingRecordId, List<String> activityTypes);
}
