package com.victory.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.victory.entity.ReadingProgressLog;

public interface ReadingProgressLogRepository extends JpaRepository<ReadingProgressLog, Long> {

    /*
     * 같은 날 여러 번 저장해도(오전/오후) 이 한 행을 계속 UPSERT한다 -
     * uk_progress_logs_student_record_date UNIQUE 제약과 짝을 이룬다.
     */
    Optional<ReadingProgressLog> findByStudent_IdAndReadingRecord_IdAndLogDate(
            Long studentId, Long readingRecordId, LocalDate logDate);

    /*
     * 개별읽기 지표 계산(독서일수)용. logDate가 실제 컬럼이라 UTC 변환 없이
     * 바로 날짜 집합을 만들 수 있다.
     */
    List<ReadingProgressLog> findByReadingRecord_Id(Long readingRecordId);

    /*
     * "오늘 읽은 쪽수"(당일 증가분)를 계산하기 위한 직전 날짜의 누적 쪽수 조회.
     * 오늘보다 이전 날짜 중 가장 최근 로그 하나만 있으면 되므로 Top1로 가져온다.
     */
    Optional<ReadingProgressLog> findTopByStudent_IdAndReadingRecord_IdAndLogDateLessThanOrderByLogDateDesc(
            Long studentId, Long readingRecordId, LocalDate logDate);
}
