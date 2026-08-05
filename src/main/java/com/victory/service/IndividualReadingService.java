package com.victory.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.BookTypeStatsResponse;
import com.victory.dto.IndividualAfterCompleteResponse;
import com.victory.dto.IndividualAfterResponseItem;
import com.victory.dto.IndividualAfterResponseSaveRequest;
import com.victory.dto.IndividualAchievementResult;
import com.victory.dto.IndividualBeforeResponseItem;
import com.victory.dto.IndividualBeforeResponseSaveRequest;
import com.victory.dto.IndividualBookRegisterRequest;
import com.victory.dto.IndividualDuringResponseItem;
import com.victory.dto.IndividualDuringResponseSaveRequest;
import com.victory.dto.IndividualDuringTodayResponse;
import com.victory.dto.IndividualFinishCandidatesResponse;
import com.victory.dto.IndividualQaRecordItem;
import com.victory.dto.IndividualReadingArchiveDetailResponse;
import com.victory.dto.IndividualReadingArchiveItem;
import com.victory.dto.IndividualReadingFinishRequest;
import com.victory.dto.IndividualReadingFinishResponse;
import com.victory.dto.IndividualReadingPagesRequest;
import com.victory.dto.IndividualReadingRecordResponse;
import com.victory.dto.IndividualSummaryResponse;
import com.victory.dto.IndividualSummarySaveRequest;
import com.victory.dto.MonthlyCompletionStatsResponse;
import com.victory.dto.StudentStatsResponse;
import com.victory.entity.Book;
import com.victory.entity.ReadingProgressLog;
import com.victory.entity.ReadingRecord;
import com.victory.entity.Response;
import com.victory.entity.Summary;
import com.victory.entity.User;
import com.victory.repository.BookRepository;
import com.victory.repository.ReadingProgressLogRepository;
import com.victory.repository.ReadingRecordRepository;
import com.victory.repository.ResponseRepository;
import com.victory.repository.SummaryRepository;
import com.victory.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/*
 * 개별읽기 1단계(책 등록/진행 중 책 조회/쪽수 수정/완독 처리) +
 * 2단계(읽기 전 질문·답 저장/조회)를 다룬다. 읽기 중·읽기 후 질문·답,
 * AI 피드백 결과 저장, 보관함 상세 데이터는 다음 단계에서 다룬다.
 */
@Service
@RequiredArgsConstructor
public class IndividualReadingService {

    private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");
    private static final String SOURCE_INDIVIDUAL = "individual";
    private static final String STAGE_BEFORE = "before";
    private static final String STAGE_DURING = "during";
    private static final String STAGE_AFTER = "after";
    private static final String STAGE_COMPLETED = "completed";

    /*
     * responses 테이블 재사용 시 개별읽기와 연습읽기를 구분하는 값.
     * 연습읽기 읽기 전 저장(ResponseService)은 항상 mode="class"만 쓰고,
     * PracticeAchievementService의 참여도·이해도 계산도 mode="class"로만
     * 조회하므로, mode="individual" 행은 그 계산에 절대 섞이지 않는다.
     */
    private static final String MODE_INDIVIDUAL = "individual";
    private static final String CONTENT_TYPE_ANSWER = "answer";

    /*
     * individual-before-reading.html의 실제 4단계(steps[1..4].key)와
     * 정확히 같은 값이어야 한다 - 여기 목록이 화면과 어긋나면 beforeDone
     * 판정이 절대 true가 되지 않는다.
     */
    private static final List<String> BEFORE_STEP_TYPES = List.of("title", "contents", "picture", "skim");

    /*
     * individual-during-reading.html의 questionTypes 4종(find/infer/feel/connect)과
     * 정확히 같은 값이어야 한다.
     */
    private static final List<String> DURING_QUESTION_TYPES = List.of("find", "infer", "feel", "connect");

    /*
     * 책속 생각쓰기는 하루에 질문·답 3개를 완성하는 활동이다. 저장 고유
     * 식별자는 이 슬롯 번호이고(studentId+readingRecordId+activityDate+
     * questionSlot), questionType은 슬롯에 붙는 유형 정보일 뿐이라 같은
     * 유형을 여러 슬롯에서 써도 각각 별도로 보존된다.
     */
    private static final List<Integer> DURING_QUESTION_SLOTS = List.of(1, 2, 3);

    /*
     * individual-after-reading.html의 간추리기 질문·답 카드는 항상 3개(1/2/3)다.
     * 읽기 전(stepType 문자열)과 달리 순서가 있는 번호라 Integer 목록을 쓴다.
     */
    private static final List<Integer> AFTER_QUESTION_INDEXES = List.of(1, 2, 3);

    private final BookRepository bookRepository;
    private final ReadingRecordRepository readingRecordRepository;
    private final UserRepository userRepository;
    private final ResponseRepository responseRepository;
    private final ReadingProgressLogRepository readingProgressLogRepository;
    private final SummaryRepository summaryRepository;
    private final IndividualBeforeReadingRewardService beforeReadingRewardService;
    private final IndividualDuringReadingRewardService duringReadingRewardService;
    private final IndividualAfterReadingRewardService afterReadingRewardService;
    private final IndividualAchievementService individualAchievementService;

    /*
     * 학생당 진행 중(finished_at IS NULL) 기록은 항상 1개만 있어야 한다.
     * 유니크 인덱스로 강제하지 않기로 했으므로(같은 book_id에 여러 완독
     * 기록이 쌓여야 해서), 등록 시 먼저 학생의 users 행을 잠근 뒤(동시
     * 요청 직렬화) 진행 중 기록 존재 여부를 확인하고 없을 때만 새로 만든다.
     */
    @Transactional
    public IndividualReadingRecordResponse registerBook(Long studentId, IndividualBookRegisterRequest request) {

        User student = userRepository.findByIdForUpdate(studentId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "학생 정보를 찾을 수 없습니다. studentId=" + studentId
            ));

        if (readingRecordRepository.findByStudent_IdAndFinishedAtIsNull(studentId).isPresent()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "현재 읽고 있는 책을 먼저 마쳐 주세요."
            );
        }

        Book book = bookRepository
            .findFirstBySourceAndTitleAndAuthor(SOURCE_INDIVIDUAL, request.getTitle(), request.getAuthor())
            .orElseGet(() -> {
                Book newBook = new Book();
                newBook.setTitle(request.getTitle());
                newBook.setAuthor(request.getAuthor());
                newBook.setBookType(request.getBookType());
                newBook.setCoverImage(request.getCoverImage());
                newBook.setSource(SOURCE_INDIVIDUAL);
                newBook.setRegisteredBy(student);
                return bookRepository.save(newBook);
            });

        ReadingRecord record = new ReadingRecord();
        record.setStudent(student);
        record.setBook(book);
        record.setCurrentStage(STAGE_BEFORE);
        record.setBeforeDone(false);
        record.setDuringDone(false);
        record.setAfterDone(false);
        record.setCurrentPage(0);
        record.setTotalPages(request.getTotalPages());

        ReadingRecord saved = readingRecordRepository.save(record);

        return toResponse(saved, book);
    }

    @Transactional(readOnly = true)
    public Optional<IndividualReadingRecordResponse> getCurrentReadingRecord(Long studentId) {
        return readingRecordRepository.findByStudent_IdAndFinishedAtIsNull(studentId)
            .map(record -> toResponse(record, record.getBook()));
    }

    @Transactional
    public IndividualReadingRecordResponse updatePages(
            Long studentId,
            Long readingRecordId,
            IndividualReadingPagesRequest request) {

        ReadingRecord record = requireOwnedRecord(studentId, readingRecordId);

        if (record.getFinishedAt() != null) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "이미 완독한 책의 진행 상황은 수정할 수 없습니다."
            );
        }

        Integer totalPages = request.getTotalPages();
        Integer currentPage = request.getCurrentPage();

        if (totalPages == null || totalPages <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "전체 쪽수는 1 이상이어야 합니다.");
        }

        if (currentPage == null || currentPage < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "오늘 읽은 쪽수는 0 이상이어야 합니다.");
        }

        if (currentPage > totalPages) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "오늘 읽은 쪽수는 전체 쪽수보다 클 수 없습니다.");
        }

        if (record.getCurrentPage() != null && currentPage < record.getCurrentPage()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "읽은 쪽수는 이전에 저장한 기록보다 줄어들 수 없습니다."
            );
        }

        record.setTotalPages(totalPages);
        record.setCurrentPage(currentPage);

        ReadingRecord saved = readingRecordRepository.save(record);

        upsertTodayProgressLog(saved.getStudent(), saved, currentPage, totalPages);

        return toResponse(saved, saved.getBook());
    }

    /*
     * 이미 완독된 기록에 다시 완독 요청이 와도(중복 클릭 등) 에러 없이
     * 현재 상태를 그대로 반환한다(idempotent) - finishedAt/rating/대표 질문을
     * 덮어쓰지 않는다(이미 저장된 별점·대표 질문을 재요청으로 바꾸는 기능은
     * 이번 범위에 포함하지 않는다 - 필요해지면 별도 "수정" API로 다룬다).
     *
     * 읽기 후 완료(afterDone)가 먼저 끝나 있어야만 책을 완독 처리할 수 있다 -
     * 읽기 후 간추리기 완료 보상(체력+3/마법력+1/지혜+1)은 오직
     * completeAfterReading()에서만 지급되고, 이 메서드는 보상을 전혀 지급하지
     * 않는다(책 완독 처리와 읽기 후 완료 보상을 분리하기 위함). 별점·대표
     * 질문 저장에도 별도 능력치 보상은 없다.
     */
    @Transactional
    public IndividualReadingFinishResponse completeReadingRecord(
            Long studentId, Long readingRecordId, IndividualReadingFinishRequest request) {

        ReadingRecord record = requireOwnedRecord(studentId, readingRecordId);

        if (record.getFinishedAt() == null) {
            if (!Boolean.TRUE.equals(record.getAfterDone())) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "읽기 후 활동을 먼저 완료해야 책을 완독 처리할 수 있어요."
                );
            }

            Response representative = requireRepresentativeCandidate(
                studentId, readingRecordId, request.getRepresentativeResponseId());
            IndividualAchievementResult finalScores =
                individualAchievementService.calculate(readingRecordId);

            record.setRating(request.getRating());
            record.setRepresentResponse(representative);
            record.setFinalReadingPracticeScore(roundedScore(finalScores.getReadingPracticeScore()));
            record.setFinalRecordCompletionScore(roundedScore(finalScores.getRecordCompletionScore()));
            record.setFinishedAt(LocalDateTime.now(ZONE_SEOUL));
            record.setCurrentStage(STAGE_COMPLETED);
            readingRecordRepository.save(record);
        }

        return toFinishResponse(record);
    }

    private int roundedScore(double score) {
        return (int) Math.round(score);
    }

    /*
     * 이 책 마무리하기 팝업의 읽기 전/중/후 탭이 보여줄 대표 질문 후보.
     * 완독한 기록도 조회는 허용한다(마무리 이후에도 팝업을 다시 열어 볼 수
     * 있게). 읽기 전·중 응답은 저장 자체가 "AI 통과 직후에만 호출"이라는
     * 기존 관례를 전제로 별도 통과 여부 컬럼 없이 전부 후보로 삼고, 읽기
     * 후 응답만 실제 passed=true인 것만 후보로 남긴다(유일하게 서버가
     * 판정 결과를 저장하는 단계라서).
     */
    @Transactional(readOnly = true)
    public IndividualFinishCandidatesResponse getFinishCandidates(Long studentId, Long readingRecordId) {

        requireOwnedRecord(studentId, readingRecordId);

        List<IndividualQaRecordItem> before =
            toQaRecordItems(findBeforeResponses(studentId, readingRecordId), false);
        List<IndividualQaRecordItem> during =
            toQaRecordItems(findDuringResponses(studentId, readingRecordId), false);
        List<IndividualQaRecordItem> after =
            toQaRecordItems(findAfterResponses(studentId, readingRecordId), true);

        return new IndividualFinishCandidatesResponse(before, during, after);
    }

    /*
     * 나의 독서 보관함 목록. 완독(finished_at IS NOT NULL)한 기록만
     * 최신순으로 반환한다. 아직 별점·대표 질문이 없는(과거) 완독 기록도
     * 그대로 포함하고, 해당 필드는 null로 내려준다 - 임의로 값을 채우거나
     * 기록을 제외하지 않는다.
     */
    @Transactional(readOnly = true)
    public List<IndividualReadingArchiveItem> getArchive(Long studentId) {

        List<ReadingRecord> records = readingRecordRepository.findByStudent_IdAndFinishedAtIsNotNull(studentId);

        return records.stream()
            .sorted(Comparator.comparing(ReadingRecord::getFinishedAt).reversed())
            .map(this::toArchiveItem)
            .toList();
    }

    /*
     * 나의 독서 보관함 상세. 완독 여부와 무관하게 본인 기록이면 조회를
     * 허용한다(완독 전에도 지금까지의 기록을 미리 볼 수 있게) - 쓰기가
     * 아니라 읽기 전용이라 finishedAt을 강제하지 않는다.
     */
    @Transactional(readOnly = true)
    public IndividualReadingArchiveDetailResponse getArchiveDetail(Long studentId, Long readingRecordId) {

        ReadingRecord record = requireOwnedRecord(studentId, readingRecordId);
        Response representative = record.getRepresentResponse();
        Book book = record.getBook();

        List<IndividualQaRecordItem> before =
            toQaRecordItems(findBeforeResponses(studentId, readingRecordId), false);
        List<IndividualQaRecordItem> during =
            toQaRecordItems(findDuringResponses(studentId, readingRecordId), false);
        List<IndividualQaRecordItem> after =
            toQaRecordItems(findAfterResponses(studentId, readingRecordId), true);

        Summary summary = summaryRepository
            .findByStudent_IdAndReadingRecord_Id(studentId, readingRecordId)
            .orElse(null);

        return new IndividualReadingArchiveDetailResponse(
            record.getId(),
            book.getId(),
            book.getTitle(),
            book.getAuthor(),
            book.getCoverImage(),
            record.getFinishedAt(),
            record.getRating(),
            representative == null ? null : extractStringFromExtraData(representative, "question"),
            representative == null ? null : representative.getContent(),
            representative == null ? null : representative.getStage(),
            before,
            during,
            after,
            summary == null ? null : summary.getSummaryText(),
            summary == null ? null : summary.getAiPassed()
        );
    }

    /*
     * 대표 질문으로 선택된 responseId를 검증한다: 실제로 존재하고, 로그인
     * 학생 본인의 응답이며, 이 readingRecordId에 속하고, 개별읽기
     * 질문·답(mode=individual, contentType=answer, stage=before/during/after)
     * 이어야 한다. 다른 학생·다른 책의 responseId는 각각 403/400으로
     * 막는다. 읽기 후 응답만 실제 passed 컬럼을 검증한다(읽기 전·중은
     * 저장 자체가 이미 AI 통과를 전제하므로 별도 컬럼이 없다).
     */
    private Response requireRepresentativeCandidate(Long studentId, Long readingRecordId, Long responseId) {

        Response response = responseRepository.findById(responseId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "대표 질문을 찾을 수 없습니다. responseId=" + responseId
            ));

        if (response.getStudent() == null || !response.getStudent().getId().equals(studentId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "본인의 질문·답만 대표 질문으로 선택할 수 있습니다."
            );
        }

        if (response.getReadingRecord() == null || !response.getReadingRecord().getId().equals(readingRecordId)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "이 책의 질문·답만 대표 질문으로 선택할 수 있습니다."
            );
        }

        if (!MODE_INDIVIDUAL.equals(response.getMode()) || !CONTENT_TYPE_ANSWER.equals(response.getContentType())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "대표 질문으로 선택할 수 없는 응답입니다."
            );
        }

        String stage = response.getStage();
        boolean validStage = STAGE_BEFORE.equals(stage) || STAGE_DURING.equals(stage) || STAGE_AFTER.equals(stage);

        if (!validStage) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "대표 질문으로 선택할 수 없는 응답입니다."
            );
        }

        if (STAGE_AFTER.equals(stage) && !Boolean.TRUE.equals(response.getPassed())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "루미 피드백을 통과한 질문·답만 대표 질문으로 선택할 수 있습니다."
            );
        }

        if (response.getContent() == null || response.getContent().isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "답이 비어 있는 질문은 대표 질문으로 선택할 수 없습니다."
            );
        }

        return response;
    }

    private List<IndividualQaRecordItem> toQaRecordItems(List<Response> responses, boolean requireExplicitPass) {
        return responses.stream()
            .filter(response -> response.getContent() != null && !response.getContent().isBlank())
            .filter(response -> !requireExplicitPass || Boolean.TRUE.equals(response.getPassed()))
            .map(this::toQaRecordItem)
            .toList();
    }

    private IndividualQaRecordItem toQaRecordItem(Response response) {
        boolean isAfter = STAGE_AFTER.equals(response.getStage());

        return new IndividualQaRecordItem(
            response.getId(),
            extractStringFromExtraData(response, "question"),
            response.getContent(),
            isAfter ? response.getPassed() : Boolean.TRUE,
            extractStringFromExtraData(response, "aiFeedback"),
            extractStringFromExtraData(response, "stepType"),
            extractStringFromExtraData(response, "questionType"),
            extractQuestionIndex(response),
            response.getCreatedAt()
        );
    }

    private String extractStringFromExtraData(Response response, String key) {
        Object value = response.getExtraData() == null ? null : response.getExtraData().get(key);
        return value == null ? null : value.toString();
    }

    private IndividualReadingFinishResponse toFinishResponse(ReadingRecord record) {
        Response representative = record.getRepresentResponse();

        return new IndividualReadingFinishResponse(
            record.getId(),
            record.getFinishedAt() != null,
            record.getFinishedAt(),
            record.getRating(),
            representative == null ? null : extractStringFromExtraData(representative, "question"),
            representative == null ? null : representative.getContent(),
            representative == null ? null : representative.getStage()
        );
    }

    private IndividualReadingArchiveItem toArchiveItem(ReadingRecord record) {
        Response representative = record.getRepresentResponse();
        Book book = record.getBook();

        return new IndividualReadingArchiveItem(
            record.getId(),
            book.getId(),
            book.getTitle(),
            book.getAuthor(),
            book.getCoverImage(),
            record.getFinishedAt(),
            record.getRating(),
            representative == null ? null : extractStringFromExtraData(representative, "question"),
            representative == null ? null : representative.getStage()
        );
    }

    /*
     * 완독한 기록도 조회는 허용해야 한다(나중에 보관함 상세 화면이 읽기 전
     * 질문·답을 보여줄 수 있게). 그래서 requireOwnedRecord만 쓰고
     * finishedAt 여부는 따로 검사하지 않는다 - 저장(쓰기)만 별도로 막는다.
     */
    @Transactional(readOnly = true)
    public List<IndividualBeforeResponseItem> getBeforeResponses(Long studentId, Long readingRecordId) {

        requireOwnedRecord(studentId, readingRecordId);

        return findBeforeResponses(studentId, readingRecordId).stream()
            .map(IndividualBeforeResponseItem::from)
            .toList();
    }

    /*
     * 같은 readingRecordId + 같은 stepType으로 다시 저장하면 새 행을
     * 만들지 않고 기존 행을 UPDATE한다. 다른 stepType이거나 다른
     * readingRecordId(같은 책을 재독해 bookId는 같아도)면 완전히 별도
     * 행으로 취급한다 - readingRecordId가 실제 FK 컬럼이라 이 조건을
     * DB 조회에서 그대로 걸 수 있다.
     */
    @Transactional
    public IndividualBeforeResponseItem saveBeforeResponse(
            Long studentId,
            Long readingRecordId,
            String stepType,
            IndividualBeforeResponseSaveRequest request) {

        if (!BEFORE_STEP_TYPES.contains(stepType)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "지원하지 않는 읽기 전 단계입니다. stepType=" + stepType
            );
        }

        /*
         * skipped(차례 없음)가 아닌 일반 저장은 기존과 동일하게 질문·답을
         * 반드시 요구한다. skipped=true는 "차례 없음" 버튼으로만 오는
         * 경로라 실제 작성값이 없는 게 정상이므로 여기서는 검증하지 않는다.
         */
        if (!request.isSkipped()) {
            if (request.getQuestion() == null || request.getQuestion().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "question은 비어 있을 수 없습니다.");
            }
            if (request.getAnswer() == null || request.getAnswer().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "answer는 비어 있을 수 없습니다.");
            }
        }

        ReadingRecord record = requireOwnedRecord(studentId, readingRecordId);

        if (record.getFinishedAt() != null) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "이미 완독한 책의 읽기 전 기록은 수정할 수 없습니다."
            );
        }

        List<Response> existing = findBeforeResponses(studentId, readingRecordId);

        Response response = existing.stream()
            .filter(r -> stepType.equals(extractStepType(r)))
            .findFirst()
            .orElseGet(() -> {
                Response newResponse = new Response();
                newResponse.setStudent(record.getStudent());
                newResponse.setReadingRecord(record);
                newResponse.setMode(MODE_INDIVIDUAL);
                newResponse.setContentType(CONTENT_TYPE_ANSWER);
                newResponse.setStage(STAGE_BEFORE);
                return newResponse;
            });

        /*
         * skipped=true일 때는 학생이 실제로 쓴 질문·답이 아니므로 절대
         * question/answer 텍스트를 content/extra_data에 저장하지 않는다
         * (빈 문자열로 저장). 대신 extra_data.skipped=true로 "차례 없음으로
         * 통과했다"는 사실만 남긴다. 아래 refreshBeforeDoneState()는 이
         * stepType이 저장돼 있는지만 보므로(내용은 보지 않음), 차례 없음도
         * 4단계 완료 판정에 정상적으로 포함된다.
         */
        response.setContent(request.isSkipped() ? "" : request.getAnswer());

        Map<String, Object> extraData = new HashMap<>();
        extraData.put("stepType", stepType);
        extraData.put("question", request.isSkipped() ? "" : request.getQuestion());
        extraData.put("skipped", request.isSkipped());
        response.setExtraData(extraData);

        Response saved = responseRepository.save(response);

        IndividualBeforeReadingRewardService.RewardResult rewardResult =
            refreshBeforeDoneState(studentId, record);

        IndividualBeforeResponseItem result = IndividualBeforeResponseItem.from(saved);

        if (rewardResult != null) {
            result.setRewardGranted(rewardResult.isRewardGranted());
            result.setStats(StudentStatsResponse.from(rewardResult.getStats()));
        }

        return result;
    }

    private List<Response> findBeforeResponses(Long studentId, Long readingRecordId) {
        return responseRepository
            .findByStudent_IdAndReadingRecord_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                studentId, readingRecordId, MODE_INDIVIDUAL, CONTENT_TYPE_ANSWER, STAGE_BEFORE);
    }

    /*
     * 프론트가 "4단계 다 됐다"고 보내는 값을 신뢰하지 않고, 실제 저장된
     * 행을 다시 조회해 4개 stepType이 모두 있는지 서버가 직접 판정한다.
     * 이미 beforeDone=true인 기록을 다시 false로 되돌리지는 않는다(그런
     * 경우가 생기지 않지만, 방어적으로 단조 증가만 허용).
     */
    private IndividualBeforeReadingRewardService.RewardResult refreshBeforeDoneState(
            Long studentId, ReadingRecord record) {

        List<Response> allBefore = findBeforeResponses(studentId, record.getId());

        Set<String> savedStepTypes = new HashSet<>();

        for (Response response : allBefore) {
            String stepType = extractStepType(response);

            if (stepType != null) {
                savedStepTypes.add(stepType);
            }
        }

        boolean allStepsSaved = savedStepTypes.containsAll(BEFORE_STEP_TYPES);

        if (allStepsSaved && !Boolean.TRUE.equals(record.getBeforeDone())) {
            record.setBeforeDone(true);
            record.setCurrentStage(STAGE_DURING);
            readingRecordRepository.save(record);

            return beforeReadingRewardService.grantBeforeCompleteRewardOnce(
                record.getStudent(), record.getId());
        }

        return null;
    }

    private String extractStepType(Response response) {
        Object stepType = response.getExtraData() == null
            ? null
            : response.getExtraData().get("stepType");

        return stepType == null ? null : stepType.toString();
    }

    /*
     * 개별읽기 읽기 중(책속 생각쓰기)의 "오늘 기록"을 조회한다. 서버가
     * 결정한 Asia/Seoul 오늘 날짜를 함께 돌려줘서, 프론트가 브라우저
     * 날짜를 신뢰하지 않고 이 값을 "오늘"로 쓰게 한다.
     */
    @Transactional(readOnly = true)
    public IndividualDuringTodayResponse getDuringResponsesToday(Long studentId, Long readingRecordId) {

        requireOwnedRecord(studentId, readingRecordId);

        LocalDate today = LocalDate.now(ZONE_SEOUL);

        List<IndividualDuringResponseItem> items = findDuringResponsesByDate(studentId, readingRecordId, today)
            .stream()
            .map(response -> IndividualDuringResponseItem.from(response, null))
            .toList();

        return new IndividualDuringTodayResponse(today, items);
    }

    /*
     * 개별읽기 읽기 중(책속 생각쓰기)의 전체 날짜 기록을 조회한다(질문
     * 보관함용). 완독 후에도 그대로 조회 가능하다(쓰기만 별도로 막는다).
     */
    @Transactional(readOnly = true)
    public List<IndividualDuringResponseItem> getDuringResponsesHistory(Long studentId, Long readingRecordId) {

        requireOwnedRecord(studentId, readingRecordId);

        return findDuringResponses(studentId, readingRecordId).stream()
            .map(response -> IndividualDuringResponseItem.from(response, null))
            .toList();
    }

    /*
     * 같은 readingRecordId + 같은 날짜(Asia/Seoul) + 같은 questionSlot으로
     * 다시 저장하면 새 행을 만들지 않고 그날의 최종본으로 UPDATE한다.
     * questionType은 그 슬롯의 유형 정보일 뿐이라 같은 유형을 여러 슬롯에서
     * 써도 각각 별도 행으로 보존된다. 날짜가 바뀌면 새 행을 INSERT한다
     * (전날 기록은 그대로 보존).
     *
     * 일일 완료 판정: 이 저장 이후 오늘 슬롯 1·2·3이 모두 존재할 때만
     * 일일 보상을 시도한다(1개·2개만 있을 때는 보상 서비스를 아예 호출하지
     * 않는다). 보상 서비스 자체가 readingRecordId+날짜당 1회만 지급하므로,
     * 3번째 저장 이후의 모든 재저장·수정·추가 호출에서도 이 조건은 계속
     * 참이지만 추가 지급은 일어나지 않는다(같은 트랜잭션 안에서 저장·판정·
     * 능력치 증가·보상 로그 저장이 모두 처리된다).
     */
    @Transactional
    public IndividualDuringResponseItem saveDuringResponse(
            Long studentId,
            Long readingRecordId,
            Integer questionSlot,
            IndividualDuringResponseSaveRequest request) {

        if (questionSlot == null || !DURING_QUESTION_SLOTS.contains(questionSlot)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "지원하지 않는 읽기 중 질문 슬롯입니다. questionSlot=" + questionSlot
            );
        }

        String questionType = request.getQuestionType();

        if (!DURING_QUESTION_TYPES.contains(questionType)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "지원하지 않는 읽기 중 질문 유형입니다. questionType=" + questionType
            );
        }

        ReadingRecord record = requireOwnedRecord(studentId, readingRecordId);

        if (record.getFinishedAt() != null) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "이미 완독한 책에는 새로운 읽기 중 활동을 추가할 수 없습니다."
            );
        }

        LocalDate today = LocalDate.now(ZONE_SEOUL);

        List<Response> todayResponses = findDuringResponsesByDate(studentId, readingRecordId, today);

        Response response = todayResponses.stream()
            .filter(r -> questionSlot.equals(extractQuestionSlot(r)))
            .findFirst()
            .orElseGet(() -> {
                Response newResponse = new Response();
                newResponse.setStudent(record.getStudent());
                newResponse.setReadingRecord(record);
                newResponse.setMode(MODE_INDIVIDUAL);
                newResponse.setContentType(CONTENT_TYPE_ANSWER);
                newResponse.setStage(STAGE_DURING);
                newResponse.setActivityDate(today);
                return newResponse;
            });

        response.setContent(request.getAnswer());

        Map<String, Object> extraData = new HashMap<>();
        extraData.put("questionSlot", questionSlot);
        extraData.put("questionType", questionType);
        extraData.put("question", request.getQuestion());
        response.setExtraData(extraData);

        Response saved = responseRepository.save(response);

        applyPageProgressIfIncreased(record.getStudent(), record, request.getCurrentPage());

        IndividualDuringReadingRewardService.RewardResult rewardResult = null;

        if (allDailySlotsFilled(studentId, readingRecordId, today)) {
            rewardResult = duringReadingRewardService.grantDuringDailyRewardOnce(
                record.getStudent(), readingRecordId, today);
        }

        IndividualDuringResponseItem result =
            IndividualDuringResponseItem.from(saved, record.getCurrentPage());

        if (rewardResult != null) {
            result.setRewardGranted(rewardResult.isRewardGranted());
            result.setStats(StudentStatsResponse.from(rewardResult.getStats()));
        }

        return result;
    }

    /*
     * 프론트가 "완료했다"고 보내는 값을 신뢰하지 않고, 오늘 저장된 행을
     * 다시 조회해 서로 다른 슬롯 1·2·3이 모두 존재하는지 서버가 직접 판정한다.
     */
    private boolean allDailySlotsFilled(Long studentId, Long readingRecordId, LocalDate date) {
        Set<Integer> filledSlots = new HashSet<>();

        for (Response response : findDuringResponsesByDate(studentId, readingRecordId, date)) {
            Integer slot = extractQuestionSlot(response);

            if (slot != null) {
                filledSlots.add(slot);
            }
        }

        return filledSlots.containsAll(DURING_QUESTION_SLOTS);
    }

    /*
     * 확정 최소 조건: currentPage == totalPages, 그리고 이 독서 세션에
     * 최종 통과한 읽기 중 질문·답이 최소 1개 이상 존재. 프론트가 보낸
     * 값은 전혀 받지 않고(body 없음) 서버가 저장된 값만으로 판정한다.
     * 이미 duringDone이면 그대로 현재 상태를 반환한다(idempotent).
     */
    @Transactional
    public IndividualReadingRecordResponse completeDuringReading(Long studentId, Long readingRecordId) {

        ReadingRecord record = requireOwnedRecord(studentId, readingRecordId);

        if (record.getFinishedAt() != null) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "이미 완독한 책입니다."
            );
        }

        if (Boolean.TRUE.equals(record.getDuringDone())) {
            return toResponse(record, record.getBook());
        }

        List<String> problems = new ArrayList<>();

        boolean pageComplete = record.getTotalPages() != null
            && record.getCurrentPage() != null
            && record.getCurrentPage().intValue() == record.getTotalPages().intValue();

        if (!pageComplete) {
            problems.add("전체 쪽수만큼 아직 다 읽지 않았어요.");
        }

        boolean hasDuringResponse = !findDuringResponses(studentId, readingRecordId).isEmpty();

        if (!hasDuringResponse) {
            problems.add("책 속 생각쓰기를 아직 하나도 완성하지 않았어요.");
        }

        if (!problems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.join(" ", problems));
        }

        record.setDuringDone(true);
        record.setCurrentStage(STAGE_AFTER);
        ReadingRecord saved = readingRecordRepository.save(record);

        return toResponse(saved, saved.getBook());
    }

    /*
     * 완독한 기록도 조회는 허용한다(추후 보관함 상세 화면용). 쓰기만 별도로 막는다.
     */
    @Transactional(readOnly = true)
    public List<IndividualAfterResponseItem> getAfterResponses(Long studentId, Long readingRecordId) {

        requireOwnedRecord(studentId, readingRecordId);

        return findAfterResponses(studentId, readingRecordId).stream()
            .map(IndividualAfterResponseItem::from)
            .toList();
    }

    /*
     * 읽기 후 간추리기 질문·답 한 세트(질문 번호 1/2/3)를 저장한다. 읽기 전과
     * 같은 find-or-create-by-extraData-key 관례를 쓰되, aiPassed는 프론트가
     * "루미 피드백을 이미 통과했다"고 보내는 값을 그대로 믿지 않고, 완료
     * API(completeAfterReading)에서 다시 한 번 서버가 재검증한다 - 여기서는
     * 명시적으로 false가 온 경우만 저장을 막는다(아직 통과하지 못한 답이
     * "통과"로 남는 것을 방지).
     */
    @Transactional
    public IndividualAfterResponseItem saveAfterResponse(
            Long studentId,
            Long readingRecordId,
            Integer questionIndex,
            IndividualAfterResponseSaveRequest request) {

        if (questionIndex == null || !AFTER_QUESTION_INDEXES.contains(questionIndex)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "지원하지 않는 읽기 후 질문 번호입니다. questionIndex=" + questionIndex
            );
        }

        ReadingRecord record = requireOwnedRecord(studentId, readingRecordId);

        if (record.getFinishedAt() != null) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "이미 완독한 책의 읽기 후 기록은 수정할 수 없습니다."
            );
        }

        String question = request.getQuestion() == null ? "" : request.getQuestion().trim();
        String answer = request.getAnswer() == null ? "" : request.getAnswer().trim();

        if (question.isEmpty() || answer.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "질문과 답을 모두 입력해야 합니다.");
        }

        if (Boolean.FALSE.equals(request.getAiPassed())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "루미 피드백을 통과한 질문과 답만 저장할 수 있어요."
            );
        }

        List<Response> existing = findAfterResponses(studentId, readingRecordId);

        Response response = existing.stream()
            .filter(r -> questionIndex.equals(extractQuestionIndex(r)))
            .findFirst()
            .orElseGet(() -> {
                Response newResponse = new Response();
                newResponse.setStudent(record.getStudent());
                newResponse.setReadingRecord(record);
                newResponse.setMode(MODE_INDIVIDUAL);
                newResponse.setContentType(CONTENT_TYPE_ANSWER);
                newResponse.setStage(STAGE_AFTER);
                return newResponse;
            });

        response.setContent(answer);
        response.setPassed(request.getAiPassed() == null ? true : request.getAiPassed());

        Map<String, Object> extraData = new HashMap<>();
        extraData.put("questionIndex", questionIndex);
        extraData.put("question", question);

        String aiFeedback = request.getAiFeedback() == null ? "" : request.getAiFeedback().trim();
        if (!aiFeedback.isEmpty()) {
            extraData.put("aiFeedback", aiFeedback);
        }

        response.setExtraData(extraData);

        Response saved = responseRepository.save(response);

        return IndividualAfterResponseItem.from(saved);
    }

    private List<Response> findAfterResponses(Long studentId, Long readingRecordId) {
        return responseRepository
            .findByStudent_IdAndReadingRecord_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                studentId, readingRecordId, MODE_INDIVIDUAL, CONTENT_TYPE_ANSWER, STAGE_AFTER);
    }

    private Integer extractQuestionIndex(Response response) {
        Object value = response.getExtraData() == null
            ? null
            : response.getExtraData().get("questionIndex");

        if (value instanceof Number number) {
            return number.intValue();
        }

        if (value == null) {
            return null;
        }

        try {
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /*
     * 완독한 기록도 조회는 허용한다. 아직 저장된 간추리기가 없으면 empty를
     * 돌려준다(컨트롤러가 204로 변환).
     */
    @Transactional(readOnly = true)
    public Optional<IndividualSummaryResponse> getAfterSummary(Long studentId, Long readingRecordId) {

        requireOwnedRecord(studentId, readingRecordId);

        return summaryRepository.findByStudent_IdAndReadingRecord_Id(studentId, readingRecordId)
            .map(IndividualSummaryResponse::from);
    }

    /*
     * 최종 간추린 내용을 저장한다(summaries 테이블, reading_record_id로 연결).
     * 같은 readingRecordId로 다시 저장하면 새 행 대신 기존 행을 UPDATE한다 -
     * 다른 책(다른 readingRecordId)의 간추리기와는 절대 섞이지 않는다.
     */
    @Transactional
    public IndividualSummaryResponse saveAfterSummary(
            Long studentId,
            Long readingRecordId,
            IndividualSummarySaveRequest request) {

        ReadingRecord record = requireOwnedRecord(studentId, readingRecordId);

        if (record.getFinishedAt() != null) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "이미 완독한 책의 읽기 후 기록은 수정할 수 없습니다."
            );
        }

        String bookType = request.getBookType() == null ? "" : request.getBookType().trim();
        String summaryText = request.getSummaryText() == null ? "" : request.getSummaryText().trim();

        if (bookType.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "책 종류를 선택해야 합니다.");
        }

        if (summaryText.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "최종 간추린 내용을 입력해야 합니다.");
        }

        if (Boolean.FALSE.equals(request.getAiPassed())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "루미 피드백을 통과한 간추리기만 저장할 수 있어요."
            );
        }

        Summary summary = summaryRepository
            .findByStudent_IdAndReadingRecord_Id(studentId, readingRecordId)
            .orElseGet(Summary::new);

        summary.setStudent(record.getStudent());
        summary.setReadingRecord(record);
        summary.setBookType(bookType);
        summary.setSummaryText(summaryText);
        summary.setStatus("approved");
        summary.setAiPassed(request.getAiPassed() == null ? true : request.getAiPassed());

        if (summary.getIsShared() == null) {
            summary.setIsShared(false);
        }

        Summary saved = summaryRepository.save(summary);

        return IndividualSummaryResponse.from(saved);
    }

    /*
     * 읽기 후 최종 완료 처리. 프론트가 보낸 값은 전혀 받지 않고(body 없음)
     * 서버에 저장된 값만으로 판정한다: 질문·답 3세트가 모두 존재하고 모두
     * AI 통과 상태이며, 최종 간추린 내용이 존재하고 AI 통과 상태여야 한다.
     * 조건을 만족하지 못하면 무엇이 부족한지 메시지로 안내하고 afterDone은
     * 그대로 둔다(부분 완료 상태로 남지 않음).
     *
     * afterDone 갱신과 보상 지급이 같은 트랜잭션 안에서 함께 처리되므로,
     * 보상만 지급되고 afterDone이 false로 남거나 그 반대가 되는 상황이
     * 생기지 않는다. 이미 afterDone=true인 기록에 다시 호출해도(새로고침 후
     * 재클릭 등) 검증을 다시 하지 않고 보상 서비스의 자체 중복 방지에 맡긴다
     * (idempotent).
     */
    @Transactional
    public IndividualAfterCompleteResponse completeAfterReading(Long studentId, Long readingRecordId) {

        ReadingRecord record = requireOwnedRecord(studentId, readingRecordId);

        if (record.getFinishedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 완독한 책입니다.");
        }

        if (!Boolean.TRUE.equals(record.getAfterDone())) {
            validateAfterReadingComplete(studentId, readingRecordId);
            record.setAfterDone(true);
            readingRecordRepository.save(record);
        }

        IndividualAfterReadingRewardService.RewardResult rewardResult =
            afterReadingRewardService.grantAfterCompleteRewardOnce(record.getStudent(), readingRecordId);

        return new IndividualAfterCompleteResponse(
            record.getId(),
            record.getAfterDone(),
            record.getCurrentStage(),
            rewardResult.isRewardGranted(),
            StudentStatsResponse.from(rewardResult.getStats())
        );
    }

    private void validateAfterReadingComplete(Long studentId, Long readingRecordId) {

        List<Response> afterResponses = findAfterResponses(studentId, readingRecordId);

        Map<Integer, Response> byIndex = new HashMap<>();
        for (Response response : afterResponses) {
            Integer index = extractQuestionIndex(response);
            if (index != null) {
                byIndex.put(index, response);
            }
        }

        List<String> problems = new ArrayList<>();

        for (Integer index : AFTER_QUESTION_INDEXES) {
            Response response = byIndex.get(index);

            if (response == null || response.getContent() == null || response.getContent().isBlank()) {
                problems.add("질문 " + index + "의 답이 아직 없어요.");
            } else if (!Boolean.TRUE.equals(response.getPassed())) {
                problems.add("질문 " + index + "이 아직 루미 피드백을 통과하지 못했어요.");
            }
        }

        Summary summary = summaryRepository
            .findByStudent_IdAndReadingRecord_Id(studentId, readingRecordId)
            .orElse(null);

        if (summary == null || summary.getSummaryText() == null || summary.getSummaryText().isBlank()) {
            problems.add("최종 간추린 내용이 아직 없어요.");
        } else if (!Boolean.TRUE.equals(summary.getAiPassed())) {
            problems.add("최종 간추리기가 아직 루미 피드백을 통과하지 못했어요.");
        }

        if (!problems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.join(" ", problems));
        }
    }

    /*
 * 학생이 지금까지 개별읽기로 등록한 모든 reading_records를 집계한다.
 *
 * 진행 중인 책도 책을 등록한 순간부터 통계에 포함하며,
 * 완독 후에도 기록이 남아 계속 누적된다.
 *
 * 같은 책을 다시 읽더라도 새로운 readingRecordId가 생성되므로
 * 새로운 독서 기록 1건으로 집계한다.
 *
 * books.book_type을 화면 표시용 4종으로 변환한다.
 * - story   → 이야기책
 * - info    → 정보를 담은 책
 * - opinion → 주장을 담은 책
 * - 그 외   → 그 밖의 책
 */
@Transactional(readOnly = true)
public BookTypeStatsResponse getBookTypeStats(Long studentId) {

    List<ReadingRecord> records =
        readingRecordRepository.findByStudent_IdOrderByCreatedAtDesc(studentId);

    int story = 0;
    int information = 0;
    int argument = 0;
    int other = 0;

    for (ReadingRecord record : records) {
        String bookType = record.getBook().getBookType();

        if ("story".equals(bookType)) {
            story++;
        } else if ("info".equals(bookType)) {
            information++;
        } else if ("opinion".equals(bookType)) {
            argument++;
        } else {
            other++;
        }
    }

    int total = records.size();

    return new BookTypeStatsResponse(
        total,
        story,
        information,
        argument,
        other,
        new BookTypeStatsResponse.Percentages(
            percentOf(story, total),
            percentOf(information, total),
            percentOf(argument, total),
            percentOf(other, total)
        )
    );
}

    /*
     * 학생 메인 화면 "월별 완독 기록" 그래프용. 완독(finished_at IS NOT NULL)한
     * 기록만 집계 대상이며, 현재 연도(Asia/Seoul 기준)에 완독한 것만 1~12월에
     * 나눠 센다. 다른 해에 완독한 기록은 포함하지 않는다. 반환되는
     * monthlyCounts는 항상 12개 원소이고(0권인 달도 0으로 채움), index 0이
     * 1월이다.
     */
    @Transactional(readOnly = true)
    public MonthlyCompletionStatsResponse getMonthlyCompletionStats(Long studentId) {

        int currentYear = LocalDate.now(ZONE_SEOUL).getYear();

        List<ReadingRecord> completedRecords =
            readingRecordRepository.findByStudent_IdAndFinishedAtIsNotNull(studentId);

        int[] monthlyCounts = new int[12];

        for (ReadingRecord record : completedRecords) {
            LocalDateTime finishedAt = record.getFinishedAt();

            if (finishedAt != null && finishedAt.getYear() == currentYear) {
                monthlyCounts[finishedAt.getMonthValue() - 1]++;
            }
        }

        List<Integer> monthlyCountsList = new ArrayList<>();
        for (int count : monthlyCounts) {
            monthlyCountsList.add(count);
        }

        return new MonthlyCompletionStatsResponse(currentYear, monthlyCountsList);
    }

    private double percentOf(int count, int total) {
        if (total == 0) {
            return 0.0;
        }

        return Math.round(count * 1000.0 / total) / 10.0;
    }

    private List<Response> findDuringResponses(Long studentId, Long readingRecordId) {
        return responseRepository
            .findByStudent_IdAndReadingRecord_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                studentId, readingRecordId, MODE_INDIVIDUAL, CONTENT_TYPE_ANSWER, STAGE_DURING);
    }

    private List<Response> findDuringResponsesByDate(Long studentId, Long readingRecordId, LocalDate date) {
        return responseRepository
            .findByStudent_IdAndReadingRecord_IdAndModeAndContentTypeAndStageAndActivityDateAndDeletedAtIsNullOrderByIdAsc(
                studentId, readingRecordId, MODE_INDIVIDUAL, CONTENT_TYPE_ANSWER, STAGE_DURING, date);
    }

    private Integer extractQuestionSlot(Response response) {
        Object slot = response.getExtraData() == null
            ? null
            : response.getExtraData().get("questionSlot");

        if (slot == null) {
            return null;
        }

        if (slot instanceof Number number) {
            return number.intValue();
        }

        try {
            return Integer.parseInt(slot.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /*
     * 읽기 중(책속 생각쓰기) 저장에 곁들여 온 currentPage는 참고용이라,
     * PUT .../pages처럼 엄격하게 검증(감소 거부)하지 않는다 - 이전 기록보다
     * 늘어난 경우에만 반영하고, 아니면 조용히 무시한다(질문 저장 자체는
     * 그대로 성공시킨다).
     */
    private void applyPageProgressIfIncreased(User student, ReadingRecord record, Integer newCurrentPage) {

        if (newCurrentPage == null) {
            return;
        }

        Integer existingCurrentPage = record.getCurrentPage();

        if (existingCurrentPage != null && newCurrentPage <= existingCurrentPage) {
            return;
        }

        Integer totalPages = record.getTotalPages();
        int clamped = totalPages != null ? Math.min(newCurrentPage, totalPages) : newCurrentPage;

        record.setCurrentPage(clamped);
        ReadingRecord saved = readingRecordRepository.save(record);

        upsertTodayProgressLog(student, saved, clamped, totalPages == null ? clamped : totalPages);
    }

    /*
     * 같은 날 여러 번 저장해도 그날 한 행만 UPSERT한다(중복 합산 금지).
     * "오늘 읽은 쪽수"는 이 값과 직전 날짜 로그의 차이로 별도 계산한다.
     */
    private ReadingProgressLog upsertTodayProgressLog(
            User student, ReadingRecord record, int currentPage, int totalPages) {

        LocalDate today = LocalDate.now(ZONE_SEOUL);

        ReadingProgressLog log = readingProgressLogRepository
            .findByStudent_IdAndReadingRecord_IdAndLogDate(student.getId(), record.getId(), today)
            .orElseGet(() -> {
                ReadingProgressLog newLog = new ReadingProgressLog();
                newLog.setStudent(student);
                newLog.setReadingRecord(record);
                newLog.setLogDate(today);
                return newLog;
            });

        log.setCumulativePage(currentPage);
        log.setTotalPages(totalPages);
        log.setProgressPercent((int) percentOf(currentPage, totalPages));

        return readingProgressLogRepository.save(log);
    }

    /*
     * "오늘 읽은 쪽수"(당일 증가분) = 오늘까지의 누적 쪽수 - 오늘보다
     * 이전인 가장 최근 날짜의 누적 쪽수(없으면 0, 즉 이 책을 처음 읽기
     * 시작한 날). 같은 날 몇 번을 저장하든 이 계산 자체는 매번 동일한
     * 기준(직전 "날짜")으로만 비교하므로 10+25=35처럼 중복 합산되지 않는다.
     */
    private int computeTodayReadPages(Long studentId, Long readingRecordId, Integer currentPage) {

        if (currentPage == null) {
            return 0;
        }

        LocalDate today = LocalDate.now(ZONE_SEOUL);

        int previousCumulative = readingProgressLogRepository
            .findTopByStudent_IdAndReadingRecord_IdAndLogDateLessThanOrderByLogDateDesc(
                studentId, readingRecordId, today)
            .map(ReadingProgressLog::getCumulativePage)
            .orElse(0);

        return Math.max(0, currentPage - previousCumulative);
    }

    private ReadingRecord requireOwnedRecord(Long studentId, Long readingRecordId) {
        return readingRecordRepository.findByIdAndStudent_Id(readingRecordId, studentId)
            .orElseThrow(() -> {
                boolean existsForAnyStudent = readingRecordRepository.findById(readingRecordId).isPresent();

                if (existsForAnyStudent) {
                    return new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "본인의 독서 기록만 수정할 수 있습니다."
                    );
                }

                return new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "독서 기록을 찾을 수 없습니다. readingRecordId=" + readingRecordId
                );
            });
    }

    private IndividualReadingRecordResponse toResponse(ReadingRecord record, Book book) {
        LocalDate today = LocalDate.now(ZONE_SEOUL);

        Integer currentPage = record.getCurrentPage();
        Integer totalPages = record.getTotalPages();

        int todayReadPages = computeTodayReadPages(record.getStudent().getId(), record.getId(), currentPage);
        int progressPercent = (currentPage == null || totalPages == null || totalPages <= 0)
            ? 0
            : (int) percentOf(currentPage, totalPages);

        return new IndividualReadingRecordResponse(
            record.getId(),
            book.getId(),
            book.getTitle(),
            book.getAuthor(),
            book.getBookType(),
            book.getCoverImage(),
            totalPages,
            currentPage,
            record.getCurrentStage(),
            record.getBeforeDone(),
            record.getDuringDone(),
            record.getAfterDone(),
            record.getCreatedAt(),
            record.getFinishedAt(),
            record.getFinishedAt() != null,
            todayReadPages,
            progressPercent,
            today
        );
    }
}
