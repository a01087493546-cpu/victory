package com.victory.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.victory.entity.Summary;

public interface SummaryRepository extends JpaRepository<Summary, Long> {

    Optional<Summary> findByStudent_IdAndClassReadingBookId(
            Long studentId,
            Long classReadingBookId);

    List<Summary> findByClassReadingBookIdAndIsSharedTrueOrderByUpdatedAtDesc(
            Long classReadingBookId);

    /*
     * 개별읽기 읽기 후 최종 간추리기 조회/저장 전용. readingRecordId가 실제
     * 컬럼(FK)이라 같은 책을 재독해도 readingRecordId가 다르면 완전히 분리된다
     * (SummaryRepository의 다른 메서드들이 온책읽기 classReadingBookId 기준인 것과 대응).
     */
    Optional<Summary> findByStudent_IdAndReadingRecord_Id(
            Long studentId,
            Long readingRecordId);

    /*
     * 개별읽기 "우리 반 간추리기 모음"(학생 화면) / 교사용 간추리기 확인 화면이
     * 공통으로 쓰는 조회. readingRecord가 있는 행만 개별읽기 간추리기이고
     * (classReadingBookId 기준인 온책읽기 간추리기와 섞이지 않는다), AI 통과 +
     * 승인 상태인 것만 공유 대상으로 본다 - isShared 컬럼에 의존하지 않는다
     * (이미 완료된 기존 기록도 별도 마이그레이션 없이 바로 노출되어야 하므로).
     */
    @Query("""
        SELECT s FROM Summary s
        WHERE s.student.id IN :studentIds
          AND s.readingRecord IS NOT NULL
          AND s.aiPassed = true
          AND s.status = 'approved'
          AND s.createdAt >= :startAt
          AND s.createdAt < :endAt
        ORDER BY s.createdAt DESC, s.id DESC
        """)
    List<Summary> findSharedIndividualSummariesByStudentIdsAndCreatedAtBetween(
            @Param("studentIds") List<Long> studentIds,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt);

    @Query("""
        SELECT s FROM Summary s
        WHERE s.student.id IN :studentIds
          AND s.readingRecord IS NOT NULL
          AND s.aiPassed = true
          AND s.status = 'approved'
        ORDER BY s.createdAt DESC, s.id DESC
        """)
    List<Summary> findAllSharedIndividualSummariesByStudentIds(
            @Param("studentIds") List<Long> studentIds);

    List<Summary> findByStudent_IdAndReadingRecordIsNotNullAndAiPassedTrueAndStatusOrderByCreatedAtDesc(
            Long studentId,
            String status);

    @Query("""
        SELECT s FROM Summary s
        WHERE s.student.id IN :studentIds
          AND s.readingRecord IS NOT NULL
          AND s.aiPassed = true
        ORDER BY s.updatedAt DESC, s.id DESC
        """)
    List<Summary> findAllReviewableIndividualSummariesByStudentIds(
            @Param("studentIds") List<Long> studentIds);

    @Query("""
        SELECT s FROM Summary s
        WHERE s.student.id IN :studentIds
          AND s.classReadingBookId IS NOT NULL
          AND s.isShared = true
          AND s.aiPassed = true
        ORDER BY s.updatedAt DESC, s.id DESC
        """)
    List<Summary> findAllReviewablePracticeSummariesByStudentIds(
            @Param("studentIds") List<Long> studentIds);
}
