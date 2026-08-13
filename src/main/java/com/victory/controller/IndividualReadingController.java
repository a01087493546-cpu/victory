package com.victory.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.BookTypeStatsResponse;
import com.victory.dto.IndividualAfterCompleteResponse;
import com.victory.dto.IndividualAfterResponseItem;
import com.victory.dto.IndividualAfterResponseSaveRequest;
import com.victory.dto.IndividualBeforeResponseItem;
import com.victory.dto.IndividualBeforeResponseSaveRequest;
import com.victory.dto.IndividualBookRegisterRequest;
import com.victory.dto.IndividualDuringResponseItem;
import com.victory.dto.IndividualDuringResponseSaveRequest;
import com.victory.dto.IndividualDuringTodayResponse;
import com.victory.dto.IndividualFinishCandidatesResponse;
import com.victory.dto.IndividualReadingArchiveDetailResponse;
import com.victory.dto.IndividualReadingArchiveItem;
import com.victory.dto.IndividualReadingFinishRequest;
import com.victory.dto.IndividualReadingFinishResponse;
import com.victory.dto.IndividualReadingPagesRequest;
import com.victory.dto.IndividualReadingRecordResponse;
import com.victory.dto.IndividualSummaryLikeResponse;
import com.victory.dto.IndividualSummaryResponse;
import com.victory.dto.IndividualSummarySaveRequest;
import com.victory.dto.IndividualSummaryShareItem;
import com.victory.dto.MonthlyCompletionStatsResponse;
import com.victory.service.IndividualReadingService;
import com.victory.service.IndividualSummaryShareService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/*
 * 개별읽기 1단계(책 등록/진행 중 책 조회/쪽수 수정/완독 처리) 전용 컨트롤러.
 * /api/students/** 패턴에 걸려 SecurityConfig의 기존 규칙
 * (.requestMatchers("/api/students/**").hasAuthority("student"))만으로
 * 이미 "student 권한 필수" 인증이 적용된다 - 새 규칙을 추가하지 않았다.
 * 학생 ID는 항상 JWT(Authentication)에서만 가져온다(요청 body/URL 값은 신뢰하지 않음).
 */
@RestController
@RequestMapping("/api/students/me/individual-reading")
@RequiredArgsConstructor
public class IndividualReadingController {

    private final IndividualReadingService individualReadingService;
    private final IndividualSummaryShareService individualSummaryShareService;

    @PostMapping("/books")
    public ResponseEntity<IndividualReadingRecordResponse> registerBook(
            @Valid @RequestBody IndividualBookRegisterRequest request,
            Authentication authentication) {

        Long studentId = requireStudentId(authentication);
        IndividualReadingRecordResponse response = individualReadingService.registerBook(studentId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/current")
    public ResponseEntity<IndividualReadingRecordResponse> getCurrentBook(Authentication authentication) {
        Long studentId = requireStudentId(authentication);

        return individualReadingService.getCurrentReadingRecord(studentId)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping("/{readingRecordId}/pages")
    public ResponseEntity<IndividualReadingRecordResponse> updatePages(
            @PathVariable Long readingRecordId,
            @Valid @RequestBody IndividualReadingPagesRequest request,
            Authentication authentication) {

        Long studentId = requireStudentId(authentication);
        IndividualReadingRecordResponse response =
            individualReadingService.updatePages(studentId, readingRecordId, request);

        return ResponseEntity.ok(response);
    }

    /*
     * 책 마무리하기(완독 처리 + 별점 + 대표 질문 저장을 한 번에). 대표
     * 질문은 텍스트가 아니라 실제 responseId로 받아 서버가 검증·조회한다.
     */
    @PostMapping("/{readingRecordId}/complete")
    public ResponseEntity<IndividualReadingFinishResponse> completeBook(
            @PathVariable Long readingRecordId,
            @Valid @RequestBody IndividualReadingFinishRequest request,
            Authentication authentication) {

        Long studentId = requireStudentId(authentication);
        IndividualReadingFinishResponse response =
            individualReadingService.completeReadingRecord(studentId, readingRecordId, request);

        return ResponseEntity.ok(response);
    }

    /*
     * 이 책 마무리하기 팝업의 읽기 전/중/후 탭용 대표 질문 후보 조회.
     */
    @GetMapping("/{readingRecordId}/finish-candidates")
    public ResponseEntity<IndividualFinishCandidatesResponse> getFinishCandidates(
            @PathVariable Long readingRecordId,
            Authentication authentication) {

        Long studentId = requireStudentId(authentication);

        return ResponseEntity.ok(
            individualReadingService.getFinishCandidates(studentId, readingRecordId));
    }

    /*
     * 완독한 기록도 조회는 허용한다(나중에 보관함 상세 화면에서 사용).
     */
    @GetMapping("/{readingRecordId}/before-responses")
    public ResponseEntity<List<IndividualBeforeResponseItem>> getBeforeResponses(
            @PathVariable Long readingRecordId,
            Authentication authentication) {

        Long studentId = requireStudentId(authentication);

        return ResponseEntity.ok(
            individualReadingService.getBeforeResponses(studentId, readingRecordId));
    }

    /*
     * AI가 good으로 판정한 직후에만 프론트에서 호출된다. AI 판정 자체는
     * 이 API 호출 전에 공개 AI 검사 API로 이미 끝나 있어야 하며, 여기서는
     * 판정 결과를 전혀 받지도 저장하지도 않는다.
     */
    @PutMapping("/{readingRecordId}/before-responses/{stepType}")
    public ResponseEntity<IndividualBeforeResponseItem> saveBeforeResponse(
            @PathVariable Long readingRecordId,
            @PathVariable String stepType,
            @Valid @RequestBody IndividualBeforeResponseSaveRequest request,
            Authentication authentication) {

        Long studentId = requireStudentId(authentication);

        return ResponseEntity.ok(
            individualReadingService.saveBeforeResponse(studentId, readingRecordId, stepType, request));
    }

    /*
     * 개별읽기 읽기 중(책속 생각쓰기) "오늘 기록"만 조회한다. 서버가 결정한
     * Asia/Seoul 오늘 날짜도 함께 내려줘서 프론트가 브라우저 날짜를
     * 신뢰하지 않게 한다.
     */
    @GetMapping("/{readingRecordId}/during-responses/today")
    public ResponseEntity<IndividualDuringTodayResponse> getDuringResponsesToday(
            @PathVariable Long readingRecordId,
            Authentication authentication) {

        Long studentId = requireStudentId(authentication);

        return ResponseEntity.ok(
            individualReadingService.getDuringResponsesToday(studentId, readingRecordId));
    }

    /*
     * 개별읽기 읽기 중(책속 생각쓰기)의 전체 날짜 기록 조회(질문 보관함용).
     * 완독한 기록도 조회는 허용한다.
     */
    @GetMapping("/{readingRecordId}/during-responses/history")
    public ResponseEntity<List<IndividualDuringResponseItem>> getDuringResponsesHistory(
            @PathVariable Long readingRecordId,
            Authentication authentication) {

        Long studentId = requireStudentId(authentication);

        return ResponseEntity.ok(
            individualReadingService.getDuringResponsesHistory(studentId, readingRecordId));
    }

    @PutMapping("/{readingRecordId}/during-responses/{questionSlot}")
    public ResponseEntity<IndividualDuringResponseItem> saveDuringResponse(
            @PathVariable Long readingRecordId,
            @PathVariable Integer questionSlot,
            @Valid @RequestBody IndividualDuringResponseSaveRequest request,
            Authentication authentication) {

        Long studentId = requireStudentId(authentication);
        IndividualDuringResponseItem response =
            individualReadingService.saveDuringResponse(studentId, readingRecordId, questionSlot, request);

        return ResponseEntity.ok(response);
    }

    /*
     * 읽기 중 완료 처리. body 없음 - 프론트가 보낸 값은 신뢰하지 않고
     * 서버에 저장된 currentPage/totalPages/읽기 중 질문 존재 여부만으로
     * 판정한다.
     */
    @PostMapping("/{readingRecordId}/during-complete")
    public ResponseEntity<IndividualReadingRecordResponse> completeDuringReading(
            @PathVariable Long readingRecordId,
            Authentication authentication) {

        Long studentId = requireStudentId(authentication);

        return ResponseEntity.ok(
            individualReadingService.completeDuringReading(studentId, readingRecordId));
    }

    /*
     * 완독한 기록도 조회는 허용한다(나중에 보관함 상세 화면에서 사용).
     */
    @GetMapping("/{readingRecordId}/after-responses")
    public ResponseEntity<List<IndividualAfterResponseItem>> getAfterResponses(
            @PathVariable Long readingRecordId,
            Authentication authentication) {

        Long studentId = requireStudentId(authentication);

        return ResponseEntity.ok(
            individualReadingService.getAfterResponses(studentId, readingRecordId));
    }

    /*
     * 읽기 후 간추리기 질문·답 한 세트(질문 번호 1/2/3)를 저장한다. AI가
     * good으로 판정한 직후에만 프론트에서 호출되는 것을 전제로 하되, 서버는
     * aiPassed가 명시적으로 false로 온 경우 저장 자체를 거부해 한 번 더 검증한다.
     */
    @PutMapping("/{readingRecordId}/after-responses/{questionIndex}")
    public ResponseEntity<IndividualAfterResponseItem> saveAfterResponse(
            @PathVariable Long readingRecordId,
            @PathVariable Integer questionIndex,
            @Valid @RequestBody IndividualAfterResponseSaveRequest request,
            Authentication authentication) {

        Long studentId = requireStudentId(authentication);

        return ResponseEntity.ok(
            individualReadingService.saveAfterResponse(studentId, readingRecordId, questionIndex, request));
    }

    /*
     * 최종 간추리기 조회. 아직 저장된 내용이 없으면 204를 돌려준다(다른 책의
     * 간추리기가 화면에 잘못 남아있지 않도록 프론트가 이 값을 기준으로 입력창을
     * 초기화한다).
     */
    @GetMapping("/{readingRecordId}/summary")
    public ResponseEntity<IndividualSummaryResponse> getAfterSummary(
            @PathVariable Long readingRecordId,
            Authentication authentication) {

        Long studentId = requireStudentId(authentication);

        return individualReadingService.getAfterSummary(studentId, readingRecordId)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping("/{readingRecordId}/summary")
    public ResponseEntity<IndividualSummaryResponse> saveAfterSummary(
            @PathVariable Long readingRecordId,
            @Valid @RequestBody IndividualSummarySaveRequest request,
            Authentication authentication) {

        Long studentId = requireStudentId(authentication);

        return ResponseEntity.ok(
            individualReadingService.saveAfterSummary(studentId, readingRecordId, request));
    }

    /*
     * 읽기 후 완료 처리. body 없음 - 질문·답 3세트 + AI 통과 + 최종 간추리기
     * 존재 여부를 서버가 저장된 값만으로 다시 판정하고, 통과 시에만 afterDone과
     * 보상을 같은 트랜잭션으로 함께 반영한다.
     */
    @PostMapping("/{readingRecordId}/after-complete")
    public ResponseEntity<IndividualAfterCompleteResponse> completeAfterReading(
            @PathVariable Long readingRecordId,
            Authentication authentication) {

        Long studentId = requireStudentId(authentication);

        return ResponseEntity.ok(
            individualReadingService.completeAfterReading(studentId, readingRecordId));
    }

    /*
     * 우리 반 간추리기 모음. 진행 중인 책이 없어도(완독한 기록이어도)
     * 공유 조건(AI 통과 + 승인 상태)을 충족한 같은 학급 학생들의 간추리기를
     * 모두 조회한다 - /current(진행 중 책)에 의존하지 않는다.
     */
    @GetMapping("/summaries/class")
    public ResponseEntity<List<IndividualSummaryShareItem>> getClassSummaries(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            Authentication authentication) {
        Long studentId = requireStudentId(authentication);

        return ResponseEntity.ok(
            individualSummaryShareService.getClassSummariesForStudent(studentId, date));
    }

    /*
     * 간추리기 좋아요 토글(있으면 취소, 없으면 추가). 같은 학급의 공유된
     * 간추리기에만 가능하며, 좋아요 수는 학생/교사 구분 없이 content_likes를
     * 함께 집계한다.
     */
    @PostMapping("/summaries/{summaryId}/like")
    public ResponseEntity<IndividualSummaryLikeResponse> toggleSummaryLike(
            @PathVariable Long summaryId,
            Authentication authentication) {

        Long studentId = requireStudentId(authentication);

        return ResponseEntity.ok(
            individualSummaryShareService.toggleLikeAsStudent(studentId, summaryId));
    }

    @PutMapping("/summaries/{summaryId}/resubmit")
    public ResponseEntity<IndividualSummaryShareItem> resubmitSummary(
            @PathVariable Long summaryId,
            @RequestBody java.util.Map<String, String> body,
            Authentication authentication) {
        Long studentId = requireStudentId(authentication);
        return ResponseEntity.ok(individualSummaryShareService.resubmit(studentId, summaryId, body.get("summaryText")));
    }

    @DeleteMapping("/summaries/{summaryId}")
    public ResponseEntity<Void> deleteRejectedSummary(
            @PathVariable Long summaryId, Authentication authentication) {
        Long studentId = requireStudentId(authentication);
        individualSummaryShareService.deleteRejected(studentId, summaryId);
        return ResponseEntity.noContent().build();
    }

    /*
     * 나의 독서 보관함 목록. 완독한 기록만 최신순으로 반환한다.
     */
    @GetMapping("/archive")
    public ResponseEntity<List<IndividualReadingArchiveItem>> getArchive(Authentication authentication) {
        Long studentId = requireStudentId(authentication);

        return ResponseEntity.ok(individualReadingService.getArchive(studentId));
    }

    /*
     * 나의 독서 보관함 상세: 책 정보, 별점·대표 질문, 읽기 전/중/후 최종
     * 통과 질문·답 전체, 최종 간추리기.
     */
    @GetMapping("/archive/{readingRecordId}")
    public ResponseEntity<IndividualReadingArchiveDetailResponse> getArchiveDetail(
            @PathVariable Long readingRecordId,
            Authentication authentication) {

        Long studentId = requireStudentId(authentication);

        return ResponseEntity.ok(
            individualReadingService.getArchiveDetail(studentId, readingRecordId));
    }

    /*
     * 읽은 책 종류 통계. 완독(finished_at IS NOT NULL)한 기록만 집계한다.
     */
    @GetMapping("/book-type-stats")
    public ResponseEntity<BookTypeStatsResponse> getBookTypeStats(Authentication authentication) {
        Long studentId = requireStudentId(authentication);

        return ResponseEntity.ok(individualReadingService.getBookTypeStats(studentId));
    }

    /*
     * 학생 메인 화면 "월별 완독 기록" 그래프용. 현재 연도 1~12월 완독 권수를
     * 반환한다.
     */
    @GetMapping("/monthly-completion-stats")
    public ResponseEntity<MonthlyCompletionStatsResponse> getMonthlyCompletionStats(
            Authentication authentication) {

        Long studentId = requireStudentId(authentication);

        return ResponseEntity.ok(individualReadingService.getMonthlyCompletionStats(studentId));
    }

    /*
     * SecurityConfig가 이미 인증(401)과 student 권한(다른 role은 403)을
     * 강제하므로, 여기서는 principal이 예상한 타입(Long userId)인지만
     * 방어적으로 한 번 더 확인한다(ResponseController.requireSelf와 같은 관례).
     */
    private Long requireStudentId(Authentication authentication) {

        Object principal =
            authentication == null ? null : authentication.getPrincipal();

        if (!(principal instanceof Long studentId)) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "학생 로그인이 필요합니다."
            );
        }

        return studentId;
    }
}
