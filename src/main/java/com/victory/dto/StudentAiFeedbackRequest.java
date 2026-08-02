package com.victory.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * 인증된 학생 전용 AI 검사 요청(/api/students/me/feedback/ai-review).
 * 공개 AiFeedbackRequest와 달리 studentId 필드 자체가 없다 - 학생 ID는
 * 컨트롤러가 JWT(Authentication)에서만 가져오고, 이 DTO에는 위조할 수 있는
 * 값을 아예 받지 않는다.
 *
 * activityType/questionType은 기존 AiFeedbackRequest의 type/stepType과
 * 같은 값을 그대로 쓴다(예: pre_reading_question, during_reading_question,
 * during_reading_practice_deep, during_reading_practice_review) - AI
 * 프롬프트 선택 로직(FeedbackAiService.buildRequestBody)이 그 값으로
 * 분기하므로 그대로 재사용해야 한다.
 *
 * answer는 일부러 @NotBlank를 붙이지 않았다 - 읽기 중 "심화 연습"
 * (during_reading_practice_deep)은 질문만 만들고 답은 아예 받지 않는
 * 화면이라 빈 문자열을 보낸다. answer가 꼭 필요한 나머지 활동 유형은
 * StudentFeedbackAiController가 activityType을 보고 직접 검증한다
 * (Bean Validation으로 "이 필드가 저 필드 값일 때만 필수"를 표현하기
 * 어려워서 컨트롤러에서 처리 - FeedbackAiService.isPracticeReadingAiCheckType
 * 목록과 어긋나지 않게 같은 상수 문자열을 그대로 비교한다).
 */
@Getter
@NoArgsConstructor
public class StudentAiFeedbackRequest {

    /*
     * final_summary는 question/answer 한 쌍이 아니라 qaList(참고용 질문·답
     * 3개)와 summaryText(최종 간추리기)로 평가하므로 question에도 @NotBlank를
     * 걸 수 없다. activityType별 필수 필드는
     * StudentFeedbackAiController.validateRequiredFieldsByActivityType이
     * 직접 검증한다.
     */
    private String question;

    private String answer;

    @NotBlank
    private String activityType;

    private String questionType;

    /*
     * 같은 질문을 고쳐 쓰며 재요청할 때 항상 같은 값을 보내야 하는 키.
     * 다른 질문이면 다른 값을 보내야 한다(프론트의 evaluationKey 생성
     * 규칙은 이후 별도 작업).
     */
    @NotBlank
    private String evaluationKey;

    /*
     * 이 평가가 속한 학급의 온책읽기 책 id(ClassReadingBook.id). 온책읽기
     * 화면만 채워 보낸다. 서버가 "이 학생이 실제로 그 학급 책에 참여할 수
     * 있는지"를 검증한 뒤에만 저장을 허용한다.
     *
     * 개별읽기 화면은 ClassReadingBook이 없으므로 이 값 대신 아래
     * readingRecordId를 채워 보낸다. 둘 다 없으면 서버가 400으로 거부한다
     * (FeedbackAiService.validateEvaluationScope).
     */
    private Long classReadingBookId;

    /*
     * 개별읽기 화면 전용(ReadingRecord.id). 서버가 "이 학생이 실제로 그
     * 개별읽기 기록의 주인인지"를 검증한 뒤에만 저장을 허용한다.
     */
    private Long readingRecordId;

    /*
     * pre_reading_question(stepType=title일 때)/extra_practice 등 일부
     * 유형만 쓰는 선택 필드. 기존 AiFeedbackRequest와 같은 이름을 그대로
     * 맞춰서 나중에 프론트를 이 엔드포인트로 옮길 때 매핑이 1:1이 되게 한다.
     */
    private String bookType;
    private String bookTitle;
    private String passage;

    /*
     * final_summary 전용 필드. qaList는 학생이 앞서 만든 질문·답 참고
     * 자료(보통 3개), summaryText는 학생이 쓴 최종 간추리기 내용이다.
     * 다른 activityType은 사용하지 않는다(항상 null).
     */
    private List<AiFeedbackRequest.QAItem> qaList;
    private String summaryText;
}
