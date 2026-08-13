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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.BookChatQuizAnswerItem;
import com.victory.dto.BookChatQuizAnswerRequest;
import com.victory.dto.BookChatReplyItem;
import com.victory.dto.BookChatReplyRequest;
import com.victory.dto.BookChatThoughtItem;
import com.victory.dto.BookChatThoughtRequest;
import com.victory.dto.BookThoughtResponseItem;
import com.victory.service.ResponseService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/students/{studentId}/book-chat")
@RequiredArgsConstructor
public class BookChatController {

    private final ResponseService responseService;

    @GetMapping("/questions")
    public ResponseEntity<List<BookThoughtResponseItem>> getBookChatQuestions(
            @PathVariable Long studentId,
            @RequestParam Long classReadingBookId,
            Authentication authentication) {

        requireSelf(studentId, authentication);

        return ResponseEntity.ok(
            responseService.getBookChatQuestions(studentId, classReadingBookId));
    }

    @GetMapping("/questions/{questionId}/thoughts")
    public ResponseEntity<List<BookChatThoughtItem>> getThoughts(
            @PathVariable Long studentId,
            @PathVariable Long questionId,
            Authentication authentication) {

        requireSelf(studentId, authentication);

        return ResponseEntity.ok(
            responseService.getBookChatThoughts(studentId, questionId));
    }

    @PutMapping("/questions/{questionId}/thought")
    public ResponseEntity<BookChatThoughtItem> saveThought(
            @PathVariable Long studentId,
            @PathVariable Long questionId,
            @Valid @RequestBody BookChatThoughtRequest request,
            Authentication authentication) {

        requireSelf(studentId, authentication);

        return ResponseEntity.ok(
            responseService.saveBookChatThought(studentId, questionId, request));
    }

    @GetMapping("/questions/{questionId}/quiz-answers")
    public ResponseEntity<List<BookChatQuizAnswerItem>> getQuizAnswers(
            @PathVariable Long studentId,
            @PathVariable Long questionId,
            Authentication authentication) {

        requireSelf(studentId, authentication);

        return ResponseEntity.ok(
            responseService.getBookChatQuizAnswers(studentId, questionId));
    }

    @PutMapping("/questions/{questionId}/quiz-answer")
    public ResponseEntity<BookChatQuizAnswerItem> saveQuizAnswer(
            @PathVariable Long studentId,
            @PathVariable Long questionId,
            @Valid @RequestBody BookChatQuizAnswerRequest request,
            Authentication authentication) {

        requireSelf(studentId, authentication);

        return ResponseEntity.ok(
            responseService.saveBookChatQuizAnswer(studentId, questionId, request));
    }

    @GetMapping("/questions/{questionId}/replies")
    public ResponseEntity<List<BookChatReplyItem>> getReplies(
            @PathVariable Long studentId,
            @PathVariable Long questionId,
            Authentication authentication) {

        requireSelf(studentId, authentication);

        return ResponseEntity.ok(
            responseService.getBookChatReplies(studentId, questionId));
    }

    @PostMapping("/questions/{questionId}/thoughts/{thoughtId}/replies")
    public ResponseEntity<BookChatReplyItem> saveReply(
            @PathVariable Long studentId,
            @PathVariable Long questionId,
            @PathVariable Long thoughtId,
            @Valid @RequestBody BookChatReplyRequest request,
            Authentication authentication) {

        requireSelf(studentId, authentication);

        return ResponseEntity.ok(
            responseService.saveBookChatReply(
                studentId, questionId, thoughtId, request));
    }

    @PutMapping("/questions/{questionId}/thoughts/{thoughtId}")
    public ResponseEntity<BookChatThoughtItem> updateThought(
            @PathVariable Long studentId, @PathVariable Long questionId,
            @PathVariable Long thoughtId, @Valid @RequestBody BookChatThoughtRequest request,
            Authentication authentication) {
        requireSelf(studentId, authentication);
        return ResponseEntity.ok(responseService.updateBookChatThought(studentId, questionId, thoughtId, request));
    }

    @DeleteMapping("/questions/{questionId}/thoughts/{thoughtId}")
    public ResponseEntity<Void> deleteThought(
            @PathVariable Long studentId, @PathVariable Long questionId,
            @PathVariable Long thoughtId, Authentication authentication) {
        requireSelf(studentId, authentication);
        responseService.deleteBookChatThought(studentId, questionId, thoughtId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/questions/{questionId}/replies/{replyId}")
    public ResponseEntity<BookChatReplyItem> updateReply(
            @PathVariable Long studentId, @PathVariable Long questionId,
            @PathVariable Long replyId, @Valid @RequestBody BookChatReplyRequest request,
            Authentication authentication) {
        requireSelf(studentId, authentication);
        return ResponseEntity.ok(responseService.updateBookChatReply(studentId, questionId, replyId, request));
    }

    @DeleteMapping("/questions/{questionId}/replies/{replyId}")
    public ResponseEntity<Void> deleteReply(
            @PathVariable Long studentId, @PathVariable Long questionId,
            @PathVariable Long replyId, Authentication authentication) {
        requireSelf(studentId, authentication);
        responseService.deleteBookChatReply(studentId, questionId, replyId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/questions/{questionId}/quiz-answers/{answerId}")
    public ResponseEntity<BookChatQuizAnswerItem> updateQuizAnswer(
            @PathVariable Long studentId, @PathVariable Long questionId,
            @PathVariable Long answerId, @Valid @RequestBody BookChatQuizAnswerRequest request,
            Authentication authentication) {
        requireSelf(studentId, authentication);
        return ResponseEntity.ok(responseService.updateBookChatQuizAnswer(studentId, questionId, answerId, request));
    }

    @DeleteMapping("/questions/{questionId}/quiz-answers/{answerId}")
    public ResponseEntity<Void> deleteQuizAnswer(
            @PathVariable Long studentId, @PathVariable Long questionId,
            @PathVariable Long answerId, Authentication authentication) {
        requireSelf(studentId, authentication);
        responseService.deleteBookChatQuizAnswer(studentId, questionId, answerId);
        return ResponseEntity.noContent().build();
    }

    private void requireSelf(Long studentId, Authentication authentication) {

        Object principal =
            authentication == null ? null : authentication.getPrincipal();

        if (!(principal instanceof Long loginUserId)
                || !loginUserId.equals(studentId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "본인 계정으로만 책수다방 질문을 조회할 수 있습니다."
            );
        }
    }
}
