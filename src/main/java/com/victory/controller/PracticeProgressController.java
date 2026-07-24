package com.victory.controller;

import com.victory.dto.PracticeProgressRequest;
import com.victory.dto.PracticeProgressResponse;
import com.victory.service.PracticeProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/students/{studentId}/practice-progress")
@RequiredArgsConstructor
public class PracticeProgressController {

    private final PracticeProgressService practiceProgressService;

    /*
     * 학생의 현재 연습읽기 진행 상태 조회
     */
    @GetMapping
    public ResponseEntity<PracticeProgressResponse> getProgress(
            @PathVariable Long studentId,
            Authentication authentication) {

        requireSelfOrTeacher(studentId, authentication);

        PracticeProgressResponse response =
                practiceProgressService.getProgress(studentId);

        return ResponseEntity.ok(response);
    }

    /*
     * 학생의 연습읽기 진행 상태 저장
     */
    @PostMapping
    public ResponseEntity<PracticeProgressResponse> saveProgress(
            @PathVariable Long studentId,
            @RequestBody PracticeProgressRequest request,
            Authentication authentication) {

        return saveOwnProgress(studentId, request, authentication);
    }

    /*
     * 학생의 연습읽기 진행 상태 저장
     */
    @PutMapping
    public ResponseEntity<PracticeProgressResponse> updateProgress(
            @PathVariable Long studentId,
            @RequestBody PracticeProgressRequest request,
            Authentication authentication) {

        return saveOwnProgress(studentId, request, authentication);
    }

    private ResponseEntity<PracticeProgressResponse> saveOwnProgress(
            Long studentId,
            PracticeProgressRequest request,
            Authentication authentication) {

        requireSelf(studentId, authentication);

        PracticeProgressResponse response =
                practiceProgressService.saveProgress(studentId, request);
        return ResponseEntity.ok(response);
    }

    private void requireSelfOrTeacher(
            Long studentId,
            Authentication authentication) {

        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        "teacher".equals(authority.getAuthority()))) {
            return;
        }

        requireSelf(studentId, authentication);
    }

    private void requireSelf(Long studentId, Authentication authentication) {

        Object principal =
                authentication == null ? null : authentication.getPrincipal();

        if (!(principal instanceof Long loginUserId)
                || !loginUserId.equals(studentId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "본인 진행 상태만 저장할 수 있습니다."
            );
        }
    }
}
