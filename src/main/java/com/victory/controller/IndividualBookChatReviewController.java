package com.victory.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.BookThoughtRejectRequest;
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
