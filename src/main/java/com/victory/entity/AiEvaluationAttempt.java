package com.victory.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
 * 연습읽기 AI 검사(질문/답 판정) 시도 기록.
 *
 * responses 테이블은 "AI가 good을 준 뒤에만" 저장되므로, 그 이전에 학생이
 * need를 몇 번 받았는지는 responses만으로는 절대 재구성할 수 없다. 이 테이블은
 * FeedbackAiController(/api/feedback/ai-review)가 실제로 정상 판정을 완료할
 * 때마다(성공적으로 파싱된 good/need 결과가 나올 때만, 네트워크/파싱 오류는
 * 제외) 한 행씩 남겨서, "이 질문을 몇 번째 시도에서 통과했는지"를 나중에
 * 재구성할 수 있게 한다.
 *
 * 다만 /api/feedback/ai-review는 인증이 없는 permitAll 엔드포인트라 기존
 * 프론트 어디에서도 studentId를 함께 보내지 않는다(프론트 수정은 이번
 * 작업 범위 밖). 그래서 studentId가 없는 요청은 아예 기록하지 않는다
 * (FeedbackAiService 참고). 프론트가 studentId를 함께 보내도록 바뀌기
 * 전까지는 이 테이블에 실제로 쌓이는 행이 없을 수 있다는 뜻이다.
 */
@Entity
@Table(
    name = "ai_evaluation_attempts",
    indexes = {
        @Index(
            name = "idx_ai_eval_attempts_student_activity_question",
            columnList = "student_id, activity_type, question_type"
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiEvaluationAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    /*
     * AiFeedbackRequest.type 값 그대로(예: pre_reading_question,
     * during_reading_question, during_reading_practice_deep,
     * during_reading_practice_review). 연습읽기 AI 검사 유형만 기록한다.
     */
    @Column(name = "activity_type", nullable = false, length = 60)
    private String activityType;

    /*
     * AiFeedbackRequest.stepType 값(질문 유형/단계). 화면에 따라 null일 수
     * 있다. activityType과 함께 "같은 질문(슬롯)"을 구분하는 키로 쓴다.
     */
    @Column(name = "question_type", length = 30)
    private String questionType;

    /*
     * AI가 실제로 반환한 판정 결과: good 또는 need.
     */
    @Column(name = "status", nullable = false, length = 10)
    private String status;

    /*
     * 동일 질문(같은 학생이 같은 질문을 고쳐 쓰며 반복 요청한 재시도들)을
     * 하나로 묶는 키. 인증된 학생 전용 엔드포인트
     * (/api/students/me/feedback/ai-review)를 통해서만 채워진다 - 공개
     * /api/feedback/ai-review 경로는 애초에 시도 기록 자체를 남기지 않는다.
     * null이면(과거 방식이거나 evaluationKey를 안 보낸 요청) 질문 단위로
     * 묶을 수 없으므로 이해도 계산에서 완전히 제외한다(임의로 묶지 않음).
     */
    @Column(name = "evaluation_key", length = 191)
    private String evaluationKey;

    /*
     * 같은 student_id + evaluation_key 조합에서 "정상 평가가 몇 번째
     * 시도였는지"를 백엔드가 계산해서 채운다(클라이언트 값은 신뢰하지
     * 않음). 1부터 시작. null이면 evaluationKey도 없는 레코드라는 뜻이다.
     */
    @Column(name = "attempt_number")
    private Integer attemptNumber;

    /*
     * 이 평가가 어느 학급의 온책읽기 책에 대한 것인지. 인증 학생 API가
     * 요청의 classReadingBookId를 검증(학생이 그 학급 책에 실제로 속하는지)한
     * 뒤 그대로 저장한다. PracticeAchievementService가 이해도를 계산할 때
     * studentId만이 아니라 이 값으로도 함께 걸러서, 학생이 나중에 다른
     * 학급(다른 책)에 들어가더라도 이전 책의 평가와 섞이지 않게 한다.
     */
    @Column(name = "class_reading_book_id")
    private Long classReadingBookId;

    /*
     * 개별읽기 전용. 이 평가가 어느 ReadingRecord(개별읽기 등록 책)에 대한
     * 것인지. 개별읽기는 온책읽기와 달리 ClassReadingBook이 없으므로
     * classReadingBookId 대신 이 컬럼으로 범위를 구분한다. 두 컬럼은
     * 동시에 값이 들어가지 않는다(온책읽기 요청은 classReadingBookId만,
     * 개별읽기 요청은 readingRecordId만 채워서 보낸다).
     */
    @Column(name = "reading_record_id")
    private Long readingRecordId;

    @Column(name = "evaluated_at", nullable = false, updatable = false)
    private LocalDateTime evaluatedAt;

    @PrePersist
    protected void onCreate() {
        this.evaluatedAt = LocalDateTime.now();
    }
}
