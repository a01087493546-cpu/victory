package com.victory.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.TeacherIndividualReadingDashboardResponse;
import com.victory.service.IndividualReadingDashboardService;

import lombok.RequiredArgsConstructor;

/*
 * 교사용 개별읽기 대시보드 조회. 경로 규칙은 기존
 * TeacherPracticeAchievementController의 "/api/teachers/{teacherId}/classes/{classId}/..."
 * 패턴을 그대로 따른다. /api/teachers/**는 SecurityConfig에서 이미
 * hasAuthority("teacher")로 걸려 있으므로 student 계정은 이 컨트롤러에
 * 도달하기 전에 403으로 걸러진다.
 */
@RestController
@RequiredArgsConstructor
public class TeacherIndividualReadingDashboardController {

    private final IndividualReadingDashboardService individualReadingDashboardService;

    @GetMapping("/api/teachers/{teacherId}/classes/{classId}/individual-reading/dashboard")
    public ResponseEntity<TeacherIndividualReadingDashboardResponse> getDashboard(
            @PathVariable Long teacherId,
            @PathVariable Long classId,
            Authentication authentication) {

        requireSelf(teacherId, authentication);

        return ResponseEntity.ok(individualReadingDashboardService.getDashboard(teacherId, classId));
    }

    private void requireSelf(Long teacherId, Authentication authentication) {
        Object principal = authentication == null ? null : authentication.getPrincipal();

        if (!(principal instanceof Long loginUserId) || !loginUserId.equals(teacherId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "본인 계정으로만 개별읽기 대시보드를 조회할 수 있습니다."
            );
        }
    }
}
