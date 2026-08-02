package com.victory.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.victory.entity.ReadingRecord;

public interface ReadingRecordRepository extends JpaRepository<ReadingRecord, Long> {

    /*
     * "진행 중" 기록 = finished_at이 아직 없는 기록. 학생당 이 조회 결과가
     * 항상 0개 또는 1개여야 한다는 것을 IndividualReadingService가 등록 시
     * 트랜잭션으로 강제한다.
     */
    Optional<ReadingRecord> findByStudent_IdAndFinishedAtIsNull(Long studentId);

    List<ReadingRecord> findByStudent_IdOrderByCreatedAtDesc(Long studentId);

    /*
     * 소유권 확인용 조회. 다른 학생의 readingRecordId로 조회하면 빈 값이
     * 반환되어 컨트롤러/서비스가 403으로 처리할 수 있다.
     */
    Optional<ReadingRecord> findByIdAndStudent_Id(Long id, Long studentId);

    /*
     * 읽은 책 종류 통계용. 완독(finished_at IS NOT NULL) 기록만 집계 대상이고,
     * 같은 책을 재독해 여러 readingRecordId를 완독했다면 그만큼 여러 건이
     * 반환되어 각각 새로운 완독 1권으로 집계된다.
     */
    List<ReadingRecord> findByStudent_IdAndFinishedAtIsNotNull(Long studentId);

    /*
     * 던전 입장 조건(requiredBooks) 판정용 완독 권수 카운트.
     */
    long countByStudent_IdAndFinishedAtIsNotNull(Long studentId);

    /*
     * 교사용 개별읽기 대시보드의 대표 기록 선정용. 진행 중 기록이 없을 때,
     * finished_at이 가장 최근인 완독 기록 1건을 대표로 쓴다.
     */
    Optional<ReadingRecord> findFirstByStudent_IdAndFinishedAtIsNotNullOrderByFinishedAtDesc(Long studentId);
}
