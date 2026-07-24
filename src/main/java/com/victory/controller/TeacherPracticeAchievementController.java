package com.victory.controller;

import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.PracticeAchievementHistoryResponse;
import com.victory.dto.PracticeAchievementResponse;
import com.victory.service.PracticeAchievementService;

import lombok.RequiredArgsConstructor;

/*
 * 교사용 연습읽기 성취도/그래프 조회. 경로 규칙은 기존
 * AfterReadingController의 "/api/teachers/{teacherId}/classes/{classId}/..."
 * 패턴을 그대로 따른다.
 */
@RestController
@RequiredArgsConstructor
public class TeacherPracticeAchievementController {

    private final PracticeAchievementService practiceAchievementService;

    @GetMapping("/api/teachers/{teacherId}/classes/{classId}/practice-achievement")
    public ResponseEntity<PracticeAchievementResponse> getPracticeAchievement(
            @PathVariable Long teacherId,
            @PathVariable Long classId,
            @RequestParam(required = false) Long classReadingBookId,
            Authentication authentication) {

        requireSelf(teacherId, authentication);

        return ResponseEntity.ok(
            practiceAchievementService.getClassAchievement(
                teacherId, classId, classReadingBookId));
    }

    @GetMapping("/api/teachers/{teacherId}/classes/{classId}/practice-achievement/history")
    public ResponseEntity<PracticeAchievementHistoryResponse> getPracticeAchievementHistory(
            @PathVariable Long teacherId,
            @PathVariable Long classId,
            @RequestParam(required = false) Long classReadingBookId,
            @RequestParam Long studentId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            Authentication authentication) {

        requireSelf(teacherId, authentication);

        return ResponseEntity.ok(
            practiceAchievementService.getAchievementHistory(
                teacherId, classId, classReadingBookId, studentId, from, to));
    }

    private void requireSelf(Long teacherId, Authentication authentication) {

        Object principal =
            authentication == null ? null : authentication.getPrincipal();

        if (!(principal instanceof Long loginUserId)
                || !loginUserId.equals(teacherId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "본인 계정으로만 성취도 데이터를 조회할 수 있습니다."
            );
        }
    }
}
