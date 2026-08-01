package com.victory.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.AiBookRecommendationRequest;
import com.victory.dto.AiBookRecommendationResponse;
import com.victory.service.AiBookRecommendationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/*
 * 학생 취향 기반 AI 책 추천 전용 API. 기존 태그 점수 후보 API
 * (RecommendationBookController)는 그대로 두고, 후보 중 AI가 3권을 고르는
 * 새 경로만 분리해서 추가한다. studentId는 요청 본문/URL로 받지 않고 JWT
 * Authentication에서만 확인한다.
 */
@RestController
@RequestMapping("/api/students/me/book-recommendations")
@RequiredArgsConstructor
public class BookRecommendationAiController {

    private final AiBookRecommendationService aiBookRecommendationService;

    @PostMapping("/ai")
    public ResponseEntity<AiBookRecommendationResponse> getAiRecommendations(
            @Valid @RequestBody AiBookRecommendationRequest request,
            Authentication authentication) {

        requireStudent(authentication);

        return ResponseEntity.ok(aiBookRecommendationService.recommend(request));
    }

    private Long requireStudent(Authentication authentication) {
        Object principal = authentication == null ? null : authentication.getPrincipal();

        if (!(principal instanceof Long studentId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "학생 로그인이 필요합니다.");
        }

        return studentId;
    }
}
