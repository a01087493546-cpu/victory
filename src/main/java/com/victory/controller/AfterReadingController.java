package com.victory.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.AfterReadingDataResponse;
import com.victory.dto.AfterReadingBookTypeRequest;
import com.victory.dto.AfterReadingSaveRequest;
import com.victory.dto.AfterReadingSummaryItem;
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
