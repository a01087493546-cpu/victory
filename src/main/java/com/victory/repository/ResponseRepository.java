package com.victory.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.victory.entity.Response;

public interface ResponseRepository extends JpaRepository<Response, Long> {

    List<Response> findByStudent_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
            Long studentId, String mode, String contentType, String stage);

    List<Response> findByModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
            String mode, String contentType, String stage);

    List<Response> findByParent_IdAndModeAndContentTypeAndDeletedAtIsNullOrderByIdAsc(
            Long parentId, String mode, String contentType);

    List<Response> findByParent_IdAndModeAndContentTypeAndStudent_IdAndDeletedAtIsNullOrderByIdAsc(
            Long parentId, String mode, String contentType, Long studentId);

    /*
     * 개별읽기 읽기 전 질문·답 조회/저장 전용. readingRecordId가 실제 컬럼(FK)이라
     * DB 레벨에서 바로 걸러낼 수 있다 - 연습읽기 읽기 전 저장(classReadingBookId를
     * extra_data JSON에만 넣고 studentId+stage로만 걸러 자바에서 stepType을
     * 다시 거르는 방식)과 달리, 같은 책을 재독해도 readingRecordId가 다르면
     * 이 조회에서 완전히 분리된다.
     */
    List<Response> findByStudent_IdAndReadingRecord_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
            Long studentId, Long readingRecordId, String mode, String contentType, String stage);

    /*
     * 개별읽기 읽기 중(책속 생각쓰기)의 "오늘 기록"만 조회한다. activityDate가
     * 실제 컬럼이라 DB 레벨에서 바로 걸러낼 수 있다 - 읽기 전과 달리 같은
     * readingRecordId 안에서도 날짜별로 여러 행이 계속 쌓이는 반복 활동이라
     * stage 조회만으로는 "오늘 것"을 구분할 수 없어서 이 메서드가 필요하다.
     */
    List<Response> findByStudent_IdAndReadingRecord_IdAndModeAndContentTypeAndStageAndActivityDateAndDeletedAtIsNullOrderByIdAsc(
            Long studentId, Long readingRecordId, String mode, String contentType, String stage, LocalDate activityDate);

    /*
     * 개별읽기 책수다방 글(chat_post) 조회 전용. 책수다방 글은 읽기 전/중처럼
     * stage에 묶이지 않는 독립 활동이라 stage 없는 버전이 필요하다.
     * 최신 글이 먼저 보이도록 id 역순으로 정렬한다.
     */
    List<Response> findByStudent_IdAndModeAndContentTypeAndDeletedAtIsNullOrderByIdDesc(
            Long studentId, String mode, String contentType);

    /*
     * 같은 학급 학생들의 책수다방 글 전체 조회용. studentIds는 ClassStudent로
     * 구한 "같은 학급" 학생 id 목록이라, 다른 학급 글은 이 조회 자체에서
     * 걸러진다(자바 코드에서 다시 필터링할 필요가 없다).
     */
    List<Response> findByStudent_IdInAndModeAndContentTypeAndDeletedAtIsNullOrderByIdDesc(
            List<Long> studentIds, String mode, String contentType);

    /*
     * 여러 책수다 글의 댓글(A/B 선택 수 집계용)을 한 번에 가져온다 -
     * 글 목록 화면에서 글마다 따로 조회하면 N+1이 되는 것을 막는다.
     */
    List<Response> findByParent_IdInAndModeAndContentTypeAndDeletedAtIsNullOrderByIdAsc(
            List<Long> parentIds, String mode, String contentType);

    /*
     * 댓글을 남기려는 책수다 글이 실제로 존재하는지(삭제되지 않았는지)
     * 확인할 때 쓴다 - 글쓴이가 누구든(다른 학생 글이어도) 조회 가능해야
     * 해서 studentId로는 걸지 않는다. 같은 학급인지는 서비스 계층에서
     * 별도로 검증한다.
     */
    Optional<Response> findByIdAndModeAndContentTypeAndDeletedAtIsNull(
            Long id, String mode, String contentType);

    Optional<Response> findByIdAndStudent_IdAndModeAndContentType(
            Long id, Long studentId, String mode, String contentType);
}
