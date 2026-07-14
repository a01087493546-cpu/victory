package com.victory.controller;

import com.victory.dto.PracticeProgressRequest;
import com.victory.dto.PracticeProgressResponse;
import com.victory.service.PracticeProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            @PathVariable Long studentId) {

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
            @RequestBody PracticeProgressRequest request) {

        PracticeProgressResponse response =
                practiceProgressService.saveProgress(studentId, request);

        return ResponseEntity.ok(response);
    }
}