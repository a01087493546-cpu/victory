package com.victory.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.ReadingFocusAddRequest;
import com.victory.dto.ReadingFocusResponse;
import com.victory.service.StudentStatsService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/*
 * 독서 집중 시간 누적 초(연습읽기+개별읽기 공통) 조회/추가.
 * 심사계정은 프론트(reading-focus.html)에서 아예 이 API를 호출하지 않고
 * 브라우저 localStorage만 사용한다 - StudentStatsService.addReadingFocusSeconds가
 * 혹시 모를 호출에도 DB에는 반영하지 않도록 한 번 더 막는다.
 */
@RestController
@RequestMapping("/api/students/{studentId}/reading-focus")
@RequiredArgsConstructor
public class ReadingFocusController {

    private final StudentStatsService studentStatsService;

    @GetMapping
    public ResponseEntity<ReadingFocusResponse> getReadingFocus(
            @PathVariable Long studentId,
            Authentication authentication) {

        requireSelf(studentId, authentication);

        return ResponseEntity.ok(studentStatsService.getReadingFocus(studentId));
    }

    @PostMapping
    public ResponseEntity<ReadingFocusResponse> addReadingFocus(
            @PathVariable Long studentId,
            @Valid @RequestBody ReadingFocusAddRequest request,
            Authentication authentication) {

        requireSelf(studentId, authentication);

        ReadingFocusResponse response =
            studentStatsService.addReadingFocusSeconds(studentId, request.getAddSeconds());

        return ResponseEntity.ok(response);
    }

    private void requireSelf(Long studentId, Authentication authentication) {
        Object principal =
            authentication == null ? null : authentication.getPrincipal();

        if (!(principal instanceof Long loginUserId)
                || !loginUserId.equals(studentId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "본인 독서 집중 시간만 조회/저장할 수 있습니다.");
        }
    }
}
