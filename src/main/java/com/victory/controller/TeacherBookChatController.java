package com.victory.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.BookChatReplyItem;
import com.victory.dto.BookChatReplyRequest;
import com.victory.dto.BookChatQuizAnswerItem;
import com.victory.dto.BookChatThoughtItem;
import com.victory.dto.BookChatThoughtRequest;
import com.victory.service.ResponseService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/*
 * 교사가 "책수다방 들어가기"로 연습읽기 책수다방에 직접 들어가 학생들과
 * 같은 대화(생각/답글)를 읽고 참여하는 전용 API. 학생용 BookChatController
 * (/api/students/{studentId}/book-chat)는 SecurityConfig에서 student
 * 권한만 허용하므로 교사 토큰으로는 애초에 도달할 수 없다 - 그래서 같은
 * 모양의 엔드포인트를 teacher 권한 네임스페이스에 별도로 둔다(좋아요
 * 기능의 기존 ...AsTeacher 패턴과 동일).
 *
 * 질문(책수다방 게시글) 목록은 새로 만들지 않는다 - 기존
 * GET /api/teachers/{teacherId}/book-thought-reviews?status=APPROVED
 * (BookThoughtReviewController)가 이미 "학생들이 실제로 보는 승인된
 * 질문 목록"과 정확히 같은 데이터를 돌려주므로 프론트가 그대로 재사용한다.
 */
@RestController
@RequestMapping("/api/teachers/{teacherId}/book-chat")
@RequiredArgsConstructor
public class TeacherBookChatController {

    private final ResponseService responseService;

    @GetMapping("/questions/{questionId}/thoughts")
    public ResponseEntity<List<BookChatThoughtItem>> getThoughts(
            @PathVariable Long teacherId,
            @PathVariable Long questionId,
            Authentication authentication) {

        requireSelf(teacherId, authentication);

        return ResponseEntity.ok(
            responseService.getBookChatThoughtsForTeacher(teacherId, questionId));
    }

    @GetMapping("/questions/{questionId}/quiz-answers")
    public ResponseEntity<List<BookChatQuizAnswerItem>> getQuizAnswers(
            @PathVariable Long teacherId,
            @PathVariable Long questionId,
            Authentication authentication) {

        requireSelf(teacherId, authentication);

        return ResponseEntity.ok(
            responseService.getBookChatQuizAnswersForTeacher(teacherId, questionId));
    }

    @GetMapping("/questions/{questionId}/replies")
    public ResponseEntity<List<BookChatReplyItem>> getReplies(
            @PathVariable Long teacherId,
            @PathVariable Long questionId,
            Authentication authentication) {

        requireSelf(teacherId, authentication);

        return ResponseEntity.ok(
            responseService.getBookChatRepliesForTeacher(teacherId, questionId));
    }

    /*
     * 학생용 saveThought(PUT .../questions/{id}/thought, 단수)와 같은
     * 경로 모양을 그대로 써서, 프론트가 역할별로 API 베이스 경로만
     * 바꾸면 되게 한다(HTTP 메서드/경로 구조가 다르면 분기 코드가
     * 훨씬 커진다).
     */
    @PostMapping("/questions/{questionId}/thoughts/{thoughtId}/replies")
    public ResponseEntity<BookChatReplyItem> saveReply(
            @PathVariable Long teacherId,
            @PathVariable Long questionId,
            @PathVariable Long thoughtId,
            @Valid @RequestBody BookChatReplyRequest request,
            Authentication authentication) {

        requireSelf(teacherId, authentication);

        return ResponseEntity.ok(
            responseService.saveBookChatReplyAsTeacher(
                teacherId, questionId, thoughtId, request));
    }

    @PutMapping("/questions/{questionId}/thoughts/{thoughtId}")
    public ResponseEntity<BookChatThoughtItem> updateThought(
            @PathVariable Long teacherId, @PathVariable Long questionId,
            @PathVariable Long thoughtId, @Valid @RequestBody BookChatThoughtRequest request,
            Authentication authentication) {
        requireSelf(teacherId, authentication);
        return ResponseEntity.ok(responseService.updateBookChatThoughtAsTeacher(teacherId, questionId, thoughtId, request));
    }

    @DeleteMapping("/questions/{questionId}/thoughts/{thoughtId}")
    public ResponseEntity<Void> deleteThought(
            @PathVariable Long teacherId, @PathVariable Long questionId,
            @PathVariable Long thoughtId, Authentication authentication) {
        requireSelf(teacherId, authentication);
        responseService.deleteBookChatThoughtAsTeacher(teacherId, questionId, thoughtId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/questions/{questionId}/replies/{replyId}")
    public ResponseEntity<BookChatReplyItem> updateReply(
            @PathVariable Long teacherId, @PathVariable Long questionId,
            @PathVariable Long replyId, @Valid @RequestBody BookChatReplyRequest request,
            Authentication authentication) {
        requireSelf(teacherId, authentication);
        return ResponseEntity.ok(responseService.updateBookChatReplyAsTeacher(teacherId, questionId, replyId, request));
    }

    @DeleteMapping("/questions/{questionId}/replies/{replyId}")
    public ResponseEntity<Void> deleteReply(
            @PathVariable Long teacherId, @PathVariable Long questionId,
            @PathVariable Long replyId, Authentication authentication) {
        requireSelf(teacherId, authentication);
        responseService.deleteBookChatReplyAsTeacher(teacherId, questionId, replyId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/questions/{questionId}/quiz-answers/{answerId}")
    public ResponseEntity<Void> deleteQuizAnswer(
            @PathVariable Long teacherId, @PathVariable Long questionId,
            @PathVariable Long answerId, Authentication authentication) {
        requireSelf(teacherId, authentication);
        responseService.deleteBookChatQuizAnswerAsTeacher(teacherId, questionId, answerId);
        return ResponseEntity.noContent().build();
    }

    private void requireSelf(Long teacherId, Authentication authentication) {

        Object principal =
            authentication == null ? null : authentication.getPrincipal();

        if (!(principal instanceof Long loginUserId)
                || !loginUserId.equals(teacherId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "본인 계정으로만 책수다방에 참여할 수 있습니다."
            );
        }
    }
}
