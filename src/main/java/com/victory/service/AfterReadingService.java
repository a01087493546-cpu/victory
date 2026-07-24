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
import com.victory.dto.AfterReadingSaveRequest;
import com.victory.dto.AfterReadingSummaryItem;
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
    private static final String CONTENT_TYPE_SUMMARY = "summary";

    private final SummaryRepository summaryRepository;
    private final ResponseRepository responseRepository;
    private final ContentLikeRepository contentLikeRepository;
    private final UserRepository userRepository;
    private final ClassStudentRepository classStudentRepository;
    private final ClassReadingBookRepository classReadingBookRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final PracticeProgressRepository practiceProgressRepository;
    private final PracticeProgressService practiceProgressService;

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

        List<AfterReadingQuestionItem> questions =
            findAfterReadingQuestions(studentId, classReadingBookId).stream()
                .map(AfterReadingQuestionItem::from)
                .toList();

        Boolean afterDone = practiceProgressRepository
            .findByStudent_Id(studentId)
            .map(PracticeProgress::getAfterDone)
            .orElse(false);

        String savedBookType = summary == null
            ? findAfterReadingBookType(studentId, classReadingBookId)
            : summary.getBookType();

        return AfterReadingDataResponse.of(
            classReadingBookId,
            summary,
            savedBookType,
            questions,
            afterDone);
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
            Response response = existingQuestions.stream()
                .filter(existing ->
                    questionRequest.getIndex().equals(extractQuestionIndex(existing)))
                .findFirst()
                .orElseGet(() -> createAfterQuestionResponse(student));

            response.setContent(questionRequest.getAnswer().trim());
            response.setPassed(questionRequest.getAiPassed() == null
                ? true
                : questionRequest.getAiPassed());

            Map<String, Object> extraData = new HashMap<>();
            extraData.put("activityType", ACTIVITY_TYPE_AFTER_QUESTION);
            extraData.put("classReadingBookId", request.getClassReadingBookId());
            extraData.put("questionIndex", questionRequest.getIndex());
            extraData.put("question", questionRequest.getQuestion().trim());
            response.setExtraData(extraData);

            responseRepository.save(response);
        }

        Summary summary = summaryRepository
            .findByStudent_IdAndClassReadingBookId(
                studentId,
                request.getClassReadingBookId())
            .orElseGet(Summary::new);

        summary.setStudent(student);
        summary.setClassReadingBookId(request.getClassReadingBookId());
        summary.setBookType(request.getBookType().trim());
        summary.setSummaryText(request.getSummary().trim());
        summary.setIsShared(true);
        summary.setStatus("approved");
        summary.setAiPassed(request.getSummaryAiPassed() == null
            ? true
            : request.getSummaryAiPassed());

        summaryRepository.save(summary);

        PracticeProgressRequest progressRequest = new PracticeProgressRequest();
        progressRequest.setAfterDone(true);
        PracticeProgressResponse progressResponse =
            practiceProgressService.saveProgress(studentId, progressRequest);

        return getMyAfterReadingData(studentId, request.getClassReadingBookId())
            .withPracticeReward(progressResponse);
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
            viewerClassStudent.getSchoolClass().getId());
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

        return buildSummaryItems(classReadingBookId, null, classId);
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

        validateClassReadingBookBelongsToClass(
            summary.getClassReadingBookId(),
            classStudent.getSchoolClass().getId());

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

    private List<AfterReadingSummaryItem> buildSummaryItems(
            Long classReadingBookId,
            Long viewerStudentId,
            Long classId) {

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
