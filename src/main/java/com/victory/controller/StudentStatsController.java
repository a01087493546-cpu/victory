package com.victory.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.StudentStatsResponse;
import com.victory.service.StudentStatsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/students/{studentId}/stats")
@RequiredArgsConstructor
public class StudentStatsController {

    private final StudentStatsService studentStatsService;

    @GetMapping
    public ResponseEntity<StudentStatsResponse> getStats(
            @PathVariable Long studentId,
            Authentication authentication) {

        requireSelf(studentId, authentication);

        return ResponseEntity.ok(studentStatsService.getStats(studentId));
    }

    private void requireSelf(Long studentId, Authentication authentication) {
        Object principal =
            authentication == null ? null : authentication.getPrincipal();

        if (!(principal instanceof Long loginUserId)
                || !loginUserId.equals(studentId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "본인 능력치만 조회할 수 있습니다.");
        }
    }
}
