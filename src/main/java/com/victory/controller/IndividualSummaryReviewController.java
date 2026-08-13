package com.victory.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.IndividualSummaryLikeResponse;
import com.victory.dto.IndividualSummaryShareItem;
import com.victory.service.IndividualSummaryShareService;

import lombok.RequiredArgsConstructor;

/*
 * 개별읽기 읽기 후 간추리기를 교사가 확인/좋아요하는 컨트롤러.
 * /api/teachers/** 패턴이라 SecurityConfig의 기존 규칙
 * (.requestMatchers("/api/teachers/**").hasAuthority("teacher"))로 이미
 * "teacher 권한 필수" 인증이 적용된다. 학생 화면(IndividualReadingController)과
 * 완전히 같은 IndividualSummaryShareService + content_likes 테이블을 함께
 * 쓰므로, 교사가 누른 좋아요가 학생 화면에도 즉시 반영된다.
 */
@RestController
@RequestMapping("/api/teachers/{teacherId}/individual-reading")
@RequiredArgsConstructor
public class IndividualSummaryReviewController {

    private final IndividualSummaryShareService individualSummaryShareService;

    @GetMapping("/summaries")
    public ResponseEntity<List<IndividualSummaryShareItem>> getClassSummaries(
            @PathVariable Long teacherId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            Authentication authentication) {

        requireSelf(teacherId, authentication);

        return ResponseEntity.ok(
            individualSummaryShareService.getClassSummariesForTeacher(teacherId, date));
    }

    @PostMapping("/summaries/{summaryId}/like")
    public ResponseEntity<IndividualSummaryLikeResponse> toggleSummaryLike(
            @PathVariable Long teacherId,
            @PathVariable Long summaryId,
            Authentication authentication) {

        requireSelf(teacherId, authentication);

        return ResponseEntity.ok(
            individualSummaryShareService.toggleLikeAsTeacher(teacherId, summaryId));
    }

    @GetMapping("/summaries/review")
    public ResponseEntity<List<IndividualSummaryShareItem>> getReviewSummaries(
            @PathVariable Long teacherId,
            @RequestParam(required = false) String status,
            Authentication authentication) {
        requireSelf(teacherId, authentication);
        return ResponseEntity.ok(individualSummaryShareService.getReviewSummariesForTeacher(teacherId, status));
    }

    @PostMapping("/summaries/{summaryId}/approve")
    public ResponseEntity<IndividualSummaryShareItem> approve(
            @PathVariable Long teacherId, @PathVariable Long summaryId, Authentication authentication) {
        requireSelf(teacherId, authentication);
        return ResponseEntity.ok(individualSummaryShareService.approve(teacherId, summaryId));
    }

    @PostMapping("/summaries/{summaryId}/reject")
    public ResponseEntity<IndividualSummaryShareItem> reject(
            @PathVariable Long teacherId, @PathVariable Long summaryId,
            @RequestBody java.util.Map<String, String> body, Authentication authentication) {
        requireSelf(teacherId, authentication);
        return ResponseEntity.ok(individualSummaryShareService.reject(teacherId, summaryId, body.get("reason")));
    }

    @PostMapping("/summaries/{summaryId}/pending")
    public ResponseEntity<IndividualSummaryShareItem> returnToPending(
            @PathVariable Long teacherId, @PathVariable Long summaryId, Authentication authentication) {
        requireSelf(teacherId, authentication);
        return ResponseEntity.ok(individualSummaryShareService.returnToPending(teacherId, summaryId));
    }

    private void requireSelf(Long teacherId, Authentication authentication) {
        Object principal = authentication == null ? null : authentication.getPrincipal();

        if (!(principal instanceof Long loginUserId) || !loginUserId.equals(teacherId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "본인 교사 계정으로만 개별읽기 간추리기를 확인할 수 있습니다."
            );
        }
    }
}
