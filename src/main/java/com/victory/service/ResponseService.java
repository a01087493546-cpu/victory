package com.victory.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.BookThoughtResponseItem;
import com.victory.dto.BookThoughtResponseRequest;
import com.victory.dto.BookChatQuizAnswerItem;
import com.victory.dto.BookChatQuizAnswerRequest;
import com.victory.dto.BookChatReplyItem;
import com.victory.dto.BookChatReplyRequest;
import com.victory.dto.BookChatThoughtItem;
import com.victory.dto.BookChatThoughtRequest;
import com.victory.dto.DuringPracticeResponseItem;
import com.victory.dto.DuringPracticeResponseRequest;
import com.victory.dto.DuringReviewResponseItem;
import com.victory.dto.DuringReviewResponseRequest;
import com.victory.dto.PreReadingResponseItem;
import com.victory.dto.PreReadingResponseRequest;
import com.victory.entity.ClassReadingBook;
import com.victory.entity.ClassStudent;
import com.victory.entity.Response;
import com.victory.entity.SchoolClass;
import com.victory.entity.User;
import com.victory.repository.ClassReadingBookRepository;
import com.victory.repository.ClassStudentRepository;
import com.victory.repository.ResponseRepository;
import com.victory.repository.SchoolClassRepository;
import com.victory.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResponseService {

    /*
     * 연습읽기(학급 공통 온책읽기) 읽기 전/중 질문·답 조합.
     * class_reading_books는 학급당 1행을 덮어쓰는 구조라 책이 바뀌어도 id가
     * 유지되므로, classReadingBookId는 참고용으로만 저장하고 조회 범위를
     * 나누는 데는 쓰지 않는다(practice_progress와 같은 수준의 단순화).
     *
     * 읽기 중 두 화면(유형별 심화 연습/총복습)은 같은 stage("during")를
     * 쓰지만 contentType으로 서로 완전히 분리한다:
     * - 심화 연습(질문만, 답 없음): contentType = deep_question
     *   (Response 엔티티 주석에 이미 예정된 값)
     * - 총복습(질문+답): contentType = answer(읽기 전과 같은 값이지만
     *   stage가 다르므로 실제 조회 시 섞이지 않는다)
     * extra_data에 activityType(type_practice/final_review)도 함께 저장해
     * 조회 결과만 보고도 어느 활동인지 바로 구분할 수 있게 한다.
     */
    private static final String MODE_CLASS = "class";
    private static final String CONTENT_TYPE_ANSWER = "answer";
    private static final String CONTENT_TYPE_DEEP_QUESTION = "deep_question";
    private static final String CONTENT_TYPE_THOUGHT = "thought";
    private static final String CONTENT_TYPE_QUIZ_ANSWER = "quiz_answer";
    private static final String CONTENT_TYPE_REPLY = "reply";
    private static final String STAGE_BEFORE = "before";
    private static final String STAGE_DURING = "during";
    private static final String ACTIVITY_TYPE_PRACTICE = "type_practice";
    private static final String ACTIVITY_TYPE_REVIEW = "final_review";
    private static final String ACTIVITY_TYPE_BOOK_THOUGHT = "book_thought";
    private static final String ACTIVITY_TYPE_BOOK_CHAT_THOUGHT = "book_chat_thought";
    private static final String ACTIVITY_TYPE_BOOK_CHAT_QUIZ_ANSWER = "book_chat_quiz_answer";
    private static final String ACTIVITY_TYPE_BOOK_CHAT_REPLY = "book_chat_reply";
    private static final String APPROVAL_STATUS_PENDING = "PENDING";
    private static final String APPROVAL_STATUS_APPROVED = "APPROVED";
    private static final String APPROVAL_STATUS_REJECTED = "REJECTED";

    private final ResponseRepository responseRepository;
    private final UserRepository userRepository;
    private final ClassStudentRepository classStudentRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final ClassReadingBookRepository classReadingBookRepository;
    private final DemoAccountService demoAccountService;

    public List<PreReadingResponseItem> getPreReadingResponses(Long studentId) {

        return responseRepository
            .findByStudent_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                studentId, MODE_CLASS, CONTENT_TYPE_ANSWER, STAGE_BEFORE)
            .stream()
            .map(PreReadingResponseItem::from)
            .toList();
    }

    @Transactional
    public PreReadingResponseItem savePreReadingResponse(
            Long studentId,
            PreReadingResponseRequest request) {

        User student = findStudent(studentId);

        /*
         * skipped(차례 없음)가 아닌 일반 저장은 기존과 동일하게 질문·답을
         * 반드시 요구한다. skipped=true는 "차례 없음" 버튼으로만 오는
         * 경로라 실제 작성값이 없는 게 정상이므로 여기서는 검증하지 않는다
         * (question/answer @NotBlank를 DTO에서 뺀 대신 여기서 조건부로 검사).
         */
        if (!request.isSkipped()) {
            if (request.getQuestion() == null || request.getQuestion().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "question은 비어 있을 수 없습니다.");
            }
            if (request.getAnswer() == null || request.getAnswer().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "answer는 비어 있을 수 없습니다.");
            }
        }

        List<Response> existing = responseRepository
            .findByStudent_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                studentId, MODE_CLASS, CONTENT_TYPE_ANSWER, STAGE_BEFORE);

        Response response = existing.stream()
            .filter(r -> request.getStepType().equals(extractStepType(r)))
            .findFirst()
            .orElseGet(() -> createResponse(student));

        /*
         * skipped=true일 때는 학생이 실제로 쓴 질문·답이 아니므로 절대
         * question/answer 텍스트를 content/extra_data에 저장하지 않는다
         * (빈 문자열로 저장). 대신 extra_data.skipped=true로 "차례 없음으로
         * 통과했다"는 사실만 남긴다. passed는 이 화면이 정상 작성이든
         * 차례 없음이든 "이 단계를 완료했다"는 같은 의미로 계속 재사용한다
         * (학생 화면의 진행률·다음 버튼 활성화가 전부 passed 하나만 보므로,
         * 별도 필드를 추가하면 그 두 곳도 함께 고쳐야 해 범위가 커진다).
         */
        response.setContent(request.isSkipped() ? "" : request.getAnswer());
        response.setPassed(true);

        Map<String, Object> extraData = new HashMap<>();
        extraData.put("stepType", request.getStepType());
        extraData.put("question", request.isSkipped() ? "" : request.getQuestion());
        extraData.put("classReadingBookId", request.getClassReadingBookId());
        extraData.put("skipped", request.isSkipped());
        response.setExtraData(extraData);

        Response saved = responseRepository.save(response);

        return PreReadingResponseItem.from(saved);
    }

    public List<DuringPracticeResponseItem> getDuringPracticeResponses(Long studentId) {

        return responseRepository
            .findByStudent_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                studentId, MODE_CLASS, CONTENT_TYPE_DEEP_QUESTION, STAGE_DURING)
            .stream()
            .map(DuringPracticeResponseItem::from)
            .toList();
    }

    @Transactional
    public DuringPracticeResponseItem saveDuringPracticeResponse(
            Long studentId,
            DuringPracticeResponseRequest request) {

        User student = findStudent(studentId);

        List<Response> existing = responseRepository
            .findByStudent_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                studentId, MODE_CLASS, CONTENT_TYPE_DEEP_QUESTION, STAGE_DURING);

        Response response = existing.stream()
            .filter(r -> request.getQuestionType().equals(extractExtraField(r, "questionType")))
            .findFirst()
            .orElseGet(() -> createResponse(student, CONTENT_TYPE_DEEP_QUESTION, STAGE_DURING));

        response.setContent(request.getQuestion());
        response.setPassed(true);

        Map<String, Object> extraData = new HashMap<>();
        extraData.put("activityType", ACTIVITY_TYPE_PRACTICE);
        extraData.put("questionType", request.getQuestionType());
        extraData.put("classReadingBookId", request.getClassReadingBookId());
        response.setExtraData(extraData);

        Response saved = responseRepository.save(response);

        return DuringPracticeResponseItem.from(saved);
    }

    public List<DuringReviewResponseItem> getDuringReviewResponses(Long studentId) {

        return responseRepository
            .findByStudent_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                studentId, MODE_CLASS, CONTENT_TYPE_ANSWER, STAGE_DURING)
            .stream()
            .filter(r -> ACTIVITY_TYPE_REVIEW.equals(extractExtraField(r, "activityType")))
            .map(DuringReviewResponseItem::from)
            .toList();
    }

    @Transactional
    public DuringReviewResponseItem saveDuringReviewResponse(
            Long studentId,
            DuringReviewResponseRequest request) {

        User student = findStudent(studentId);

        /*
         * book_thought(책 속 생각 쓰기)도 같은 mode/contentType/stage(class/
         * answer/during)를 쓰므로, activityType까지 함께 걸러야 다른 활동의
         * 행을 잘못 찾아 덮어쓰지 않는다.
         */
        List<Response> existing = responseRepository
            .findByStudent_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                studentId, MODE_CLASS, CONTENT_TYPE_ANSWER, STAGE_DURING)
            .stream()
            .filter(r -> ACTIVITY_TYPE_REVIEW.equals(extractExtraField(r, "activityType")))
            .toList();

        Response response = existing.stream()
            .filter(r -> request.getQuestionType().equals(extractExtraField(r, "questionType")))
            .findFirst()
            .orElseGet(() -> createResponse(student, CONTENT_TYPE_ANSWER, STAGE_DURING));

        response.setContent(request.getAnswer());
        response.setPassed(true);

        Map<String, Object> extraData = new HashMap<>();
        extraData.put("activityType", ACTIVITY_TYPE_REVIEW);
        extraData.put("questionType", request.getQuestionType());
        extraData.put("question", request.getQuestion());
        extraData.put("classReadingBookId", request.getClassReadingBookId());
        response.setExtraData(extraData);

        Response saved = responseRepository.save(response);

        return DuringReviewResponseItem.from(saved);
    }

    /*
     * 책 속 생각 쓰기(book_thought)는 총복습(final_review)과 mode/contentType/
     * stage가 완전히 같으므로(둘 다 class/answer/during), extra_data의
     * activityType으로만 서로 구분한다. 유형별 upsert가 아니라 학생이 하루에
     * 여러 개를 만들 수 있는 화면이라 항상 새 행으로 저장하되, 완전히 같은
     * 질문·답을 같은 책에 다시 보내면 새 행을 만들지 않고 기존 행을 그대로
     * 돌려준다(중복 제출 방지).
     */
    public List<BookThoughtResponseItem> getBookThoughtResponses(
            Long studentId,
            Long classReadingBookId) {

        return findBookThoughtResponses(studentId, classReadingBookId)
            .stream()
            .map(BookThoughtResponseItem::from)
            .toList();
    }

    @Transactional
    public BookThoughtResponseItem saveBookThoughtResponse(
            Long studentId,
            BookThoughtResponseRequest request) {

        /*
         * 심사계정은 여러 심사위원이 같은 ss01/tt11을 동시에 쓰므로, 책수다방
         * 글(책 속 생각 쓰기)을 실제 DB에 남기면 한 브라우저의 작성 결과가
         * 다른 브라우저에도 그대로 보이게 된다. 프론트(book-chat.html 등)는
         * 이미 심사계정이면 이 API를 호출하지 않고 mq_demo_* 로컬 저장소만
         * 쓰지만, 백엔드에도 동일한 가드를 둬 우회 호출로 공유 DB가
         * 오염되는 것을 막는다.
         */
        if (demoAccountService.isDemoAccount(studentId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "심사계정의 책수다방 글은 브라우저에만 저장됩니다."
            );
        }

        User student = findStudent(studentId);

        List<Response> existing = findBookThoughtResponses(
            studentId, request.getClassReadingBookId());

        Response duplicate = existing.stream()
            .filter(r -> request.getQuestion().equals(extractExtraField(r, "question")))
            .filter(r -> request.getAnswer().equals(r.getContent()))
            .findFirst()
            .orElse(null);

        if (duplicate != null) {
            return BookThoughtResponseItem.from(duplicate);
        }

        Response response = createResponse(student, CONTENT_TYPE_ANSWER, STAGE_DURING);
        response.setContent(request.getAnswer());
        response.setPassed(true);

        Map<String, Object> extraData = new HashMap<>();
        extraData.put("activityType", ACTIVITY_TYPE_BOOK_THOUGHT);
        extraData.put("questionType", request.getQuestionType());
        extraData.put("question", request.getQuestion());
        extraData.put("classReadingBookId", request.getClassReadingBookId());
        extraData.put("approvalStatus", APPROVAL_STATUS_PENDING);
        response.setExtraData(extraData);

        Response saved = responseRepository.save(response);

        return BookThoughtResponseItem.from(saved);
    }

    public List<BookThoughtResponseItem> getBookChatQuestions(
            Long studentId,
            Long classReadingBookId) {

        ClassStudent loginStudentClass = findClassStudent(studentId);
        SchoolClass schoolClass = loginStudentClass.getSchoolClass();
        validateClassReadingBookBelongsToClass(classReadingBookId, schoolClass.getId());

        Set<Long> classStudentIds = classStudentRepository
            .findBySchoolClassId(schoolClass.getId())
            .stream()
            .map(classStudent -> classStudent.getStudent().getId())
            .collect(HashSet::new, Set::add, Set::addAll);

        return responseRepository
            .findByModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                MODE_CLASS,
                CONTENT_TYPE_ANSWER,
                STAGE_DURING)
            .stream()
            .filter(this::isBookThoughtResponse)
            .filter(response -> classReadingBookId.equals(extractClassReadingBookId(response)))
            .filter(response -> classStudentIds.contains(response.getStudent().getId()))
            .filter(response -> isVisibleInBookChat(response, studentId))
            .map(BookThoughtResponseItem::from)
            .toList();
    }

    public List<BookChatThoughtItem> getBookChatThoughts(
            Long studentId,
            Long questionResponseId) {

        Response question = findApprovedBookChatQuestionForStudent(
            studentId, questionResponseId);

        return responseRepository
            .findByParent_IdAndModeAndContentTypeAndDeletedAtIsNullOrderByIdAsc(
                question.getId(),
                MODE_CLASS,
                CONTENT_TYPE_THOUGHT)
            .stream()
            .filter(response -> ACTIVITY_TYPE_BOOK_CHAT_THOUGHT.equals(
                extractExtraField(response, "activityType")))
            .map(response -> BookChatThoughtItem.from(response, studentId))
            .toList();
    }

    @Transactional
    public BookChatThoughtItem saveBookChatThought(
            Long studentId,
            Long questionResponseId,
            BookChatThoughtRequest request) {

        if (demoAccountService.isDemoAccount(studentId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "심사계정의 책수다방 생각은 브라우저에만 저장됩니다."
            );
        }

        User student = findStudent(studentId);
        Response question = findApprovedBookChatQuestionForStudent(
            studentId, questionResponseId);

        Response response = responseRepository
            .findByParent_IdAndModeAndContentTypeAndStudent_IdAndDeletedAtIsNullOrderByIdAsc(
                question.getId(),
                MODE_CLASS,
                CONTENT_TYPE_THOUGHT,
                studentId)
            .stream()
            .filter(item -> ACTIVITY_TYPE_BOOK_CHAT_THOUGHT.equals(
                extractExtraField(item, "activityType")))
            .findFirst()
            .orElseGet(() -> {
                Response created = createResponse(
                    student, CONTENT_TYPE_THOUGHT, STAGE_DURING);
                created.setParent(question);
                return created;
            });

        response.setContent(request.getMain().trim());
        response.setStatus("approved");

        Map<String, Object> extraData = mutableExtraData(response);
        extraData.put("activityType", ACTIVITY_TYPE_BOOK_CHAT_THOUGHT);
        extraData.put("questionResponseId", question.getId());
        extraData.put("classReadingBookId", extractClassReadingBookId(question));
        extraData.put("reason", request.getReason().trim());

        return BookChatThoughtItem.from(
            responseRepository.save(response),
            studentId);
    }

    public List<BookChatQuizAnswerItem> getBookChatQuizAnswers(
            Long studentId,
            Long questionResponseId) {

        Response question = findApprovedBookChatQuestionForStudent(
            studentId, questionResponseId);

        return responseRepository
            .findByParent_IdAndModeAndContentTypeAndDeletedAtIsNullOrderByIdAsc(
                question.getId(),
                MODE_CLASS,
                CONTENT_TYPE_QUIZ_ANSWER)
            .stream()
            .filter(response -> ACTIVITY_TYPE_BOOK_CHAT_QUIZ_ANSWER.equals(
                extractExtraField(response, "activityType")))
            .map(response -> BookChatQuizAnswerItem.from(response, studentId))
            .toList();
    }

    @Transactional
    public BookChatQuizAnswerItem saveBookChatQuizAnswer(
            Long studentId,
            Long questionResponseId,
            BookChatQuizAnswerRequest request) {

        if (demoAccountService.isDemoAccount(studentId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "심사계정의 책수다방 퀴즈 답은 브라우저에만 저장됩니다."
            );
        }

        User student = findStudent(studentId);
        Response question = findApprovedBookChatQuestionForStudent(
            studentId, questionResponseId);

        if (!"direct".equals(normalizeTypeForBookChat(question))) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "책 퀴즈 질문에만 답을 쓸 수 있습니다."
            );
        }

        Response response = responseRepository
            .findByParent_IdAndModeAndContentTypeAndStudent_IdAndDeletedAtIsNullOrderByIdAsc(
                question.getId(),
                MODE_CLASS,
                CONTENT_TYPE_QUIZ_ANSWER,
                studentId)
            .stream()
            .filter(item -> ACTIVITY_TYPE_BOOK_CHAT_QUIZ_ANSWER.equals(
                extractExtraField(item, "activityType")))
            .findFirst()
            .orElseGet(() -> {
                Response created = createResponse(
                    student, CONTENT_TYPE_QUIZ_ANSWER, STAGE_DURING);
                created.setParent(question);
                return created;
            });

        response.setContent(request.getAnswer().trim());
        response.setStatus("approved");

        Map<String, Object> extraData = mutableExtraData(response);
        extraData.put("activityType", ACTIVITY_TYPE_BOOK_CHAT_QUIZ_ANSWER);
        extraData.put("questionResponseId", question.getId());
        extraData.put("classReadingBookId", extractClassReadingBookId(question));

        return BookChatQuizAnswerItem.from(
            responseRepository.save(response),
            studentId);
    }

    public List<BookChatReplyItem> getBookChatReplies(
            Long studentId,
            Long questionResponseId) {

        Response question = findApprovedBookChatQuestionForStudent(
            studentId, questionResponseId);

        Set<Long> thoughtIds = responseRepository
            .findByParent_IdAndModeAndContentTypeAndDeletedAtIsNullOrderByIdAsc(
                question.getId(),
                MODE_CLASS,
                CONTENT_TYPE_THOUGHT)
            .stream()
            .filter(response -> ACTIVITY_TYPE_BOOK_CHAT_THOUGHT.equals(
                extractExtraField(response, "activityType")))
            .map(Response::getId)
            .collect(HashSet::new, Set::add, Set::addAll);

        if (thoughtIds.isEmpty()) {
            return List.of();
        }

        return responseRepository
            .findByModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                MODE_CLASS,
                CONTENT_TYPE_REPLY,
                STAGE_DURING)
            .stream()
            .filter(response -> response.getParent() != null)
            .filter(response -> thoughtIds.contains(response.getParent().getId()))
            .filter(response -> ACTIVITY_TYPE_BOOK_CHAT_REPLY.equals(
                extractExtraField(response, "activityType")))
            .map(response -> BookChatReplyItem.from(response, studentId))
            .toList();
    }

    @Transactional
    public BookChatReplyItem saveBookChatReply(
            Long studentId,
            Long questionResponseId,
            Long thoughtId,
            BookChatReplyRequest request) {

        if (demoAccountService.isDemoAccount(studentId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "심사계정의 책수다방 답글은 브라우저에만 저장됩니다."
            );
        }

        User student = findStudent(studentId);
        Response question = findApprovedBookChatQuestionForStudent(
            studentId, questionResponseId);
        Response thought = findBookChatThoughtForQuestion(question, thoughtId);
        String replyType = normalizeReplyType(request.getReplyType());

        Response response = createResponse(
            student, CONTENT_TYPE_REPLY, STAGE_DURING);
        response.setParent(thought);
        response.setContent(request.getText().trim());
        response.setStatus("approved");

        Map<String, Object> extraData = mutableExtraData(response);
        extraData.put("activityType", ACTIVITY_TYPE_BOOK_CHAT_REPLY);
        extraData.put("questionResponseId", question.getId());
        extraData.put("classReadingBookId", extractClassReadingBookId(question));
        extraData.put("replyType", replyType);

        return BookChatReplyItem.from(
            responseRepository.save(response),
            studentId);
    }

    @Transactional
    public void deleteRejectedBookThoughtResponse(Long studentId, Long responseId) {

        Response response = findBookThoughtById(responseId);

        if (!response.getStudent().getId().equals(studentId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "본인이 작성한 질문만 삭제할 수 있습니다."
            );
        }

        if (!APPROVAL_STATUS_REJECTED.equals(getApprovalStatus(response))) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "거절된 질문만 삭제할 수 있습니다."
            );
        }

        response.setDeletedAt(LocalDateTime.now());
        responseRepository.save(response);
    }

    public List<BookThoughtResponseItem> getTeacherBookThoughtReviews(
            Long teacherId,
            Long classId,
            Long classReadingBookId,
            String status) {

        SchoolClass schoolClass = findTeacherClass(teacherId);
        validateRequestedClass(schoolClass, classId);

        if (classReadingBookId != null) {
            validateClassReadingBookBelongsToClass(classReadingBookId, schoolClass.getId());
        }

        Set<Long> classStudentIds = classStudentRepository
            .findBySchoolClassId(schoolClass.getId())
            .stream()
            .map(classStudent -> classStudent.getStudent().getId())
            .collect(HashSet::new, Set::add, Set::addAll);

        String normalizedStatus =
            status == null || status.isBlank()
                ? APPROVAL_STATUS_PENDING
                : normalizeApprovalStatus(status);

        return responseRepository
            .findByModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                MODE_CLASS,
                CONTENT_TYPE_ANSWER,
                STAGE_DURING)
            .stream()
            .filter(this::isBookThoughtResponse)
            .filter(response -> classStudentIds.contains(response.getStudent().getId()))
            .filter(response -> classReadingBookId == null
                || classReadingBookId.equals(extractClassReadingBookId(response)))
            .filter(response -> normalizedStatus.equals(getApprovalStatus(response)))
            .map(BookThoughtResponseItem::from)
            .toList();
    }

    @Transactional
    public BookThoughtResponseItem approveBookThoughtResponse(
            Long teacherId,
            Long responseId) {

        Response response = findTeacherManagedBookThought(teacherId, responseId);

        /*
         * 승인/거절/보류 되돌리기는 "심사위원이 직접 조작한 결과"이므로
         * 같은 tt11을 쓰는 다른 브라우저에 영향을 주면 안 된다. 프론트
         * (book-chat-manage.html)는 이미 심사계정이면 이 API를 호출하지
         * 않고 mq_demo_* 로컬 오버라이드만 쓰지만, 우회 호출에 대비해
         * 백엔드에서도 저장 없이 현재 상태만 그대로 돌려준다.
         */
        if (demoAccountService.isDemoAccount(teacherId)) {
            return BookThoughtResponseItem.from(response);
        }

        User teacher = findTeacher(teacherId);
        Map<String, Object> extraData = mutableExtraData(response);

        extraData.put("approvalStatus", APPROVAL_STATUS_APPROVED);
        extraData.remove("rejectionReason");
        extraData.put("reviewedByTeacherId", teacherId);
        extraData.put("reviewedAt", LocalDateTime.now().toString());
        response.setExtraData(extraData);
        response.setStatus("approved");
        response.setRejectReason(null);
        response.setReviewedBy(teacher);
        response.setReviewedAt(LocalDateTime.now());

        return BookThoughtResponseItem.from(responseRepository.save(response));
    }

    @Transactional
    public BookThoughtResponseItem rejectBookThoughtResponse(
            Long teacherId,
            Long responseId,
            String reason) {

        if (reason == null || reason.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "거절 사유를 입력해야 합니다."
            );
        }

        Response response = findTeacherManagedBookThought(teacherId, responseId);

        if (demoAccountService.isDemoAccount(teacherId)) {
            return BookThoughtResponseItem.from(response);
        }

        User teacher = findTeacher(teacherId);
        LocalDateTime reviewedAt = LocalDateTime.now();
        Map<String, Object> extraData = mutableExtraData(response);

        extraData.put("approvalStatus", APPROVAL_STATUS_REJECTED);
        extraData.put("rejectionReason", reason.trim());
        extraData.put("reviewedByTeacherId", teacherId);
        extraData.put("reviewedAt", reviewedAt.toString());
        response.setExtraData(extraData);
        response.setStatus("rejected");
        response.setRejectReason(reason.trim());
        response.setReviewedBy(teacher);
        response.setReviewedAt(reviewedAt);

        return BookThoughtResponseItem.from(responseRepository.save(response));
    }

    /*
     * "승인 취소" - 거절이 아니라 단순히 다시 검토 대기 상태로 되돌리는 것이므로
     * 거절 사유/검수 이력을 남기지 않는다(rejectBookThoughtResponse와 달리
     * reason 파라미터가 없다).
     */
    @Transactional
    public BookThoughtResponseItem returnBookThoughtResponseToPending(
            Long teacherId,
            Long responseId) {

        Response response = findTeacherManagedBookThought(teacherId, responseId);

        if (demoAccountService.isDemoAccount(teacherId)) {
            return BookThoughtResponseItem.from(response);
        }

        Map<String, Object> extraData = mutableExtraData(response);

        extraData.put("approvalStatus", APPROVAL_STATUS_PENDING);
        extraData.remove("rejectionReason");
        extraData.remove("reviewedByTeacherId");
        extraData.remove("reviewedAt");
        response.setExtraData(extraData);
        response.setStatus("pending");
        response.setRejectReason(null);
        response.setReviewedBy(null);
        response.setReviewedAt(null);

        return BookThoughtResponseItem.from(responseRepository.save(response));
    }

    private Response findApprovedBookChatQuestionForStudent(
            Long studentId,
            Long questionResponseId) {

        Response question = findBookThoughtById(questionResponseId);

        if (!APPROVAL_STATUS_APPROVED.equals(getApprovalStatus(question))) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "승인된 책수다방 질문에만 접근할 수 있습니다."
            );
        }

        ClassStudent viewerClass = findClassStudent(studentId);
        ClassStudent writerClass = findClassStudent(question.getStudent().getId());

        if (!viewerClass.getSchoolClass().getId()
                .equals(writerClass.getSchoolClass().getId())) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "같은 학급의 책수다방 글만 볼 수 있습니다."
            );
        }

        Long classReadingBookId = extractClassReadingBookId(question);

        if (classReadingBookId == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "질문의 온책읽기 책 정보가 없습니다."
            );
        }

        validateClassReadingBookBelongsToClass(
            classReadingBookId,
            viewerClass.getSchoolClass().getId());

        return question;
    }

    private Response findBookChatThoughtForQuestion(
            Response question,
            Long thoughtId) {

        Response thought = responseRepository.findById(thoughtId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "생각 글을 찾을 수 없습니다. thoughtId=" + thoughtId
            ));

        if (thought.getDeletedAt() != null
                || thought.getParent() == null
                || !thought.getParent().getId().equals(question.getId())
                || !MODE_CLASS.equals(thought.getMode())
                || !CONTENT_TYPE_THOUGHT.equals(thought.getContentType())
                || !ACTIVITY_TYPE_BOOK_CHAT_THOUGHT.equals(
                    extractExtraField(thought, "activityType"))) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "해당 질문의 생각 글을 찾을 수 없습니다."
            );
        }

        return thought;
    }

    private String normalizeTypeForBookChat(Response question) {

        String type = extractExtraField(question, "questionType");

        if ("opinion".equals(type)) {
            return "feeling";
        }

        if ("connect".equals(type)) {
            return "life";
        }

        return type == null || type.isBlank() ? "direct" : type;
    }

    private String normalizeReplyType(String replyType) {

        String normalized = replyType == null
            ? ""
            : replyType.trim().toUpperCase();

        if ("SIMILAR".equals(normalized) || "DIFFERENT".equals(normalized)) {
            return normalized;
        }

        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "replyType은 SIMILAR 또는 DIFFERENT여야 합니다."
        );
    }

    private List<Response> findBookThoughtResponses(
        Long studentId,
        Long classReadingBookId) {

    return responseRepository
        .findByStudent_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
            studentId,
            MODE_CLASS,
            CONTENT_TYPE_ANSWER,
            STAGE_DURING)
        .stream()
        .filter(this::isBookThoughtResponse)
        .filter(r ->
            classReadingBookId == null
                || classReadingBookId.equals(
                    extractClassReadingBookId(r)
                )
        )
        .toList();
}

    private boolean isVisibleInBookChat(Response response, Long viewerStudentId) {

        if (response.getStudent().getId().equals(viewerStudentId)) {
            return true;
        }

        return APPROVAL_STATUS_APPROVED.equals(getApprovalStatus(response));
    }

    private Response findBookThoughtById(Long responseId) {

        Response response = responseRepository.findById(responseId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "질문을 찾을 수 없습니다. responseId=" + responseId
            ));

        if (response.getDeletedAt() != null || !isBookThoughtResponse(response)) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "책 속 생각쓰기 질문을 찾을 수 없습니다."
            );
        }

        return response;
    }

    private Response findTeacherManagedBookThought(Long teacherId, Long responseId) {

        Response response = findBookThoughtById(responseId);
        ClassStudent writerClass = findClassStudent(response.getStudent().getId());

        if (!writerClass.getSchoolClass().getTeacher().getId().equals(teacherId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "담당 학급 학생의 질문만 처리할 수 있습니다."
            );
        }

        return response;
    }

    private boolean isBookThoughtResponse(Response response) {

        return ACTIVITY_TYPE_BOOK_THOUGHT.equals(
            extractExtraField(response, "activityType")
        );
    }

    private String getApprovalStatus(Response response) {

        String status = extractExtraField(response, "approvalStatus");

        if (status == null || status.isBlank()) {
            return APPROVAL_STATUS_PENDING;
        }

        return normalizeApprovalStatus(status);
    }

    private String normalizeApprovalStatus(String status) {

        String normalized = status.trim().toUpperCase();

        if (!APPROVAL_STATUS_PENDING.equals(normalized)
                && !APPROVAL_STATUS_APPROVED.equals(normalized)
                && !APPROVAL_STATUS_REJECTED.equals(normalized)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "approvalStatus는 PENDING, APPROVED, REJECTED 중 하나여야 합니다."
            );
        }

        return normalized;
    }

    private Map<String, Object> mutableExtraData(Response response) {

        Map<String, Object> extraData =
            response.getExtraData() == null
                ? new HashMap<>()
                : new HashMap<>(response.getExtraData());

        response.setExtraData(extraData);

        return extraData;
    }

    private void validateClassReadingBookBelongsToClass(
            Long classReadingBookId,
            Long classId) {

        ClassReadingBook classReadingBook = classReadingBookRepository
            .findById(classReadingBookId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "온책읽기 책을 찾을 수 없습니다. classReadingBookId=" + classReadingBookId
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

    private void validateRequestedClass(SchoolClass teacherClass, Long classId) {

        if (classId != null && !teacherClass.getId().equals(classId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "담당 학급만 조회할 수 있습니다."
            );
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
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Response createResponse(User student) {
        return createResponse(student, CONTENT_TYPE_ANSWER, STAGE_BEFORE);
    }

    private Response createResponse(User student, String contentType, String stage) {

        Response response = new Response();
        response.setStudent(student);
        response.setMode(MODE_CLASS);
        response.setContentType(contentType);
        response.setStage(stage);

        return response;
    }

    private String extractStepType(Response response) {
        return extractExtraField(response, "stepType");
    }

    private String extractExtraField(Response response, String key) {

        if (response.getExtraData() == null) {
            return null;
        }

        Object value = response.getExtraData().get(key);

        return value == null ? null : value.toString();
    }

    private User findStudent(Long studentId) {

        User user = userRepository.findById(studentId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "학생을 찾을 수 없습니다. studentId=" + studentId
            ));

        if (!"student".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "학생 계정만 읽기 전 질문·답을 저장할 수 있습니다."
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

        if (!"teacher".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "교사 계정만 처리할 수 있습니다."
            );
        }

        return user;
    }
}
