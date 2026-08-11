package com.victory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.BookRecommendationClassWallResponse;
import com.victory.dto.BookRecommendationCompletedBookItem;
import com.victory.dto.BookRecommendationCreateRequest;
import com.victory.dto.BookRecommendationItem;
import com.victory.dto.BookRecommendationLikeResponse;
import com.victory.dto.BookRecommendationQuestionItem;
import com.victory.entity.Book;
import com.victory.entity.BookRecommendation;
import com.victory.entity.ClassStudent;
import com.victory.entity.ContentLike;
import com.victory.entity.ReadingRecord;
import com.victory.entity.Response;
import com.victory.entity.SchoolClass;
import com.victory.entity.StudentStats;
import com.victory.entity.User;
import com.victory.repository.BookRecommendationRepository;
import com.victory.repository.ClassStudentRepository;
import com.victory.repository.ContentLikeRepository;
import com.victory.repository.ReadingRecordRepository;
import com.victory.repository.ResponseRepository;
import com.victory.repository.SchoolClassRepository;
import com.victory.repository.UserRepository;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class BookRecommendationServiceTest {

    private static final Long CLASS_A_ID = 10L;
    private static final Long CLASS_B_ID = 20L;
    private static final Long TEACHER_A_ID = 1L;
    private static final Long STUDENT_1_ID = 100L; // 학급 A
    private static final Long STUDENT_2_ID = 200L; // 학급 A
    private static final Long STUDENT_3_ID = 300L; // 학급 B(다른 학급)

    private final BookRecommendationRepository bookRecommendationRepository =
        mock(BookRecommendationRepository.class);
    private final BookRecommendationRewardService rewardService = mock(BookRecommendationRewardService.class);
    private final ContentLikeRepository contentLikeRepository = mock(ContentLikeRepository.class);
    private final ClassStudentRepository classStudentRepository = mock(ClassStudentRepository.class);
    private final ReadingRecordRepository readingRecordRepository = mock(ReadingRecordRepository.class);
    private final ResponseRepository responseRepository = mock(ResponseRepository.class);
    private final SchoolClassRepository schoolClassRepository = mock(SchoolClassRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final IndividualAchievementService individualAchievementService = mock(IndividualAchievementService.class);

    private final BookRecommendationService service = new BookRecommendationService(
        bookRecommendationRepository,
        rewardService,
        contentLikeRepository,
        classStudentRepository,
        readingRecordRepository,
        responseRepository,
        schoolClassRepository,
        userRepository,
        individualAchievementService);

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

    private BookRecommendation buildRecommendation(
            Long id, User student, String title, String author, String reason, LocalDateTime createdAt) {
        BookRecommendation recommendation = new BookRecommendation();
        recommendation.setId(id);
        recommendation.setStudent(student);
        recommendation.setTitle(title);
        recommendation.setAuthor(author);
        recommendation.setReason(reason);
        recommendation.setCreatedAt(createdAt);
        return recommendation;
    }

    private BookRecommendationCreateRequest buildCreateRequest(String title, String author, String reason) {
        return buildCreateRequest(800L, title, author, reason, List.of(700L));
    }

    private BookRecommendationCreateRequest buildCreateRequest(
            String title,
            String author,
            String reason,
            List<Long> teaserResponseIds) {
        return buildCreateRequest(800L, title, author, reason, teaserResponseIds);
    }

    private BookRecommendationCreateRequest buildCreateRequest(
            Long readingRecordId,
            String title,
            String author,
            String reason,
            List<Long> teaserResponseIds) {
        BookRecommendationCreateRequest request = new BookRecommendationCreateRequest();
        try {
            java.lang.reflect.Field readingRecordIdField =
                BookRecommendationCreateRequest.class.getDeclaredField("readingRecordId");
            readingRecordIdField.setAccessible(true);
            readingRecordIdField.set(request, readingRecordId);
            java.lang.reflect.Field titleField = BookRecommendationCreateRequest.class.getDeclaredField("title");
            titleField.setAccessible(true);
            titleField.set(request, title);
            java.lang.reflect.Field authorField = BookRecommendationCreateRequest.class.getDeclaredField("author");
            authorField.setAccessible(true);
            authorField.set(request, author);
            java.lang.reflect.Field reasonField = BookRecommendationCreateRequest.class.getDeclaredField("reason");
            reasonField.setAccessible(true);
            reasonField.set(request, reason);
            java.lang.reflect.Field teaserField =
                BookRecommendationCreateRequest.class.getDeclaredField("teaserResponseIds");
            teaserField.setAccessible(true);
            teaserField.set(request, teaserResponseIds);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return request;
    }

    private Book buildBook(Long id, String title, String author) {
        Book book = new Book();
        book.setId(id);
        book.setTitle(title);
        book.setAuthor(author);
        return book;
    }

    private ReadingRecord buildReadingRecord(Long id, User student, Book book, LocalDateTime finishedAt) {
        ReadingRecord record = new ReadingRecord();
        record.setId(id);
        record.setStudent(student);
        record.setBook(book);
        record.setFinishedAt(finishedAt);
        return record;
    }

    private Response buildTeaserResponse(
            Long id, User student, ReadingRecord readingRecord, String stage, String question, boolean passed) {
        Response response = new Response();
        response.setId(id);
        response.setStudent(student);
        response.setReadingRecord(readingRecord);
        response.setMode("individual");
        response.setContentType("answer");
        response.setStage(stage);
        response.setContent("답변");
        response.setPassed(passed);
        response.setExtraData(java.util.Map.of("question", question));
        return response;
    }

    private Response buildTeaserResponse(Long id, User student, ReadingRecord readingRecord, String question) {
        return buildTeaserResponse(id, student, readingRecord, "after", question, true);
    }

    private ReadingRecord stubCompletedReadingRecord(User student) {
        ReadingRecord record = buildReadingRecord(
            800L,
            student,
            buildBook(3000L, "긴긴밤", "루리"),
            LocalDateTime.of(2026, 7, 28, 20, 0));
        when(readingRecordRepository.findById(800L)).thenReturn(Optional.of(record));
        return record;
    }

    private StudentStats buildStats(User student, int magic, int stamina, int wisdom, int courage) {
        StudentStats stats = new StudentStats();
        stats.setStudent(student);
        stats.setMagic(magic);
        stats.setStamina(stamina);
        stats.setWisdom(wisdom);
        stats.setCourage(courage);
        return stats;
    }

    private void stubRecommendationReward(User student, Long readingRecordId, boolean granted, int courage) {
        when(rewardService.grantRecommendationRewardOnce(student, readingRecordId))
            .thenReturn(new BookRecommendationRewardService.RewardResult(
                granted,
                !granted,
                buildStats(student, 8, 8, 8, courage)));
    }

    /* 검증 1: 학생 추천 글 작성 성공 */
    @Test
    void createRecommendation_savesAndReturnsItem() {
        User me = buildUser(STUDENT_1_ID, "학생1", "student");
        SchoolClass classA = buildClass(CLASS_A_ID, buildUser(TEACHER_A_ID, "선생님", "teacher"));

        when(userRepository.findById(STUDENT_1_ID)).thenReturn(Optional.of(me));
        when(classStudentRepository.findByStudentId(STUDENT_1_ID))
            .thenReturn(Optional.of(buildClassStudent(1L, classA, me)));
        ReadingRecord record = stubCompletedReadingRecord(me);
        when(responseRepository.findAllById(List.of(700L)))
            .thenReturn(List.of(buildTeaserResponse(
                700L, me, record, "노든은 왜 코뿔소를 지키려 했을까요?")));
        when(bookRecommendationRepository.save(any(BookRecommendation.class)))
            .thenAnswer(invocation -> {
                BookRecommendation saved = invocation.getArgument(0);
                saved.setId(900L);
                saved.setCreatedAt(LocalDateTime.now());
                return saved;
            });
        stubRecommendationReward(me, 800L, true, 9);
        record.setFinalReadingPracticeScore(50);
        record.setFinalRecordCompletionScore(70);
        when(individualAchievementService.calculateLiveReadingPracticeScore(800L))
            .thenReturn(63.6);

        BookRecommendationCreateRequest request = buildCreateRequest("잘못된 제목", "잘못된 지은이", " 감동적이었어요 ");

        BookRecommendationItem result = service.createRecommendation(STUDENT_1_ID, request);

        assertThat(result.getRecommendationId()).isEqualTo(900L);
        assertThat(result.getStudentId()).isEqualTo(STUDENT_1_ID);
        assertThat(result.getTitle()).isEqualTo("긴긴밤");
        assertThat(result.getAuthor()).isEqualTo("루리");
        assertThat(result.getReason()).isEqualTo("감동적이었어요");
        assertThat(result.getTeaserQuestions()).containsExactly("노든은 왜 코뿔소를 지키려 했을까요?");
        assertThat(result.getRewardGranted()).isTrue();
        assertThat(result.getCourage()).isEqualTo(9);
        assertThat(result.getIsMine()).isTrue();
        assertThat(result.getLikeCount()).isEqualTo(0L);
        assertThat(result.isLikedByMe()).isFalse();
        assertThat(record.getFinalReadingPracticeScore()).isEqualTo(64);
        assertThat(record.getFinalRecordCompletionScore()).isEqualTo(70);

        org.mockito.ArgumentCaptor<BookRecommendation> captor =
            org.mockito.ArgumentCaptor.forClass(BookRecommendation.class);
        verify(bookRecommendationRepository).save(captor.capture());
        assertThat(captor.getValue().getReadingRecord()).isSameAs(record);
    }

    /*
     * 심사계정 브라우저 격리: 같은 ss01을 여러 심사위원이 동시에 쓸 수
     * 있으므로 책추천 글쓰기는 공용 DB에 저장되면 안 된다(좋아요
     * toggleLikeAsStudent/AsTeacher는 이미 같은 가드가 있음).
     */
    @Test
    void createRecommendation_demoAccount_neverPersistsToSharedDb() {
        User demoMe = buildUser(STUDENT_1_ID, "학생1", "student");
        demoMe.setDemoAccount(true);
        when(userRepository.findById(STUDENT_1_ID)).thenReturn(Optional.of(demoMe));

        BookRecommendationCreateRequest request = buildCreateRequest("잘못된 제목", "잘못된 지은이", "감동적이었어요");

        assertThatThrownBy(() -> service.createRecommendation(STUDENT_1_ID, request))
            .isInstanceOf(ResponseStatusException.class);

        verify(bookRecommendationRepository, never()).save(any(BookRecommendation.class));
    }

    @Test
    void createRecommendation_deduplicatesAndStoresSingleTeaserResponseId() {
        User me = buildUser(STUDENT_1_ID, "학생1", "student");
        SchoolClass classA = buildClass(CLASS_A_ID, buildUser(TEACHER_A_ID, "선생님", "teacher"));

        when(userRepository.findById(STUDENT_1_ID)).thenReturn(Optional.of(me));
        when(classStudentRepository.findByStudentId(STUDENT_1_ID))
            .thenReturn(Optional.of(buildClassStudent(1L, classA, me)));
        ReadingRecord record = stubCompletedReadingRecord(me);
        when(responseRepository.findAllById(List.of(700L)))
            .thenReturn(List.of(buildTeaserResponse(700L, me, record, "질문1")));
        when(bookRecommendationRepository.save(any(BookRecommendation.class)))
            .thenAnswer(invocation -> {
                BookRecommendation saved = invocation.getArgument(0);
                saved.setId(900L);
                saved.setCreatedAt(LocalDateTime.now());
                return saved;
            });
        stubRecommendationReward(me, 800L, true, 9);
        record.setFinalRecordCompletionScore(70);
        when(individualAchievementService.calculateLiveReadingPracticeScore(800L))
            .thenReturn(80.0);

        service.createRecommendation(
            STUDENT_1_ID,
            buildCreateRequest("책", "작가", "이유", List.of(700L, 700L)));

        org.mockito.ArgumentCaptor<BookRecommendation> captor =
            org.mockito.ArgumentCaptor.forClass(BookRecommendation.class);
        verify(bookRecommendationRepository).save(captor.capture());
        assertThat(captor.getValue().getTeaserResponseIds()).containsExactly(700L);
        assertThat(record.getFinalReadingPracticeScore()).isEqualTo(80);
        assertThat(record.getFinalRecordCompletionScore()).isEqualTo(70);
        verify(rewardService).grantRecommendationRewardOnce(me, 800L);
    }

    @Test
    void createRecommendation_rejectsEmptyTeaserResponseIds() {
        User me = buildUser(STUDENT_1_ID, "학생1", "student");
        SchoolClass classA = buildClass(CLASS_A_ID, buildUser(TEACHER_A_ID, "선생님", "teacher"));

        when(userRepository.findById(STUDENT_1_ID)).thenReturn(Optional.of(me));
        when(classStudentRepository.findByStudentId(STUDENT_1_ID))
            .thenReturn(Optional.of(buildClassStudent(1L, classA, me)));
        stubCompletedReadingRecord(me);

        assertThatThrownBy(() -> service.createRecommendation(
                STUDENT_1_ID,
                buildCreateRequest("책", "작가", "이유", List.of())))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400");

        verify(bookRecommendationRepository, never()).save(any(BookRecommendation.class));
        verify(rewardService, never()).grantRecommendationRewardOnce(any(User.class), any(Long.class));
    }

    @Test
    void createRecommendation_rejectsMissingTeaserResponseId() {
        User me = buildUser(STUDENT_1_ID, "학생1", "student");
        SchoolClass classA = buildClass(CLASS_A_ID, buildUser(TEACHER_A_ID, "선생님", "teacher"));

        when(userRepository.findById(STUDENT_1_ID)).thenReturn(Optional.of(me));
        when(classStudentRepository.findByStudentId(STUDENT_1_ID))
            .thenReturn(Optional.of(buildClassStudent(1L, classA, me)));
        stubCompletedReadingRecord(me);
        when(responseRepository.findAllById(List.of(999L))).thenReturn(List.of());

        assertThatThrownBy(() -> service.createRecommendation(
                STUDENT_1_ID,
                buildCreateRequest("책", "작가", "이유", List.of(999L))))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("404");

        verify(bookRecommendationRepository, never()).save(any(BookRecommendation.class));
        verify(rewardService, never()).grantRecommendationRewardOnce(any(User.class), any(Long.class));
    }

    @Test
    void createRecommendation_rejectsOtherStudentTeaserResponseId() {
        User me = buildUser(STUDENT_1_ID, "학생1", "student");
        User other = buildUser(STUDENT_2_ID, "학생2", "student");
        SchoolClass classA = buildClass(CLASS_A_ID, buildUser(TEACHER_A_ID, "선생님", "teacher"));

        when(userRepository.findById(STUDENT_1_ID)).thenReturn(Optional.of(me));
        when(classStudentRepository.findByStudentId(STUDENT_1_ID))
            .thenReturn(Optional.of(buildClassStudent(1L, classA, me)));
        ReadingRecord record = stubCompletedReadingRecord(me);
        when(responseRepository.findAllById(List.of(700L)))
            .thenReturn(List.of(buildTeaserResponse(700L, other, record, "친구 질문")));

        assertThatThrownBy(() -> service.createRecommendation(
                STUDENT_1_ID,
                buildCreateRequest("책", "작가", "이유", List.of(700L))))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("403");

        verify(bookRecommendationRepository, never()).save(any(BookRecommendation.class));
        verify(rewardService, never()).grantRecommendationRewardOnce(any(User.class), any(Long.class));
    }

    /* 검증: 추천할 완독 책과 추천 이유는 필수이고, 제목/지은이는 서버가 결정한다 */
    @Test
    void createRequest_rejectsMissingReadingRecordAndBlankReason() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            assertThat(validator.validate(buildCreateRequest(null, "제목", "지은이", "이유", List.of(700L))))
                .isNotEmpty();
            assertThat(validator.validate(buildCreateRequest(800L, "제목", "지은이", "", List.of(700L))))
                .isNotEmpty();
            assertThat(validator.validate(buildCreateRequest(800L, "", "", "이유", List.of(700L))))
                .isEmpty();
        }
    }

    @Test
    void getCompletedBooks_returnsOnlyCompletedBooksSortedLatestFirst() {
        User me = buildUser(STUDENT_1_ID, "학생1", "student");
        ReadingRecord older = buildReadingRecord(
            800L,
            me,
            buildBook(3000L, "긴긴밤", "루리"),
            LocalDateTime.of(2026, 7, 27, 18, 0));
        ReadingRecord newer = buildReadingRecord(
            801L,
            me,
            buildBook(3001L, "완득이", "김려령"),
            LocalDateTime.of(2026, 7, 28, 18, 0));

        when(readingRecordRepository.findByStudent_IdAndFinishedAtIsNotNull(STUDENT_1_ID))
            .thenReturn(List.of(older, newer));

        List<BookRecommendationCompletedBookItem> result = service.getCompletedBooks(STUDENT_1_ID);

        assertThat(result).extracting(BookRecommendationCompletedBookItem::getReadingRecordId)
            .containsExactly(801L, 800L);
        assertThat(result.get(0).getBookTitle()).isEqualTo("완득이");
    }

    @Test
    void getCompletedBookQuestions_rejectsOtherStudentReadingRecord() {
        User other = buildUser(STUDENT_2_ID, "학생2", "student");
        ReadingRecord record = buildReadingRecord(
            800L,
            other,
            buildBook(3000L, "긴긴밤", "루리"),
            LocalDateTime.of(2026, 7, 28, 18, 0));

        when(readingRecordRepository.findById(800L)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.getCompletedBookQuestions(STUDENT_1_ID, 800L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("403");
    }

    @Test
    void getCompletedBookQuestions_returnsOnlySelectedBookQuestions() {
        User me = buildUser(STUDENT_1_ID, "학생1", "student");
        ReadingRecord record = stubCompletedReadingRecord(me);
        Response before = buildTeaserResponse(700L, me, record, "before", "표지를 보고 궁금한 점은?", true);
        before.setExtraData(java.util.Map.of("question", "표지를 보고 궁금한 점은?", "stepType", "title"));
        Response during = buildTeaserResponse(701L, me, record, "during", "인물의 마음은 어땠을까요?", true);
        during.setExtraData(java.util.Map.of("question", "인물의 마음은 어땠을까요?", "questionType", "feel"));
        Response passedAfter = buildTeaserResponse(702L, me, record, "after", "마지막 장면이 궁금한 까닭은?", true);
        Response failedAfter = buildTeaserResponse(703L, me, record, "after", "통과하지 않은 질문", false);

        when(responseRepository.findByStudent_IdAndReadingRecord_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                STUDENT_1_ID, 800L, "individual", "answer", "before"))
            .thenReturn(List.of(before));
        when(responseRepository.findByStudent_IdAndReadingRecord_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                STUDENT_1_ID, 800L, "individual", "answer", "during"))
            .thenReturn(List.of(during));
        when(responseRepository.findByStudent_IdAndReadingRecord_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                STUDENT_1_ID, 800L, "individual", "answer", "after"))
            .thenReturn(List.of(passedAfter, failedAfter));

        List<BookRecommendationQuestionItem> result = service.getCompletedBookQuestions(STUDENT_1_ID, 800L);

        assertThat(result).extracting(BookRecommendationQuestionItem::getResponseId)
            .containsExactly(700L, 701L, 702L);
        assertThat(result).extracting(BookRecommendationQuestionItem::getCategory)
            .containsExactly("before", "during", "after");
        assertThat(result.get(0).getDetailLabel()).isEqualTo("제목 보고 질문 만들기");
        assertThat(result.get(1).getDetailLabel()).isEqualTo("생각·느낌");
    }

    @Test
    void createRecommendation_rejectsTeaserResponseFromDifferentReadingRecord() {
        User me = buildUser(STUDENT_1_ID, "학생1", "student");
        SchoolClass classA = buildClass(CLASS_A_ID, buildUser(TEACHER_A_ID, "선생님", "teacher"));
        ReadingRecord selectedRecord = stubCompletedReadingRecord(me);
        ReadingRecord otherRecord = buildReadingRecord(
            801L,
            me,
            buildBook(3001L, "완득이", "김려령"),
            LocalDateTime.of(2026, 7, 29, 18, 0));

        when(userRepository.findById(STUDENT_1_ID)).thenReturn(Optional.of(me));
        when(classStudentRepository.findByStudentId(STUDENT_1_ID))
            .thenReturn(Optional.of(buildClassStudent(1L, classA, me)));
        when(responseRepository.findAllById(List.of(700L)))
            .thenReturn(List.of(buildTeaserResponse(700L, me, otherRecord, "다른 책 질문")));

        assertThatThrownBy(() -> service.createRecommendation(
                STUDENT_1_ID,
                buildCreateRequest(selectedRecord.getId(), "책", "작가", "이유", List.of(700L))))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400");

        verify(bookRecommendationRepository, never()).save(any(BookRecommendation.class));
    }

    @Test
    void createRecommendation_rejectsUnfinishedReadingRecord() {
        User me = buildUser(STUDENT_1_ID, "학생1", "student");
        SchoolClass classA = buildClass(CLASS_A_ID, buildUser(TEACHER_A_ID, "선생님", "teacher"));
        ReadingRecord record = buildReadingRecord(800L, me, buildBook(3000L, "긴긴밤", "루리"), null);

        when(userRepository.findById(STUDENT_1_ID)).thenReturn(Optional.of(me));
        when(classStudentRepository.findByStudentId(STUDENT_1_ID))
            .thenReturn(Optional.of(buildClassStudent(1L, classA, me)));
        when(readingRecordRepository.findById(800L)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.createRecommendation(
                STUDENT_1_ID,
                buildCreateRequest("책", "작가", "이유", List.of(700L))))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("409");

        verify(bookRecommendationRepository, never()).save(any(BookRecommendation.class));
        verify(individualAchievementService, never()).calculate(any(Long.class));
    }

    /* 다른 학생의 readingRecordId로 추천 글을 쓰려 하면 403이고 저장되지 않는다 */
    @Test
    void createRecommendation_rejectsReadingRecordOwnedByAnotherStudent() {
        User me = buildUser(STUDENT_1_ID, "학생1", "student");
        User other = buildUser(STUDENT_2_ID, "학생2", "student");
        SchoolClass classA = buildClass(CLASS_A_ID, buildUser(TEACHER_A_ID, "선생님", "teacher"));
        ReadingRecord othersRecord = buildReadingRecord(
            800L, other, buildBook(3000L, "긴긴밤", "루리"), LocalDateTime.of(2026, 7, 28, 20, 0));

        when(userRepository.findById(STUDENT_1_ID)).thenReturn(Optional.of(me));
        when(classStudentRepository.findByStudentId(STUDENT_1_ID))
            .thenReturn(Optional.of(buildClassStudent(1L, classA, me)));
        when(readingRecordRepository.findById(800L)).thenReturn(Optional.of(othersRecord));

        assertThatThrownBy(() -> service.createRecommendation(
                STUDENT_1_ID,
                buildCreateRequest("책", "작가", "이유", List.of(700L))))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("403");

        verify(bookRecommendationRepository, never()).save(any(BookRecommendation.class));
    }

    /* 존재하지 않는 readingRecordId로 추천 글을 쓰려 하면 404이고 저장되지 않는다 */
    @Test
    void createRecommendation_rejectsNonExistentReadingRecord() {
        User me = buildUser(STUDENT_1_ID, "학생1", "student");
        SchoolClass classA = buildClass(CLASS_A_ID, buildUser(TEACHER_A_ID, "선생님", "teacher"));

        when(userRepository.findById(STUDENT_1_ID)).thenReturn(Optional.of(me));
        when(classStudentRepository.findByStudentId(STUDENT_1_ID))
            .thenReturn(Optional.of(buildClassStudent(1L, classA, me)));
        when(readingRecordRepository.findById(800L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createRecommendation(
                STUDENT_1_ID,
                buildCreateRequest("책", "작가", "이유", List.of(700L))))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("404");

        verify(bookRecommendationRepository, never()).save(any(BookRecommendation.class));
    }

    @Test
    void createRecommendation_acceptsOneQuestionAndRejectsTwo() {
        User me = buildUser(STUDENT_1_ID, "학생1", "student");
        SchoolClass classA = buildClass(CLASS_A_ID, buildUser(TEACHER_A_ID, "선생님", "teacher"));
        ReadingRecord record = stubCompletedReadingRecord(me);

        when(userRepository.findById(STUDENT_1_ID)).thenReturn(Optional.of(me));
        when(classStudentRepository.findByStudentId(STUDENT_1_ID))
            .thenReturn(Optional.of(buildClassStudent(1L, classA, me)));
        when(bookRecommendationRepository.save(any(BookRecommendation.class)))
            .thenAnswer(invocation -> {
                BookRecommendation saved = invocation.getArgument(0);
                saved.setId(900L);
                saved.setCreatedAt(LocalDateTime.now());
                return saved;
            });
        stubRecommendationReward(me, 800L, true, 9);
        when(individualAchievementService.calculateLiveReadingPracticeScore(800L))
            .thenReturn(90.0);

        when(responseRepository.findAllById(List.of(700L)))
            .thenReturn(List.of(buildTeaserResponse(700L, me, record, "질문1")));
        service.createRecommendation(STUDENT_1_ID, buildCreateRequest("책", "작가", "이유", List.of(700L)));

        assertThatThrownBy(() -> service.createRecommendation(
                STUDENT_1_ID,
                buildCreateRequest("책", "작가", "이유", List.of(700L, 701L))))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400");
    }

    @Test
    void createRecommendation_doesNotGrantRewardWhenRecommendationSaveFails() {
        User me = buildUser(STUDENT_1_ID, "학생1", "student");
        SchoolClass classA = buildClass(CLASS_A_ID, buildUser(TEACHER_A_ID, "선생님", "teacher"));
        ReadingRecord record = stubCompletedReadingRecord(me);

        when(userRepository.findById(STUDENT_1_ID)).thenReturn(Optional.of(me));
        when(classStudentRepository.findByStudentId(STUDENT_1_ID))
            .thenReturn(Optional.of(buildClassStudent(1L, classA, me)));
        when(responseRepository.findAllById(List.of(700L)))
            .thenReturn(List.of(buildTeaserResponse(700L, me, record, "질문1")));
        when(bookRecommendationRepository.save(any(BookRecommendation.class)))
            .thenThrow(new IllegalStateException("save failed"));
        record.setFinalReadingPracticeScore(50);

        assertThatThrownBy(() -> service.createRecommendation(
                STUDENT_1_ID,
                buildCreateRequest("책", "작가", "이유", List.of(700L))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("save failed");

        assertThat(record.getFinalReadingPracticeScore()).isEqualTo(50);
        verify(rewardService, never()).grantRecommendationRewardOnce(any(User.class), any(Long.class));
        verify(individualAchievementService, never()).calculateLiveReadingPracticeScore(any(Long.class));
    }

    @Test
    void createRecommendation_propagatesRewardFailureSoTransactionCanRollback() {
        User me = buildUser(STUDENT_1_ID, "학생1", "student");
        SchoolClass classA = buildClass(CLASS_A_ID, buildUser(TEACHER_A_ID, "선생님", "teacher"));
        ReadingRecord record = stubCompletedReadingRecord(me);

        when(userRepository.findById(STUDENT_1_ID)).thenReturn(Optional.of(me));
        when(classStudentRepository.findByStudentId(STUDENT_1_ID))
            .thenReturn(Optional.of(buildClassStudent(1L, classA, me)));
        when(responseRepository.findAllById(List.of(700L)))
            .thenReturn(List.of(buildTeaserResponse(700L, me, record, "질문1")));
        when(bookRecommendationRepository.save(any(BookRecommendation.class)))
            .thenAnswer(invocation -> {
                BookRecommendation saved = invocation.getArgument(0);
                saved.setId(900L);
                saved.setCreatedAt(LocalDateTime.now());
                return saved;
            });
        when(rewardService.grantRecommendationRewardOnce(me, 800L))
            .thenThrow(new IllegalStateException("reward failed"));

        assertThatThrownBy(() -> service.createRecommendation(
                STUDENT_1_ID,
                buildCreateRequest("책", "작가", "이유", List.of(700L))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("reward failed");

        verify(individualAchievementService, never()).calculateLiveReadingPracticeScore(any(Long.class));
    }

    /* 검증 5: 같은 학급 목록 조회 */
    @Test
    void getClassWallForStudent_returnsSameClassRecommendations() {
        User me = buildUser(STUDENT_1_ID, "학생1", "student");
        User classmate = buildUser(STUDENT_2_ID, "학생2", "student");
        SchoolClass classA = buildClass(CLASS_A_ID, buildUser(TEACHER_A_ID, "선생님", "teacher"));
        ClassStudent myMembership = buildClassStudent(1L, classA, me);

        when(classStudentRepository.findByStudentId(STUDENT_1_ID)).thenReturn(Optional.of(myMembership));
        when(classStudentRepository.findBySchoolClassId(CLASS_A_ID)).thenReturn(List.of(
            myMembership, buildClassStudent(2L, classA, classmate)));

        BookRecommendation r1 = buildRecommendation(
            500L, me, "긴긴밤", "루리", "내 추천", LocalDateTime.of(2026, 7, 28, 10, 0));
        BookRecommendation r2 = buildRecommendation(
            501L, classmate, "완득이", "김려령", "친구 추천", LocalDateTime.of(2026, 7, 27, 10, 0));

        when(bookRecommendationRepository.findByStudent_IdInOrderByCreatedAtDescIdDesc(
                List.of(STUDENT_1_ID, STUDENT_2_ID)))
            .thenReturn(List.of(r1, r2));
        when(contentLikeRepository.countByContentTypeAndContentId(eq("book_recommendation"), any()))
            .thenReturn(0L);
        when(contentLikeRepository.findByStudent_IdAndContentTypeAndContentIdIn(
                eq(STUDENT_1_ID), eq("book_recommendation"), anyCollection()))
            .thenReturn(List.of());

        BookRecommendationClassWallResponse result = service.getClassWallForStudent(STUDENT_1_ID);

        assertThat(result.getBest()).hasSize(2);
        assertThat(result.getRecent()).isEmpty();
    }

    /* 검증 6: 다른 학급 글은 조회 대상에 포함되지 않는다(studentIds 자체가 같은 학급으로만 구성됨) */
    @Test
    void getClassWallForStudent_neverIncludesOtherClassStudentIds() {
        User me = buildUser(STUDENT_1_ID, "학생1", "student");
        SchoolClass classA = buildClass(CLASS_A_ID, buildUser(TEACHER_A_ID, "선생님", "teacher"));
        ClassStudent myMembership = buildClassStudent(1L, classA, me);

        when(classStudentRepository.findByStudentId(STUDENT_1_ID)).thenReturn(Optional.of(myMembership));
        when(classStudentRepository.findBySchoolClassId(CLASS_A_ID)).thenReturn(List.of(myMembership));
        when(bookRecommendationRepository.findByStudent_IdInOrderByCreatedAtDescIdDesc(List.of(STUDENT_1_ID)))
            .thenReturn(List.of());

        service.getClassWallForStudent(STUDENT_1_ID);

        verify(bookRecommendationRepository)
            .findByStudent_IdInOrderByCreatedAtDescIdDesc(List.of(STUDENT_1_ID));
        verify(bookRecommendationRepository, never())
            .findByStudent_IdInOrderByCreatedAtDescIdDesc(List.of(STUDENT_3_ID));
    }

    /* 검증 7/8: BEST는 좋아요 순, 동률이면 최신순으로 정렬되고 BEST3 제외한 나머지가 recent에 최신순으로 남는다 */
    @Test
    void getClassWallForStudent_sortsBestByLikesThenRecency_andSplitsRecent() {
        User me = buildUser(STUDENT_1_ID, "학생1", "student");
        SchoolClass classA = buildClass(CLASS_A_ID, buildUser(TEACHER_A_ID, "선생님", "teacher"));
        ClassStudent myMembership = buildClassStudent(1L, classA, me);

        when(classStudentRepository.findByStudentId(STUDENT_1_ID)).thenReturn(Optional.of(myMembership));
        when(classStudentRepository.findBySchoolClassId(CLASS_A_ID)).thenReturn(List.of(myMembership));

        // r1: likes=2, r2: likes=5, r3: likes=2(더 최근 => r1보다 앞), r4: likes=0, r5: likes=0(가장 최근)
        BookRecommendation r1 = buildRecommendation(1L, me, "책1", "작가1", "이유1", LocalDateTime.of(2026, 7, 20, 0, 0));
        BookRecommendation r2 = buildRecommendation(2L, me, "책2", "작가2", "이유2", LocalDateTime.of(2026, 7, 21, 0, 0));
        BookRecommendation r3 = buildRecommendation(3L, me, "책3", "작가3", "이유3", LocalDateTime.of(2026, 7, 22, 0, 0));
        BookRecommendation r4 = buildRecommendation(4L, me, "책4", "작가4", "이유4", LocalDateTime.of(2026, 7, 23, 0, 0));
        BookRecommendation r5 = buildRecommendation(5L, me, "책5", "작가5", "이유5", LocalDateTime.of(2026, 7, 24, 0, 0));

        // repository는 이미 createdAt desc, id desc로 정렬해서 반환한다고 가정
        when(bookRecommendationRepository.findByStudent_IdInOrderByCreatedAtDescIdDesc(List.of(STUDENT_1_ID)))
            .thenReturn(List.of(r5, r4, r3, r2, r1));

        when(contentLikeRepository.countByContentTypeAndContentId("book_recommendation", 1L)).thenReturn(2L);
        when(contentLikeRepository.countByContentTypeAndContentId("book_recommendation", 2L)).thenReturn(5L);
        when(contentLikeRepository.countByContentTypeAndContentId("book_recommendation", 3L)).thenReturn(2L);
        when(contentLikeRepository.countByContentTypeAndContentId("book_recommendation", 4L)).thenReturn(0L);
        when(contentLikeRepository.countByContentTypeAndContentId("book_recommendation", 5L)).thenReturn(0L);
        when(contentLikeRepository.findByStudent_IdAndContentTypeAndContentIdIn(
                eq(STUDENT_1_ID), eq("book_recommendation"), anyCollection()))
            .thenReturn(List.of());

        BookRecommendationClassWallResponse result = service.getClassWallForStudent(STUDENT_1_ID);

        assertThat(result.getBest()).hasSize(3);
        assertThat(result.getBest().get(0).getRecommendationId()).isEqualTo(2L); // likes=5
        assertThat(result.getBest().get(0).getRank()).isEqualTo(1);
        assertThat(result.getBest().get(1).getRecommendationId()).isEqualTo(3L); // likes=2, 더 최근
        assertThat(result.getBest().get(1).getRank()).isEqualTo(2);
        assertThat(result.getBest().get(2).getRecommendationId()).isEqualTo(1L); // likes=2, 더 오래됨
        assertThat(result.getBest().get(2).getRank()).isEqualTo(3);

        assertThat(result.getRecent()).hasSize(2);
        assertThat(result.getRecent().get(0).getRecommendationId()).isEqualTo(5L); // 최신순
        assertThat(result.getRecent().get(1).getRecommendationId()).isEqualTo(4L);
        assertThat(result.getRecent()).allSatisfy(item -> assertThat(item.getRank()).isNull());
    }

    /* 검증 10/11: 학생 좋아요 생성 -> 취소 */
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

        BookRecommendation recommendation = buildRecommendation(
            500L, owner, "긴긴밤", "루리", "이유", LocalDateTime.now());
        when(bookRecommendationRepository.findById(500L)).thenReturn(Optional.of(recommendation));

        when(contentLikeRepository.findByStudent_IdAndContentTypeAndContentId(
                STUDENT_1_ID, "book_recommendation", 500L))
            .thenReturn(Optional.empty());
        when(contentLikeRepository.countByContentTypeAndContentId("book_recommendation", 500L)).thenReturn(1L);

        BookRecommendationLikeResponse firstResult = service.toggleLikeAsStudent(STUDENT_1_ID, 500L);

        assertThat(firstResult.isLiked()).isTrue();
        assertThat(firstResult.getLikeCount()).isEqualTo(1L);
        verify(contentLikeRepository).save(any(ContentLike.class));

        ContentLike existingLike = new ContentLike();
        existingLike.setId(900L);
        when(contentLikeRepository.findByStudent_IdAndContentTypeAndContentId(
                STUDENT_1_ID, "book_recommendation", 500L))
            .thenReturn(Optional.of(existingLike));
        when(contentLikeRepository.countByContentTypeAndContentId("book_recommendation", 500L)).thenReturn(0L);

        BookRecommendationLikeResponse secondResult = service.toggleLikeAsStudent(STUDENT_1_ID, 500L);

        assertThat(secondResult.isLiked()).isFalse();
        assertThat(secondResult.getLikeCount()).isEqualTo(0L);
        verify(contentLikeRepository).delete(existingLike);
    }

    /* 검증 12: 같은 사용자가 같은 글에 두 번 좋아요를 시도해도 UNIQUE 제약 특성상 토글로만 동작(중복 방지) */
    @Test
    void toggleLikeAsStudent_secondCallWithExistingLikeDeletesNotDuplicates() {
        User me = buildUser(STUDENT_1_ID, "학생1", "student");
        SchoolClass classA = buildClass(CLASS_A_ID, buildUser(TEACHER_A_ID, "선생님", "teacher"));

        when(classStudentRepository.findByStudentId(STUDENT_1_ID))
            .thenReturn(Optional.of(buildClassStudent(1L, classA, me)));
        when(userRepository.findById(STUDENT_1_ID)).thenReturn(Optional.of(me));

        BookRecommendation recommendation = buildRecommendation(
            500L, me, "긴긴밤", "루리", "이유", LocalDateTime.now());
        when(bookRecommendationRepository.findById(500L)).thenReturn(Optional.of(recommendation));

        ContentLike existingLike = new ContentLike();
        existingLike.setId(900L);
        when(contentLikeRepository.findByStudent_IdAndContentTypeAndContentId(
                STUDENT_1_ID, "book_recommendation", 500L))
            .thenReturn(Optional.of(existingLike));
        when(contentLikeRepository.countByContentTypeAndContentId("book_recommendation", 500L)).thenReturn(0L);

        service.toggleLikeAsStudent(STUDENT_1_ID, 500L);

        verify(contentLikeRepository, never()).save(any(ContentLike.class));
        verify(contentLikeRepository).delete(existingLike);
    }

    /* 검증 13: 교사는 담당 학급 학생들의 추천 글만 조회한다 */
    @Test
    void getClassWallForTeacher_returnsOnlyOwnClassStudents() {
        User teacher = buildUser(TEACHER_A_ID, "선생님", "teacher");
        User student1 = buildUser(STUDENT_1_ID, "학생1", "student");
        User student2 = buildUser(STUDENT_2_ID, "학생2", "student");
        SchoolClass classA = buildClass(CLASS_A_ID, teacher);

        when(userRepository.findById(TEACHER_A_ID)).thenReturn(Optional.of(teacher));
        when(schoolClassRepository.findByTeacherId(TEACHER_A_ID)).thenReturn(Optional.of(classA));
        when(classStudentRepository.findBySchoolClassId(CLASS_A_ID)).thenReturn(List.of(
            buildClassStudent(1L, classA, student1), buildClassStudent(2L, classA, student2)));

        BookRecommendation r1 = buildRecommendation(500L, student1, "책", "작가", "이유", LocalDateTime.now());
        when(bookRecommendationRepository.findByStudent_IdInOrderByCreatedAtDescIdDesc(
                List.of(STUDENT_1_ID, STUDENT_2_ID)))
            .thenReturn(List.of(r1));
        when(contentLikeRepository.countByContentTypeAndContentId("book_recommendation", 500L)).thenReturn(2L);
        when(contentLikeRepository.findByStudent_IdAndContentTypeAndContentIdIn(
                eq(TEACHER_A_ID), eq("book_recommendation"), anyCollection()))
            .thenReturn(List.of());

        BookRecommendationClassWallResponse result = service.getClassWallForTeacher(TEACHER_A_ID);

        assertThat(result.getBest()).hasSize(1);
        assertThat(result.getBest().get(0).getIsMine()).isFalse();
        assertThat(result.getBest().get(0).getLikeCount()).isEqualTo(2L);
    }

    /* 검증 14: 교사가 담당하지 않는 학급의 글에 좋아요를 시도하면 403 */
    @Test
    void toggleLikeAsTeacher_throwsForbiddenForDifferentClass() {
        User teacher = buildUser(TEACHER_A_ID, "선생님", "teacher");
        User owner = buildUser(STUDENT_3_ID, "학생3", "student"); // 학급 B
        SchoolClass classA = buildClass(CLASS_A_ID, teacher);
        SchoolClass classB = buildClass(CLASS_B_ID, buildUser(2L, "다른선생님", "teacher"));

        when(userRepository.findById(TEACHER_A_ID)).thenReturn(Optional.of(teacher));
        when(schoolClassRepository.findByTeacherId(TEACHER_A_ID)).thenReturn(Optional.of(classA));
        when(classStudentRepository.findByStudentId(STUDENT_3_ID))
            .thenReturn(Optional.of(buildClassStudent(3L, classB, owner)));

        BookRecommendation recommendation = buildRecommendation(
            500L, owner, "책", "작가", "이유", LocalDateTime.now());
        when(bookRecommendationRepository.findById(500L)).thenReturn(Optional.of(recommendation));

        assertThatThrownBy(() -> service.toggleLikeAsTeacher(TEACHER_A_ID, 500L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("403");

        verify(contentLikeRepository, never()).save(any(ContentLike.class));
    }

    /* 검증: 존재하지 않는 추천 글은 404 */
    @Test
    void toggleLikeAsStudent_throwsNotFoundForMissingRecommendation() {
        User me = buildUser(STUDENT_1_ID, "학생1", "student");
        SchoolClass classA = buildClass(CLASS_A_ID, buildUser(TEACHER_A_ID, "선생님", "teacher"));

        when(classStudentRepository.findByStudentId(STUDENT_1_ID))
            .thenReturn(Optional.of(buildClassStudent(1L, classA, me)));
        when(bookRecommendationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.toggleLikeAsStudent(STUDENT_1_ID, 999L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("404");
    }

    /* 검증 15: 교사 좋아요 생성 -> 취소 */
    @Test
    void toggleLikeAsTeacher_createsThenCancels() {
        User teacher = buildUser(TEACHER_A_ID, "선생님", "teacher");
        User owner = buildUser(STUDENT_1_ID, "학생1", "student");
        SchoolClass classA = buildClass(CLASS_A_ID, teacher);

        when(userRepository.findById(TEACHER_A_ID)).thenReturn(Optional.of(teacher));
        when(schoolClassRepository.findByTeacherId(TEACHER_A_ID)).thenReturn(Optional.of(classA));
        when(classStudentRepository.findByStudentId(STUDENT_1_ID))
            .thenReturn(Optional.of(buildClassStudent(1L, classA, owner)));

        BookRecommendation recommendation = buildRecommendation(
            500L, owner, "책", "작가", "이유", LocalDateTime.now());
        when(bookRecommendationRepository.findById(500L)).thenReturn(Optional.of(recommendation));

        when(contentLikeRepository.findByStudent_IdAndContentTypeAndContentId(
                TEACHER_A_ID, "book_recommendation", 500L))
            .thenReturn(Optional.empty());
        when(contentLikeRepository.countByContentTypeAndContentId("book_recommendation", 500L)).thenReturn(1L);

        BookRecommendationLikeResponse firstResult = service.toggleLikeAsTeacher(TEACHER_A_ID, 500L);
        assertThat(firstResult.isLiked()).isTrue();

        ContentLike existingLike = new ContentLike();
        existingLike.setId(901L);
        when(contentLikeRepository.findByStudent_IdAndContentTypeAndContentId(
                TEACHER_A_ID, "book_recommendation", 500L))
            .thenReturn(Optional.of(existingLike));
        when(contentLikeRepository.countByContentTypeAndContentId("book_recommendation", 500L)).thenReturn(0L);

        BookRecommendationLikeResponse secondResult = service.toggleLikeAsTeacher(TEACHER_A_ID, 500L);
        assertThat(secondResult.isLiked()).isFalse();
    }

    /* 검증 17/18: likedByMe는 조회자 기준으로만 계산되고, 학생/교사 좋아요는 같은 content_likes로 합산되어 likeCount가 일치한다 */
    @Test
    void getClassWall_likedByMeReflectsOnlyViewer_andLikeCountSharedAcrossRoles() {
        User me = buildUser(STUDENT_1_ID, "학생1", "student");
        User other = buildUser(STUDENT_2_ID, "학생2", "student");
        SchoolClass classA = buildClass(CLASS_A_ID, buildUser(TEACHER_A_ID, "선생님", "teacher"));
        ClassStudent myMembership = buildClassStudent(1L, classA, me);

        when(classStudentRepository.findByStudentId(STUDENT_1_ID)).thenReturn(Optional.of(myMembership));
        when(classStudentRepository.findBySchoolClassId(CLASS_A_ID))
            .thenReturn(List.of(myMembership, buildClassStudent(2L, classA, other)));

        BookRecommendation recommendation = buildRecommendation(
            500L, other, "책", "작가", "이유", LocalDateTime.now());
        when(bookRecommendationRepository.findByStudent_IdInOrderByCreatedAtDescIdDesc(
                List.of(STUDENT_1_ID, STUDENT_2_ID)))
            .thenReturn(List.of(recommendation));
        // 좋아요는 학생1명 + 교사1명이 눌러 총 2건(합산)이라고 가정
        when(contentLikeRepository.countByContentTypeAndContentId("book_recommendation", 500L)).thenReturn(2L);
        // 조회자(STUDENT_1_ID)는 좋아요를 누르지 않았음
        when(contentLikeRepository.findByStudent_IdAndContentTypeAndContentIdIn(
                eq(STUDENT_1_ID), eq("book_recommendation"), anyCollection()))
            .thenReturn(List.of());

        BookRecommendationClassWallResponse result = service.getClassWallForStudent(STUDENT_1_ID);

        assertThat(result.getBest()).hasSize(1);
        assertThat(result.getBest().get(0).isLikedByMe()).isFalse();
        assertThat(result.getBest().get(0).getLikeCount()).isEqualTo(2L);
    }

    /* 검증: 추천 글이 하나도 없는 학급은 best/recent 모두 빈 목록 */
    @Test
    void getClassWallForStudent_returnsEmptyWhenNoRecommendations() {
        User me = buildUser(STUDENT_1_ID, "학생1", "student");
        SchoolClass classA = buildClass(CLASS_A_ID, buildUser(TEACHER_A_ID, "선생님", "teacher"));
        ClassStudent myMembership = buildClassStudent(1L, classA, me);

        when(classStudentRepository.findByStudentId(STUDENT_1_ID)).thenReturn(Optional.of(myMembership));
        when(classStudentRepository.findBySchoolClassId(CLASS_A_ID)).thenReturn(List.of(myMembership));
        when(bookRecommendationRepository.findByStudent_IdInOrderByCreatedAtDescIdDesc(List.of(STUDENT_1_ID)))
            .thenReturn(List.of());

        BookRecommendationClassWallResponse result = service.getClassWallForStudent(STUDENT_1_ID);

        assertThat(result.getBest()).isEmpty();
        assertThat(result.getRecent()).isEmpty();
    }

    /* 검증: 교사가 담당 학급이 없으면 400 */
    @Test
    void getClassWallForTeacher_throwsBadRequestWhenNoClass() {
        User teacher = buildUser(TEACHER_A_ID, "선생님", "teacher");

        when(userRepository.findById(TEACHER_A_ID)).thenReturn(Optional.of(teacher));
        when(schoolClassRepository.findByTeacherId(TEACHER_A_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getClassWallForTeacher(TEACHER_A_ID))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400");
    }
}
