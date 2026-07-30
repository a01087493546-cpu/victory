package com.victory.controller;

import java.util.List;

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

import com.victory.dto.BookRecommendationClassWallResponse;
import com.victory.dto.BookRecommendationCompletedBookItem;
import com.victory.dto.BookRecommendationCreateRequest;
import com.victory.dto.BookRecommendationItem;
import com.victory.dto.BookRecommendationLikeResponse;
import com.victory.dto.BookRecommendationQuestionItem;
import com.victory.service.BookRecommendationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/*
 * 우리 반 추천 책장(학생 화면) 전용 컨트롤러. /api/students/** 패턴이라
 * SecurityConfig의 기존 규칙(.requestMatchers("/api/students/**").hasAuthority("student"))
 * 만으로 이미 "student 권한 필수" 인증이 적용된다 - 교사 계정으로는
 * 애초에 이 경로를 호출할 수 없다. 학생 ID는 항상 JWT(Authentication)에서만
 * 가져온다(요청 body/URL 값은 신뢰하지 않음).
 */
@RestController
@RequestMapping("/api/students/me/book-recommendations")
@RequiredArgsConstructor
public class BookRecommendationController {

    private final BookRecommendationService bookRecommendationService;

    @PostMapping
    public ResponseEntity<BookRecommendationItem> createRecommendation(
            @Valid @RequestBody BookRecommendationCreateRequest request,
            Authentication authentication) {

        Long studentId = requireStudentId(authentication);
        BookRecommendationItem response = bookRecommendationService.createRecommendation(studentId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/class-wall")
    public ResponseEntity<BookRecommendationClassWallResponse> getClassWall(Authentication authentication) {
        Long studentId = requireStudentId(authentication);

        return ResponseEntity.ok(bookRecommendationService.getClassWallForStudent(studentId));
    }

    @GetMapping("/completed-books")
    public ResponseEntity<List<BookRecommendationCompletedBookItem>> getCompletedBooks(Authentication authentication) {
        Long studentId = requireStudentId(authentication);

        return ResponseEntity.ok(bookRecommendationService.getCompletedBooks(studentId));
    }

    @GetMapping("/completed-books/{readingRecordId}/questions")
    public ResponseEntity<List<BookRecommendationQuestionItem>> getCompletedBookQuestions(
            @PathVariable Long readingRecordId,
            Authentication authentication) {

        Long studentId = requireStudentId(authentication);

        return ResponseEntity.ok(
            bookRecommendationService.getCompletedBookQuestions(studentId, readingRecordId));
    }

    @PostMapping("/{recommendationId}/like")
    public ResponseEntity<BookRecommendationLikeResponse> toggleLike(
            @PathVariable Long recommendationId,
            Authentication authentication) {

        Long studentId = requireStudentId(authentication);

        return ResponseEntity.ok(
            bookRecommendationService.toggleLikeAsStudent(studentId, recommendationId));
    }

    private Long requireStudentId(Authentication authentication) {
        Object principal = authentication == null ? null : authentication.getPrincipal();

        if (!(principal instanceof Long studentId)) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "학생 로그인이 필요합니다."
            );
        }

        return studentId;
    }
}
