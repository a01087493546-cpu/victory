package com.victory.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.AfterReadingDataResponse;
import com.victory.dto.AfterReadingBookTypeRequest;
import com.victory.dto.AfterReadingQuestionItem;
import com.victory.dto.AfterReadingQuestionRequest;
import com.victory.dto.AfterReadingQuestionSaveRequest;
import com.victory.dto.AfterReadingSaveRequest;
import com.victory.dto.AfterReadingSummaryItem;
import com.victory.dto.AfterReadingSummarySaveRequest;
import com.victory.dto.AfterReadingTypePracticeItem;
import com.victory.dto.AfterReadingTypePracticeRequest;
import com.victory.dto.PracticeProgressRequest;
import com.victory.dto.PracticeProgressResponse;
import com.victory.entity.ClassReadingBook;
import com.victory.entity.ClassStudent;
import com.victory.entity.ContentLike;
import com.victory.entity.PracticeProgress;
import com.victory.entity.Response;
import com.victory.entity.SchoolClass;
import com.victory.entity.Summary;
import com.victory.entity.User;
import com.victory.repository.ClassReadingBookRepository;
import com.victory.repository.ClassStudentRepository;
import com.victory.repository.ContentLikeRepository;
import com.victory.repository.PracticeProgressRepository;
import com.victory.repository.ResponseRepository;
import com.victory.repository.SchoolClassRepository;
import com.victory.repository.SummaryRepository;
import com.victory.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AfterReadingService {

    private static final String MODE_CLASS = "class";
    private static final String CONTENT_TYPE_ANSWER = "answer";
    private static final String STAGE_AFTER = "after";
    private static final String ACTIVITY_TYPE_AFTER_QUESTION = "after_reading_question";
    private static final String ACTIVITY_TYPE_AFTER_BOOK_TYPE = "after_reading_book_type";
    private static final String ACTIVITY_TYPE_TYPE_PRACTICE = "after_reading_type_practice";
    private static final String CONTENT_TYPE_SUMMARY = "summary";
    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_APPROVED = "approved";
    private static final String STATUS_REJECTED = "rejected";

    private final SummaryRepository summaryRepository;
    private final ResponseRepository responseRepository;
    private final ContentLikeRepository contentLikeRepository;
    private final UserRepository userRepository;
    private final ClassStudentRepository classStudentRepository;
    private final ClassReadingBookRepository classReadingBookRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final PracticeProgressRepository practiceProgressRepository;
    private final PracticeProgressService practiceProgressService;
    private final DemoAccountService demoAccountService;

    public AfterReadingDataResponse getMyAfterReadingData(
            Long studentId,
            Long classReadingBookId) {

        ClassStudent classStudent = findClassStudent(studentId);
        validateClassReadingBookBelongsToClass(
            classReadingBookId,
            classStudent.getSchoolClass().getId());

        Summary summary = summaryRepository
            .findByStudent_IdAndClassReadingBookId(studentId, classReadingBookId)
            .orElse(null);

        Boolean afterDone = practiceProgressRepository
            .findByStudent_Id(studentId)
            .map(PracticeProgress::getAfterDone)
            .orElse(false);

        String savedBookType = summary == null
            ? findAfterReadingBookType(studentId, classReadingBookId)
            : summary.getBookType();

        Map<String, List<AfterReadingQuestionItem>> questionsByBookType =
            buildQuestionsByBookType(studentId, classReadingBookId, savedBookType);
        if (savedBookType == null && questionsByBookType.size() == 1) {
            savedBookType = questionsByBookType.keySet().iterator().next();
        }
        List<AfterReadingQuestionItem> questions = savedBookType == null
            ? List.of()
            : questionsByBookType.getOrDefault(savedBookType, List.of());

        return AfterReadingDataResponse.of(
            classReadingBookId,
            summary,
            savedBookType,
            questions,
            afterDone,
            findTypePracticeAnswers(studentId, classReadingBookId),
            questionsByBookType);
    }

    @Transactional
    public AfterReadingDataResponse saveBookType(
            Long studentId,
            AfterReadingBookTypeRequest request) {

        String bookType = request.getBookType() == null
            ? ""
            : request.getBookType().trim();

        if (bookType.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "책 유형을 선택해야 합니다."
            );
        }

        User student = findStudent(studentId);
        ClassStudent classStudent = findClassStudent(studentId);
        validateClassReadingBookBelongsToClass(
            request.getClassReadingBookId(),
            classStudent.getSchoolClass().getId());

        String previousBookType = findAfterReadingBookType(studentId, request.getClassReadingBookId());
        if (previousBookType != null && !previousBookType.isBlank()) {
            findAfterReadingQuestions(studentId, request.getClassReadingBookId()).stream()
                .filter(item -> extractExtraField(item, "bookType") == null)
                .forEach(item -> {
                    Map<String, Object> migrated = new HashMap<>(item.getExtraData());
                    migrated.put("bookType", previousBookType);
                    item.setExtraData(migrated);
                    responseRepository.save(item);
                });
        }

        Response response = responseRepository
            .findByStudent_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                studentId,
                MODE_CLASS,
                CONTENT_TYPE_ANSWER,
                STAGE_AFTER)
            .stream()
            .filter(item ->
                ACTIVITY_TYPE_AFTER_BOOK_TYPE.equals(
                    extractExtraField(item, "activityType")))
            .filter(item ->
                request.getClassReadingBookId().equals(
                    extractClassReadingBookId(item)))
            .findFirst()
            .orElseGet(() -> createAfterQuestionResponse(student));

        response.setContent(bookType);
        response.setPassed(true);

        Map<String, Object> extraData = new HashMap<>();
        extraData.put("activityType", ACTIVITY_TYPE_AFTER_BOOK_TYPE);
        extraData.put("classReadingBookId", request.getClassReadingBookId());
        response.setExtraData(extraData);

        responseRepository.save(response);

        return getMyAfterReadingData(studentId, request.getClassReadingBookId());
    }

    @Transactional
    public AfterReadingDataResponse saveMyAfterReadingData(
            Long studentId,
            AfterReadingSaveRequest request) {

        validateCompleteRequest(request);

        User student = findStudent(studentId);
        ClassStudent classStudent = findClassStudent(studentId);
        validateClassReadingBookBelongsToClass(
            request.getClassReadingBookId(),
            classStudent.getSchoolClass().getId());

        List<Response> existingQuestions =
            findAfterReadingQuestions(studentId, request.getClassReadingBookId());

        for (AfterReadingQuestionRequest questionRequest : request.getQuestions()) {
            upsertAfterReadingQuestionResponse(
                student,
                request.getClassReadingBookId(),
                existingQuestions,
                request.getBookType().trim(),
                questionRequest.getIndex(),
                questionRequest.getQuestion(),
                questionRequest.getAnswer(),
                questionRequest.getAiPassed());
        }

        Summary summary = summaryRepository
            .findByStudent_IdAndClassReadingBookId(
                studentId,
                request.getClassReadingBookId())
            .orElseGet(Summary::new);

        /*
         * 이 메서드는 최초 제출과(거절된 글의) 재제출을 모두 처리하는
         * 하나의 upsert 경로다(연습읽기는 개별 resubmit 엔드포인트를
         * 프론트에서 쓰지 않고 이 저장 경로로 재제출까지 이어진다).
         * "이전 상태가 rejected였고 본문이 실제로 바뀐 경우"에만 기존
         * 좋아요를 지운다 - 수정 전 글에 대한 반응이 수정본에 그대로
         * 이어지면 안 되기 때문이다. 교사가 상태만 바꾸는 reviewSummary는
         * 이 메서드를 타지 않으므로 좋아요가 그대로 보존된다.
         */
        boolean wasRejected = "rejected".equalsIgnoreCase(summary.getStatus());
        String previousSummaryText = summary.getSummaryText();
        String newSummaryText = request.getSummary().trim();

        summary.setStudent(student);
        summary.setClassReadingBookId(request.getClassReadingBookId());
        summary.setBookType(request.getBookType().trim());
        summary.setSummaryText(newSummaryText);
        summary.setIsShared(true);
        summary.setStatus("pending");
        summary.setRejectionReason(null);
        summary.setAiPassed(request.getSummaryAiPassed() == null
            ? true
            : request.getSummaryAiPassed());

        Summary saved = summaryRepository.save(summary);

        if (wasRejected && !newSummaryText.equals(previousSummaryText)) {
            contentLikeRepository.deleteByContentTypeAndContentId(CONTENT_TYPE_SUMMARY, saved.getId());
        }

        PracticeProgressRequest progressRequest = new PracticeProgressRequest();
        progressRequest.setAfterDone(true);
        PracticeProgressResponse progressResponse =
            practiceProgressService.saveProgress(studentId, progressRequest);

        return getMyAfterReadingData(studentId, request.getClassReadingBookId())
            .withPracticeReward(progressResponse);
    }

    /*
     * "루미 피드백 통과 = 자동저장" 정책 - 학생이 내 책 질문 1개(question1~3
     * 중 하나)를 AI에게 통과받은 시점에, 아직 나머지 질문/간추리기가
     * 없어도 그 질문 하나만 즉시 저장한다. saveMyAfterReadingData(최종
     * 완료)와 같은 upsert 로직(upsertAfterReadingQuestionResponse)을
     * 재사용하되, summary/afterDone/보상은 전혀 건드리지 않는다 - 완료
     * 처리는 여전히 saveSummary()가 부르는 saveMyAfterReadingData에서만
     * 일어난다.
     */
    @Transactional
    public AfterReadingDataResponse saveAfterReadingQuestionDraft(
            Long studentId,
            AfterReadingQuestionSaveRequest request) {

        User student = findStudent(studentId);
        ClassStudent classStudent = findClassStudent(studentId);
        validateClassReadingBookBelongsToClass(
            request.getClassReadingBookId(),
            classStudent.getSchoolClass().getId());

        List<Response> existingQuestions =
            findAfterReadingQuestions(studentId, request.getClassReadingBookId());

        String draftBookType = request.getBookType() == null || request.getBookType().isBlank()
            ? findAfterReadingBookType(studentId, request.getClassReadingBookId())
            : request.getBookType().trim();
        if (draftBookType == null || draftBookType.isBlank()) {
            draftBookType = "story";
        }

        upsertAfterReadingQuestionResponse(
            student,
            request.getClassReadingBookId(),
            existingQuestions,
            draftBookType,
            request.getIndex(),
            request.getQuestion(),
            request.getAnswer(),
            true);

        return getMyAfterReadingData(studentId, request.getClassReadingBookId());
    }

    /*
     * 위와 같은 정책을 간추리기에도 적용한다. isShared/status/afterDone은
     * 절대 건드리지 않는다 - "우리 반 간추리기 모음"에 공개되는 시점은
     * 여전히 saveMyAfterReadingData(최종 "다음으로" 버튼)뿐이다. 이미
     * 공유된 뒤에 학생이 다시 고쳐서 재통과하면, 최종 완료 때와 동일하게
     * 같은 행을 덮어써(UPDATE) 이미 공개된 카드도 최신 내용을 보여준다
     * (isShared 값 자체를 여기서 바꾸지 않으므로 true였으면 true로 유지된다).
     */
    @Transactional
    public AfterReadingDataResponse saveAfterReadingSummaryDraft(
            Long studentId,
            AfterReadingSummarySaveRequest request) {

        User student = findStudent(studentId);
        ClassStudent classStudent = findClassStudent(studentId);
        validateClassReadingBookBelongsToClass(
            request.getClassReadingBookId(),
            classStudent.getSchoolClass().getId());

        Summary summary = summaryRepository
            .findByStudent_IdAndClassReadingBookId(
                studentId,
                request.getClassReadingBookId())
            .orElseGet(Summary::new);

        summary.setStudent(student);
        summary.setClassReadingBookId(request.getClassReadingBookId());
        summary.setBookType(request.getBookType().trim());
        summary.setSummaryText(request.getSummary().trim());
        summary.setAiPassed(true);

        summaryRepository.save(summary);

        return getMyAfterReadingData(studentId, request.getClassReadingBookId());
    }

    /*
     * 책 유형별(이야기책/정보를 담은 책/주장을 담은 책) 연습 질문 2개+답
     * 2개는 AfterReadingSaveRequest/PracticeProgress 어디에도 저장되지
     * 않던 화면 전용 값이었다 - 이제 루미 검사를 통과한 순간 이 유형의
     * 최신 통과본으로 upsert한다(같은 studentId+classReadingBookId+bookType
     * 조합이면 새 row를 쌓지 않고 덮어쓴다).
     */
    @Transactional
    public AfterReadingDataResponse saveTypePracticeAnswers(
            Long studentId,
            AfterReadingTypePracticeRequest request) {

        User student = findStudent(studentId);
        ClassStudent classStudent = findClassStudent(studentId);
        validateClassReadingBookBelongsToClass(
            request.getClassReadingBookId(),
            classStudent.getSchoolClass().getId());

        String bookType = request.getBookType().trim();

        Response response = findTypePracticeResponses(studentId, request.getClassReadingBookId())
            .stream()
            .filter(existing -> bookType.equals(extractExtraField(existing, "bookType")))
            .findFirst()
            .orElseGet(() -> createAfterQuestionResponse(student));

        response.setContent(bookType);
        response.setPassed(true);

        Map<String, Object> extraData = new HashMap<>();
        extraData.put("activityType", ACTIVITY_TYPE_TYPE_PRACTICE);
        extraData.put("classReadingBookId", request.getClassReadingBookId());
        extraData.put("bookType", bookType);
        extraData.put("question1", request.getQuestion1().trim());
        extraData.put("answer1", request.getAnswer1().trim());
        extraData.put("question2", request.getQuestion2().trim());
        extraData.put("answer2", request.getAnswer2().trim());
        response.setExtraData(extraData);

        responseRepository.save(response);

        return getMyAfterReadingData(studentId, request.getClassReadingBookId());
    }

    public List<AfterReadingSummaryItem> getSharedSummaries(
            Long studentId,
            Long classReadingBookId) {

        ClassStudent viewerClassStudent = findClassStudent(studentId);
        validateClassReadingBookBelongsToClass(
            classReadingBookId,
            viewerClassStudent.getSchoolClass().getId());

        return buildSummaryItems(
            classReadingBookId,
            studentId,
            viewerClassStudent.getSchoolClass().getId(),
            studentId);
    }

    public List<AfterReadingSummaryItem> getTeacherSharedSummaries(
            Long teacherId,
            Long classId,
            Long classReadingBookId) {

        SchoolClass teacherClass = findTeacherClass(teacherId);

        if (!teacherClass.getId().equals(classId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "담당 학급의 간추리기만 조회할 수 있습니다."
            );
        }

        validateClassReadingBookBelongsToClass(classReadingBookId, classId);

        return buildSummaryItems(classReadingBookId, teacherId, classId, null);
    }

    public List<AfterReadingSummaryItem> getTeacherReviewSummaries(Long teacherId, Long classId, String status) {
        SchoolClass teacherClass = findTeacherClass(teacherId);
        if (!teacherClass.getId().equals(classId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "담당 학급의 간추리기만 조회할 수 있습니다.");
        }
        String normalized = status == null || status.isBlank() ? null : status.trim().toLowerCase();
        Map<Long, ClassStudent> members = classStudentRepository.findBySchoolClassId(classId).stream()
            .collect(Collectors.toMap(item -> item.getStudent().getId(), item -> item));
        return summaryRepository.findAllReviewablePracticeSummariesByStudentIds(members.keySet().stream().toList()).stream()
            .filter(summary -> normalized == null || normalized.equals(normalizeSummaryStatus(summary)))
            .map(summary -> AfterReadingSummaryItem.from(summary, members.get(summary.getStudent().getId()),
                contentLikeRepository.countByContentTypeAndContentId(CONTENT_TYPE_SUMMARY, summary.getId()), false, null))
            .toList();
    }

    @Transactional
    public AfterReadingSummaryItem reviewSummary(Long teacherId, Long classId, Long summaryId, String status, String reason) {
        SchoolClass teacherClass = findTeacherClass(teacherId);
        if (!teacherClass.getId().equals(classId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "담당 학급만 관리할 수 있습니다.");
        Summary summary = summaryRepository.findById(summaryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "간추리기를 찾을 수 없습니다."));
        validateClassReadingBookBelongsToClass(summary.getClassReadingBookId(), classId);
        String normalized = status == null ? "" : status.trim().toLowerCase();
        if (!Set.of(STATUS_PENDING, STATUS_APPROVED, STATUS_REJECTED).contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "대기, 승인 또는 거절 상태만 지정할 수 있습니다.");
        }
        if (STATUS_REJECTED.equals(normalized) && (reason == null || reason.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "거절 사유를 입력해야 합니다.");
        }
        summary.setStatus(normalized);
        summary.setRejectionReason(STATUS_REJECTED.equals(normalized) ? reason.trim() : null);
        Summary saved = summaryRepository.save(summary);
        return AfterReadingSummaryItem.from(saved, null,
            contentLikeRepository.countByContentTypeAndContentId(CONTENT_TYPE_SUMMARY, saved.getId()), false, null);
    }

    @Transactional
    public AfterReadingSummaryItem resubmitSummary(Long studentId, Long summaryId, String text) {
        Summary summary = requireOwnedRejectedSummary(studentId, summaryId);
        if (text == null || text.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "간추리기 내용을 입력해야 합니다.");
        String trimmed = text.trim();
        boolean textChanged = !trimmed.equals(summary.getSummaryText());
        summary.setSummaryText(trimmed);
        summary.setStatus(STATUS_PENDING);
        summary.setRejectionReason(null);
        Summary saved = summaryRepository.save(summary);
        if (textChanged) {
            contentLikeRepository.deleteByContentTypeAndContentId(CONTENT_TYPE_SUMMARY, saved.getId());
        }
        return AfterReadingSummaryItem.from(saved, null,
            contentLikeRepository.countByContentTypeAndContentId(CONTENT_TYPE_SUMMARY, saved.getId()), false, studentId);
    }

    @Transactional
    public void deleteRejectedSummary(Long studentId, Long summaryId) {
        summaryRepository.delete(requireOwnedRejectedSummary(studentId, summaryId));
    }

    @Transactional
    public AfterReadingSummaryItem toggleSummaryLike(
            Long studentId,
            Long summaryId) {

        ClassStudent classStudent = findClassStudent(studentId);
        Summary summary = summaryRepository.findById(summaryId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "간추리기를 찾을 수 없습니다. summaryId=" + summaryId
            ));

        if (!STATUS_APPROVED.equals(normalizeSummaryStatus(summary))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "승인된 간추리기에만 좋아요를 누를 수 있습니다.");
        }

        validateClassReadingBookBelongsToClass(
            summary.getClassReadingBookId(),
            classStudent.getSchoolClass().getId());

        if (demoAccountService.isDemoAccount(studentId)) {
            ClassStudent writerClassStudent = findClassStudent(summary.getStudent().getId());
            long fixedLikeCount = contentLikeRepository.countByContentTypeAndContentId(
                CONTENT_TYPE_SUMMARY, summaryId);
            boolean likedByMe = contentLikeRepository
                .findByStudent_IdAndContentTypeAndContentId(studentId, CONTENT_TYPE_SUMMARY, summaryId)
                .isPresent();
            return AfterReadingSummaryItem.from(
                summary, writerClassStudent, fixedLikeCount, likedByMe, studentId);
        }

        contentLikeRepository
            .findByStudent_IdAndContentTypeAndContentId(
                studentId,
                CONTENT_TYPE_SUMMARY,
                summaryId)
            .ifPresentOrElse(
                contentLikeRepository::delete,
                () -> {
                    ContentLike like = new ContentLike();
                    like.setStudent(findStudent(studentId));
                    like.setContentType(CONTENT_TYPE_SUMMARY);
                    like.setContentId(summaryId);
                    contentLikeRepository.save(like);
                });

        ClassStudent writerClassStudent = findClassStudent(summary.getStudent().getId());
        long likeCount = contentLikeRepository
            .countByContentTypeAndContentId(CONTENT_TYPE_SUMMARY, summaryId);
        boolean likedByMe = contentLikeRepository
            .findByStudent_IdAndContentTypeAndContentId(
                studentId,
                CONTENT_TYPE_SUMMARY,
                summaryId)
            .isPresent();

        return AfterReadingSummaryItem.from(
            summary,
            writerClassStudent,
            likeCount,
            likedByMe,
            studentId);
    }

    @Transactional
    public AfterReadingSummaryItem toggleSummaryLikeAsTeacher(
            Long teacherId,
            Long classId,
            Long summaryId) {

        SchoolClass teacherClass = findTeacherClass(teacherId);
        if (!teacherClass.getId().equals(classId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "담당 학급의 간추리기에만 좋아요를 누를 수 있습니다."
            );
        }

        Summary summary = summaryRepository.findById(summaryId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "간추리기를 찾을 수 없습니다. summaryId=" + summaryId
            ));
        if (!STATUS_APPROVED.equals(normalizeSummaryStatus(summary))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "승인된 간추리기에만 좋아요를 누를 수 있습니다.");
        }
        validateClassReadingBookBelongsToClass(summary.getClassReadingBookId(), classId);

        // 학생용 toggleSummaryLike와 동일한 이유로 교사(심사계정)도 공용 DB에 좋아요를 남기지 않는다.
        if (demoAccountService.isDemoAccount(teacherId)) {
            ClassStudent writerClassStudent = findClassStudent(summary.getStudent().getId());
            long fixedLikeCount = contentLikeRepository.countByContentTypeAndContentId(
                CONTENT_TYPE_SUMMARY, summaryId);
            boolean likedByMe = contentLikeRepository
                .findByStudent_IdAndContentTypeAndContentId(teacherId, CONTENT_TYPE_SUMMARY, summaryId)
                .isPresent();
            return AfterReadingSummaryItem.from(
                summary, writerClassStudent, fixedLikeCount, likedByMe, teacherId);
        }

        contentLikeRepository
            .findByStudent_IdAndContentTypeAndContentId(
                teacherId,
                CONTENT_TYPE_SUMMARY,
                summaryId)
            .ifPresentOrElse(
                contentLikeRepository::delete,
                () -> {
                    ContentLike like = new ContentLike();
                    like.setStudent(findTeacher(teacherId));
                    like.setContentType(CONTENT_TYPE_SUMMARY);
                    like.setContentId(summaryId);
                    contentLikeRepository.save(like);
                });

        ClassStudent writerClassStudent = findClassStudent(summary.getStudent().getId());
        long likeCount = contentLikeRepository
            .countByContentTypeAndContentId(CONTENT_TYPE_SUMMARY, summaryId);
        boolean likedByMe = contentLikeRepository
            .findByStudent_IdAndContentTypeAndContentId(
                teacherId,
                CONTENT_TYPE_SUMMARY,
                summaryId)
            .isPresent();

        return AfterReadingSummaryItem.from(
            summary,
            writerClassStudent,
            likeCount,
            likedByMe,
            teacherId);
    }

    private List<AfterReadingSummaryItem> buildSummaryItems(
            Long classReadingBookId,
            Long viewerStudentId,
            Long classId,
            Long mineStudentId) {

        Map<Long, ClassStudent> classStudentByStudentId =
            classStudentRepository.findBySchoolClassId(classId).stream()
                .collect(Collectors.toMap(
                    classStudent -> classStudent.getStudent().getId(),
                    classStudent -> classStudent,
                    (first, ignored) -> first));

        List<Summary> summaries = summaryRepository
            .findByClassReadingBookIdAndIsSharedTrueOrderByUpdatedAtDesc(
                classReadingBookId)
            .stream()
            .filter(summary ->
                classStudentByStudentId.containsKey(summary.getStudent().getId()))
            .filter(summary -> STATUS_APPROVED.equals(normalizeSummaryStatus(summary))
                || (mineStudentId != null && mineStudentId.equals(summary.getStudent().getId())))
            .toList();

        Set<Long> likedSummaryIds =
            viewerStudentId == null || summaries.isEmpty()
                ? Set.of()
                : contentLikeRepository
                    .findByStudent_IdAndContentTypeAndContentIdIn(
                        viewerStudentId,
                        CONTENT_TYPE_SUMMARY,
                        summaries.stream().map(Summary::getId).toList())
                    .stream()
                    .map(ContentLike::getContentId)
                    .collect(Collectors.toSet());

        return summaries.stream()
            .map(summary -> AfterReadingSummaryItem.from(
                summary,
                classStudentByStudentId.get(summary.getStudent().getId()),
                contentLikeRepository.countByContentTypeAndContentId(
                    CONTENT_TYPE_SUMMARY,
                    summary.getId()),
                likedSummaryIds.contains(summary.getId()),
                viewerStudentId))
            .toList();
    }

    private Summary requireOwnedRejectedSummary(Long studentId, Long summaryId) {
        Summary summary = summaryRepository.findById(summaryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "간추리기를 찾을 수 없습니다."));
        if (!studentId.equals(summary.getStudent().getId()) || !STATUS_REJECTED.equals(normalizeSummaryStatus(summary))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "거절된 내 간추리기만 수정하거나 삭제할 수 있습니다.");
        }
        return summary;
    }

    private String normalizeSummaryStatus(Summary summary) {
        return summary.getStatus() == null ? STATUS_PENDING : summary.getStatus().trim().toLowerCase();
    }

    private void validateCompleteRequest(AfterReadingSaveRequest request) {

        String bookType = request.getBookType() == null
            ? ""
            : request.getBookType().trim();

        if (bookType.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "책 유형을 선택해야 합니다."
            );
        }

        String summary = request.getSummary() == null
            ? ""
            : request.getSummary().trim();

        if (summary.isEmpty() || summary.contains("________________")) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "최종 간추리기를 완성해야 합니다."
            );
        }

        if (request.getQuestions() == null || request.getQuestions().size() != 3) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "질문과 답 3세트가 모두 필요합니다."
            );
        }

        Set<Integer> indexes = request.getQuestions().stream()
            .map(AfterReadingQuestionRequest::getIndex)
            .collect(Collectors.toSet());

        if (!indexes.equals(Set.of(1, 2, 3))) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "질문 번호는 1, 2, 3이어야 합니다."
            );
        }

        request.getQuestions().forEach(question -> {
            if (question.getQuestion() == null
                    || question.getQuestion().trim().isEmpty()
                    || question.getAnswer() == null
                    || question.getAnswer().trim().isEmpty()) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "빈 질문이나 빈 답은 저장할 수 없습니다."
                );
            }

            if (Boolean.FALSE.equals(question.getAiPassed())) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "AI 피드백을 통과한 질문과 답만 완료할 수 있습니다."
                );
            }
        });

        if (Boolean.FALSE.equals(request.getSummaryAiPassed())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "AI 피드백을 통과한 간추리기만 완료할 수 있습니다."
            );
        }
    }

    private List<Response> findAfterReadingQuestions(
            Long studentId,
            Long classReadingBookId) {

        return responseRepository
            .findByStudent_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                studentId,
                MODE_CLASS,
                CONTENT_TYPE_ANSWER,
                STAGE_AFTER)
            .stream()
            .filter(response ->
                ACTIVITY_TYPE_AFTER_QUESTION.equals(
                    extractExtraField(response, "activityType")))
            .filter(response ->
                classReadingBookId.equals(extractClassReadingBookId(response)))
            .toList();
    }

    private String findAfterReadingBookType(
            Long studentId,
            Long classReadingBookId) {

        return responseRepository
            .findByStudent_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                studentId,
                MODE_CLASS,
                CONTENT_TYPE_ANSWER,
                STAGE_AFTER)
            .stream()
            .filter(response ->
                ACTIVITY_TYPE_AFTER_BOOK_TYPE.equals(
                    extractExtraField(response, "activityType")))
            .filter(response ->
                classReadingBookId.equals(extractClassReadingBookId(response)))
            .map(Response::getContent)
            .findFirst()
            .orElse(null);
    }

    /*
     * questionRequest 1건을 index 기준으로 upsert한다 - 최종 완료
     * (saveMyAfterReadingData)와 자동저장(saveAfterReadingQuestionDraft)이
     * 완전히 같은 저장 로직을 쓰도록 공유한다.
     */
    private void upsertAfterReadingQuestionResponse(
            User student,
            Long classReadingBookId,
            List<Response> existingQuestions,
            String bookType,
            Integer index,
            String question,
            String answer,
            Boolean aiPassed) {

        Response response = existingQuestions.stream()
            .filter(existing -> bookType.equals(extractExtraField(existing, "bookType")))
            .filter(existing -> index.equals(extractQuestionIndex(existing)))
            .findFirst()
            .orElseGet(() -> createAfterQuestionResponse(student));

        response.setContent(answer.trim());
        response.setPassed(aiPassed == null ? true : aiPassed);

        Map<String, Object> extraData = new HashMap<>();
        extraData.put("activityType", ACTIVITY_TYPE_AFTER_QUESTION);
        extraData.put("classReadingBookId", classReadingBookId);
        extraData.put("bookType", bookType);
        extraData.put("questionIndex", index);
        extraData.put("question", question.trim());
        response.setExtraData(extraData);

        responseRepository.save(response);
    }

    private Map<String, List<AfterReadingQuestionItem>> buildQuestionsByBookType(
            Long studentId, Long classReadingBookId, String legacyBookType) {
        Map<String, List<AfterReadingQuestionItem>> result = new HashMap<>();
        findAfterReadingQuestions(studentId, classReadingBookId).forEach(response -> {
            String bookType = extractExtraField(response, "bookType");
            if ((bookType == null || bookType.isBlank()) && legacyBookType != null) {
                bookType = legacyBookType;
            }
            if (bookType == null || bookType.isBlank()) return;
            result.computeIfAbsent(bookType, ignored -> new java.util.ArrayList<>())
                .add(AfterReadingQuestionItem.from(response));
        });
        return result;
    }

    private List<Response> findTypePracticeResponses(
            Long studentId,
            Long classReadingBookId) {

        return responseRepository
            .findByStudent_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                studentId,
                MODE_CLASS,
                CONTENT_TYPE_ANSWER,
                STAGE_AFTER)
            .stream()
            .filter(response ->
                ACTIVITY_TYPE_TYPE_PRACTICE.equals(
                    extractExtraField(response, "activityType")))
            .filter(response ->
                classReadingBookId.equals(extractClassReadingBookId(response)))
            .toList();
    }

    private Map<String, AfterReadingTypePracticeItem> findTypePracticeAnswers(
            Long studentId,
            Long classReadingBookId) {

        Map<String, AfterReadingTypePracticeItem> result = new HashMap<>();

        findTypePracticeResponses(studentId, classReadingBookId).forEach(response -> {
            AfterReadingTypePracticeItem item = AfterReadingTypePracticeItem.from(response);
            if (item.getBookType() != null) {
                result.put(item.getBookType(), item);
            }
        });

        return result;
    }

    private Response createAfterQuestionResponse(User student) {

        Response response = new Response();
        response.setStudent(student);
        response.setMode(MODE_CLASS);
        response.setContentType(CONTENT_TYPE_ANSWER);
        response.setStage(STAGE_AFTER);
        response.setStatus("approved");

        return response;
    }

    private Integer extractQuestionIndex(Response response) {

        if (response.getExtraData() == null) {
            return null;
        }

        Object value = response.getExtraData().get("questionIndex");

        if (value instanceof Number number) {
            return number.intValue();
        }

        if (value == null) {
            return null;
        }

        try {
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Long extractClassReadingBookId(Response response) {

        if (response.getExtraData() == null) {
            return null;
        }

        Object value = response.getExtraData().get("classReadingBookId");

        if (value instanceof Number number) {
            return number.longValue();
        }

        if (value == null) {
            return null;
        }

        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String extractExtraField(Response response, String fieldName) {

        if (response.getExtraData() == null) {
            return null;
        }

        Object value = response.getExtraData().get(fieldName);
        return value == null ? null : value.toString();
    }

    private void validateClassReadingBookBelongsToClass(
            Long classReadingBookId,
            Long classId) {

        ClassReadingBook classReadingBook = classReadingBookRepository
            .findById(classReadingBookId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "온책읽기 책을 찾을 수 없습니다. classReadingBookId="
                    + classReadingBookId
            ));

        if (!classReadingBook.getSchoolClass().getId().equals(classId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "해당 학급의 온책읽기 책만 사용할 수 있습니다."
            );
        }
    }

    private ClassStudent findClassStudent(Long studentId) {

        return classStudentRepository.findByStudentId(studentId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "학생의 학급 정보를 찾을 수 없습니다. studentId=" + studentId
            ));
    }

    private SchoolClass findTeacherClass(Long teacherId) {

        findTeacher(teacherId);

        return schoolClassRepository.findByTeacherId(teacherId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "교사의 담당 학급을 찾을 수 없습니다. teacherId=" + teacherId
            ));
    }

    private User findStudent(Long studentId) {

        User user = userRepository.findById(studentId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "학생을 찾을 수 없습니다. studentId=" + studentId
            ));

        if (!"student".equals(user.getRole())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "학생 계정만 사용할 수 있습니다."
            );
        }

        return user;
    }

    private User findTeacher(Long teacherId) {

        User user = userRepository.findById(teacherId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "교사를 찾을 수 없습니다. teacherId=" + teacherId
            ));

        if (!"teacher".equals(user.getRole())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "교사 계정만 사용할 수 있습니다."
            );
        }

        return user;
    }
}
