package com.victory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.BookTypeStatsResponse;
import com.victory.dto.IndividualAfterCompleteResponse;
import com.victory.dto.IndividualAfterResponseItem;
import com.victory.dto.IndividualAfterResponseSaveRequest;
import com.victory.dto.IndividualAchievementLevel;
import com.victory.dto.IndividualAchievementResult;
import com.victory.dto.IndividualBeforeResponseItem;
import com.victory.dto.IndividualBeforeResponseSaveRequest;
import com.victory.dto.IndividualBookRegisterRequest;
import com.victory.dto.IndividualDuringResponseItem;
import com.victory.dto.IndividualDuringResponseSaveRequest;
import com.victory.dto.IndividualDuringTodayResponse;
import com.victory.dto.IndividualFinishCandidatesResponse;
import com.victory.dto.IndividualReadingArchiveDetailResponse;
import com.victory.dto.IndividualReadingArchiveItem;
import com.victory.dto.IndividualReadingFinishRequest;
import com.victory.dto.IndividualReadingFinishResponse;
import com.victory.dto.IndividualReadingPagesRequest;
import com.victory.dto.IndividualReadingRecordResponse;
import com.victory.dto.IndividualSummaryResponse;
import com.victory.dto.IndividualSummarySaveRequest;
import com.victory.dto.MonthlyCompletionStatsResponse;
import com.victory.entity.Book;
import com.victory.entity.ReadingProgressLog;
import com.victory.entity.ReadingRecord;
import com.victory.entity.Response;
import com.victory.entity.StudentStats;
import com.victory.entity.Summary;
import com.victory.entity.User;
import com.victory.repository.BookRepository;
import com.victory.repository.ReadingProgressLogRepository;
import com.victory.repository.ReadingRecordRepository;
import com.victory.repository.ResponseRepository;
import com.victory.repository.SummaryRepository;
import com.victory.repository.UserRepository;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

@ExtendWith(MockitoExtension.class)
class IndividualReadingServiceTest {

    private static final Long STUDENT_ID = 100L;
    private static final Long OTHER_STUDENT_ID = 200L;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private ReadingRecordRepository readingRecordRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ResponseRepository responseRepository;

    @Mock
    private ReadingProgressLogRepository readingProgressLogRepository;

    @Mock
    private SummaryRepository summaryRepository;

    @Mock
    private IndividualBeforeReadingRewardService beforeReadingRewardService;

    @Mock
    private IndividualDuringReadingRewardService duringReadingRewardService;

    @Mock
    private IndividualAfterReadingRewardService afterReadingRewardService;

    @Mock
    private IndividualAchievementService individualAchievementService;

    private IndividualReadingService service;

    private User student;

    @BeforeEach
    void setUp() {
        service = new IndividualReadingService(
            bookRepository, readingRecordRepository, userRepository, responseRepository,
            readingProgressLogRepository, summaryRepository,
            beforeReadingRewardService, duringReadingRewardService, afterReadingRewardService,
            individualAchievementService);

        student = new User();
        student.setId(STUDENT_ID);
        student.setName("학생1");
        student.setRole("student");

        lenient().when(userRepository.findByIdForUpdate(STUDENT_ID)).thenReturn(Optional.of(student));
    }

    private IndividualBookRegisterRequest buildRegisterRequest(String title, String author) {
        IndividualBookRegisterRequest request = new IndividualBookRegisterRequest();
        setField(request, "title", title);
        setField(request, "author", author);
        setField(request, "bookType", "story");
        return request;
    }

    private IndividualAchievementResult achievementResult(double readingPracticeScore, double recordCompletionScore) {
        double overall = (readingPracticeScore + recordCompletionScore) / 2.0;
        return new IndividualAchievementResult(
            STUDENT_ID,
            10L,
            3,
            60.0,
            4,
            80.0,
            readingPracticeScore,
            3,
            100.0,
            5,
            4,
            80.0,
            recordCompletionScore,
            overall,
            (int) Math.round(overall),
            IndividualAchievementLevel.GOOD,
            1L,
            true,
            1,
            LocalDate.now());
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private Book buildBook(Long id, String title, String author) {
        Book book = new Book();
        book.setId(id);
        book.setTitle(title);
        book.setAuthor(author);
        book.setBookType("story");
        book.setSource("individual");
        return book;
    }

    private ReadingRecord buildRecord(Long id, User owner, Book book) {
        ReadingRecord record = new ReadingRecord();
        record.setId(id);
        record.setStudent(owner);
        record.setBook(book);
        record.setCurrentStage("before");
        record.setBeforeDone(false);
        record.setDuringDone(false);
        record.setAfterDone(false);
        record.setCurrentPage(0);
        return record;
    }

    /* 검증 1: 최초 책 등록 성공 */
    @Test
    void registerBook_succeedsWhenNoInProgressRecord() {
        when(readingRecordRepository.findByStudent_IdAndFinishedAtIsNull(STUDENT_ID))
            .thenReturn(Optional.empty());
        when(bookRepository.findFirstBySourceAndTitleAndAuthor("individual", "아몬드", "손원평"))
            .thenReturn(Optional.empty());

        Book newBook = buildBook(1L, "아몬드", "손원평");
        when(bookRepository.save(any(Book.class))).thenReturn(newBook);

        when(readingRecordRepository.save(any(ReadingRecord.class))).thenAnswer(invocation -> {
            ReadingRecord toSave = invocation.getArgument(0);
            toSave.setId(10L);
            return toSave;
        });

        IndividualReadingRecordResponse response =
            service.registerBook(STUDENT_ID, buildRegisterRequest("아몬드", "손원평"));

        assertThat(response.getReadingRecordId()).isEqualTo(10L);
        assertThat(response.getBookId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("아몬드");
        assertThat(response.getCurrentStage()).isEqualTo("before");
        assertThat(response.getBeforeDone()).isFalse();
        assertThat(response.getCurrentPage()).isEqualTo(0);
        assertThat(response.getCompleted()).isFalse();
    }

    /* 검증 2: 진행 중 책 조회 성공 */
    @Test
    void getCurrentReadingRecord_returnsInProgressRecord() {
        Book book = buildBook(1L, "아몬드", "손원평");
        ReadingRecord record = buildRecord(10L, student, book);

        when(readingRecordRepository.findByStudent_IdAndFinishedAtIsNull(STUDENT_ID))
            .thenReturn(Optional.of(record));

        Optional<IndividualReadingRecordResponse> result = service.getCurrentReadingRecord(STUDENT_ID);

        assertThat(result).isPresent();
        assertThat(result.get().getReadingRecordId()).isEqualTo(10L);
        assertThat(result.get().getCompleted()).isFalse();
    }

    /* 검증: 진행 중인 책이 없으면 빈 값 */
    @Test
    void getCurrentReadingRecord_returnsEmptyWhenNoInProgressRecord() {
        when(readingRecordRepository.findByStudent_IdAndFinishedAtIsNull(STUDENT_ID))
            .thenReturn(Optional.empty());

        Optional<IndividualReadingRecordResponse> result = service.getCurrentReadingRecord(STUDENT_ID);

        assertThat(result).isEmpty();
    }

    /* 검증 3: 진행 중 책이 있는데 두 번째 책 등록 시 409 */
    @Test
    void registerBook_throwsConflictWhenInProgressRecordExists() {
        Book existingBook = buildBook(1L, "아몬드", "손원평");
        ReadingRecord existingRecord = buildRecord(10L, student, existingBook);

        when(readingRecordRepository.findByStudent_IdAndFinishedAtIsNull(STUDENT_ID))
            .thenReturn(Optional.of(existingRecord));

        assertThatThrownBy(() ->
            service.registerBook(STUDENT_ID, buildRegisterRequest("다른 책", "다른 작가"))
        )
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("409")
            .hasMessageContaining("현재 읽고 있는 책을 먼저 마쳐 주세요");

        verify(bookRepository, never()).save(any(Book.class));
        verify(readingRecordRepository, never()).save(any(ReadingRecord.class));
    }

    /* 검증 4/5: 완독 후 새 책 등록 성공 + 같은 책 재등록 시 다른 readingRecordId 생성 */
    @Test
    void registerBook_succeedsAgainAfterPreviousBookCompletedAndCreatesNewRecordId() {
        // 완독 후에는 findByStudent_IdAndFinishedAtIsNull이 빈 값을 반환한다.
        when(readingRecordRepository.findByStudent_IdAndFinishedAtIsNull(STUDENT_ID))
            .thenReturn(Optional.empty());

        Book sameBook = buildBook(1L, "아몬드", "손원평");
        // 같은 책이므로 기존 book 행을 재사용한다.
        when(bookRepository.findFirstBySourceAndTitleAndAuthor("individual", "아몬드", "손원평"))
            .thenReturn(Optional.of(sameBook));

        when(readingRecordRepository.save(any(ReadingRecord.class))).thenAnswer(invocation -> {
            ReadingRecord toSave = invocation.getArgument(0);
            toSave.setId(20L);
            return toSave;
        });

        IndividualReadingRecordResponse response =
            service.registerBook(STUDENT_ID, buildRegisterRequest("아몬드", "손원평"));

        // 같은 책(bookId=1)인데 새로운 readingRecordId(20L, 이전 완독 기록의 10L과 다름)가 생성됨
        assertThat(response.getBookId()).isEqualTo(1L);
        assertThat(response.getReadingRecordId()).isEqualTo(20L);
        assertThat(response.getReadingRecordId()).isNotEqualTo(10L);

        verify(bookRepository, never()).save(any(Book.class));
    }

    /* 검증 6: 같은 bookId에 여러 readingRecord 허용(재사용 book_id로 저장이 성공함을 확인) */
    @Test
    void registerBook_allowsMultipleReadingRecordsForSameBookId() {
        when(readingRecordRepository.findByStudent_IdAndFinishedAtIsNull(STUDENT_ID))
            .thenReturn(Optional.empty());

        Book sameBook = buildBook(5L, "마당을 나온 암탉", "황선미");
        when(bookRepository.findFirstBySourceAndTitleAndAuthor("individual", "마당을 나온 암탉", "황선미"))
            .thenReturn(Optional.of(sameBook));

        when(readingRecordRepository.save(any(ReadingRecord.class))).thenAnswer(invocation -> {
            ReadingRecord toSave = invocation.getArgument(0);
            toSave.setId(30L);
            return toSave;
        });

        IndividualReadingRecordResponse response =
            service.registerBook(STUDENT_ID, buildRegisterRequest("마당을 나온 암탉", "황선미"));

        assertThat(response.getBookId()).isEqualTo(5L);
        assertThat(response.getReadingRecordId()).isEqualTo(30L);
    }

    /* 검증 7: 다른 학생 기록 수정 차단 */
    @Test
    void updatePages_throwsForbiddenWhenRecordBelongsToAnotherStudent() {
        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.empty());
        when(readingRecordRepository.findById(10L)).thenReturn(
            Optional.of(buildRecord(10L, otherStudent(), buildBook(1L, "책", "작가"))));

        IndividualReadingPagesRequest request = buildPagesRequest(100, 50);

        assertThatThrownBy(() -> service.updatePages(STUDENT_ID, 10L, request))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("403");
    }

    /* 검증 7: 다른 학생 기록 완독 처리 차단 */
    @Test
    void completeReadingRecord_throwsForbiddenWhenRecordBelongsToAnotherStudent() {
        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.empty());
        when(readingRecordRepository.findById(10L)).thenReturn(
            Optional.of(buildRecord(10L, otherStudent(), buildBook(1L, "책", "작가"))));

        assertThatThrownBy(() -> service.completeReadingRecord(STUDENT_ID, 10L, buildFinishRequest(4, 500L)))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("403");
    }

    /* 검증: 존재하지 않는 기록은 404 */
    @Test
    void updatePages_throwsNotFoundWhenRecordDoesNotExist() {
        when(readingRecordRepository.findByIdAndStudent_Id(999L, STUDENT_ID)).thenReturn(Optional.empty());
        when(readingRecordRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updatePages(STUDENT_ID, 999L, buildPagesRequest(100, 50)))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("404");
    }

    /* 검증 8: currentPage > totalPages 거부 */
    @Test
    void updatePages_throwsBadRequestWhenCurrentPageExceedsTotalPages() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.updatePages(STUDENT_ID, 10L, buildPagesRequest(100, 150)))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400");

        verify(readingRecordRepository, never()).save(any(ReadingRecord.class));
    }

    /* 검증: 완독된 기록의 쪽수 수정 금지 */
    @Test
    void updatePages_throwsConflictWhenRecordAlreadyFinished() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        record.setFinishedAt(java.time.LocalDateTime.now());

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.updatePages(STUDENT_ID, 10L, buildPagesRequest(100, 50)))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("409");
    }

    /* 검증: 정상 쪽수 수정 성공 */
    @Test
    void updatePages_succeedsWithValidPages() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        when(readingRecordRepository.save(any(ReadingRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IndividualReadingRecordResponse response = service.updatePages(STUDENT_ID, 10L, buildPagesRequest(100, 50));

        assertThat(response.getTotalPages()).isEqualTo(100);
        assertThat(response.getCurrentPage()).isEqualTo(50);
    }

    /* 검증 9: afterDone=true인 기록을 완독 처리하면 finishedAt/rating/대표 질문이 저장된다 */
    @Test
    void completeReadingRecord_setsFinishedAtAndSavesRatingAndRepresentative() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        record.setAfterDone(true);
        Response representative = buildAfterResponseEntity(500L, student, record, 1, "질문?", "답이에요", true);

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        when(responseRepository.findById(500L)).thenReturn(Optional.of(representative));
        when(individualAchievementService.calculate(10L)).thenReturn(achievementResult(72.4, 88.6));
        when(readingRecordRepository.save(any(ReadingRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IndividualReadingFinishResponse response =
            service.completeReadingRecord(STUDENT_ID, 10L, buildFinishRequest(4, 500L));

        assertThat(response.isFinished()).isTrue();
        assertThat(response.getFinishedAt()).isNotNull();
        assertThat(response.getRating()).isEqualTo(4);
        assertThat(response.getRepresentativeQuestion()).isEqualTo("질문?");
        assertThat(response.getRepresentativeAnswer()).isEqualTo("답이에요");
        assertThat(response.getRepresentativeCategory()).isEqualTo("after");
        assertThat(record.getRating()).isEqualTo(4);
        assertThat(record.getRepresentResponse()).isEqualTo(representative);
        assertThat(record.getFinalReadingPracticeScore()).isEqualTo(72);
        assertThat(record.getFinalRecordCompletionScore()).isEqualTo(89);
        assertThat(record.getCurrentStage()).isEqualTo("completed");
    }

    @Test
    void completeReadingRecord_doesNotSetFinishedAtWhenAchievementCalculationFails() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        record.setAfterDone(true);
        Response representative = buildAfterResponseEntity(500L, student, record, 1, "질문?", "답이에요", true);

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        when(responseRepository.findById(500L)).thenReturn(Optional.of(representative));
        when(individualAchievementService.calculate(10L)).thenThrow(new IllegalStateException("calculate failed"));

        assertThatThrownBy(() -> service.completeReadingRecord(STUDENT_ID, 10L, buildFinishRequest(4, 500L)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("calculate failed");

        assertThat(record.getFinishedAt()).isNull();
        assertThat(record.getFinalReadingPracticeScore()).isNull();
        assertThat(record.getFinalRecordCompletionScore()).isNull();
        verify(readingRecordRepository, never()).save(any(ReadingRecord.class));
    }

    /* 검증: 읽기 후 완료(afterDone) 전에는 책을 완독 처리할 수 없다(책 완독과 읽기 후 완료 보상을 분리) */
    @Test
    void completeReadingRecord_throwsBadRequestWhenAfterNotDoneYet() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.completeReadingRecord(STUDENT_ID, 10L, buildFinishRequest(4, 500L)))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400");

        verify(readingRecordRepository, never()).save(any(ReadingRecord.class));
    }

    /* 검증: 완독 처리는 idempotent - 두 번째 호출 시 finishedAt/rating이 바뀌지 않음 */
    @Test
    void completeReadingRecord_isIdempotentWhenAlreadyCompleted() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        java.time.LocalDateTime firstFinishedAt = java.time.LocalDateTime.of(2026, 7, 20, 10, 0);
        record.setFinishedAt(firstFinishedAt);
        record.setAfterDone(true);
        record.setCurrentStage("completed");
        record.setRating(5);
        record.setFinalReadingPracticeScore(66);
        record.setFinalRecordCompletionScore(77);

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));

        IndividualReadingFinishResponse response =
            service.completeReadingRecord(STUDENT_ID, 10L, buildFinishRequest(1, 999L));

        assertThat(response.getFinishedAt()).isEqualTo(firstFinishedAt);
        assertThat(response.getRating()).isEqualTo(5);
        assertThat(record.getFinalReadingPracticeScore()).isEqualTo(66);
        assertThat(record.getFinalRecordCompletionScore()).isEqualTo(77);
        verify(readingRecordRepository, never()).save(any(ReadingRecord.class));
        verify(responseRepository, never()).findById(any());
        verify(individualAchievementService, never()).calculate(any());
    }

    /* 검증: 다른 학생의 응답을 대표 질문으로 제출하면 403 */
    @Test
    void completeReadingRecord_throwsForbiddenWhenRepresentativeResponseBelongsToAnotherStudent() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        record.setAfterDone(true);
        Response othersResponse =
            buildAfterResponseEntity(500L, otherStudent(), record, 1, "질문?", "답", true);

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        when(responseRepository.findById(500L)).thenReturn(Optional.of(othersResponse));

        assertThatThrownBy(() -> service.completeReadingRecord(STUDENT_ID, 10L, buildFinishRequest(4, 500L)))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("403");

        verify(readingRecordRepository, never()).save(any(ReadingRecord.class));
    }

    /* 검증: 다른 책(readingRecordId)의 응답을 대표 질문으로 제출하면 400 */
    @Test
    void completeReadingRecord_throwsBadRequestWhenRepresentativeResponseBelongsToDifferentBook() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        record.setAfterDone(true);
        ReadingRecord otherRecord = buildRecord(20L, student, buildBook(2L, "다른 책", "다른 작가"));
        Response otherBookResponse =
            buildAfterResponseEntity(500L, student, otherRecord, 1, "질문?", "답", true);

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        when(responseRepository.findById(500L)).thenReturn(Optional.of(otherBookResponse));

        assertThatThrownBy(() -> service.completeReadingRecord(STUDENT_ID, 10L, buildFinishRequest(4, 500L)))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400");

        verify(readingRecordRepository, never()).save(any(ReadingRecord.class));
    }

    /* 검증: 루미 피드백을 통과하지 못한(passed=false) 읽기 후 응답은 대표 질문으로 선택할 수 없다 */
    @Test
    void completeReadingRecord_throwsBadRequestWhenRepresentativeResponseNotPassed() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        record.setAfterDone(true);
        Response notPassed = buildAfterResponseEntity(500L, student, record, 1, "질문?", "답", false);

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        when(responseRepository.findById(500L)).thenReturn(Optional.of(notPassed));

        assertThatThrownBy(() -> service.completeReadingRecord(STUDENT_ID, 10L, buildFinishRequest(4, 500L)))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400");

        verify(readingRecordRepository, never()).save(any(ReadingRecord.class));
    }

    /* 검증: 읽기 전 응답도 대표 질문으로 선택할 수 있다(존재 자체가 AI 통과를 의미하는 기존 관례) */
    @Test
    void completeReadingRecord_acceptsBeforeStageResponseAsRepresentative() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        record.setAfterDone(true);
        Response beforeResponse = buildBeforeResponseEntity(500L, student, record, "title", "제목 질문?", "제목 답");

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        when(responseRepository.findById(500L)).thenReturn(Optional.of(beforeResponse));
        when(individualAchievementService.calculate(10L)).thenReturn(achievementResult(81.0, 91.0));
        when(readingRecordRepository.save(any(ReadingRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IndividualReadingFinishResponse response =
            service.completeReadingRecord(STUDENT_ID, 10L, buildFinishRequest(3, 500L));

        assertThat(response.getRepresentativeCategory()).isEqualTo("before");
        assertThat(response.getRepresentativeQuestion()).isEqualTo("제목 질문?");
        assertThat(record.getFinalReadingPracticeScore()).isEqualTo(81);
        assertThat(record.getFinalRecordCompletionScore()).isEqualTo(91);
    }

    /* 검증: rating이 1~5 범위를 벗어나면 Bean Validation에서 거부된다 */
    @Test
    void finishRequest_rejectsRatingOutOfRange() {
        try (jakarta.validation.ValidatorFactory factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            jakarta.validation.Validator validator = factory.getValidator();

            assertThat(validator.validate(buildFinishRequest(0, 500L))).isNotEmpty();
            assertThat(validator.validate(buildFinishRequest(6, 500L))).isNotEmpty();
            assertThat(validator.validate(buildFinishRequest(1, 500L))).isEmpty();
            assertThat(validator.validate(buildFinishRequest(5, 500L))).isEmpty();
        }
    }

    /* 검증: 존재하지 않는 responseId를 대표 질문으로 제출하면 404 */
    @Test
    void completeReadingRecord_throwsNotFoundWhenRepresentativeResponseDoesNotExist() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        record.setAfterDone(true);

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        when(responseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.completeReadingRecord(STUDENT_ID, 10L, buildFinishRequest(4, 999L)))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("404");
    }

    /* 검증: 이 책 마무리하기 팝업의 읽기 전/중/후 대표 질문 후보를 각각 반환한다 */
    @Test
    void getFinishCandidates_returnsBeforeDuringAfterGroupedCandidates() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        when(responseRepository
                .findByStudent_IdAndReadingRecord_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                    STUDENT_ID, 10L, "individual", "answer", "before"))
            .thenReturn(List.of(buildBeforeResponseEntity(1L, student, record, "title", "제목 질문?", "제목 답")));
        when(responseRepository
                .findByStudent_IdAndReadingRecord_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                    STUDENT_ID, 10L, "individual", "answer", "during"))
            .thenReturn(List.of(buildDuringResponseEntity(
                2L, student, record, 1, "infer", "생각 질문?", "생각 답", LocalDate.of(2026, 7, 27))));
        when(responseRepository
                .findByStudent_IdAndReadingRecord_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                    STUDENT_ID, 10L, "individual", "answer", "after"))
            .thenReturn(List.of(
                buildAfterResponseEntity(3L, student, record, 1, "간추리기 질문?", "간추리기 답", true),
                buildAfterResponseEntity(4L, student, record, 2, "통과 못함?", "미통과 답", false)));

        IndividualFinishCandidatesResponse result = service.getFinishCandidates(STUDENT_ID, 10L);

        assertThat(result.getBefore()).hasSize(1);
        assertThat(result.getBefore().get(0).getQuestion()).isEqualTo("제목 질문?");
        assertThat(result.getDuring()).hasSize(1);
        assertThat(result.getDuring().get(0).getQuestion()).isEqualTo("생각 질문?");
        // after는 passed=true인 것만 후보로 남는다(passed=false는 제외)
        assertThat(result.getAfter()).hasSize(1);
        assertThat(result.getAfter().get(0).getQuestion()).isEqualTo("간추리기 질문?");
    }

    /* 검증: 완독한 기록만 나의 독서 보관함 목록에 최신순으로 표시된다 */
    @Test
    void getArchive_returnsFinishedRecordsSortedByFinishedAtDesc() {
        ReadingRecord older = buildRecord(10L, student, buildBook(1L, "먼저 읽은 책", "작가1"));
        older.setFinishedAt(java.time.LocalDateTime.of(2026, 7, 1, 10, 0));
        older.setRating(3);

        ReadingRecord newer = buildRecord(20L, student, buildBook(2L, "나중에 읽은 책", "작가2"));
        newer.setFinishedAt(java.time.LocalDateTime.of(2026, 7, 28, 10, 0));

        when(readingRecordRepository.findByStudent_IdAndFinishedAtIsNotNull(STUDENT_ID))
            .thenReturn(List.of(older, newer));

        List<IndividualReadingArchiveItem> result = service.getArchive(STUDENT_ID);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getBookTitle()).isEqualTo("나중에 읽은 책");
        assertThat(result.get(1).getBookTitle()).isEqualTo("먼저 읽은 책");
        assertThat(result.get(1).getRating()).isEqualTo(3);
    }

    /* 검증: 아직 별점·대표 질문이 없는 과거 완독 기록도 목록에서 제외되지 않고 null로 표시된다 */
    @Test
    void getArchive_showsNullRatingAndRepresentativeForLegacyRecordsWithoutData() {
        ReadingRecord legacy = buildRecord(8L, student, buildBook(1L, "예전에 완독한 책", "작가"));
        legacy.setFinishedAt(java.time.LocalDateTime.of(2026, 6, 1, 10, 0));

        when(readingRecordRepository.findByStudent_IdAndFinishedAtIsNotNull(STUDENT_ID))
            .thenReturn(List.of(legacy));

        List<IndividualReadingArchiveItem> result = service.getArchive(STUDENT_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRating()).isNull();
        assertThat(result.get(0).getRepresentativeQuestion()).isNull();
    }

    /* 검증: 보관함 상세는 읽기 전·중·후 전체 기록과 최종 간추리기를 함께 반환한다 */
    @Test
    void getArchiveDetail_returnsFullRecordsAndSummary() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        record.setFinishedAt(java.time.LocalDateTime.of(2026, 7, 28, 10, 0));
        record.setRating(5);
        Response representative = buildAfterResponseEntity(3L, student, record, 1, "간추리기 질문?", "간추리기 답", true);
        record.setRepresentResponse(representative);

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        when(responseRepository
                .findByStudent_IdAndReadingRecord_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                    STUDENT_ID, 10L, "individual", "answer", "before"))
            .thenReturn(List.of(buildBeforeResponseEntity(1L, student, record, "title", "제목 질문?", "제목 답")));
        when(responseRepository
                .findByStudent_IdAndReadingRecord_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                    STUDENT_ID, 10L, "individual", "answer", "during"))
            .thenReturn(List.of());
        when(responseRepository
                .findByStudent_IdAndReadingRecord_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                    STUDENT_ID, 10L, "individual", "answer", "after"))
            .thenReturn(List.of(representative));
        when(summaryRepository.findByStudent_IdAndReadingRecord_Id(STUDENT_ID, 10L))
            .thenReturn(Optional.of(buildSummary(500L, student, record, "story", "최종 간추린 내용", true)));

        IndividualReadingArchiveDetailResponse result = service.getArchiveDetail(STUDENT_ID, 10L);

        assertThat(result.getRating()).isEqualTo(5);
        assertThat(result.getRepresentativeQuestion()).isEqualTo("간추리기 질문?");
        assertThat(result.getRepresentativeAnswer()).isEqualTo("간추리기 답");
        assertThat(result.getRepresentativeCategory()).isEqualTo("after");
        assertThat(result.getBeforeResponses()).hasSize(1);
        assertThat(result.getAfterResponses()).hasSize(1);
        assertThat(result.getSummaryText()).isEqualTo("최종 간추린 내용");
        assertThat(result.getSummaryAiPassed()).isTrue();
    }

    /* 검증: 다른 학생의 보관함 상세는 조회할 수 없다(403) */
    @Test
    void getArchiveDetail_throwsForbiddenForOtherStudentsRecord() {
        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.empty());
        when(readingRecordRepository.findById(10L)).thenReturn(
            Optional.of(buildRecord(10L, otherStudent(), buildBook(1L, "책", "작가"))));

        assertThatThrownBy(() -> service.getArchiveDetail(STUDENT_ID, 10L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("403");
    }

    private IndividualReadingFinishRequest buildFinishRequest(Integer rating, Long representativeResponseId) {
        IndividualReadingFinishRequest request = new IndividualReadingFinishRequest();
        setField(request, "rating", rating);
        setField(request, "representativeResponseId", representativeResponseId);
        return request;
    }

    private User otherStudent() {
        User other = new User();
        other.setId(OTHER_STUDENT_ID);
        other.setName("다른학생");
        other.setRole("student");
        return other;
    }

    private IndividualReadingPagesRequest buildPagesRequest(int totalPages, int currentPage) {
        IndividualReadingPagesRequest request = new IndividualReadingPagesRequest();
        setField(request, "totalPages", totalPages);
        setField(request, "currentPage", currentPage);
        return request;
    }

    private IndividualBeforeResponseSaveRequest buildBeforeSaveRequest(String question, String answer) {
        IndividualBeforeResponseSaveRequest request = new IndividualBeforeResponseSaveRequest();
        setField(request, "question", question);
        setField(request, "answer", answer);
        return request;
    }

    private Response buildBeforeResponseEntity(
            Long id, User owner, ReadingRecord record, String stepType, String question, String answer) {

        Response response = new Response();
        response.setId(id);
        response.setStudent(owner);
        response.setReadingRecord(record);
        response.setMode("individual");
        response.setContentType("answer");
        response.setStage("before");
        response.setContent(answer);
        response.setExtraData(new java.util.HashMap<>(Map.of("stepType", stepType, "question", question)));
        return response;
    }

    /* 검증 1: title 최초 저장 성공 */
    @Test
    void saveBeforeResponse_createsNewResponseForTitleStep() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "아몬드", "손원평"));

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        when(responseRepository
                .findByStudent_IdAndReadingRecord_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                    STUDENT_ID, 10L, "individual", "answer", "before"))
            .thenReturn(List.of());
        when(responseRepository.save(any(Response.class))).thenAnswer(invocation -> {
            Response toSave = invocation.getArgument(0);
            toSave.setId(1000L);
            return toSave;
        });

        IndividualBeforeResponseItem result = service.saveBeforeResponse(
            STUDENT_ID, 10L, "title", buildBeforeSaveRequest("제목 질문?", "제목 답"));

        assertThat(result.getResponseId()).isEqualTo(1000L);
        assertThat(result.getReadingRecordId()).isEqualTo(10L);
        assertThat(result.getStepType()).isEqualTo("title");
        assertThat(result.getQuestion()).isEqualTo("제목 질문?");
        assertThat(result.getAnswer()).isEqualTo("제목 답");
    }

    /* 검증 2: 같은 readingRecordId + title 재저장 시 같은 행 UPDATE */
    @Test
    void saveBeforeResponse_updatesExistingRowForSameReadingRecordAndStepType() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "아몬드", "손원평"));
        Response existing = buildBeforeResponseEntity(1000L, student, record, "title", "원래 질문", "원래 답");

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        when(responseRepository
                .findByStudent_IdAndReadingRecord_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                    STUDENT_ID, 10L, "individual", "answer", "before"))
            .thenReturn(List.of(existing));
        when(responseRepository.save(any(Response.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IndividualBeforeResponseItem result = service.saveBeforeResponse(
            STUDENT_ID, 10L, "title", buildBeforeSaveRequest("수정된 질문", "수정된 답"));

        assertThat(result.getResponseId()).isEqualTo(1000L);
        assertThat(result.getQuestion()).isEqualTo("수정된 질문");
        assertThat(result.getAnswer()).isEqualTo("수정된 답");

        verify(responseRepository, org.mockito.Mockito.times(1)).save(any(Response.class));
    }

    /* 검증 3: 같은 readingRecordId의 다른 stepType은 별도 저장(기존 title 행을 건드리지 않음) */
    @Test
    void saveBeforeResponse_createsSeparateRowForDifferentStepTypeSameRecord() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "아몬드", "손원평"));
        Response titleResponse = buildBeforeResponseEntity(1000L, student, record, "title", "제목질문", "제목답");

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        when(responseRepository
                .findByStudent_IdAndReadingRecord_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                    STUDENT_ID, 10L, "individual", "answer", "before"))
            .thenReturn(List.of(titleResponse));
        when(responseRepository.save(any(Response.class))).thenAnswer(invocation -> {
            Response toSave = invocation.getArgument(0);
            if (toSave.getId() == null) {
                toSave.setId(2000L);
            }
            return toSave;
        });

        IndividualBeforeResponseItem result = service.saveBeforeResponse(
            STUDENT_ID, 10L, "contents", buildBeforeSaveRequest("차례질문", "차례답"));

        assertThat(result.getResponseId()).isEqualTo(2000L);
        assertThat(result.getStepType()).isEqualTo("contents");
        // 기존 title 행은 그대로 유지되어야 한다(내용이 안 바뀜)
        assertThat(titleResponse.getContent()).isEqualTo("제목답");
    }

    /* 검증 4/5: 다른 readingRecordId(같은 bookId 재독 포함)의 같은 stepType은 완전히 분리되어 저장/조회된다 */
    @Test
    void getBeforeResponses_doesNotMixDataBetweenDifferentReadingRecordsOfSameBook() {
        Book sameBook = buildBook(1L, "아몬드", "손원평");
        ReadingRecord firstRead = buildRecord(10L, student, sameBook);
        ReadingRecord secondRead = buildRecord(20L, student, sameBook);

        Response firstReadTitle = buildBeforeResponseEntity(1000L, student, firstRead, "title", "1회독 질문", "1회독 답");
        Response secondReadTitle = buildBeforeResponseEntity(2000L, student, secondRead, "title", "2회독 질문", "2회독 답");

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(firstRead));
        when(readingRecordRepository.findByIdAndStudent_Id(20L, STUDENT_ID)).thenReturn(Optional.of(secondRead));

        when(responseRepository
                .findByStudent_IdAndReadingRecord_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                    STUDENT_ID, 10L, "individual", "answer", "before"))
            .thenReturn(List.of(firstReadTitle));
        when(responseRepository
                .findByStudent_IdAndReadingRecord_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                    STUDENT_ID, 20L, "individual", "answer", "before"))
            .thenReturn(List.of(secondReadTitle));

        List<IndividualBeforeResponseItem> firstReadResult = service.getBeforeResponses(STUDENT_ID, 10L);
        List<IndividualBeforeResponseItem> secondReadResult = service.getBeforeResponses(STUDENT_ID, 20L);

        assertThat(firstReadResult).hasSize(1);
        assertThat(firstReadResult.get(0).getQuestion()).isEqualTo("1회독 질문");
        assertThat(secondReadResult).hasSize(1);
        assertThat(secondReadResult.get(0).getQuestion()).isEqualTo("2회독 질문");
    }

    /* 검증 6: 다른 학생 readingRecord의 읽기 전 기록 저장 차단 */
    @Test
    void saveBeforeResponse_throwsForbiddenWhenRecordBelongsToAnotherStudent() {
        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.empty());
        when(readingRecordRepository.findById(10L)).thenReturn(
            Optional.of(buildRecord(10L, otherStudent(), buildBook(1L, "책", "작가"))));

        assertThatThrownBy(() ->
            service.saveBeforeResponse(STUDENT_ID, 10L, "title", buildBeforeSaveRequest("질문?", "답"))
        )
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("403");
    }

    /* 검증 7: 존재하지 않는 readingRecordId는 404 */
    @Test
    void getBeforeResponses_throwsNotFoundWhenRecordDoesNotExist() {
        when(readingRecordRepository.findByIdAndStudent_Id(999L, STUDENT_ID)).thenReturn(Optional.empty());
        when(readingRecordRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getBeforeResponses(STUDENT_ID, 999L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("404");
    }

    /* 검증 8: 완독 기록의 읽기 전 저장(수정) 차단 */
    @Test
    void saveBeforeResponse_throwsConflictWhenRecordAlreadyCompleted() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        record.setFinishedAt(java.time.LocalDateTime.now());

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));

        assertThatThrownBy(() ->
            service.saveBeforeResponse(STUDENT_ID, 10L, "title", buildBeforeSaveRequest("질문?", "답"))
        )
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("409");

        verify(responseRepository, never()).save(any(Response.class));
    }

    /* 검증 9: 완독 기록도 읽기 전 질문·답 조회는 허용된다(추후 보관함용) */
    @Test
    void getBeforeResponses_allowsQueryOnCompletedRecord() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        record.setFinishedAt(java.time.LocalDateTime.now());
        Response saved = buildBeforeResponseEntity(1000L, student, record, "title", "질문", "답");

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        when(responseRepository
                .findByStudent_IdAndReadingRecord_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                    STUDENT_ID, 10L, "individual", "answer", "before"))
            .thenReturn(List.of(saved));

        List<IndividualBeforeResponseItem> result = service.getBeforeResponses(STUDENT_ID, 10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getQuestion()).isEqualTo("질문");
    }

    /*
     * 검증 10/11: 빈 질문·빈 답 거부. "차례 없음"(skipped=true) 기능
     * 추가로 question/answer의 @NotBlank를 DTO에서 빼고(skipped=true일
     * 때는 비어 있는 게 정상이므로) 서비스 레이어에서 skipped 여부에
     * 따라 조건부로 검증하도록 옮겼다 - 이 동작은
     * saveBeforeResponse_notSkipped_blankQuestion_throwsBadRequest /
     * saveBeforeResponse_notSkipped_blankAnswer_throwsBadRequest에서
     * 검증한다. Bean Validation 어노테이션이 빠졌으므로 DTO 자체에는
     * 더 이상 이 제약이 없다는 것만 확인한다.
     */
    @Test
    void saveRequest_noLongerEnforcesNotBlankAtDtoLevel() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            IndividualBeforeResponseSaveRequest blankQuestion = buildBeforeSaveRequest("", "답");
            assertThat(validator.validate(blankQuestion)).isEmpty();

            IndividualBeforeResponseSaveRequest valid = buildBeforeSaveRequest("질문?", "답");
            assertThat(validator.validate(valid)).isEmpty();
        }
    }

    /* 검증 12: 일부 단계만 저장되면 beforeDone=false, currentStage=before 유지 */
    @Test
    void saveBeforeResponse_keepsBeforeNotDoneWhenOnlySomeStepsSaved() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        when(responseRepository
                .findByStudent_IdAndReadingRecord_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                    eq(STUDENT_ID), eq(10L), eq("individual"), eq("answer"), eq("before")))
            .thenReturn(List.of())
            .thenReturn(List.of(buildBeforeResponseEntity(1000L, student, record, "title", "질문", "답")));
        when(responseRepository.save(any(Response.class))).thenAnswer(invocation -> {
            Response toSave = invocation.getArgument(0);
            toSave.setId(1000L);
            return toSave;
        });

        service.saveBeforeResponse(STUDENT_ID, 10L, "title", buildBeforeSaveRequest("질문", "답"));

        assertThat(record.getBeforeDone()).isFalse();
        assertThat(record.getCurrentStage()).isEqualTo("before");
        verify(readingRecordRepository, never()).save(any(ReadingRecord.class));
    }

    /* 검증 13: 4개 단계(title/contents/picture/skim) 저장 완료 시 beforeDone=true, currentStage=during */
    @Test
    void saveBeforeResponse_setsBeforeDoneTrueWhenAllFourStepsSaved() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));

        Response titleR = buildBeforeResponseEntity(1L, student, record, "title", "q1", "a1");
        Response contentsR = buildBeforeResponseEntity(2L, student, record, "contents", "q2", "a2");
        Response pictureR = buildBeforeResponseEntity(3L, student, record, "picture", "q3", "a3");
        Response skimR = buildBeforeResponseEntity(4L, student, record, "skim", "q4", "a4");

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        when(responseRepository
                .findByStudent_IdAndReadingRecord_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                    eq(STUDENT_ID), eq(10L), eq("individual"), eq("answer"), eq("before")))
            .thenReturn(List.of(titleR, contentsR, pictureR))
            .thenReturn(List.of(titleR, contentsR, pictureR, skimR));
        when(responseRepository.save(any(Response.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(readingRecordRepository.save(any(ReadingRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.saveBeforeResponse(STUDENT_ID, 10L, "skim", buildBeforeSaveRequest("q4", "a4"));

        assertThat(record.getBeforeDone()).isTrue();
        assertThat(record.getCurrentStage()).isEqualTo("during");
        verify(readingRecordRepository, org.mockito.Mockito.times(1)).save(record);
    }

    /* 검증 16: AI good/need 판정·피드백·evaluationKey가 DB(Response)에 저장되지 않음 */
    @Test
    void saveBeforeResponse_neverStoresAiResultOrEvaluationKey() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        when(responseRepository
                .findByStudent_IdAndReadingRecord_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                    STUDENT_ID, 10L, "individual", "answer", "before"))
            .thenReturn(List.of());

        org.mockito.ArgumentCaptor<Response> captor = org.mockito.ArgumentCaptor.forClass(Response.class);
        when(responseRepository.save(captor.capture())).thenAnswer(invocation -> {
            Response toSave = invocation.getArgument(0);
            toSave.setId(1000L);
            return toSave;
        });

        service.saveBeforeResponse(STUDENT_ID, 10L, "title", buildBeforeSaveRequest("질문?", "답"));

        Response saved = captor.getValue();
        assertThat(saved.getPassed()).isNull();
        assertThat(saved.getExtraData()).containsOnlyKeys("stepType", "question", "skipped");
        assertThat(saved.getExtraData()).doesNotContainKey("evaluationKey");
        assertThat(saved.getExtraData()).doesNotContainKey("status");
        assertThat(saved.getExtraData()).doesNotContainKey("aiFeedback");
    }

    /* 검증: 읽기 전 3단계까지만 저장되면 보상 서비스가 아예 호출되지 않는다 */
    @Test
    void saveBeforeResponse_doesNotGrantRewardWhenOnlyThreeStepsSaved() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        when(responseRepository
                .findByStudent_IdAndReadingRecord_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                    eq(STUDENT_ID), eq(10L), eq("individual"), eq("answer"), eq("before")))
            .thenReturn(List.of())
            .thenReturn(List.of(buildBeforeResponseEntity(1000L, student, record, "title", "질문", "답")));
        when(responseRepository.save(any(Response.class))).thenAnswer(invocation -> {
            Response toSave = invocation.getArgument(0);
            toSave.setId(1000L);
            return toSave;
        });

        IndividualBeforeResponseItem result = service.saveBeforeResponse(
            STUDENT_ID, 10L, "title", buildBeforeSaveRequest("질문", "답"));

        assertThat(result.isRewardGranted()).isFalse();
        assertThat(result.getStats()).isNull();
        verify(beforeReadingRewardService, never())
            .grantBeforeCompleteRewardOnce(any(User.class), any(Long.class));
    }

    /* 검증: 읽기 전 4단계 최초 완료 시 보상 서비스가 정확한 학생·readingRecordId로 정확히 1번 호출되고, 응답에 반영된다 */
    @Test
    void saveBeforeResponse_grantsRewardOnceWhenAllFourStepsFirstCompleted() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));

        Response titleR = buildBeforeResponseEntity(1L, student, record, "title", "q1", "a1");
        Response contentsR = buildBeforeResponseEntity(2L, student, record, "contents", "q2", "a2");
        Response pictureR = buildBeforeResponseEntity(3L, student, record, "picture", "q3", "a3");
        Response skimR = buildBeforeResponseEntity(4L, student, record, "skim", "q4", "a4");

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        when(responseRepository
                .findByStudent_IdAndReadingRecord_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                    eq(STUDENT_ID), eq(10L), eq("individual"), eq("answer"), eq("before")))
            .thenReturn(List.of(titleR, contentsR, pictureR))
            .thenReturn(List.of(titleR, contentsR, pictureR, skimR));
        when(responseRepository.save(any(Response.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(readingRecordRepository.save(any(ReadingRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentStats stats = new StudentStats();
        stats.setStudent(student);
        stats.setMagic(9);
        stats.setStamina(8);
        stats.setWisdom(9);
        stats.setCourage(8);

        when(beforeReadingRewardService.grantBeforeCompleteRewardOnce(student, 10L))
            .thenReturn(new IndividualBeforeReadingRewardService.RewardResult(true, false, stats));

        IndividualBeforeResponseItem result = service.saveBeforeResponse(
            STUDENT_ID, 10L, "skim", buildBeforeSaveRequest("q4", "a4"));

        assertThat(result.isRewardGranted()).isTrue();
        assertThat(result.getStats().getMagic()).isEqualTo(9);
        assertThat(result.getStats().getWisdom()).isEqualTo(9);
        verify(beforeReadingRewardService, org.mockito.Mockito.times(1))
            .grantBeforeCompleteRewardOnce(student, 10L);
    }

    /*
     * 검증: 이미 beforeDone=true인 기록에 같은 stepType을 다시 저장해도(동일 API
     * 재호출·새로고침 후 재실행 시나리오) allStepsSaved는 true지만 beforeDone이
     * 이미 true라 전환 자체가 일어나지 않으므로 보상 서비스가 다시 호출되지 않는다.
     */
    @Test
    void saveBeforeResponse_doesNotCallRewardServiceAgainWhenBeforeDoneAlreadyTrue() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        record.setBeforeDone(true);
        record.setCurrentStage("during");

        Response titleR = buildBeforeResponseEntity(1L, student, record, "title", "q1", "a1");
        Response contentsR = buildBeforeResponseEntity(2L, student, record, "contents", "q2", "a2");
        Response pictureR = buildBeforeResponseEntity(3L, student, record, "picture", "q3", "a3");
        Response skimR = buildBeforeResponseEntity(4L, student, record, "skim", "q4", "a4");

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        when(responseRepository
                .findByStudent_IdAndReadingRecord_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                    eq(STUDENT_ID), eq(10L), eq("individual"), eq("answer"), eq("before")))
            .thenReturn(List.of(titleR, contentsR, pictureR, skimR));
        when(responseRepository.save(any(Response.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IndividualBeforeResponseItem result = service.saveBeforeResponse(
            STUDENT_ID, 10L, "title", buildBeforeSaveRequest("q1-수정", "a1-수정"));

        assertThat(result.isRewardGranted()).isFalse();
        assertThat(result.getStats()).isNull();
        verify(beforeReadingRewardService, never())
            .grantBeforeCompleteRewardOnce(any(User.class), any(Long.class));
        verify(readingRecordRepository, never()).save(any(ReadingRecord.class));
    }

    // =========================================================
    // 개별읽기 읽기 전 "차례 없음"(skipped) 처리
    // =========================================================

    private IndividualBeforeResponseSaveRequest buildBeforeSkipRequest() {
        IndividualBeforeResponseSaveRequest request = new IndividualBeforeResponseSaveRequest();
        setField(request, "question", "");
        setField(request, "answer", "");
        setField(request, "skipped", true);
        return request;
    }

    /* 차례 없음: 질문/답 없이도 저장되고, 실제 텍스트를 가짜로 채우지 않음 */
    @Test
    void saveBeforeResponse_skippedContents_storesEmptyQuestionAndAnswer() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "아몬드", "손원평"));

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        when(responseRepository
                .findByStudent_IdAndReadingRecord_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                    STUDENT_ID, 10L, "individual", "answer", "before"))
            .thenReturn(List.of());
        when(responseRepository.save(any(Response.class))).thenAnswer(invocation -> {
            Response toSave = invocation.getArgument(0);
            toSave.setId(2000L);
            return toSave;
        });

        IndividualBeforeResponseItem result = service.saveBeforeResponse(
            STUDENT_ID, 10L, "contents", buildBeforeSkipRequest());

        assertThat(result.getQuestion()).isEmpty();
        assertThat(result.getAnswer()).isEmpty();
        assertThat(result.isSkipped()).isTrue();
    }

    /* 차례 없음은 "차례 없음"/"건너뜀" 같은 문자열을 학생의 질문·답으로 저장하지 않음 */
    @Test
    void saveBeforeResponse_skippedContents_neverStoresPlaceholderTextAsRealAnswer() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "아몬드", "손원평"));

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        when(responseRepository
                .findByStudent_IdAndReadingRecord_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                    STUDENT_ID, 10L, "individual", "answer", "before"))
            .thenReturn(List.of());

        org.mockito.ArgumentCaptor<Response> captor = org.mockito.ArgumentCaptor.forClass(Response.class);
        when(responseRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        service.saveBeforeResponse(STUDENT_ID, 10L, "contents", buildBeforeSkipRequest());

        Response saved = captor.getValue();
        assertThat(saved.getContent()).isEmpty();
        assertThat(saved.getExtraData().get("question")).isEqualTo("");
        assertThat(saved.getExtraData().get("skipped")).isEqualTo(true);
    }

    /* 일반 저장(회귀): skipped가 아니면 여전히 질문/답이 비어 있으면 400 */
    @Test
    void saveBeforeResponse_notSkipped_blankQuestion_throwsBadRequest() {
        assertThatThrownBy(() ->
            service.saveBeforeResponse(STUDENT_ID, 10L, "contents", buildBeforeSaveRequest("", "답만 있음")))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400");
    }

    @Test
    void saveBeforeResponse_notSkipped_blankAnswer_throwsBadRequest() {
        assertThatThrownBy(() ->
            service.saveBeforeResponse(STUDENT_ID, 10L, "contents", buildBeforeSaveRequest("질문만 있음", "")))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400");
    }

    /* 차례 없음 취소 후 실제 질문·답으로 다시 저장하면 skipped=false로 완전히 덮어써짐(동일 stepType 행 재사용) */
    @Test
    void saveBeforeResponse_realAnswerAfterSkip_overwritesSkippedState() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "아몬드", "손원평"));
        Response existingSkipped = buildBeforeResponseEntity(2000L, student, record, "contents", "", "");
        existingSkipped.setExtraData(new java.util.HashMap<>(
            Map.of("stepType", "contents", "question", "", "skipped", true)));

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        when(responseRepository
                .findByStudent_IdAndReadingRecord_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                    STUDENT_ID, 10L, "individual", "answer", "before"))
            .thenReturn(new java.util.ArrayList<>(List.of(existingSkipped)));
        when(responseRepository.save(any(Response.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IndividualBeforeResponseItem result = service.saveBeforeResponse(
            STUDENT_ID, 10L, "contents", buildBeforeSaveRequest("차례를 보니 어떤 일이 생길까?", "모험을 떠날 것 같다."));

        assertThat(result.isSkipped()).isFalse();
        assertThat(result.getQuestion()).isEqualTo("차례를 보니 어떤 일이 생길까?");
        assertThat(result.getAnswer()).isEqualTo("모험을 떠날 것 같다.");
    }

    /* 차례 없음도 4단계 완료 판정에 정상 포함되어 보상이 지급됨(가짜 아님) */
    @Test
    void saveBeforeResponse_allFourStepsWithContentsSkipped_grantsRewardOnce() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "아몬드", "손원평"));
        Response titleR = buildBeforeResponseEntity(1L, student, record, "title", "q1", "a1");
        Response contentsSkipped = buildBeforeResponseEntity(2L, student, record, "contents", "", "");
        contentsSkipped.setExtraData(new java.util.HashMap<>(
            Map.of("stepType", "contents", "question", "", "skipped", true)));
        Response pictureR = buildBeforeResponseEntity(3L, student, record, "picture", "q3", "a3");
        Response skimR = buildBeforeResponseEntity(4L, student, record, "skim", "q4", "a4");

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        when(responseRepository
                .findByStudent_IdAndReadingRecord_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                    eq(STUDENT_ID), eq(10L), eq("individual"), eq("answer"), eq("before")))
            .thenReturn(List.of(titleR, contentsSkipped, pictureR))
            .thenReturn(List.of(titleR, contentsSkipped, pictureR, skimR));
        when(responseRepository.save(any(Response.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(readingRecordRepository.save(any(ReadingRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IndividualBeforeReadingRewardService.RewardResult rewardResult =
            mock(IndividualBeforeReadingRewardService.RewardResult.class);
        when(rewardResult.isRewardGranted()).thenReturn(true);
        when(beforeReadingRewardService.grantBeforeCompleteRewardOnce(any(User.class), eq(10L)))
            .thenReturn(rewardResult);

        IndividualBeforeResponseItem result = service.saveBeforeResponse(
            STUDENT_ID, 10L, "skim", buildBeforeSaveRequest("q4", "a4"));

        assertThat(result.isRewardGranted()).isTrue();
        verify(beforeReadingRewardService, org.mockito.Mockito.times(1))
            .grantBeforeCompleteRewardOnce(any(User.class), eq(10L));
    }

    // =========================================================
    // 읽기 중 - 나의 책 진행 상황 (ReadingProgressLog)
    // =========================================================

    private IndividualDuringResponseSaveRequest buildDuringSaveRequest(
            String questionType, String question, String answer, Integer currentPage) {
        IndividualDuringResponseSaveRequest request = new IndividualDuringResponseSaveRequest();
        setField(request, "questionType", questionType);
        setField(request, "question", question);
        setField(request, "answer", answer);
        setField(request, "currentPage", currentPage);
        return request;
    }

    private Response buildDuringResponseEntity(
            Long id, User owner, ReadingRecord record, Integer questionSlot, String questionType,
            String question, String answer, LocalDate date) {

        Response response = new Response();
        response.setId(id);
        response.setStudent(owner);
        response.setReadingRecord(record);
        response.setMode("individual");
        response.setContentType("answer");
        response.setStage("during");
        response.setContent(answer);
        response.setActivityDate(date);

        Map<String, Object> extraData = new java.util.HashMap<>();
        extraData.put("questionSlot", questionSlot);
        extraData.put("questionType", questionType);
        extraData.put("question", question);
        response.setExtraData(extraData);
        return response;
    }

    /* 오늘 이미 저장된 슬롯들(findDuringResponsesByDate 조회 결과)을 목킹하는 헬퍼 */
    private void mockTodayDuringResponses(Long readingRecordId, LocalDate date, List<Response> existing) {
        when(responseRepository
                .findByStudent_IdAndReadingRecord_IdAndModeAndContentTypeAndStageAndActivityDateAndDeletedAtIsNullOrderByIdAsc(
                    STUDENT_ID, readingRecordId, "individual", "answer", "during", date))
            .thenReturn(existing);
    }

    /* 검증: 정상 쪽수 저장 시 오늘 날짜로 ReadingProgressLog가 upsert된다 */
    @Test
    void updatePages_upsertsTodayProgressLog() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        when(readingRecordRepository.save(any(ReadingRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(readingProgressLogRepository.findByStudent_IdAndReadingRecord_IdAndLogDate(
                eq(STUDENT_ID), eq(10L), any(LocalDate.class)))
            .thenReturn(Optional.empty());
        when(readingProgressLogRepository.save(any(ReadingProgressLog.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        IndividualReadingRecordResponse response = service.updatePages(STUDENT_ID, 10L, buildPagesRequest(100, 25));

        assertThat(response.getTodayReadPages()).isEqualTo(25);
        assertThat(response.getProgressPercent()).isEqualTo(25);
        assertThat(response.getReadingDate()).isEqualTo(LocalDate.now(java.time.ZoneId.of("Asia/Seoul")));

        org.mockito.ArgumentCaptor<ReadingProgressLog> captor =
            org.mockito.ArgumentCaptor.forClass(ReadingProgressLog.class);
        verify(readingProgressLogRepository).save(captor.capture());
        assertThat(captor.getValue().getCumulativePage()).isEqualTo(25);
        assertThat(captor.getValue().getTotalPages()).isEqualTo(100);
    }

    /* 검증 3: currentPage가 이전 기록보다 작아지면 거부 */
    @Test
    void updatePages_rejectsDecreasingCurrentPage() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        record.setTotalPages(100);
        record.setCurrentPage(50);

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.updatePages(STUDENT_ID, 10L, buildPagesRequest(100, 30)))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400");

        verify(readingRecordRepository, never()).save(any(ReadingRecord.class));
        verify(readingProgressLogRepository, never()).save(any(ReadingProgressLog.class));
    }

    /* 검증 5: 같은 날 여러 번 저장해도 오늘 로그 한 행만 UPSERT되고, cumulativePage가 최신값으로만 덮인다(중복 합산 없음) */
    @Test
    void updatePages_sameDayMultipleSavesOverwriteInsteadOfAccumulating() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        record.setTotalPages(100);
        record.setCurrentPage(10);

        ReadingProgressLog existingTodayLog = new ReadingProgressLog();
        existingTodayLog.setId(1L);
        existingTodayLog.setStudent(student);
        existingTodayLog.setReadingRecord(record);
        existingTodayLog.setLogDate(LocalDate.now(java.time.ZoneId.of("Asia/Seoul")));
        existingTodayLog.setCumulativePage(10);
        existingTodayLog.setTotalPages(100);

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        when(readingRecordRepository.save(any(ReadingRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(readingProgressLogRepository.findByStudent_IdAndReadingRecord_IdAndLogDate(
                eq(STUDENT_ID), eq(10L), any(LocalDate.class)))
            .thenReturn(Optional.of(existingTodayLog));
        when(readingProgressLogRepository.save(any(ReadingProgressLog.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // 오후에 25까지 읽었다고 다시 저장 (오전 10 -> 오후 25, 35로 합산되면 안 됨)
        IndividualReadingRecordResponse response = service.updatePages(STUDENT_ID, 10L, buildPagesRequest(100, 25));

        assertThat(response.getTodayReadPages()).isEqualTo(25);

        org.mockito.ArgumentCaptor<ReadingProgressLog> captor =
            org.mockito.ArgumentCaptor.forClass(ReadingProgressLog.class);
        verify(readingProgressLogRepository).save(captor.capture());
        // 같은 로그 행(id=1)을 재사용하고, cumulativePage는 25로 덮인다(10+25=35가 아님)
        assertThat(captor.getValue().getId()).isEqualTo(1L);
        assertThat(captor.getValue().getCumulativePage()).isEqualTo(25);
    }

    /* 검증 6: 다음 날 저장하면 직전 날짜 누적값을 기준으로 오늘 읽은 쪽수를 계산하고, 별도 로그 행을 새로 만든다 */
    @Test
    void updatePages_createsSeparateLogOnNextDay() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        record.setTotalPages(100);
        record.setCurrentPage(25);

        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));

        // 오늘 날짜 로그는 아직 없음(오늘 처음 저장)
        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        when(readingRecordRepository.save(any(ReadingRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(readingProgressLogRepository.findByStudent_IdAndReadingRecord_IdAndLogDate(STUDENT_ID, 10L, today))
            .thenReturn(Optional.empty());
        when(readingProgressLogRepository.save(any(ReadingProgressLog.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // 어제까지 25쪽 읽었었다는 로그가 있다고 가정
        ReadingProgressLog yesterdayLog = new ReadingProgressLog();
        yesterdayLog.setId(2L);
        yesterdayLog.setCumulativePage(25);
        when(readingProgressLogRepository
                .findTopByStudent_IdAndReadingRecord_IdAndLogDateLessThanOrderByLogDateDesc(STUDENT_ID, 10L, today))
            .thenReturn(Optional.of(yesterdayLog));

        // 오늘 40쪽까지 읽었다고 저장
        IndividualReadingRecordResponse response = service.updatePages(STUDENT_ID, 10L, buildPagesRequest(100, 40));

        // 오늘 읽은 쪽수 = 40(오늘 누적) - 25(어제까지 누적) = 15
        assertThat(response.getTodayReadPages()).isEqualTo(15);

        org.mockito.ArgumentCaptor<ReadingProgressLog> captor =
            org.mockito.ArgumentCaptor.forClass(ReadingProgressLog.class);
        verify(readingProgressLogRepository).save(captor.capture());
        // 어제 로그(id=2)를 덮어쓰지 않고 새 행(id 없음)을 만든다
        assertThat(captor.getValue().getId()).isNull();
        assertThat(captor.getValue().getCumulativePage()).isEqualTo(40);
    }

    // =========================================================
    // 읽기 중 - 책속 생각쓰기 (during-responses, 슬롯 1/2/3 기반)
    // =========================================================

    /* 검증: 지원하지 않는 questionSlot(범위 밖)은 400 */
    @Test
    void saveDuringResponse_throwsBadRequestForUnknownQuestionSlot() {
        assertThatThrownBy(() ->
            service.saveDuringResponse(STUDENT_ID, 10L, 4, buildDuringSaveRequest("find", "q", "a", null))
        )
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400");
    }

    /* 검증: 지원하지 않는 questionType은 400 */
    @Test
    void saveDuringResponse_throwsBadRequestForUnknownQuestionType() {
        assertThatThrownBy(() ->
            service.saveDuringResponse(STUDENT_ID, 10L, 1, buildDuringSaveRequest("unknown", "q", "a", null))
        )
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400");
    }

    /* 검증: 완독된 책에는 읽기 중 활동 추가 불가 */
    @Test
    void saveDuringResponse_throwsConflictWhenRecordAlreadyFinished() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        record.setFinishedAt(java.time.LocalDateTime.now());

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));

        assertThatThrownBy(() ->
            service.saveDuringResponse(STUDENT_ID, 10L, 1, buildDuringSaveRequest("find", "q", "a", null))
        )
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("409");
    }

    /* 검증 1: 오늘 슬롯 1개만 저장하면 보상 없음 */
    @Test
    void saveDuringResponse_savingFirstSlotOnly_grantsNoReward() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        // 슬롯 1 저장을 위한 조회(비어있음)와, 저장 후 재조회(슬롯 1만 존재)를 순서대로 반환한다.
        mockTodayDuringResponses(10L, today, List.of());
        when(responseRepository.save(any(Response.class))).thenAnswer(invocation -> {
            Response toSave = invocation.getArgument(0);
            toSave.setId(500L);
            return toSave;
        });

        IndividualDuringResponseItem result = service.saveDuringResponse(
            STUDENT_ID, 10L, 1, buildDuringSaveRequest("find", "질문?", "답", null));

        assertThat(result.getQuestionSlot()).isEqualTo(1);
        assertThat(result.getQuestionType()).isEqualTo("find");
        assertThat(result.isRewardGranted()).isFalse();
        assertThat(result.getStats()).isNull();
        verify(duringReadingRewardService, never())
            .grantDuringDailyRewardOnce(any(User.class), any(Long.class), any(LocalDate.class));
    }

    /* 검증 2: 오늘 슬롯 2개까지만 저장해도 보상 없음 */
    @Test
    void saveDuringResponse_savingSecondSlot_stillGrantsNoReward() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));

        Response slot1 = buildDuringResponseEntity(701L, student, record, 1, "find", "q1", "a1", today);

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        // 슬롯 2를 저장하는 시점에는 슬롯 1만 이미 있고, 저장 이후 재조회해도 1·2뿐이다(3은 없음).
        mockTodayDuringResponses(10L, today, List.of(slot1));
        when(responseRepository.save(any(Response.class))).thenAnswer(invocation -> {
            Response toSave = invocation.getArgument(0);
            toSave.setId(702L);
            return toSave;
        });

        IndividualDuringResponseItem result = service.saveDuringResponse(
            STUDENT_ID, 10L, 2, buildDuringSaveRequest("infer", "q2", "a2", null));

        assertThat(result.getQuestionSlot()).isEqualTo(2);
        assertThat(result.isRewardGranted()).isFalse();
        verify(duringReadingRewardService, never())
            .grantDuringDailyRewardOnce(any(User.class), any(Long.class), any(LocalDate.class));
    }

    /* 검증 3: 세 번째 슬롯 저장이 성공해 오늘 1·2·3이 모두 채워지는 순간 최초 1회 보상 지급 */
    @Test
    void saveDuringResponse_savingThirdSlot_grantsRewardOnce() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));

        Response slot1 = buildDuringResponseEntity(701L, student, record, 1, "find", "q1", "a1", today);
        Response slot2 = buildDuringResponseEntity(702L, student, record, 2, "infer", "q2", "a2", today);

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        when(responseRepository.save(any(Response.class))).thenAnswer(invocation -> {
            Response toSave = invocation.getArgument(0);
            toSave.setId(703L);
            return toSave;
        });

        /*
          저장 로직이 "저장 전 오늘 목록"과 "저장 후 재조회한 오늘 목록"을
          각각 한 번씩 조회하므로, 첫 호출은 슬롯1·2만, 두 번째(재조회)
          호출은 방금 저장된 슬롯3까지 포함해서 반환하도록 순서대로 스텁한다.
        */
        Response slot3 = buildDuringResponseEntity(703L, student, record, 3, "feel", "q3", "a3", today);
        when(responseRepository
                .findByStudent_IdAndReadingRecord_IdAndModeAndContentTypeAndStageAndActivityDateAndDeletedAtIsNullOrderByIdAsc(
                    STUDENT_ID, 10L, "individual", "answer", "during", today))
            .thenReturn(List.of(slot1, slot2))
            .thenReturn(List.of(slot1, slot2, slot3));

        StudentStats stats = new StudentStats();
        stats.setStudent(student);
        stats.setMagic(9);
        stats.setStamina(8);
        stats.setWisdom(9);
        stats.setCourage(8);

        when(duringReadingRewardService.grantDuringDailyRewardOnce(student, 10L, today))
            .thenReturn(new IndividualDuringReadingRewardService.RewardResult(true, false, stats));

        IndividualDuringResponseItem result = service.saveDuringResponse(
            STUDENT_ID, 10L, 3, buildDuringSaveRequest("feel", "q3", "a3", null));

        assertThat(result.isRewardGranted()).isTrue();
        assertThat(result.getStats().getMagic()).isEqualTo(9);
        assertThat(result.getStats().getWisdom()).isEqualTo(9);
        verify(duringReadingRewardService, org.mockito.Mockito.times(1))
            .grantDuringDailyRewardOnce(student, 10L, today);
    }

    /* 검증 4: 이미 3개를 채운 뒤 세 번째 슬롯을 다시 저장해도 보상 서비스는 매번 호출되지만(멱등) 추가 지급은 없다 */
    @Test
    void saveDuringResponse_resavingThirdSlotAfterAllThreeDone_callsRewardServiceButNoAdditionalGrant() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));

        Response slot1 = buildDuringResponseEntity(701L, student, record, 1, "find", "q1", "a1", today);
        Response slot2 = buildDuringResponseEntity(702L, student, record, 2, "infer", "q2", "a2", today);
        Response slot3 = buildDuringResponseEntity(703L, student, record, 3, "feel", "q3(수정 전)", "a3", today);

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        mockTodayDuringResponses(10L, today, List.of(slot1, slot2, slot3));
        when(responseRepository.save(any(Response.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 이미 오늘 보상을 받은 상태 - 보상 서비스 자체가 알아서 재지급을 막는다(alreadyGranted).
        when(duringReadingRewardService.grantDuringDailyRewardOnce(student, 10L, today))
            .thenReturn(new IndividualDuringReadingRewardService.RewardResult(false, true, new StudentStats()));

        IndividualDuringResponseItem result = service.saveDuringResponse(
            STUDENT_ID, 10L, 3, buildDuringSaveRequest("feel", "q3(수정)", "a3(수정)", null));

        assertThat(result.isRewardGranted()).isFalse();
        verify(duringReadingRewardService, org.mockito.Mockito.times(1))
            .grantDuringDailyRewardOnce(student, 10L, today);
    }

    /* 검증 5: 이미 3개를 채운 뒤 첫 번째 슬롯을 수정해도 추가 보상 없음 */
    @Test
    void saveDuringResponse_editingFirstSlotAfterAllThreeDone_grantsNoAdditionalReward() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));

        Response slot1 = buildDuringResponseEntity(701L, student, record, 1, "find", "q1(수정 전)", "a1", today);
        Response slot2 = buildDuringResponseEntity(702L, student, record, 2, "infer", "q2", "a2", today);
        Response slot3 = buildDuringResponseEntity(703L, student, record, 3, "feel", "q3", "a3", today);

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        mockTodayDuringResponses(10L, today, List.of(slot1, slot2, slot3));
        when(responseRepository.save(any(Response.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(duringReadingRewardService.grantDuringDailyRewardOnce(student, 10L, today))
            .thenReturn(new IndividualDuringReadingRewardService.RewardResult(false, true, new StudentStats()));

        IndividualDuringResponseItem result = service.saveDuringResponse(
            STUDENT_ID, 10L, 1, buildDuringSaveRequest("find", "q1(수정)", "a1(수정)", null));

        assertThat(result.isRewardGranted()).isFalse();
        assertThat(result.getResponseId()).isEqualTo(701L);
    }

    /* 검증 6: 같은 질문 유형을 여러 슬롯에서 써도 각각 별도로 보존된다(슬롯 1과 슬롯 3 모두 find) */
    @Test
    void saveDuringResponse_sameQuestionTypeAcrossMultipleSlots_preservedSeparately() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));

        Response slot1Find = buildDuringResponseEntity(701L, student, record, 1, "find", "질문A", "답A", today);

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        // 슬롯 3 저장 시점에는 슬롯 1(find)만 있고, questionSlot=3으로 조회하므로 slot1Find와 매치되지 않는다.
        // (슬롯 1·3만 채워지므로 allDailySlotsFilled는 false - 보상 서비스는 호출되지 않는다)
        mockTodayDuringResponses(10L, today, List.of(slot1Find));

        org.mockito.ArgumentCaptor<Response> captor = org.mockito.ArgumentCaptor.forClass(Response.class);
        when(responseRepository.save(captor.capture())).thenAnswer(invocation -> {
            Response toSave = invocation.getArgument(0);
            toSave.setId(703L);
            return toSave;
        });

        // 슬롯 3에도 같은 유형(find)으로 저장한다 - 슬롯 1과는 별개 행이어야 한다.
        IndividualDuringResponseItem result = service.saveDuringResponse(
            STUDENT_ID, 10L, 3, buildDuringSaveRequest("find", "질문B", "답B", null));

        assertThat(result.getQuestionSlot()).isEqualTo(3);
        assertThat(result.getQuestionType()).isEqualTo("find");

        // 슬롯 1(701L)을 덮어쓴 게 아니라 새 Response 객체(슬롯 1과 다른 인스턴스)를 저장했는지 확인한다.
        assertThat(captor.getValue()).isNotSameAs(slot1Find);
        assertThat(result.getResponseId()).isEqualTo(703L);
    }

    /* 검증: 같은 날 같은 슬롯 재저장 시 새 행이 아니라 기존 행을 UPDATE */
    @Test
    void saveDuringResponse_updatesExistingRowForSameDaySameSlot() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));

        Response existing = buildDuringResponseEntity(700L, student, record, 1, "find", "원래 질문", "원래 답", today);

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        mockTodayDuringResponses(10L, today, List.of(existing));
        when(responseRepository.save(any(Response.class))).thenAnswer(invocation -> invocation.getArgument(0));
        // 슬롯 1개뿐이라 allDailySlotsFilled가 false이므로 보상 서비스는 아예 호출되지 않는다(스텁 불필요).

        IndividualDuringResponseItem result = service.saveDuringResponse(
            STUDENT_ID, 10L, 1, buildDuringSaveRequest("find", "수정된 질문", "수정된 답", null));

        assertThat(result.getResponseId()).isEqualTo(700L);
        assertThat(result.getQuestion()).isEqualTo("수정된 질문");
        assertThat(result.getAnswer()).isEqualTo("수정된 답");

        org.mockito.ArgumentCaptor<Response> captor = org.mockito.ArgumentCaptor.forClass(Response.class);
        verify(responseRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(700L);
    }

    /* 검증 13: AI 판정·시도 횟수 등은 저장되지 않는다(before와 동일 원칙) */
    @Test
    void saveDuringResponse_neverStoresAiResultFields() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        mockTodayDuringResponses(10L, today, List.of());

        org.mockito.ArgumentCaptor<Response> captor = org.mockito.ArgumentCaptor.forClass(Response.class);
        when(responseRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));
        // 슬롯 1개뿐이라 allDailySlotsFilled가 false이므로 보상 서비스는 아예 호출되지 않는다(스텁 불필요).

        service.saveDuringResponse(STUDENT_ID, 10L, 1, buildDuringSaveRequest("feel", "q", "a", null));

        Response saved = captor.getValue();
        assertThat(saved.getPassed()).isNull();
        assertThat(saved.getExtraData()).containsOnlyKeys("questionSlot", "questionType", "question");
    }

    /* 검증: currentPage가 함께 오고 기존보다 늘어났으면 반영, 늘지 않았으면 조용히 무시(에러 없음) */
    @Test
    void saveDuringResponse_bumpsCurrentPageOnlyWhenIncreased() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        record.setTotalPages(100);
        record.setCurrentPage(20);
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        mockTodayDuringResponses(10L, today, List.of());
        when(responseRepository.save(any(Response.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(readingRecordRepository.save(any(ReadingRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(readingProgressLogRepository.findByStudent_IdAndReadingRecord_IdAndLogDate(eq(STUDENT_ID), eq(10L), any()))
            .thenReturn(Optional.empty());
        when(readingProgressLogRepository.save(any(ReadingProgressLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        // 매번 슬롯 1~2개뿐이라 allDailySlotsFilled가 false이므로 보상 서비스는 호출되지 않는다(스텁 불필요).

        // 30 > 20 이므로 반영됨
        service.saveDuringResponse(STUDENT_ID, 10L, 1, buildDuringSaveRequest("find", "q", "a", 30));
        assertThat(record.getCurrentPage()).isEqualTo(30);

        // 25 < 30 이므로 무시(에러 없이 그대로 30 유지)
        service.saveDuringResponse(STUDENT_ID, 10L, 2, buildDuringSaveRequest("infer", "q2", "a2", 25));
        assertThat(record.getCurrentPage()).isEqualTo(30);
    }

    /* 검증 7: 오늘 기록 조회는 서버가 판단한 오늘 날짜와 오늘 저장된 슬롯만(새로고침 복원용) 돌려준다 */
    @Test
    void getDuringResponsesToday_returnsOnlyTodaySlotsWithServerDate() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));

        Response slot1 = buildDuringResponseEntity(1L, student, record, 1, "find", "q1", "a1", today);
        Response slot2 = buildDuringResponseEntity(2L, student, record, 2, "infer", "q2", "a2", today);
        Response slot3 = buildDuringResponseEntity(3L, student, record, 3, "feel", "q3", "a3", today);

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        mockTodayDuringResponses(10L, today, List.of(slot1, slot2, slot3));

        IndividualDuringTodayResponse response = service.getDuringResponsesToday(STUDENT_ID, 10L);

        assertThat(response.getServerDate()).isEqualTo(today);
        assertThat(response.getResponses()).hasSize(3);
        assertThat(response.getResponses()).extracting(IndividualDuringResponseItem::getQuestionSlot)
            .containsExactly(1, 2, 3);
    }

    /* 검증: 오늘 이전 날짜의 슬롯은 오늘의 일일 완료 판정에 포함되지 않는다 */
    @Test
    void saveDuringResponse_doesNotCountPreviousDaySlotsTowardTodayCompletion() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));

        // 오늘 목록 조회는 findByActivityDate(today)로 걸리므로, 어제 슬롯들은 애초에 이 목록에 안 잡힌다.
        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        mockTodayDuringResponses(10L, today, List.of());
        when(responseRepository.save(any(Response.class))).thenAnswer(invocation -> {
            Response toSave = invocation.getArgument(0);
            toSave.setId(900L);
            return toSave;
        });

        IndividualDuringResponseItem result = service.saveDuringResponse(
            STUDENT_ID, 10L, 1, buildDuringSaveRequest("find", "q", "a", null));

        assertThat(result.isRewardGranted()).isFalse();
        verify(duringReadingRewardService, never())
            .grantDuringDailyRewardOnce(any(User.class), any(Long.class), any(LocalDate.class));
    }

    /* 검증 12/14: 질문 보관함은 날짜 상관없이 전체 조회 가능하고, 완독 후에도 조회된다 */
    @Test
    void getDuringResponsesHistory_returnsAllDatesEvenAfterCompletion() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        record.setFinishedAt(java.time.LocalDateTime.now());

        LocalDate day1 = LocalDate.of(2026, 7, 1);
        LocalDate day2 = LocalDate.of(2026, 7, 2);

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        when(responseRepository
                .findByStudent_IdAndReadingRecord_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                    STUDENT_ID, 10L, "individual", "answer", "during"))
            .thenReturn(List.of(
                buildDuringResponseEntity(1L, student, record, 1, "find", "q1", "a1", day1),
                buildDuringResponseEntity(2L, student, record, 1, "infer", "q2", "a2", day2)
            ));

        List<IndividualDuringResponseItem> history = service.getDuringResponsesHistory(STUDENT_ID, 10L);

        assertThat(history).hasSize(2);
        assertThat(history).extracting(IndividualDuringResponseItem::getActivityDate)
            .containsExactly(day1, day2);
    }

    // =========================================================
    // 읽기 중 완료 (duringDone/currentStage=after)
    // =========================================================

    /* 검증 24: currentPage가 totalPages에 도달하지 않으면 거부 */
    @Test
    void completeDuringReading_rejectsWhenPageNotComplete() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        record.setTotalPages(100);
        record.setCurrentPage(50);

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        when(responseRepository
                .findByStudent_IdAndReadingRecord_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                    STUDENT_ID, 10L, "individual", "answer", "during"))
            .thenReturn(List.of(buildDuringResponseEntity(1L, student, record, 1, "find", "q", "a", LocalDate.now())));

        assertThatThrownBy(() -> service.completeDuringReading(STUDENT_ID, 10L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400");

        verify(readingRecordRepository, never()).save(any(ReadingRecord.class));
    }

    /* 검증 25: 읽기 중 질문이 하나도 없으면 거부 */
    @Test
    void completeDuringReading_rejectsWhenNoDuringResponseExists() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        record.setTotalPages(100);
        record.setCurrentPage(100);

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        when(responseRepository
                .findByStudent_IdAndReadingRecord_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                    STUDENT_ID, 10L, "individual", "answer", "during"))
            .thenReturn(List.of());

        assertThatThrownBy(() -> service.completeDuringReading(STUDENT_ID, 10L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400");
    }

    /* 검증 26: 조건 충족 시 duringDone=true, currentStage=after */
    @Test
    void completeDuringReading_succeedsWhenConditionsMet() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        record.setTotalPages(100);
        record.setCurrentPage(100);

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        when(responseRepository
                .findByStudent_IdAndReadingRecord_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                    STUDENT_ID, 10L, "individual", "answer", "during"))
            .thenReturn(List.of(buildDuringResponseEntity(1L, student, record, 1, "find", "q", "a", LocalDate.now())));
        when(readingRecordRepository.save(any(ReadingRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IndividualReadingRecordResponse response = service.completeDuringReading(STUDENT_ID, 10L);

        assertThat(response.getDuringDone()).isTrue();
        assertThat(response.getCurrentStage()).isEqualTo("after");
    }

    /* 검증 28: 이미 duringDone인 기록은 재확인 없이 idempotent하게 현재 상태만 반환(중복 지급/재검증 없음) */
    @Test
    void completeDuringReading_isIdempotentWhenAlreadyDone() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        record.setDuringDone(true);
        record.setCurrentStage("after");

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));

        IndividualReadingRecordResponse response = service.completeDuringReading(STUDENT_ID, 10L);

        assertThat(response.getDuringDone()).isTrue();
        verify(readingRecordRepository, never()).save(any(ReadingRecord.class));
        verify(responseRepository, never())
            .findByStudent_IdAndReadingRecord_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                any(), any(), any(), any(), any());
    }

    /* 검증: 완독된 책은 읽기 중 완료 처리 자체가 거부됨(finishedAt을 임의로 건드리지 않음) */
    @Test
    void completeDuringReading_throwsConflictWhenAlreadyFinished() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        record.setFinishedAt(java.time.LocalDateTime.now());

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.completeDuringReading(STUDENT_ID, 10L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("409");
    }

    /* 검증 32: 다른 학생의 기록은 완료 처리할 수 없음 */
    @Test
    void completeDuringReading_throwsForbiddenWhenRecordBelongsToAnotherStudent() {
        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.empty());
        when(readingRecordRepository.findById(10L)).thenReturn(
            Optional.of(buildRecord(10L, otherStudent(), buildBook(1L, "책", "작가"))));

        assertThatThrownBy(() -> service.completeDuringReading(STUDENT_ID, 10L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("403");
    }

    // =========================================================
    // 읽은 책 종류 통계 (book-type-stats)
    // =========================================================

    /* 검증 19/20/23: 완독 기록만 집계하고, 유형별 개수와 총 완독 권수가 누적된다 */
    @Test
    void getBookTypeStats_countsOnlyCompletedRecordsByType() {
        ReadingRecord storyRecord = buildRecord(1L, student, buildBook(1L, "이야기책", "작가", "story"));
        ReadingRecord infoRecord = buildRecord(2L, student, buildBook(2L, "정보책", "작가", "info"));
        ReadingRecord opinionRecord = buildRecord(3L, student, buildBook(3L, "주장책", "작가", "opinion"));

        when(readingRecordRepository.findByStudent_IdOrderByCreatedAtDesc(STUDENT_ID))
            .thenReturn(List.of(storyRecord, infoRecord, opinionRecord));

        BookTypeStatsResponse stats = service.getBookTypeStats(STUDENT_ID);

        assertThat(stats.getTotalCompletedBooks()).isEqualTo(3);
        assertThat(stats.getStory()).isEqualTo(1);
        assertThat(stats.getInformation()).isEqualTo(1);
        assertThat(stats.getArgument()).isEqualTo(1);
        assertThat(stats.getOther()).isEqualTo(0);
        assertThat(stats.getPercentages().getStory()).isEqualTo(33.3);
    }

    /* 검증: 알 수 없는/누락된 book_type은 "그 밖의 책"으로 집계 */
    @Test
    void getBookTypeStats_mapsUnknownBookTypeToOther() {
        ReadingRecord etcRecord = buildRecord(1L, student, buildBook(1L, "기타책", "작가", "etc"));
        ReadingRecord nullTypeRecord = buildRecord(2L, student, buildBook(2L, "무명책", "작가", null));

        when(readingRecordRepository.findByStudent_IdOrderByCreatedAtDesc(STUDENT_ID))
            .thenReturn(List.of(etcRecord, nullTypeRecord));

        BookTypeStatsResponse stats = service.getBookTypeStats(STUDENT_ID);

        assertThat(stats.getOther()).isEqualTo(2);
    }

    /* 검증: 완독 기록이 없으면 모두 0으로 표시 */
    @Test
    void getBookTypeStats_returnsZeroWhenNoCompletedBooks() {
        when(readingRecordRepository.findByStudent_IdOrderByCreatedAtDesc(STUDENT_ID))
            .thenReturn(List.of());

        BookTypeStatsResponse stats = service.getBookTypeStats(STUDENT_ID);

        assertThat(stats.getTotalCompletedBooks()).isEqualTo(0);
        assertThat(stats.getStory()).isEqualTo(0);
        assertThat(stats.getPercentages().getStory()).isEqualTo(0.0);
    }

    /* 검증 22: 학생별로 분리되어 집계된다 */
    @Test
    void getBookTypeStats_isIsolatedPerStudent() {
        when(readingRecordRepository.findByStudent_IdOrderByCreatedAtDesc(STUDENT_ID))
            .thenReturn(List.of(buildRecord(1L, student, buildBook(1L, "책", "작가", "story"))));
        when(readingRecordRepository.findByStudent_IdOrderByCreatedAtDesc(OTHER_STUDENT_ID))
            .thenReturn(List.of());

        BookTypeStatsResponse myStats = service.getBookTypeStats(STUDENT_ID);
        BookTypeStatsResponse otherStats = service.getBookTypeStats(OTHER_STUDENT_ID);

        assertThat(myStats.getTotalCompletedBooks()).isEqualTo(1);
        assertThat(otherStats.getTotalCompletedBooks()).isEqualTo(0);
    }

    // =========================================================
    // 월별 완독 기록 (monthly-completion-stats)
    // =========================================================

    private static final java.time.ZoneId ZONE_SEOUL_FOR_TEST = java.time.ZoneId.of("Asia/Seoul");

    private ReadingRecord buildCompletedRecord(Long id, User owner, java.time.LocalDateTime finishedAt) {
        ReadingRecord record = buildRecord(id, owner, buildBook(id, "책" + id, "작가"));
        record.setFinishedAt(finishedAt);
        return record;
    }

    /* 모든 달이 0권이어도 12개 원소가 모두 0으로 채워짐(그래프가 깨지지 않음) */
    @Test
    void getMonthlyCompletionStats_returnsAllZerosWhenNoCompletedBooks() {
        when(readingRecordRepository.findByStudent_IdAndFinishedAtIsNotNull(STUDENT_ID))
            .thenReturn(List.of());

        MonthlyCompletionStatsResponse stats = service.getMonthlyCompletionStats(STUDENT_ID);

        assertThat(stats.getMonthlyCounts()).hasSize(12);
        assertThat(stats.getMonthlyCounts()).containsOnly(0);
        assertThat(stats.getYear()).isEqualTo(LocalDate.now(ZONE_SEOUL_FOR_TEST).getYear());
    }

    /* 완독한 달의 개수가 정확히 누적되고, 나머지 달은 0으로 남음 */
    @Test
    void getMonthlyCompletionStats_groupsCompletedRecordsByMonth() {
        int year = LocalDate.now(ZONE_SEOUL_FOR_TEST).getYear();

        ReadingRecord marchFirst = buildCompletedRecord(
            1L, student, java.time.LocalDateTime.of(year, 3, 5, 10, 0));
        ReadingRecord marchSecond = buildCompletedRecord(
            2L, student, java.time.LocalDateTime.of(year, 3, 20, 10, 0));
        ReadingRecord juneRecord = buildCompletedRecord(
            3L, student, java.time.LocalDateTime.of(year, 6, 1, 10, 0));

        when(readingRecordRepository.findByStudent_IdAndFinishedAtIsNotNull(STUDENT_ID))
            .thenReturn(List.of(marchFirst, marchSecond, juneRecord));

        MonthlyCompletionStatsResponse stats = service.getMonthlyCompletionStats(STUDENT_ID);

        assertThat(stats.getMonthlyCounts().get(2)).isEqualTo(2); // 3월(index 2)
        assertThat(stats.getMonthlyCounts().get(5)).isEqualTo(1); // 6월(index 5)
        assertThat(stats.getMonthlyCounts().get(0)).isEqualTo(0); // 1월은 0권
        assertThat(stats.getMonthlyCounts()).hasSize(12);
    }

    /* 다른 연도에 완독한 기록은 현재 연도 집계에서 제외됨 */
    @Test
    void getMonthlyCompletionStats_excludesCompletionsFromOtherYears() {
        int year = LocalDate.now(ZONE_SEOUL_FOR_TEST).getYear();

        ReadingRecord lastYearRecord = buildCompletedRecord(
            1L, student, java.time.LocalDateTime.of(year - 1, 12, 31, 23, 59));
        ReadingRecord thisYearRecord = buildCompletedRecord(
            2L, student, java.time.LocalDateTime.of(year, 1, 1, 0, 0));

        when(readingRecordRepository.findByStudent_IdAndFinishedAtIsNotNull(STUDENT_ID))
            .thenReturn(List.of(lastYearRecord, thisYearRecord));

        MonthlyCompletionStatsResponse stats = service.getMonthlyCompletionStats(STUDENT_ID);

        int totalCount = stats.getMonthlyCounts().stream().mapToInt(Integer::intValue).sum();
        assertThat(totalCount).isEqualTo(1);
        assertThat(stats.getMonthlyCounts().get(0)).isEqualTo(1); // 1월만 집계
    }

    /* 학생별로 분리되어 집계된다 */
    @Test
    void getMonthlyCompletionStats_isIsolatedPerStudent() {
        int year = LocalDate.now(ZONE_SEOUL_FOR_TEST).getYear();

        when(readingRecordRepository.findByStudent_IdAndFinishedAtIsNotNull(STUDENT_ID))
            .thenReturn(List.of(buildCompletedRecord(
                1L, student, java.time.LocalDateTime.of(year, 4, 1, 0, 0))));
        when(readingRecordRepository.findByStudent_IdAndFinishedAtIsNotNull(OTHER_STUDENT_ID))
            .thenReturn(List.of());

        MonthlyCompletionStatsResponse myStats = service.getMonthlyCompletionStats(STUDENT_ID);
        MonthlyCompletionStatsResponse otherStats = service.getMonthlyCompletionStats(OTHER_STUDENT_ID);

        assertThat(myStats.getMonthlyCounts().get(3)).isEqualTo(1);
        assertThat(otherStats.getMonthlyCounts()).containsOnly(0);
    }

    private Book buildBook(Long id, String title, String author, String bookType) {
        Book book = buildBook(id, title, author);
        book.setBookType(bookType);
        return book;
    }

    /* ===================== 읽기 후(after) ===================== */

    private IndividualAfterResponseSaveRequest buildAfterSaveRequest(
            String question, String answer, Boolean aiPassed) {
        IndividualAfterResponseSaveRequest request = new IndividualAfterResponseSaveRequest();
        setField(request, "question", question);
        setField(request, "answer", answer);
        setField(request, "aiPassed", aiPassed);
        return request;
    }

    private Response buildAfterResponseEntity(
            Long id, User owner, ReadingRecord record, Integer questionIndex,
            String question, String answer, Boolean passed) {

        Response response = new Response();
        response.setId(id);
        response.setStudent(owner);
        response.setReadingRecord(record);
        response.setMode("individual");
        response.setContentType("answer");
        response.setStage("after");
        response.setContent(answer);
        response.setPassed(passed);

        Map<String, Object> extraData = new java.util.HashMap<>(
            Map.of("questionIndex", questionIndex, "question", question));
        response.setExtraData(extraData);
        return response;
    }

    private void mockAfterResponses(Long readingRecordId, List<Response> existing) {
        when(responseRepository
                .findByStudent_IdAndReadingRecord_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                    STUDENT_ID, readingRecordId, "individual", "answer", "after"))
            .thenReturn(existing);
    }

    private IndividualSummarySaveRequest buildSummarySaveRequest(
            String bookType, String summaryText, Boolean aiPassed) {
        IndividualSummarySaveRequest request = new IndividualSummarySaveRequest();
        setField(request, "bookType", bookType);
        setField(request, "summaryText", summaryText);
        setField(request, "aiPassed", aiPassed);
        return request;
    }

    private Summary buildSummary(
            Long id, User owner, ReadingRecord record, String bookType, String summaryText, Boolean aiPassed) {
        Summary summary = new Summary();
        summary.setId(id);
        summary.setStudent(owner);
        summary.setReadingRecord(record);
        summary.setBookType(bookType);
        summary.setSummaryText(summaryText);
        summary.setAiPassed(aiPassed);
        summary.setIsShared(false);
        summary.setStatus("approved");
        return summary;
    }

    /* 검증: 읽기 후 질문·답 한 세트를 새로 저장한다 */
    @Test
    void saveAfterResponse_savesNewQuestionAnswer() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        mockAfterResponses(10L, List.of());
        when(responseRepository.save(any(Response.class))).thenAnswer(invocation -> {
            Response toSave = invocation.getArgument(0);
            toSave.setId(2000L);
            return toSave;
        });

        IndividualAfterResponseItem result = service.saveAfterResponse(
            STUDENT_ID, 10L, 1, buildAfterSaveRequest("질문1?", "답1입니다", true));

        assertThat(result.getQuestionIndex()).isEqualTo(1);
        assertThat(result.getQuestion()).isEqualTo("질문1?");
        assertThat(result.getAnswer()).isEqualTo("답1입니다");
        assertThat(result.getAiPassed()).isTrue();
    }

    /* 검증: 같은 질문 번호로 다시 저장하면 새 행 대신 기존 행을 갱신한다(upsert) */
    @Test
    void saveAfterResponse_updatesExistingQuestionAnswerInsteadOfInserting() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        Response existing = buildAfterResponseEntity(2000L, student, record, 1, "옛 질문?", "옛 답", true);

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        mockAfterResponses(10L, List.of(existing));
        when(responseRepository.save(any(Response.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IndividualAfterResponseItem result = service.saveAfterResponse(
            STUDENT_ID, 10L, 1, buildAfterSaveRequest("새 질문?", "새 답", true));

        assertThat(result.getResponseId()).isEqualTo(2000L);
        assertThat(result.getQuestion()).isEqualTo("새 질문?");
        assertThat(result.getAnswer()).isEqualTo("새 답");
        verify(responseRepository, org.mockito.Mockito.times(1)).save(any(Response.class));
    }

    /* 검증: 다른 학생의 readingRecordId에는 저장할 수 없다(403) */
    @Test
    void saveAfterResponse_throwsForbiddenWhenRecordBelongsToAnotherStudent() {
        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.empty());
        when(readingRecordRepository.findById(10L)).thenReturn(
            Optional.of(buildRecord(10L, otherStudent(), buildBook(1L, "책", "작가"))));

        assertThatThrownBy(() ->
            service.saveAfterResponse(STUDENT_ID, 10L, 1, buildAfterSaveRequest("질문?", "답", true))
        )
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("403");

        verify(responseRepository, never()).save(any(Response.class));
    }

    /* 검증: AI가 need로 판정한(aiPassed=false) 답은 저장 자체를 거부한다 */
    @Test
    void saveAfterResponse_rejectsWhenAiPassedIsExplicitlyFalse() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));

        assertThatThrownBy(() ->
            service.saveAfterResponse(STUDENT_ID, 10L, 1, buildAfterSaveRequest("질문?", "답", false))
        )
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400");

        verify(responseRepository, never()).save(any(Response.class));
    }

    /* 검증: 완독한 기록의 읽기 후 질문·답은 수정할 수 없다(409) */
    @Test
    void saveAfterResponse_throwsConflictWhenRecordAlreadyCompleted() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        record.setFinishedAt(java.time.LocalDateTime.now());
        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));

        assertThatThrownBy(() ->
            service.saveAfterResponse(STUDENT_ID, 10L, 1, buildAfterSaveRequest("질문?", "답", true))
        )
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("409");
    }

    /* 검증: 저장된 질문·답 3세트를 순서대로 다시 불러온다(새로고침/재로그인 복원) */
    @Test
    void getAfterResponses_restoresSavedThreeSets() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        Response r1 = buildAfterResponseEntity(1L, student, record, 1, "q1?", "a1", true);
        Response r2 = buildAfterResponseEntity(2L, student, record, 2, "q2?", "a2", true);
        Response r3 = buildAfterResponseEntity(3L, student, record, 3, "q3?", "a3", true);

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        mockAfterResponses(10L, List.of(r1, r2, r3));

        List<IndividualAfterResponseItem> result = service.getAfterResponses(STUDENT_ID, 10L);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getQuestionIndex()).isEqualTo(1);
        assertThat(result.get(2).getAnswer()).isEqualTo("a3");
    }

    /* 검증: 최종 간추리기를 새로 저장한다 */
    @Test
    void saveAfterSummary_savesNewSummary() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        when(summaryRepository.findByStudent_IdAndReadingRecord_Id(STUDENT_ID, 10L)).thenReturn(Optional.empty());
        when(summaryRepository.save(any(Summary.class))).thenAnswer(invocation -> {
            Summary toSave = invocation.getArgument(0);
            toSave.setId(500L);
            return toSave;
        });

        IndividualSummaryResponse result = service.saveAfterSummary(
            STUDENT_ID, 10L, buildSummarySaveRequest("story", "간추린 내용입니다", true));

        assertThat(result.getSummaryId()).isEqualTo(500L);
        assertThat(result.getSummaryText()).isEqualTo("간추린 내용입니다");
        assertThat(result.getBookType()).isEqualTo("story");
    }

    /* 검증: 같은 readingRecordId로 다시 저장하면 기존 간추리기를 갱신한다(다른 책 내용과 섞이지 않음) */
    @Test
    void saveAfterSummary_updatesExistingSummaryInsteadOfInserting() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        Summary existing = buildSummary(500L, student, record, "story", "옛 내용", true);

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        when(summaryRepository.findByStudent_IdAndReadingRecord_Id(STUDENT_ID, 10L)).thenReturn(Optional.of(existing));
        when(summaryRepository.save(any(Summary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IndividualSummaryResponse result = service.saveAfterSummary(
            STUDENT_ID, 10L, buildSummarySaveRequest("info", "새로운 간추린 내용", true));

        assertThat(result.getSummaryId()).isEqualTo(500L);
        assertThat(result.getSummaryText()).isEqualTo("새로운 간추린 내용");
        assertThat(result.getBookType()).isEqualTo("info");
        verify(summaryRepository, org.mockito.Mockito.times(1)).save(any(Summary.class));
    }

    /* 검증: 저장된 간추리기를 다시 불러온다(새로고침/재로그인 복원) */
    @Test
    void getAfterSummary_restoresSavedSummary() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        Summary existing = buildSummary(500L, student, record, "story", "저장된 내용", true);

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        when(summaryRepository.findByStudent_IdAndReadingRecord_Id(STUDENT_ID, 10L)).thenReturn(Optional.of(existing));

        Optional<IndividualSummaryResponse> result = service.getAfterSummary(STUDENT_ID, 10L);

        assertThat(result).isPresent();
        assertThat(result.get().getSummaryText()).isEqualTo("저장된 내용");
    }

    /* 검증: 질문 3세트 중 하나라도 없으면 완료가 실패한다 */
    @Test
    void completeAfterReading_failsWhenOneQuestionMissing() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        Response r1 = buildAfterResponseEntity(1L, student, record, 1, "q1?", "a1", true);
        Response r2 = buildAfterResponseEntity(2L, student, record, 2, "q2?", "a2", true);

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        mockAfterResponses(10L, List.of(r1, r2));
        when(summaryRepository.findByStudent_IdAndReadingRecord_Id(STUDENT_ID, 10L))
            .thenReturn(Optional.of(buildSummary(500L, student, record, "story", "간추린 내용", true)));

        assertThatThrownBy(() -> service.completeAfterReading(STUDENT_ID, 10L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400");

        verify(readingRecordRepository, never()).save(any(ReadingRecord.class));
        verify(afterReadingRewardService, never())
            .grantAfterCompleteRewardOnce(any(User.class), any(Long.class));
    }

    /* 검증: 질문 3세트 중 하나라도 AI 통과 상태가 아니면 완료가 실패한다 */
    @Test
    void completeAfterReading_failsWhenOneQuestionNotPassed() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        Response r1 = buildAfterResponseEntity(1L, student, record, 1, "q1?", "a1", true);
        Response r2 = buildAfterResponseEntity(2L, student, record, 2, "q2?", "a2", true);
        Response r3 = buildAfterResponseEntity(3L, student, record, 3, "q3?", "a3", false);

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        mockAfterResponses(10L, List.of(r1, r2, r3));
        when(summaryRepository.findByStudent_IdAndReadingRecord_Id(STUDENT_ID, 10L))
            .thenReturn(Optional.of(buildSummary(500L, student, record, "story", "간추린 내용", true)));

        assertThatThrownBy(() -> service.completeAfterReading(STUDENT_ID, 10L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400");

        verify(afterReadingRewardService, never())
            .grantAfterCompleteRewardOnce(any(User.class), any(Long.class));
    }

    /* 검증: 최종 간추리기가 비어 있으면(저장 자체가 없으면) 완료가 실패한다 */
    @Test
    void completeAfterReading_failsWhenSummaryMissing() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        Response r1 = buildAfterResponseEntity(1L, student, record, 1, "q1?", "a1", true);
        Response r2 = buildAfterResponseEntity(2L, student, record, 2, "q2?", "a2", true);
        Response r3 = buildAfterResponseEntity(3L, student, record, 3, "q3?", "a3", true);

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        mockAfterResponses(10L, List.of(r1, r2, r3));
        when(summaryRepository.findByStudent_IdAndReadingRecord_Id(STUDENT_ID, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.completeAfterReading(STUDENT_ID, 10L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400");

        verify(afterReadingRewardService, never())
            .grantAfterCompleteRewardOnce(any(User.class), any(Long.class));
    }

    /*
     * "간추리기 공유" 바로가기(?view=summaryShare)는 읽기 중을 건너뛰고도
     * 읽기 후 화면에 직접 진입할 수 있는 경로였다. 프론트 버튼을
     * 숨기더라도 API를 직접 호출하면 우회될 수 있으므로, 서버가
     * beforeDone/duringDone을 다시 확인해 거부해야 한다(서버 우회 차단
     * 필수 테스트).
     */
    @Test
    void completeAfterReading_failsWhenDuringNotDone() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        record.setBeforeDone(true);
        record.setDuringDone(false);
        Response r1 = buildAfterResponseEntity(1L, student, record, 1, "q1?", "a1", true);
        Response r2 = buildAfterResponseEntity(2L, student, record, 2, "q2?", "a2", true);
        Response r3 = buildAfterResponseEntity(3L, student, record, 3, "q3?", "a3", true);

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        mockAfterResponses(10L, List.of(r1, r2, r3));
        when(summaryRepository.findByStudent_IdAndReadingRecord_Id(STUDENT_ID, 10L))
            .thenReturn(Optional.of(buildSummary(500L, student, record, "story", "간추린 내용", true)));

        assertThatThrownBy(() -> service.completeAfterReading(STUDENT_ID, 10L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400")
            .hasMessageContaining("읽기 중 활동을 먼저 완료해야 해요");

        verify(readingRecordRepository, never()).save(any(ReadingRecord.class));
        verify(afterReadingRewardService, never())
            .grantAfterCompleteRewardOnce(any(User.class), any(Long.class));
    }

    /* 읽기 전조차 안 끝난 상태에서도 동일하게 거부되는지 확인(위 테스트와 별도 조건 조합) */
    @Test
    void completeAfterReading_failsWhenBeforeNotDone() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        record.setBeforeDone(false);
        record.setDuringDone(true);
        Response r1 = buildAfterResponseEntity(1L, student, record, 1, "q1?", "a1", true);
        Response r2 = buildAfterResponseEntity(2L, student, record, 2, "q2?", "a2", true);
        Response r3 = buildAfterResponseEntity(3L, student, record, 3, "q3?", "a3", true);

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        mockAfterResponses(10L, List.of(r1, r2, r3));
        when(summaryRepository.findByStudent_IdAndReadingRecord_Id(STUDENT_ID, 10L))
            .thenReturn(Optional.of(buildSummary(500L, student, record, "story", "간추린 내용", true)));

        assertThatThrownBy(() -> service.completeAfterReading(STUDENT_ID, 10L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400")
            .hasMessageContaining("읽기 전 활동을 먼저 완료해야 해요");

        verify(afterReadingRewardService, never())
            .grantAfterCompleteRewardOnce(any(User.class), any(Long.class));
    }

    /* 검증: 조건을 모두 만족하면 afterDone=true가 되고 보상 서비스가 정확히 1번 호출된다(체력+3/마법력+1/지혜+1은 보상 서비스 단위테스트에서 검증) */
    @Test
    void completeAfterReading_setsAfterDoneTrueAndGrantsRewardWhenAllConditionsMet() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        record.setBeforeDone(true);
        record.setDuringDone(true);
        Response r1 = buildAfterResponseEntity(1L, student, record, 1, "q1?", "a1", true);
        Response r2 = buildAfterResponseEntity(2L, student, record, 2, "q2?", "a2", true);
        Response r3 = buildAfterResponseEntity(3L, student, record, 3, "q3?", "a3", true);
        Summary summary = buildSummary(500L, student, record, "story", "간추린 내용", true);

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        mockAfterResponses(10L, List.of(r1, r2, r3));
        when(summaryRepository.findByStudent_IdAndReadingRecord_Id(STUDENT_ID, 10L)).thenReturn(Optional.of(summary));
        when(readingRecordRepository.save(any(ReadingRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentStats stats = new StudentStats();
        stats.setStudent(student);
        stats.setMagic(9);
        stats.setStamina(11);
        stats.setWisdom(9);
        stats.setCourage(8);

        when(afterReadingRewardService.grantAfterCompleteRewardOnce(student, 10L))
            .thenReturn(new IndividualAfterReadingRewardService.RewardResult(true, false, stats));

        IndividualAfterCompleteResponse response = service.completeAfterReading(STUDENT_ID, 10L);

        assertThat(record.getAfterDone()).isTrue();
        assertThat(response.getAfterDone()).isTrue();
        assertThat(response.isRewardGranted()).isTrue();
        assertThat(response.getStats().getStamina()).isEqualTo(11);
        assertThat(response.getStats().getMagic()).isEqualTo(9);
        assertThat(response.getStats().getWisdom()).isEqualTo(9);
        assertThat(response.getStats().getCourage()).isEqualTo(8);
        verify(afterReadingRewardService, org.mockito.Mockito.times(1))
            .grantAfterCompleteRewardOnce(student, 10L);
    }

    /* 검증: 이미 afterDone=true인 기록에 다시 완료 요청이 와도(새로고침 후 재클릭) 조건을 다시 검증하지 않고, 보상 지급 여부는 보상 서비스의 중복 방지에만 맡긴다(중복 보상 없음) */
    @Test
    void completeAfterReading_isIdempotentWhenAlreadyAfterDone() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        record.setAfterDone(true);

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));

        StudentStats stats = new StudentStats();
        stats.setStudent(student);
        stats.setMagic(9);
        stats.setStamina(11);
        stats.setWisdom(9);
        stats.setCourage(8);

        when(afterReadingRewardService.grantAfterCompleteRewardOnce(student, 10L))
            .thenReturn(new IndividualAfterReadingRewardService.RewardResult(false, true, stats));

        IndividualAfterCompleteResponse response = service.completeAfterReading(STUDENT_ID, 10L);

        assertThat(response.isRewardGranted()).isFalse();
        verify(readingRecordRepository, never()).save(any(ReadingRecord.class));
        verify(responseRepository, never())
            .findByStudent_IdAndReadingRecord_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                any(), any(), any(), any(), any());
        verify(summaryRepository, never()).findByStudent_IdAndReadingRecord_Id(any(), any());
    }

    /* 검증: 이미 완독한 책은 읽기 후 완료를 다시 호출할 수 없다(409) */
    @Test
    void completeAfterReading_throwsConflictWhenBookAlreadyFinished() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        record.setAfterDone(true);
        record.setFinishedAt(java.time.LocalDateTime.now());

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.completeAfterReading(STUDENT_ID, 10L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("409");

        verify(afterReadingRewardService, never())
            .grantAfterCompleteRewardOnce(any(User.class), any(Long.class));
    }

    /* 검증: 책 완독 처리(completeReadingRecord)는 읽기 후 완료 보상 서비스를 절대 호출하지 않는다(완독 추가 보상 없음) */
    @Test
    void completeReadingRecord_neverGrantsAfterReadingReward() {
        ReadingRecord record = buildRecord(10L, student, buildBook(1L, "책", "작가"));
        record.setAfterDone(true);
        Response representative = buildAfterResponseEntity(500L, student, record, 1, "질문?", "답", true);

        when(readingRecordRepository.findByIdAndStudent_Id(10L, STUDENT_ID)).thenReturn(Optional.of(record));
        when(responseRepository.findById(500L)).thenReturn(Optional.of(representative));
        when(individualAchievementService.calculate(10L)).thenReturn(achievementResult(75.0, 85.0));
        when(readingRecordRepository.save(any(ReadingRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.completeReadingRecord(STUDENT_ID, 10L, buildFinishRequest(4, 500L));

        verify(afterReadingRewardService, never())
            .grantAfterCompleteRewardOnce(any(User.class), any(Long.class));
    }
}
