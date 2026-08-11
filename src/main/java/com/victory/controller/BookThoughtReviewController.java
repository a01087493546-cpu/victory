package com.victory.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.BookThoughtRejectRequest;
import com.victory.dto.BookThoughtResponseItem;
import com.victory.service.ResponseService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/teachers/{teacherId}/book-thought-reviews")
@RequiredArgsConstructor
public class BookThoughtReviewController {

    private final ResponseService responseService;

    @GetMapping
    public ResponseEntity<List<BookThoughtResponseItem>> getBookThoughtReviews(
            @PathVariable Long teacherId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long classReadingBookId,
            @RequestParam(required = false) String status,
            Authentication authentication) {

        requireSelf(teacherId, authentication);

        return ResponseEntity.ok(
            responseService.getTeacherBookThoughtReviews(
                teacherId,
                classId,
                classReadingBookId,
                status));
    }

    @PatchMapping("/{responseId}/approve")
    public ResponseEntity<BookThoughtResponseItem> approveBookThought(
            @PathVariable Long teacherId,
            @PathVariable Long responseId,
            Authentication authentication) {

        requireSelf(teacherId, authentication);

        return ResponseEntity.ok(
            responseService.approveBookThoughtResponse(teacherId, responseId));
    }

    @PatchMapping("/{responseId}/reject")
    public ResponseEntity<BookThoughtResponseItem> rejectBookThought(
            @PathVariable Long teacherId,
            @PathVariable Long responseId,
            @Valid @RequestBody BookThoughtRejectRequest request,
            Authentication authentication) {

        requireSelf(teacherId, authentication);

        return ResponseEntity.ok(
            responseService.rejectBookThoughtResponse(
                teacherId,
                responseId,
                request.getReason()));
    }

    @PatchMapping("/{responseId}/pending")
    public ResponseEntity<BookThoughtResponseItem> returnBookThoughtToPending(
            @PathVariable Long teacherId,
            @PathVariable Long responseId,
            Authentication authentication) {

        requireSelf(teacherId, authentication);

        return ResponseEntity.ok(
            responseService.returnBookThoughtResponseToPending(teacherId, responseId));
    }

    private void requireSelf(Long teacherId, Authentication authentication) {

        Object principal =
            authentication == null ? null : authentication.getPrincipal();

        if (!(principal instanceof Long loginUserId)
                || !loginUserId.equals(teacherId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "본인 교사 계정으로만 승인 목록을 처리할 수 있습니다."
            );
        }
    }
}
