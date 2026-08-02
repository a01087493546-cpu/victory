package com.victory.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.IndividualBookChatCommentRequest;
import com.victory.dto.IndividualBookChatCommentResponse;
import com.victory.dto.IndividualBookChatFeedResponse;
import com.victory.dto.IndividualBookChatPostRequest;
import com.victory.dto.IndividualBookChatPostResponse;
import com.victory.service.IndividualBookChatService;

import lombok.RequiredArgsConstructor;

/*
 * 개별읽기 책수다방(밸런스 글쓰기 + 댓글) 전용 컨트롤러. /api/students/** 패턴에
 * 걸려 SecurityConfig의 기존 규칙만으로 student 권한 인증이 이미 적용된다.
 * 학생 ID는 항상 JWT(Authentication)에서만 가져온다.
 */
@RestController
@RequestMapping("/api/students/me/individual-reading/book-chat")
@RequiredArgsConstructor
public class IndividualBookChatController {

    private final IndividualBookChatService individualBookChatService;

    /*
     * 같은 학급 학생들의 책수다 글 전체를 돌려준다(본인 글 포함, isMine으로
     * 구분). 학급이 없는 계정은 hasClass=false와 함께 본인 글만 내려준다.
     */
    @GetMapping("/posts")
    public ResponseEntity<IndividualBookChatFeedResponse> getClassFeed(Authentication authentication) {
        Long studentId = requireStudentId(authentication);

        return ResponseEntity.ok(individualBookChatService.getClassFeed(studentId));
    }

    @PostMapping(value = "/posts", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<IndividualBookChatPostResponse> createPost(
            @RequestBody IndividualBookChatPostRequest request,
            Authentication authentication) {

        Long studentId = requireStudentId(authentication);

        IndividualBookChatPostResponse response = individualBookChatService.createPost(
            studentId,
            request.getReadingRecordId(),
            request.getBookTitle(),
            request.getTitle(),
            request.getScene(),
            request.getOptionA(),
            request.getOptionB());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /*
     * 친구 책수다 글에 A/B 선택과 이유를 남기는 댓글. postId는 실제
     * 책수다 글의 id다.
     */
    @PostMapping(value = "/comments", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<IndividualBookChatCommentResponse> createComment(
            @RequestBody IndividualBookChatCommentRequest request,
            Authentication authentication) {

        Long studentId = requireStudentId(authentication);

        IndividualBookChatCommentResponse response = individualBookChatService.createComment(
            studentId, request.getPostId(), request.getChoice(), request.getContent());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<List<IndividualBookChatCommentResponse>> getComments(
            @PathVariable Long postId,
            Authentication authentication) {

        Long studentId = requireStudentId(authentication);

        return ResponseEntity.ok(individualBookChatService.getComments(studentId, postId));
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long postId,
            Authentication authentication) {

        Long studentId = requireStudentId(authentication);
        individualBookChatService.deletePost(studentId, postId);

        return ResponseEntity.noContent().build();
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
