package com.victory.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
 * 질문-답변 + 학급/개별 책수다방 콘텐츠(글/댓글/대댓글/심화질문) 전체를 통합한 테이블.
 * mode(class/individual)와 content_type(answer/thought/chat_post/deep_question/reply)의
 * 조합으로 어떤 화면의 어떤 글인지 구분한다.
 *
 * class_id 컬럼은 일부러 두지 않는다 — mode='class' 콘텐츠는 book_id를 거쳐 학급을
 * 알아낼 수 있으므로 book_id 경유 조회로 충분하다는 결론(dashboard-metrics-audit.md §확인 완료).
 */
@Entity
@Table(
    name = "responses",
    indexes = {
        @Index(name = "idx_responses_student_id", columnList = "student_id"),
        @Index(name = "idx_responses_book_id", columnList = "book_id"),
        @Index(name = "idx_responses_parent_id", columnList = "parent_id"),
        @Index(name = "idx_responses_status", columnList = "status"),
        @Index(name = "idx_responses_created_at", columnList = "created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Response {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id")
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reading_record_id")
    private ReadingRecord readingRecord;

    /*
     * 대댓글/답글의 원글. 같은 테이블을 스스로 참조한다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Response parent;

    /*
     * class(학급형) / individual(개별형)
     */
    @Column(name = "mode", nullable = false, length = 20)
    private String mode;

    /*
     * answer(질문답변) / thought(생각나누기) / chat_post(책수다방 글) /
     * deep_question(심화질문) / reply(답글)
     */
    @Column(name = "content_type", nullable = false, length = 30)
    private String contentType;

    /*
     * before / during / after
     */
    @Column(name = "stage", length = 20)
    private String stage;

    /*
     * 개별읽기 읽기 중(책속 생각쓰기)처럼 완독 전까지 날짜별로 반복하는
     * 활동의 "그 활동을 한 날짜"(Asia/Seoul 기준, 서버가 결정). 읽기 전처럼
     * 한 번만 쓰는 활동에는 null로 둔다. extra_data JSON에 숨기지 않고
     * 명시 컬럼으로 둬서 날짜 조회·유니크 판단을 DB 레벨에서 안정적으로
     * 할 수 있게 한다(IndividualReadingSchemaInitializer가 멱등하게 추가).
     */
    @Column(name = "activity_date")
    private LocalDate activityDate;

    @Lob
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /*
     * AI(FeedbackAiController) 통과 판정 결과.
     * 학생이 여러 번 고쳐 쓸 수 있는 화면이라도, 이 컬럼은 "최초 1회 판정 결과"만 기록하는
     * 규칙으로 쓴다(대시보드 이해도 집계가 매번 최신값이 아니라 최초 시도 기준이 되도록).
     * 즉 이미 값이 채워진 뒤에는 애플리케이션 레벨에서 덮어쓰지 않는다.
     */
    @Column(name = "passed")
    private Boolean passed;

    /*
     * pending / approved / rejected.
     * 기본값 approved — 질문답변처럼 검수가 필요 없는 콘텐츠는 바로 승인 상태로 시작하고,
     * 책수다방 글(chat_post/reply)만 애플리케이션 로직에서 pending으로 생성한다.
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status = "approved";

    @Lob
    @Column(name = "reject_reason", columnDefinition = "TEXT")
    private String rejectReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    /*
     * 교사가 승인/거절 시 남기는 코멘트(반려 사유와 별개로, 승인할 때도 남길 수 있음).
     */
    @Lob
    @Column(name = "teacher_note", columnDefinition = "TEXT")
    private String teacherNote;

    /*
     * 소프트 삭제. 학생이 "삭제" 버튼을 누르면 실제 DELETE 대신 이 값을 채운다.
     * 조회 쿼리는 deleted_at IS NULL 조건을 항상 포함해야 한다.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /*
     * 개별읽기 책수다방 첨부 이미지(base64). 별도 파일 업로드 서버가 생기기 전까지 임시로 사용.
     */
    @Lob
    @Column(name = "image_data", columnDefinition = "LONGTEXT")
    private String imageData;

    /*
     * 모드별 추가 필드. 학급형(reactionType/quoteFromBook/reasonText) 또는
     * 개별형 밸런스 게임(scene/optionA/optionB/choice/choiceReason) 형태를 담는다.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extra_data", columnDefinition = "json")
    private Map<String, Object> extraData;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
