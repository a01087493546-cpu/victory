package com.victory.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.AiFeedbackRequest;
import com.victory.dto.AiFeedbackResponse;
import com.victory.dto.StudentAiFeedbackRequest;
import com.victory.service.FeedbackAiService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/*
 * 인증된 학생 전용 AI 검사. 공개 /api/feedback/ai-review(FeedbackAiController)와
 * 달리 studentId를 요청 본문이 아니라 JWT에서만 가져오므로, 다른 학생 ID를
 * 위조해서 시도 기록을 남길 방법이 없다. 이 경로만 ai_evaluation_attempts에
 * 기록을 남긴다.
 *
 * 이 경로는 /api/students/** 패턴에 걸려 SecurityConfig의 기존 규칙
 * (.requestMatchers("/api/students/**").hasAuthority("student"))만으로
 * 이미 "student 권한 필수" 인증이 적용된다 - 새 규칙을 추가하지 않았다.
 */
@RestController
@RequestMapping("/api/students/me/feedback")
@RequiredArgsConstructor
public class StudentFeedbackAiController {

    private final FeedbackAiService feedbackAiService;

    @PostMapping("/ai-review")
    public AiFeedbackResponse getAiReviewForAuthenticatedStudent(
            @Valid @RequestBody StudentAiFeedbackRequest request,
            Authentication authentication) {

        Long studentId = requireStudentId(authentication);
        validateRequiredFieldsByActivityType(request);

        AiFeedbackRequest internalRequest = new AiFeedbackRequest();
        internalRequest.setType(request.getActivityType());
        internalRequest.setStepType(request.getQuestionType());
        internalRequest.setBookType(request.getBookType());
        internalRequest.setBookTitle(request.getBookTitle());
        internalRequest.setPassage(request.getPassage());

        if (isSummaryStyleActivityType(request.getActivityType())) {
            /*
             * final_summary/individual_summary는 question/answer 한 쌍이
             * 아니라 참고용 qaList(질문·답)와 summaryText(최종 간추리기)로
             * 평가한다.
             */
            internalRequest.setQaList(request.getQaList());
            internalRequest.setSummaryText(request.getSummaryText());
        } else {
            internalRequest.setQaList(
                List.of(new AiFeedbackRequest.QAItem(request.getQuestion(), request.getAnswer())));
        }

        return feedbackAiService.getFeedbackForAuthenticatedStudent(
            studentId,
            internalRequest,
            request.getEvaluationKey(),
            request.getClassReadingBookId(),
            request.getReadingRecordId());
    }

    private boolean isSummaryStyleActivityType(String activityType) {
        return FeedbackAiService.FINAL_SUMMARY_TYPE.equals(activityType)
            || FeedbackAiService.INDIVIDUAL_SUMMARY_TYPE.equals(activityType);
    }

    /*
     * activityType별로 실제 필요한 필드만 필수로 검증한다.
     * - final_summary/individual_summary: summaryText만 필수(question/answer는
     *   애초에 안 씀)
     * - during_reading_practice_deep: question만 필수(질문만 만들고 답은
     *   안 받는 화면이라 answer는 빈 문자열이어도 됨)
     * - 그 외: question/answer 모두 필수
     * StudentAiFeedbackRequest는 activityType마다 필수 필드가 달라
     * Bean Validation(@NotBlank)만으로 표현하기 어려워서 여기서 직접 검증한다.
     */
    private void validateRequiredFieldsByActivityType(StudentAiFeedbackRequest request) {

        if (isSummaryStyleActivityType(request.getActivityType())) {
            if (request.getSummaryText() == null || request.getSummaryText().isBlank()) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "간추리기 내용을 입력해야 합니다."
                );
            }
            return;
        }

        if (request.getQuestion() == null || request.getQuestion().isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "질문을 입력해야 합니다."
            );
        }

        if (FeedbackAiService.DURING_READING_PRACTICE_DEEP_TYPE.equals(request.getActivityType())) {
            return;
        }

        if (request.getAnswer() == null || request.getAnswer().isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "답을 입력해야 합니다."
            );
        }
    }

    /*
     * SecurityConfig가 이미 인증(401)과 student 권한(다른 role은 403)을
     * 강제하므로, 여기서는 principal이 예상한 타입(Long userId)인지만
     * 방어적으로 한 번 더 확인한다(ResponseController.requireSelf와 같은
     * 관례).
     */
    private Long requireStudentId(Authentication authentication) {

        Object principal =
            authentication == null ? null : authentication.getPrincipal();

        if (!(principal instanceof Long studentId)) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "학생 로그인이 필요합니다."
            );
        }

        return studentId;
    }
}
