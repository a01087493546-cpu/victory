package com.victory.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.BookPreferenceRequest;
import com.victory.dto.RecommendationBookItem;
import com.victory.dto.RecommendationCandidateItem;
import com.victory.service.RecommendationBookService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/students/me/recommendation-books")
@RequiredArgsConstructor
public class RecommendationBookController {

    private final RecommendationBookService recommendationBookService;

    @GetMapping
    public ResponseEntity<List<RecommendationBookItem>> getActiveBooks(Authentication authentication) {
        requireStudent(authentication);

        return ResponseEntity.ok(recommendationBookService.getActiveBooks());
    }

    @PostMapping("/candidates")
    public ResponseEntity<List<RecommendationCandidateItem>> getCandidates(
            @Valid @RequestBody BookPreferenceRequest request,
            Authentication authentication) {

        requireStudent(authentication);

        return ResponseEntity.ok(recommendationBookService.getCandidates(request));
    }

    private Long requireStudent(Authentication authentication) {
        Object principal = authentication == null ? null : authentication.getPrincipal();

        if (!(principal instanceof Long studentId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "학생 로그인이 필요합니다.");
        }

        return studentId;
    }
}
