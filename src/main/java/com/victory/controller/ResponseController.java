package com.victory.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.BookThoughtResponseItem;
import com.victory.dto.BookThoughtResponseRequest;
import com.victory.dto.DuringPracticeResponseItem;
import com.victory.dto.DuringPracticeResponseRequest;
import com.victory.dto.DuringReviewResponseItem;
import com.victory.dto.DuringReviewResponseRequest;
import com.victory.dto.PreReadingResponseItem;
import com.victory.dto.PreReadingResponseRequest;
import com.victory.service.ResponseService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/students/{studentId}/responses")
@RequiredArgsConstructor
public class ResponseController {

    private final ResponseService responseService;

    /*
     * 연습읽기 읽기 전 질문·답 조회(재진입 시 복원용)
     */
    @GetMapping("/pre-reading")
    public ResponseEntity<List<PreReadingResponseItem>> getPreReadingResponses(
            @PathVariable Long studentId,
            Authentication authentication) {

        requireSelf(studentId, authentication);

        return ResponseEntity.ok(
            responseService.getPreReadingResponses(studentId));
    }

    /*
     * 연습읽기 읽기 전 질문·답 저장 또는 수정(AI good 판정 직후에만 호출됨)
     */
    @PostMapping("/pre-reading")
    public ResponseEntity<PreReadingResponseItem> savePreReadingResponse(
            @PathVariable Long studentId,
            @Valid @RequestBody PreReadingResponseRequest request,
            Authentication authentication) {

        requireSelf(studentId, authentication);

        return ResponseEntity.ok(
            responseService.savePreReadingResponse(studentId, request));
    }

    /*
     * 읽기 중 유형별 심화 연습 질문 조회(질문만 있고 답은 없음)
     */
    @GetMapping("/during-practice")
    public ResponseEntity<List<DuringPracticeResponseItem>> getDuringPracticeResponses(
            @PathVariable Long studentId,
            Authentication authentication) {

        requireSelf(studentId, authentication);

        return ResponseEntity.ok(
            responseService.getDuringPracticeResponses(studentId));
    }

    /*
     * 읽기 중 유형별 심화 연습 질문 저장 또는 수정(AI good 판정 직후에만 호출됨)
     */
    @PostMapping("/during-practice")
    public ResponseEntity<DuringPracticeResponseItem> saveDuringPracticeResponse(
            @PathVariable Long studentId,
            @Valid @RequestBody DuringPracticeResponseRequest request,
            Authentication authentication) {

        requireSelf(studentId, authentication);

        return ResponseEntity.ok(
            responseService.saveDuringPracticeResponse(studentId, request));
    }

    /*
     * 읽기 중 총복습 질문·답 조회
     */
    @GetMapping("/during-review")
    public ResponseEntity<List<DuringReviewResponseItem>> getDuringReviewResponses(
            @PathVariable Long studentId,
            Authentication authentication) {

        requireSelf(studentId, authentication);

        return ResponseEntity.ok(
            responseService.getDuringReviewResponses(studentId));
    }

    /*
     * 읽기 중 총복습 질문·답 저장 또는 수정(AI good 판정 직후에만 호출됨)
     */
    @PostMapping("/during-review")
    public ResponseEntity<DuringReviewResponseItem> saveDuringReviewResponse(
            @PathVariable Long studentId,
            @Valid @RequestBody DuringReviewResponseRequest request,
            Authentication authentication) {

        requireSelf(studentId, authentication);

        return ResponseEntity.ok(
            responseService.saveDuringReviewResponse(studentId, request));
    }

    /*
     * 읽기 중 "책 속 생각 쓰기" 조회(같은 책 기록만, 최근 생성 순)
     */
    @GetMapping("/book-thought")
    public ResponseEntity<List<BookThoughtResponseItem>> getBookThoughtResponses(
            @PathVariable Long studentId,
            @RequestParam(required = false) Long classReadingBookId,
            Authentication authentication) {

        requireSelf(studentId, authentication);

        return ResponseEntity.ok(
            responseService.getBookThoughtResponses(studentId, classReadingBookId));
    }

    /*
     * 읽기 중 "책 속 생각 쓰기" 저장(AI good 판정 직후에만 호출됨).
     * 유형별 upsert가 아니라 매번 새 기록으로 저장하되, 완전히 같은
     * 질문·답이 이미 있으면 새로 만들지 않고 기존 기록을 그대로 반환한다.
     */
    @PostMapping("/book-thought")
    public ResponseEntity<BookThoughtResponseItem> saveBookThoughtResponse(
            @PathVariable Long studentId,
            @Valid @RequestBody BookThoughtResponseRequest request,
            Authentication authentication) {

        requireSelf(studentId, authentication);

        return ResponseEntity.ok(
            responseService.saveBookThoughtResponse(studentId, request));
    }

    /*
     * 교사가 거절한 "책 속 생각 쓰기"만 학생 본인이 숨길 수 있다.
     * 실제 삭제가 아니라 deletedAt을 기록하는 소프트 삭제 방식이다.
     */
    @DeleteMapping("/book-thought/{responseId}")
    public ResponseEntity<Void> deleteRejectedBookThoughtResponse(
            @PathVariable Long studentId,
            @PathVariable Long responseId,
            Authentication authentication) {

        requireSelf(studentId, authentication);
        responseService.deleteRejectedBookThoughtResponse(studentId, responseId);

        return ResponseEntity.noContent().build();
    }

    /*
     * JWT의 로그인 사용자와 URL의 studentId가 다르면 다른 학생 데이터에
     * 접근하지 못하도록 막는다.
     */
    private void requireSelf(Long studentId, Authentication authentication) {

        Object principal =
            authentication == null ? null : authentication.getPrincipal();

        if (!(principal instanceof Long loginUserId)
                || !loginUserId.equals(studentId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "본인 데이터만 조회·수정할 수 있습니다."
            );
        }
    }
}
