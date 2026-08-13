package com.victory.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.BookThoughtRejectRequest;
import com.victory.dto.IndividualBookChatCommentRequest;
import com.victory.dto.IndividualBookChatCommentResponse;
import com.victory.dto.IndividualBookChatPostResponse;
import com.victory.service.IndividualBookChatService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/teachers/{teacherId}/individual-reading/book-chat")
@RequiredArgsConstructor
public class IndividualBookChatReviewController {

    private final IndividualBookChatService individualBookChatService;

    @GetMapping("/posts")
    public ResponseEntity<List<IndividualBookChatPostResponse>> getPosts(
            @PathVariable Long teacherId,
            @RequestParam(required = false) String status,
            Authentication authentication) {

        requireSelf(teacherId, authentication);

        return ResponseEntity.ok(
            individualBookChatService.getTeacherManagedPosts(teacherId, status));
    }

    /*
     * 교사가 "책수다방 들어가기"로 개별읽기 책수다방에 들어와 학생과 같은
     * 대화(밸런스 글에 달린 댓글)를 읽고 직접 참여하는 API. 글 목록은
     * 위 getPosts(status=APPROVED)를 그대로 재사용하고, 댓글만 새로 추가한다.
     */
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<List<IndividualBookChatCommentResponse>> getComments(
            @PathVariable Long teacherId,
            @PathVariable Long postId,
            Authentication authentication) {

        requireSelf(teacherId, authentication);

        return ResponseEntity.ok(
            individualBookChatService.getCommentsForTeacher(teacherId, postId));
    }

    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<IndividualBookChatCommentResponse> createComment(
            @PathVariable Long teacherId,
            @PathVariable Long postId,
            @RequestBody IndividualBookChatCommentRequest request,
            Authentication authentication) {

        requireSelf(teacherId, authentication);

        IndividualBookChatCommentResponse response = individualBookChatService.createCommentAsTeacher(
            teacherId, postId, request.getChoice(), request.getContent());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/comments/{commentId}")
    public ResponseEntity<IndividualBookChatCommentResponse> updateComment(
            @PathVariable Long teacherId, @PathVariable Long commentId,
            @RequestBody IndividualBookChatCommentRequest request,
            Authentication authentication) {
        requireSelf(teacherId, authentication);
        return ResponseEntity.ok(individualBookChatService.updateCommentAsTeacher(
            teacherId, commentId, request.getChoice(), request.getContent()));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long teacherId, @PathVariable Long commentId,
            Authentication authentication) {
        requireSelf(teacherId, authentication);
        individualBookChatService.deleteCommentAsTeacher(teacherId, commentId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/posts/{postId}/approve")
    public ResponseEntity<IndividualBookChatPostResponse> approvePost(
            @PathVariable Long teacherId,
            @PathVariable Long postId,
            Authentication authentication) {

        requireSelf(teacherId, authentication);

        return ResponseEntity.ok(
            individualBookChatService.approvePost(teacherId, postId));
    }

    @PatchMapping("/posts/{postId}/reject")
    public ResponseEntity<IndividualBookChatPostResponse> rejectPost(
            @PathVariable Long teacherId,
            @PathVariable Long postId,
            @Valid @RequestBody BookThoughtRejectRequest request,
            Authentication authentication) {

        requireSelf(teacherId, authentication);

        return ResponseEntity.ok(
            individualBookChatService.rejectPost(teacherId, postId, request.getReason()));
    }

    @PatchMapping("/posts/{postId}/pending")
    public ResponseEntity<IndividualBookChatPostResponse> returnPostToPending(
            @PathVariable Long teacherId,
            @PathVariable Long postId,
            Authentication authentication) {

        requireSelf(teacherId, authentication);

        return ResponseEntity.ok(
            individualBookChatService.returnPostToPending(teacherId, postId));
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long teacherId, @PathVariable Long postId,
            Authentication authentication) {
        requireSelf(teacherId, authentication);
        individualBookChatService.deletePostAsTeacher(teacherId, postId);
        return ResponseEntity.noContent().build();
    }

    private void requireSelf(Long teacherId, Authentication authentication) {
        Object principal = authentication == null ? null : authentication.getPrincipal();

        if (!(principal instanceof Long loginUserId) || !loginUserId.equals(teacherId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "본인 교사 계정으로만 개별읽기 책수다방 글을 처리할 수 있습니다."
            );
        }
    }
}
