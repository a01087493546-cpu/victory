package com.victory.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.AfterReadingDataResponse;
import com.victory.dto.AfterReadingBookTypeRequest;
import com.victory.dto.AfterReadingQuestionSaveRequest;
import com.victory.dto.AfterReadingSaveRequest;
import com.victory.dto.AfterReadingSummaryItem;
import com.victory.dto.AfterReadingSummarySaveRequest;
import com.victory.dto.AfterReadingTypePracticeRequest;
import com.victory.service.AfterReadingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AfterReadingController {

    private final AfterReadingService afterReadingService;

    @GetMapping("/api/students/{studentId}/after-reading")
    public ResponseEntity<AfterReadingDataResponse> getMyAfterReading(
            @PathVariable Long studentId,
            @RequestParam Long classReadingBookId,
            Authentication authentication) {

        requireSelf(studentId, authentication);

        return ResponseEntity.ok(
            afterReadingService.getMyAfterReadingData(
                studentId,
                classReadingBookId));
    }

    @PutMapping("/api/students/{studentId}/after-reading")
    public ResponseEntity<AfterReadingDataResponse> saveMyAfterReading(
            @PathVariable Long studentId,
            @Valid @RequestBody AfterReadingSaveRequest request,
            Authentication authentication) {

        requireSelf(studentId, authentication);

        return ResponseEntity.ok(
            afterReadingService.saveMyAfterReadingData(studentId, request));
    }

    @PutMapping("/api/students/{studentId}/after-reading/book-type")
    public ResponseEntity<AfterReadingDataResponse> saveBookType(
            @PathVariable Long studentId,
            @Valid @RequestBody AfterReadingBookTypeRequest request,
            Authentication authentication) {

        requireSelf(studentId, authentication);

        return ResponseEntity.ok(
            afterReadingService.saveBookType(studentId, request));
    }

    /*
     * 내 책 질문 1개가 루미 피드백을 통과한 순간 자동저장한다(saveMyAfterReadingData
     * 최종 완료와 달리 나머지 질문/간추리기가 없어도 저장 가능).
     */
    @PutMapping("/api/students/{studentId}/after-reading/question")
    public ResponseEntity<AfterReadingDataResponse> saveQuestionDraft(
            @PathVariable Long studentId,
            @Valid @RequestBody AfterReadingQuestionSaveRequest request,
            Authentication authentication) {

        requireSelf(studentId, authentication);

        return ResponseEntity.ok(
            afterReadingService.saveAfterReadingQuestionDraft(studentId, request));
    }

    /*
     * 간추리기가 루미 피드백을 통과한 순간 자동저장한다. isShared/afterDone은
     * 건드리지 않으므로 공유·완료는 여전히 최종 "다음으로" 버튼에서만 일어난다.
     */
    @PutMapping("/api/students/{studentId}/after-reading/summary")
    public ResponseEntity<AfterReadingDataResponse> saveSummaryDraft(
            @PathVariable Long studentId,
            @Valid @RequestBody AfterReadingSummarySaveRequest request,
            Authentication authentication) {

        requireSelf(studentId, authentication);

        return ResponseEntity.ok(
            afterReadingService.saveAfterReadingSummaryDraft(studentId, request));
    }

    /*
     * 책 유형별(이야기책/정보를 담은 책/주장을 담은 책) 연습 질문/답이 루미
     * 피드백을 통과한 순간 그 유형의 최신 통과본으로 자동저장한다.
     */
    @PutMapping("/api/students/{studentId}/after-reading/type-practice")
    public ResponseEntity<AfterReadingDataResponse> saveTypePracticeAnswers(
            @PathVariable Long studentId,
            @Valid @RequestBody AfterReadingTypePracticeRequest request,
            Authentication authentication) {

        requireSelf(studentId, authentication);

        return ResponseEntity.ok(
            afterReadingService.saveTypePracticeAnswers(studentId, request));
    }

    @GetMapping("/api/students/{studentId}/after-reading/shared")
    public ResponseEntity<List<AfterReadingSummaryItem>> getSharedSummaries(
            @PathVariable Long studentId,
            @RequestParam Long classReadingBookId,
            Authentication authentication) {

        requireSelf(studentId, authentication);

        return ResponseEntity.ok(
            afterReadingService.getSharedSummaries(
                studentId,
                classReadingBookId));
    }

    @PostMapping("/api/students/{studentId}/after-reading/summaries/{summaryId}/like")
    public ResponseEntity<AfterReadingSummaryItem> toggleSummaryLike(
            @PathVariable Long studentId,
            @PathVariable Long summaryId,
            Authentication authentication) {

        requireSelf(studentId, authentication);

        return ResponseEntity.ok(
            afterReadingService.toggleSummaryLike(studentId, summaryId));
    }

    @GetMapping("/api/teachers/{teacherId}/classes/{classId}/after-reading/summaries")
    public ResponseEntity<List<AfterReadingSummaryItem>> getTeacherSummaries(
            @PathVariable Long teacherId,
            @PathVariable Long classId,
            @RequestParam Long classReadingBookId,
            Authentication authentication) {

        requireSelf(teacherId, authentication);

        return ResponseEntity.ok(
            afterReadingService.getTeacherSharedSummaries(
                teacherId,
                classId,
                classReadingBookId));
    }

    @PostMapping("/api/teachers/{teacherId}/classes/{classId}/after-reading/summaries/{summaryId}/like")
    public ResponseEntity<AfterReadingSummaryItem> toggleTeacherSummaryLike(
            @PathVariable Long teacherId,
            @PathVariable Long classId,
            @PathVariable Long summaryId,
            Authentication authentication) {

        requireSelf(teacherId, authentication);

        return ResponseEntity.ok(
            afterReadingService.toggleSummaryLikeAsTeacher(
                teacherId,
                classId,
                summaryId));
    }

    @GetMapping("/api/teachers/{teacherId}/classes/{classId}/after-reading/summaries/review")
    public ResponseEntity<List<AfterReadingSummaryItem>> getSummaryReviews(
            @PathVariable Long teacherId, @PathVariable Long classId,
            @RequestParam(required = false) String status, Authentication authentication) {
        requireSelf(teacherId, authentication);
        return ResponseEntity.ok(afterReadingService.getTeacherReviewSummaries(teacherId, classId, status));
    }

    @PostMapping("/api/teachers/{teacherId}/classes/{classId}/after-reading/summaries/{summaryId}/review")
    public ResponseEntity<AfterReadingSummaryItem> reviewSummary(
            @PathVariable Long teacherId, @PathVariable Long classId, @PathVariable Long summaryId,
            @RequestBody java.util.Map<String, String> body, Authentication authentication) {
        requireSelf(teacherId, authentication);
        return ResponseEntity.ok(afterReadingService.reviewSummary(
            teacherId, classId, summaryId, body.get("status"), body.get("reason")));
    }

    @PutMapping("/api/students/{studentId}/after-reading/summaries/{summaryId}/resubmit")
    public ResponseEntity<AfterReadingSummaryItem> resubmitSummary(
            @PathVariable Long studentId, @PathVariable Long summaryId,
            @RequestBody java.util.Map<String, String> body, Authentication authentication) {
        requireSelf(studentId, authentication);
        return ResponseEntity.ok(afterReadingService.resubmitSummary(studentId, summaryId, body.get("summary")));
    }

    @DeleteMapping("/api/students/{studentId}/after-reading/summaries/{summaryId}")
    public ResponseEntity<Void> deleteRejectedSummary(
            @PathVariable Long studentId, @PathVariable Long summaryId, Authentication authentication) {
        requireSelf(studentId, authentication);
        afterReadingService.deleteRejectedSummary(studentId, summaryId);
        return ResponseEntity.noContent().build();
    }

    private void requireSelf(Long userId, Authentication authentication) {

        Object principal =
            authentication == null ? null : authentication.getPrincipal();

        if (!(principal instanceof Long loginUserId)
                || !loginUserId.equals(userId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "본인 계정으로만 읽기 후 데이터를 사용할 수 있습니다."
            );
        }
    }
}
