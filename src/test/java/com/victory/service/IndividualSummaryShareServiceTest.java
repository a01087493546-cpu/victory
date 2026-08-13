package com.victory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.IndividualSummaryLikeResponse;
import com.victory.dto.IndividualSummaryShareItem;
import com.victory.entity.Book;
import com.victory.entity.ClassStudent;
import com.victory.entity.ContentLike;
import com.victory.entity.ReadingRecord;
import com.victory.entity.SchoolClass;
import com.victory.entity.Summary;
import com.victory.entity.User;
import com.victory.repository.ClassStudentRepository;
import com.victory.repository.ContentLikeRepository;
import com.victory.repository.ResponseRepository;
import com.victory.repository.SchoolClassRepository;
import com.victory.repository.SummaryRepository;
import com.victory.repository.UserRepository;

class IndividualSummaryShareServiceTest {

    private static final Long CLASS_A_ID = 10L;
    private static final Long CLASS_B_ID = 20L;
    private static final Long TEACHER_A_ID = 1L;
    private static final Long STUDENT_1_ID = 100L; // 학급 A
    private static final Long STUDENT_2_ID = 200L; // 학급 A
    private static final Long STUDENT_3_ID = 300L; // 학급 B(다른 학급)

    private final SummaryRepository summaryRepository = mock(SummaryRepository.class);
    private final ContentLikeRepository contentLikeRepository = mock(ContentLikeRepository.class);
    private final ClassStudentRepository classStudentRepository = mock(ClassStudentRepository.class);
    private final ResponseRepository responseRepository = mock(ResponseRepository.class);
    private final SchoolClassRepository schoolClassRepository = mock(SchoolClassRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);

    private final IndividualSummaryShareService service = new IndividualSummaryShareService(
        summaryRepository,
        contentLikeRepository,
        classStudentRepository,
        responseRepository,
        schoolClassRepository,
        userRepository);

    private User buildUser(Long id, String name, String role) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setRole(role);
        return user;
    }

    private SchoolClass buildClass(Long id, User teacher) {
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(id);
        schoolClass.setTeacher(teacher);
        return schoolClass;
    }

    private ClassStudent buildClassStudent(Long id, SchoolClass schoolClass, User student) {
        ClassStudent classStudent = new ClassStudent();
        classStudent.setId(id);
        classStudent.setSchoolClass(schoolClass);
        classStudent.setStudent(student);
        return classStudent;
    }

    private Summary buildSharedSummary(
            Long id, User student, Long readingRecordId, String bookType, String text, LocalDateTime createdAt) {

        Book book = new Book();
        book.setId(readingRecordId + 1000);
        book.setTitle("책" + readingRecordId);

        ReadingRecord record = new ReadingRecord();
        record.setId(readingRecordId);
        record.setStudent(student);
        record.setBook(book);

        Summary summary = new Summary();
        summary.setId(id);
        summary.setStudent(student);
        summary.setReadingRecord(record);
        summary.setBookType(bookType);
        summary.setSummaryText(text);
        summary.setAiPassed(true);
        summary.setStatus("approved");
        summary.setCreatedAt(createdAt);
        summary.setUpdatedAt(createdAt);
        return summary;
    }

    /* 검증 1/3: 같은 학급 학생들의(완독 여부 무관) 공유된 간추리기를 조회한다 */
    @Test
    void getClassSummariesForStudent_returnsSameClassSharedSummaries() {
        User me = buildUser(STUDENT_1_ID, "학생1", "student");
        User classmate = buildUser(STUDENT_2_ID, "학생2", "student");
        SchoolClass classA = buildClass(CLASS_A_ID, buildUser(TEACHER_A_ID, "선생님", "teacher"));
        ClassStudent myMembership = buildClassStudent(1L, classA, me);

        when(classStudentRepository.findByStudentId(STUDENT_1_ID)).thenReturn(Optional.of(myMembership));
        when(classStudentRepository.findBySchoolClassId(CLASS_A_ID)).thenReturn(List.of(
            myMembership, buildClassStudent(2L, classA, classmate)));

        Summary myFinishedSummary = buildSharedSummary(
            500L, me, 8L, "story", "완독한 책의 간추리기", LocalDateTime.of(2026, 7, 28, 10, 0));
        Summary classmateSummary = buildSharedSummary(
            501L, classmate, 11L, "info", "친구의 간추리기", LocalDateTime.of(2026, 7, 27, 10, 0));

        when(summaryRepository
            .findByStudent_IdAndReadingRecordIsNotNullAndAiPassedTrueAndStatusOrderByCreatedAtDesc(
                STUDENT_1_ID, "approved"))
            .thenReturn(List.of(myFinishedSummary));
        when(responseRepository
            .findByStudent_IdAndReadingRecord_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                eq(STUDENT_1_ID), eq(8L), eq("individual"), eq("answer"), eq("after")))
            .thenReturn(List.of(
                buildAfterResponse(1, true, "답1"),
                buildAfterResponse(2, true, "답2"),
                buildAfterResponse(3, true, "답3")));

        when(summaryRepository.findSharedIndividualSummariesByStudentIdsAndCreatedAtBetween(
                anyList(), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(List.of(myFinishedSummary, classmateSummary));
        when(contentLikeRepository.countByContentTypeAndContentId(eq("individual_summary"), any()))
            .thenReturn(0L);
        when(contentLikeRepository.findByStudent_IdAndContentTypeAndContentIdIn(eq(STUDENT_1_ID), eq("individual_summary"), anyList()))
            .thenReturn(List.of());

        List<IndividualSummaryShareItem> result = service.getClassSummariesForStudent(
            STUDENT_1_ID, LocalDate.of(2026, 7, 28));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).isMine()).isTrue();
        assertThat(result.get(1).isMine()).isFalse();
        assertThat(result.get(1).getStudentName()).isEqualTo("학생2");
    }

    /* 검증 2: 다른 학급 학생 목록은 조회 대상에 아예 포함되지 않는다(studentIds 자체가 같은 학급으로만 구성됨) */
    @Test
    void getClassSummariesForStudent_neverIncludesOtherClassStudentIds() {
        User me = buildUser(STUDENT_1_ID, "학생1", "student");
        SchoolClass classA = buildClass(CLASS_A_ID, buildUser(TEACHER_A_ID, "선생님", "teacher"));
        ClassStudent myMembership = buildClassStudent(1L, classA, me);

        when(classStudentRepository.findByStudentId(STUDENT_1_ID)).thenReturn(Optional.of(myMembership));
        when(classStudentRepository.findBySchoolClassId(CLASS_A_ID)).thenReturn(List.of(myMembership));
        Summary mySummary = buildSharedSummary(
            500L, me, 8L, "story", "내 간추리기", LocalDateTime.of(2026, 7, 28, 10, 0));

        when(summaryRepository
            .findByStudent_IdAndReadingRecordIsNotNullAndAiPassedTrueAndStatusOrderByCreatedAtDesc(
                STUDENT_1_ID, "approved"))
            .thenReturn(List.of(mySummary));
        when(responseRepository
            .findByStudent_IdAndReadingRecord_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                eq(STUDENT_1_ID), eq(8L), eq("individual"), eq("answer"), eq("after")))
            .thenReturn(List.of(
                buildAfterResponse(1, true, "답1"),
                buildAfterResponse(2, true, "답2"),
                buildAfterResponse(3, true, "답3")));
        when(summaryRepository.findSharedIndividualSummariesByStudentIdsAndCreatedAtBetween(
                eq(List.of(STUDENT_1_ID)), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(List.of());

        service.getClassSummariesForStudent(STUDENT_1_ID, LocalDate.of(2026, 7, 28));

        verify(summaryRepository).findSharedIndividualSummariesByStudentIdsAndCreatedAtBetween(
            eq(List.of(STUDENT_1_ID)), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(summaryRepository, never()).findSharedIndividualSummariesByStudentIdsAndCreatedAtBetween(
            eq(List.of(STUDENT_3_ID)), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    /* 검증 13: 교사는 담당 학급 학생들의 간추리기만 조회한다 */
    @Test
    void getClassSummariesForTeacher_returnsOnlyOwnClassStudents() {
        User teacher = buildUser(TEACHER_A_ID, "선생님", "teacher");
        User student1 = buildUser(STUDENT_1_ID, "학생1", "student");
        User student2 = buildUser(STUDENT_2_ID, "학생2", "student");
        SchoolClass classA = buildClass(CLASS_A_ID, teacher);

        when(userRepository.findById(TEACHER_A_ID)).thenReturn(Optional.of(teacher));
        when(schoolClassRepository.findByTeacherId(TEACHER_A_ID)).thenReturn(Optional.of(classA));
        when(classStudentRepository.findBySchoolClassId(CLASS_A_ID)).thenReturn(List.of(
            buildClassStudent(1L, classA, student1), buildClassStudent(2L, classA, student2)));

        Summary s1 = buildSharedSummary(500L, student1, 8L, "story", "학생1 간추리기", LocalDateTime.now());
        when(summaryRepository.findSharedIndividualSummariesByStudentIdsAndCreatedAtBetween(
                eq(List.of(STUDENT_1_ID, STUDENT_2_ID)), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(List.of(s1));
        when(contentLikeRepository.countByContentTypeAndContentId(eq("individual_summary"), any())).thenReturn(2L);
        when(contentLikeRepository.findByStudent_IdAndContentTypeAndContentIdIn(
                eq(TEACHER_A_ID), eq("individual_summary"), anyList()))
            .thenReturn(List.of());

        List<IndividualSummaryShareItem> result = service.getClassSummariesForTeacher(
            TEACHER_A_ID, LocalDate.now());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isMine()).isFalse();
        assertThat(result.get(0).getLikeCount()).isEqualTo(2L);
        // 다른 사용자의 좋아요 2개는 총수에만 반영되고 교사 자신의 활성 상태는 false다.
        assertThat(result.get(0).isLikedByMe()).isFalse();
        verify(contentLikeRepository).findByStudent_IdAndContentTypeAndContentIdIn(
            eq(TEACHER_A_ID), eq("individual_summary"), anyList());
        verify(summaryRepository).findSharedIndividualSummariesByStudentIdsAndCreatedAtBetween(
            eq(List.of(STUDENT_1_ID, STUDENT_2_ID)), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    /* 교사가 승인된 간추리기를 다시 대기(PENDING)로 되돌린다 - 좋아요는 건드리지 않는다 */
    @Test
    void returnToPending_fromApproved_clearsReasonAndPreservesLikes() {
        User teacher = buildUser(TEACHER_A_ID, "선생님", "teacher");
        User owner = buildUser(STUDENT_1_ID, "학생1", "student");
        SchoolClass classA = buildClass(CLASS_A_ID, teacher);

        when(userRepository.findById(TEACHER_A_ID)).thenReturn(Optional.of(teacher));
        when(schoolClassRepository.findByTeacherId(TEACHER_A_ID)).thenReturn(Optional.of(classA));
        when(classStudentRepository.findByStudentId(STUDENT_1_ID))
            .thenReturn(Optional.of(buildClassStudent(1L, classA, owner)));

        Summary summary = buildSharedSummary(500L, owner, 8L, "story", "간추리기", LocalDateTime.now());
        summary.setStatus("approved");
        when(summaryRepository.findById(500L)).thenReturn(Optional.of(summary));
        when(summaryRepository.save(any(Summary.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(contentLikeRepository.countByContentTypeAndContentId(eq("individual_summary"), any())).thenReturn(2L);
        when(contentLikeRepository.findByStudent_IdAndContentTypeAndContentId(eq(TEACHER_A_ID), eq("individual_summary"), any()))
            .thenReturn(Optional.empty());

        IndividualSummaryShareItem result = service.returnToPending(TEACHER_A_ID, 500L);

        assertThat(result.getStatus()).isEqualTo("pending");
        assertThat(summary.getStatus()).isEqualTo("pending");
        assertThat(summary.getRejectionReason()).isNull();
        assertThat(result.getLikeCount()).isEqualTo(2L);
        verify(contentLikeRepository, never()).delete(any());
    }

    /* 교사가 거절된 간추리기를 대기(PENDING)로 되돌리면 거절 사유가 지워진다 */
    @Test
    void returnToPending_fromRejected_clearsRejectionReason() {
        User teacher = buildUser(TEACHER_A_ID, "선생님", "teacher");
        User owner = buildUser(STUDENT_1_ID, "학생1", "student");
        SchoolClass classA = buildClass(CLASS_A_ID, teacher);

        when(userRepository.findById(TEACHER_A_ID)).thenReturn(Optional.of(teacher));
        when(schoolClassRepository.findByTeacherId(TEACHER_A_ID)).thenReturn(Optional.of(classA));
        when(classStudentRepository.findByStudentId(STUDENT_1_ID))
            .thenReturn(Optional.of(buildClassStudent(1L, classA, owner)));

        Summary summary = buildSharedSummary(500L, owner, 8L, "story", "간추리기", LocalDateTime.now());
        summary.setStatus("rejected");
        summary.setRejectionReason("이전 거절 사유");
        when(summaryRepository.findById(500L)).thenReturn(Optional.of(summary));
        when(summaryRepository.save(any(Summary.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(contentLikeRepository.countByContentTypeAndContentId(eq("individual_summary"), any())).thenReturn(0L);
        when(contentLikeRepository.findByStudent_IdAndContentTypeAndContentId(eq(TEACHER_A_ID), eq("individual_summary"), any()))
            .thenReturn(Optional.empty());

        IndividualSummaryShareItem result = service.returnToPending(TEACHER_A_ID, 500L);

        assertThat(result.getStatus()).isEqualTo("pending");
        assertThat(summary.getRejectionReason()).isNull();
    }

    /* 검증 5/6/7: 좋아요 생성 -> 같은 사용자가 다시 누르면 취소 */
    @Test
    void toggleLikeAsStudent_createsThenCancelsOnSecondCall() {
        User me = buildUser(STUDENT_1_ID, "학생1", "student");
        User owner = buildUser(STUDENT_2_ID, "학생2", "student");
        SchoolClass classA = buildClass(CLASS_A_ID, buildUser(TEACHER_A_ID, "선생님", "teacher"));

        when(classStudentRepository.findByStudentId(STUDENT_1_ID))
            .thenReturn(Optional.of(buildClassStudent(1L, classA, me)));
        when(classStudentRepository.findByStudentId(STUDENT_2_ID))
            .thenReturn(Optional.of(buildClassStudent(2L, classA, owner)));
        when(userRepository.findById(STUDENT_1_ID)).thenReturn(Optional.of(me));

        Summary summary = buildSharedSummary(500L, owner, 11L, "story", "친구 간추리기", LocalDateTime.now());
        when(summaryRepository.findById(500L)).thenReturn(Optional.of(summary));

        // 1차 호출: 좋아요 없음 -> 생성
        when(contentLikeRepository.findByStudent_IdAndContentTypeAndContentId(STUDENT_1_ID, "individual_summary", 500L))
            .thenReturn(Optional.empty());
        when(contentLikeRepository.countByContentTypeAndContentId("individual_summary", 500L)).thenReturn(1L);

        IndividualSummaryLikeResponse firstResult = service.toggleLikeAsStudent(STUDENT_1_ID, 500L);

        assertThat(firstResult.isLiked()).isTrue();
        assertThat(firstResult.getLikeCount()).isEqualTo(1L);
        verify(contentLikeRepository).save(any(ContentLike.class));

        // 2차 호출: 이미 좋아요 있음 -> 취소
        ContentLike existingLike = new ContentLike();
        existingLike.setId(900L);
        when(contentLikeRepository.findByStudent_IdAndContentTypeAndContentId(STUDENT_1_ID, "individual_summary", 500L))
            .thenReturn(Optional.of(existingLike));
        when(contentLikeRepository.countByContentTypeAndContentId("individual_summary", 500L)).thenReturn(0L);

        IndividualSummaryLikeResponse secondResult = service.toggleLikeAsStudent(STUDENT_1_ID, 500L);

        assertThat(secondResult.isLiked()).isFalse();
        assertThat(secondResult.getLikeCount()).isEqualTo(0L);
        verify(contentLikeRepository).delete(existingLike);
    }

    /* 검증 9: 교사 좋아요도 같은 content_likes로 집계되어 학생 화면 likeCount에 그대로 반영된다 */
    @Test
    void toggleLikeAsTeacher_sharesSameLikeCountAsStudentView() {
        User teacher = buildUser(TEACHER_A_ID, "선생님", "teacher");
        User owner = buildUser(STUDENT_1_ID, "학생1", "student");
        SchoolClass classA = buildClass(CLASS_A_ID, teacher);

        when(userRepository.findById(TEACHER_A_ID)).thenReturn(Optional.of(teacher));
        when(schoolClassRepository.findByTeacherId(TEACHER_A_ID)).thenReturn(Optional.of(classA));
        when(classStudentRepository.findByStudentId(STUDENT_1_ID))
            .thenReturn(Optional.of(buildClassStudent(1L, classA, owner)));

        Summary summary = buildSharedSummary(500L, owner, 8L, "story", "간추리기", LocalDateTime.now());
        when(summaryRepository.findById(500L)).thenReturn(Optional.of(summary));
        when(contentLikeRepository.findByStudent_IdAndContentTypeAndContentId(TEACHER_A_ID, "individual_summary", 500L))
            .thenReturn(Optional.empty());
        when(contentLikeRepository.countByContentTypeAndContentId("individual_summary", 500L)).thenReturn(1L);

        IndividualSummaryLikeResponse result = service.toggleLikeAsTeacher(TEACHER_A_ID, 500L);

        assertThat(result.isLiked()).isTrue();
        assertThat(result.getLikeCount()).isEqualTo(1L);
        verify(contentLikeRepository).findByStudent_IdAndContentTypeAndContentId(
            TEACHER_A_ID, "individual_summary", 500L);
        verify(contentLikeRepository, never()).delete(any(ContentLike.class));

        org.mockito.ArgumentCaptor<ContentLike> captor = org.mockito.ArgumentCaptor.forClass(ContentLike.class);
        verify(contentLikeRepository).save(captor.capture());
        assertThat(captor.getValue().getStudent().getId()).isEqualTo(TEACHER_A_ID);
        assertThat(captor.getValue().getContentType()).isEqualTo("individual_summary");
    }

    /* 검증 11: 다른 학급 학생이 좋아요를 시도하면 403 */
    @Test
    void toggleLikeAsStudent_throwsForbiddenForDifferentClass() {
        User outsider = buildUser(STUDENT_3_ID, "학생3", "student");
        User owner = buildUser(STUDENT_1_ID, "학생1", "student");
        SchoolClass classA = buildClass(CLASS_A_ID, buildUser(TEACHER_A_ID, "선생님", "teacher"));
        SchoolClass classB = buildClass(CLASS_B_ID, buildUser(2L, "다른선생님", "teacher"));

        when(classStudentRepository.findByStudentId(STUDENT_3_ID))
            .thenReturn(Optional.of(buildClassStudent(3L, classB, outsider)));
        when(classStudentRepository.findByStudentId(STUDENT_1_ID))
            .thenReturn(Optional.of(buildClassStudent(1L, classA, owner)));

        Summary summary = buildSharedSummary(500L, owner, 8L, "story", "간추리기", LocalDateTime.now());
        when(summaryRepository.findById(500L)).thenReturn(Optional.of(summary));

        assertThatThrownBy(() -> service.toggleLikeAsStudent(STUDENT_3_ID, 500L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("403");

        verify(contentLikeRepository, never()).save(any(ContentLike.class));
    }

    /* 검증 12: 존재하지 않는 summaryId는 404 */
    @Test
    void toggleLikeAsStudent_throwsNotFoundForMissingSummary() {
        User me = buildUser(STUDENT_1_ID, "학생1", "student");
        SchoolClass classA = buildClass(CLASS_A_ID, buildUser(TEACHER_A_ID, "선생님", "teacher"));

        when(classStudentRepository.findByStudentId(STUDENT_1_ID))
            .thenReturn(Optional.of(buildClassStudent(1L, classA, me)));
        when(summaryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.toggleLikeAsStudent(STUDENT_1_ID, 999L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("404");
    }

    /* 검증: 공유 조건(AI 통과 + 승인)을 충족하지 못한 간추리기는 좋아요 대상에서도 제외되어 404로 취급된다 */
    @Test
    void toggleLikeAsStudent_throwsNotFoundWhenSummaryNotShared() {
        User me = buildUser(STUDENT_1_ID, "학생1", "student");
        User owner = buildUser(STUDENT_2_ID, "학생2", "student");
        SchoolClass classA = buildClass(CLASS_A_ID, buildUser(TEACHER_A_ID, "선생님", "teacher"));

        when(classStudentRepository.findByStudentId(STUDENT_1_ID))
            .thenReturn(Optional.of(buildClassStudent(1L, classA, me)));

        Summary notPassed = buildSharedSummary(500L, owner, 11L, "story", "아직 통과 못함", LocalDateTime.now());
        notPassed.setAiPassed(false);
        when(summaryRepository.findById(500L)).thenReturn(Optional.of(notPassed));

        assertThatThrownBy(() -> service.toggleLikeAsStudent(STUDENT_1_ID, 500L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("404");
    }

    /* 검증 10: likedByMe는 조회하는 사용자 기준으로만 계산된다(다른 학생이 좋아요했어도 나는 false) */
    @Test
    void getClassSummariesForStudent_likedByMeReflectsOnlyViewer() {
        User me = buildUser(STUDENT_1_ID, "학생1", "student");
        User other = buildUser(STUDENT_2_ID, "학생2", "student");
        SchoolClass classA = buildClass(CLASS_A_ID, buildUser(TEACHER_A_ID, "선생님", "teacher"));
        ClassStudent myMembership = buildClassStudent(1L, classA, me);

        when(classStudentRepository.findByStudentId(STUDENT_1_ID)).thenReturn(Optional.of(myMembership));
        when(classStudentRepository.findBySchoolClassId(CLASS_A_ID))
            .thenReturn(List.of(myMembership, buildClassStudent(2L, classA, other)));

        Summary summary = buildSharedSummary(500L, other, 11L, "story", "친구 간추리기", LocalDateTime.now());
        Summary mySummary = buildSharedSummary(501L, me, 12L, "story", "내 간추리기", LocalDateTime.now());
        when(summaryRepository
            .findByStudent_IdAndReadingRecordIsNotNullAndAiPassedTrueAndStatusOrderByCreatedAtDesc(
                STUDENT_1_ID, "approved"))
            .thenReturn(List.of(mySummary));
        when(responseRepository
            .findByStudent_IdAndReadingRecord_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                eq(STUDENT_1_ID), eq(12L), eq("individual"), eq("answer"), eq("after")))
            .thenReturn(List.of(
                buildAfterResponse(1, true, "답1"),
                buildAfterResponse(2, true, "답2"),
                buildAfterResponse(3, true, "답3")));
        when(summaryRepository.findSharedIndividualSummariesByStudentIdsAndCreatedAtBetween(
                anyList(), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(List.of(summary));
        when(contentLikeRepository.countByContentTypeAndContentId("individual_summary", 500L)).thenReturn(3L);
        // 다른 학생(200L)이 좋아요를 눌렀더라도, 조회자(100L) 기준으로는 조회하지 않으므로 false여야 한다
        when(contentLikeRepository.findByStudent_IdAndContentTypeAndContentIdIn(eq(STUDENT_1_ID), eq("individual_summary"), anyList()))
            .thenReturn(List.of());

        List<IndividualSummaryShareItem> result = service.getClassSummariesForStudent(
            STUDENT_1_ID, LocalDate.now());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isLikedByMe()).isFalse();
        assertThat(result.get(0).getLikeCount()).isEqualTo(3L);
        verify(contentLikeRepository, times(1))
            .findByStudent_IdAndContentTypeAndContentIdIn(eq(STUDENT_1_ID), eq("individual_summary"), anyList());
    }

    private com.victory.entity.Response buildAfterResponse(Integer questionIndex, boolean passed, String content) {
        com.victory.entity.Response response = new com.victory.entity.Response();
        response.setContent(content);
        response.setPassed(passed);
        response.setExtraData(java.util.Map.of("questionIndex", questionIndex));
        return response;
    }
}
