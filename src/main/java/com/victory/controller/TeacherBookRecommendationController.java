package com.victory.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.BookRecommendationClassWallResponse;
import com.victory.dto.BookRecommendationLikeResponse;
import com.victory.service.BookRecommendationService;

import lombok.RequiredArgsConstructor;

/*
 * 우리 반 추천 책장을 교사가 조회/좋아요하는 컨트롤러. /api/teachers/**
 * 패턴이라 SecurityConfig의 기존 규칙
 * (.requestMatchers("/api/teachers/**").hasAuthority("teacher"))로 이미
 * "teacher 권한 필수" 인증이 적용된다. 학생 화면(BookRecommendationController)과
 * 완전히 같은 BookRecommendationService + content_likes 테이블을 함께
 * 쓰므로, 교사가 누른 좋아요가 학생 화면에도 즉시 반영된다. 추천 글 작성
 * API는 의도적으로 두지 않는다(교사는 작성 불가).
 */
@RestController
@RequestMapping("/api/teachers/{teacherId}/book-recommendations")
@RequiredArgsConstructor
public class TeacherBookRecommendationController {

    private final BookRecommendationService bookRecommendationService;

    @GetMapping("/class-wall")
    public ResponseEntity<BookRecommendationClassWallResponse> getClassWall(
            @PathVariable Long teacherId,
            Authentication authentication) {

        requireSelf(teacherId, authentication);

        return ResponseEntity.ok(bookRecommendationService.getClassWallForTeacher(teacherId));
    }

    @PostMapping("/{recommendationId}/like")
    public ResponseEntity<BookRecommendationLikeResponse> toggleLike(
            @PathVariable Long teacherId,
            @PathVariable Long recommendationId,
            Authentication authentication) {

        requireSelf(teacherId, authentication);

        return ResponseEntity.ok(
            bookRecommendationService.toggleLikeAsTeacher(teacherId, recommendationId));
    }

    private void requireSelf(Long teacherId, Authentication authentication) {
        Object principal = authentication == null ? null : authentication.getPrincipal();

        if (!(principal instanceof Long loginUserId) || !loginUserId.equals(teacherId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "본인 교사 계정으로만 추천 책장을 확인할 수 있습니다."
            );
        }
    }
}
