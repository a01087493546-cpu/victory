package com.victory.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.IndividualBookChatCommentResponse;
import com.victory.dto.IndividualBookChatFeedResponse;
import com.victory.dto.IndividualBookChatPostResponse;
import com.victory.dto.StudentStatsResponse;
import com.victory.entity.ClassStudent;
import com.victory.entity.ReadingRecord;
import com.victory.entity.Response;
import com.victory.entity.SchoolClass;
import com.victory.entity.User;
import com.victory.repository.ClassStudentRepository;
import com.victory.repository.ReadingRecordRepository;
import com.victory.repository.ResponseRepository;
import com.victory.repository.SchoolClassRepository;
import com.victory.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/*
 * 개별읽기 책수다방(밸런스 글쓰기 + 댓글). 텍스트만 서버(DB)에 저장한다
 * (사진 첨부 기능은 제거됨). responses 테이블을 그대로 재사용한다
 * (mode=individual, content_type=chat_post/chat_reply) - Response 엔티티
 * 상단 주석이 이미 이 조합을 예정해 두었다.
 *
 * 글 목록은 "같은 학급 학생들의 글"만 보여준다(ClassStudent로 학급을
 * 확인) - 학급이 없는 계정은 본인 글만 보여주는 대신 hasClass=false를
 * 내려줘서 프론트가 명확한 안내를 보여줄 수 있게 한다.
 */
@Service
@RequiredArgsConstructor
public class IndividualBookChatService {

    private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");
    private static final String MODE_INDIVIDUAL = "individual";
    private static final String CONTENT_TYPE_CHAT_POST = "chat_post";
    private static final String CONTENT_TYPE_CHAT_REPLY = "chat_reply";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";

    private final ResponseRepository responseRepository;
    private final UserRepository userRepository;
    private final ClassStudentRepository classStudentRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final IndividualBookChatRewardService rewardService;
    private final ReadingRecordRepository readingRecordRepository;

    @Transactional(readOnly = true)
    public IndividualBookChatFeedResponse getClassFeed(Long viewerStudentId) {
        Optional<ClassStudent> membership = classStudentRepository.findByStudentId(viewerStudentId);

        boolean hasClass = membership.isPresent();

        List<Response> posts = hasClass
            ? responseRepository.findByStudent_IdInAndModeAndContentTypeAndDeletedAtIsNullOrderByIdDesc(
                classmateStudentIds(membership.get()), MODE_INDIVIDUAL, CONTENT_TYPE_CHAT_POST)
            : responseRepository.findByStudent_IdAndModeAndContentTypeAndDeletedAtIsNullOrderByIdDesc(
                viewerStudentId, MODE_INDIVIDUAL, CONTENT_TYPE_CHAT_POST);

        Long classId = membership
            .map(classStudent -> classStudent.getSchoolClass().getId())
            .orElse(null);

        List<Response> visiblePosts = posts.stream()
            .filter(post -> isVisibleToStudent(post, viewerStudentId))
            .toList();

        Map<Long, long[]> countsByPostId = countChoicesByPost(
            visiblePosts.stream().map(Response::getId).toList());

        List<IndividualBookChatPostResponse> postResponses = visiblePosts.stream()
            .map(post -> {
                long[] counts = countsByPostId.getOrDefault(post.getId(), new long[2]);
                boolean isMine = post.getStudent().getId().equals(viewerStudentId);
                return IndividualBookChatPostResponse.fromPost(post, isMine, classId, counts[0], counts[1]);
            })
            .toList();

        return new IndividualBookChatFeedResponse(hasClass, postResponses);
    }

    private List<Long> classmateStudentIds(ClassStudent viewerMembership) {
        return classStudentRepository
            .findBySchoolClassId(viewerMembership.getSchoolClass().getId())
            .stream()
            .map(classStudent -> classStudent.getStudent().getId())
            .toList();
    }

    private Map<Long, long[]> countChoicesByPost(List<Long> postIds) {
        if (postIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, long[]> countsByPostId = new HashMap<>();

        for (Response reply : responseRepository.findByParent_IdInAndModeAndContentTypeAndDeletedAtIsNullOrderByIdAsc(
                postIds, MODE_INDIVIDUAL, CONTENT_TYPE_CHAT_REPLY)) {

            Long parentId = reply.getParent().getId();
            long[] counts = countsByPostId.computeIfAbsent(parentId, ignored -> new long[2]);
            String choice = extractChoice(reply);

            if ("A".equals(choice)) {
                counts[0]++;
            } else if ("B".equals(choice)) {
                counts[1]++;
            }
        }

        return countsByPostId;
    }

    private String extractChoice(Response response) {
        Object value = response.getExtraData() == null ? null : response.getExtraData().get("choice");
        return value == null ? null : value.toString();
    }

    /*
     * 필수 텍스트 항목이 비어 있으면 400으로 명확한 사유를 돌려준다 -
     * 프론트가 조용히 실패하지 않고 사용자에게 안내할 수 있게 한다.
     *
     * readingRecordId는 프론트가 bookTitle(자유 입력)로 되짚어 추측하지
     * 않고, 학생이 지금 진행 중인 책의 실제 readingRecordId를 그대로
     * 보내야 한다. 서버는 이 값의 존재·소유권·진행 상태를 직접 검증한다.
     *
     * 글 저장 → 오늘 첫 등록이면 용기 +1 보상 지급까지 하나의 트랜잭션
     * 안에서 처리한다.
     */
    @Transactional
    public IndividualBookChatPostResponse createPost(
            Long studentId,
            Long readingRecordId,
            String bookTitle,
            String title,
            String scene,
            String optionA,
            String optionB) {

        if (isBlank(bookTitle) || isBlank(title) || isBlank(scene) || isBlank(optionA) || isBlank(optionB)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "책 제목, 밸런스 제목, 장면 설명, A/B 선택지를 모두 적어 주세요."
            );
        }

        ReadingRecord readingRecord = requireInProgressOwnedReadingRecord(studentId, readingRecordId);

        User student = userRepository.findById(studentId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "학생 정보를 찾을 수 없습니다. studentId=" + studentId
            ));

        LocalDate today = LocalDate.now(ZONE_SEOUL);

        Response response = new Response();
        response.setStudent(student);
        response.setReadingRecord(readingRecord);
        response.setMode(MODE_INDIVIDUAL);
        response.setContentType(CONTENT_TYPE_CHAT_POST);
        response.setContent(scene);
        response.setActivityDate(today);
        response.setStatus(toStoredStatus(STATUS_PENDING));

        Map<String, Object> extraData = new HashMap<>();
        extraData.put("bookTitle", bookTitle);
        extraData.put("title", title);
        extraData.put("optionA", optionA);
        extraData.put("optionB", optionB);
        response.setExtraData(extraData);

        Response saved = responseRepository.save(response);

        IndividualBookChatRewardService.RewardResult rewardResult =
            rewardService.grantPostDailyRewardOnce(student, today);

        Long classId = classStudentRepository.findByStudentId(studentId)
            .map(classStudent -> classStudent.getSchoolClass().getId())
            .orElse(null);

        IndividualBookChatPostResponse result = IndividualBookChatPostResponse.fromPost(saved, true, classId, 0, 0);
        result.setRewardGranted(rewardResult.isRewardGranted());
        result.setStats(StudentStatsResponse.from(rewardResult.getStats()));

        return result;
    }

    /*
     * 친구 책수다 글에 남기는 A/B 선택 + 이유(댓글). postId는 실제 chat_post
     * Response id이고, 댓글은 그 글을 parent로 하는 별도 Response 행으로
     * 저장한다(class-mode 책수다방과 같은 parent/contentType=reply 패턴).
     * 같은 학생이 같은 글에 여러 번 남기면 매번 새 댓글로 추가된다(수정이
     * 아니라 추가 - 기존 UI가 목록에 이어 붙이는 방식과 자연스럽게 맞고,
     * 보상은 어차피 하루 1회로 별도 제한되므로 댓글 개수와 무관하다).
     */
    @Transactional
    public IndividualBookChatCommentResponse createComment(
            Long studentId,
            Long postId,
            String choice,
            String content) {

        if (postId == null || isBlank(content)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "왜 그렇게 생각했는지 이유를 적어 주세요."
            );
        }

        String normalizedChoice = normalizeChoice(choice);

        User student = userRepository.findById(studentId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "학생 정보를 찾을 수 없습니다. studentId=" + studentId
            ));

        Response parentPost = findCommentablePost(studentId, postId);

        LocalDate today = LocalDate.now(ZONE_SEOUL);

        Response response = new Response();
        response.setStudent(student);
        response.setParent(parentPost);
        response.setMode(MODE_INDIVIDUAL);
        response.setContentType(CONTENT_TYPE_CHAT_REPLY);
        response.setContent(content);
        response.setActivityDate(today);
        response.setExtraData(Map.of("choice", normalizedChoice));

        Response saved = responseRepository.save(response);

        IndividualBookChatRewardService.RewardResult rewardResult =
            rewardService.grantCommentDailyRewardOnce(student, today);

        IndividualBookChatCommentResponse result =
            IndividualBookChatCommentResponse.fromComment(saved, normalizedChoice);
        result.setRewardGranted(rewardResult.isRewardGranted());
        result.setStats(StudentStatsResponse.from(rewardResult.getStats()));

        return result;
    }

    @Transactional(readOnly = true)
    public List<IndividualBookChatCommentResponse> getComments(Long viewerStudentId, Long postId) {
        findCommentablePost(viewerStudentId, postId);

        return responseRepository
            .findByParent_IdAndModeAndContentTypeAndDeletedAtIsNullOrderByIdAsc(
                postId, MODE_INDIVIDUAL, CONTENT_TYPE_CHAT_REPLY)
            .stream()
            .map(reply -> IndividualBookChatCommentResponse.fromComment(reply, extractChoice(reply)))
            .toList();
    }

    /*
     * postId가 실제 존재하는 책수다 글인지, 그리고 조회하는 학생이 그
     * 글을 볼 자격이 있는지(본인 글이거나, 같은 학급의 승인 글) 함께 확인한다.
     * 자격이 없으면 존재 여부를 알려주지 않기 위해 404로 통일한다(다른
     * 학급 글 ID를 추측해도 아무 정보도 새어나가지 않는다).
     */
    private Response findAccessiblePost(Long viewerStudentId, Long postId) {
        Response post = responseRepository
            .findByIdAndModeAndContentTypeAndDeletedAtIsNull(postId, MODE_INDIVIDUAL, CONTENT_TYPE_CHAT_POST)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "책수다 글을 찾을 수 없습니다. postId=" + postId
            ));

        Long authorId = post.getStudent().getId();

        if (!authorId.equals(viewerStudentId)
                && (!inSameClass(viewerStudentId, authorId) || !isApproved(post))) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "책수다 글을 찾을 수 없습니다. postId=" + postId
            );
        }

        return post;
    }

    private Response findCommentablePost(Long viewerStudentId, Long postId) {
        Response post = findAccessiblePost(viewerStudentId, postId);

        if (!isApproved(post)) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "책수다 글을 찾을 수 없습니다. postId=" + postId
            );
        }

        return post;
    }

    @Transactional(readOnly = true)
    public List<IndividualBookChatPostResponse> getTeacherManagedPosts(Long teacherId, String status) {
        SchoolClass schoolClass = findTeacherClass(teacherId);
        List<Long> classStudentIds = classmateStudentIdsForClass(schoolClass.getId());

        if (classStudentIds.isEmpty()) {
            return List.of();
        }

        String normalizedStatus = normalizeStatusOrAll(status);
        List<Response> posts = responseRepository
            .findByStudent_IdInAndModeAndContentTypeAndDeletedAtIsNullOrderByIdDesc(
                classStudentIds, MODE_INDIVIDUAL, CONTENT_TYPE_CHAT_POST);

        List<Response> filteredPosts = "ALL".equals(normalizedStatus)
            ? posts
            : posts.stream()
                .filter(post -> normalizedStatus.equals(normalizeStatus(post.getStatus())))
                .toList();

        Map<Long, long[]> countsByPostId = countChoicesByPost(
            filteredPosts.stream().map(Response::getId).toList());

        return filteredPosts.stream()
            .map(post -> {
                long[] counts = countsByPostId.getOrDefault(post.getId(), new long[2]);
                return IndividualBookChatPostResponse.fromPost(
                    post, false, schoolClass.getId(), counts[0], counts[1]);
            })
            .toList();
    }

    @Transactional
    public IndividualBookChatPostResponse approvePost(Long teacherId, Long postId) {
        Response post = findTeacherManagedPost(teacherId, postId);
        User teacher = findTeacher(teacherId);

        post.setStatus(toStoredStatus(STATUS_APPROVED));
        post.setRejectReason(null);
        post.setReviewedBy(teacher);
        post.setReviewedAt(LocalDateTime.now(ZONE_SEOUL));

        return toTeacherPostResponse(responseRepository.save(post), teacherId);
    }

    @Transactional
    public IndividualBookChatPostResponse rejectPost(Long teacherId, Long postId, String reason) {
        Response post = findTeacherManagedPost(teacherId, postId);
        User teacher = findTeacher(teacherId);
        String rejectReason = isBlank(reason)
            ? "고쳐서 다시 올리면 더 좋은 책수다방 글이 될 수 있어요."
            : reason.trim();

        post.setStatus(toStoredStatus(STATUS_REJECTED));
        post.setRejectReason(rejectReason);
        post.setReviewedBy(teacher);
        post.setReviewedAt(LocalDateTime.now(ZONE_SEOUL));

        return toTeacherPostResponse(responseRepository.save(post), teacherId);
    }

    @Transactional
    public IndividualBookChatPostResponse returnPostToPending(Long teacherId, Long postId) {
        Response post = findTeacherManagedPost(teacherId, postId);
        User teacher = findTeacher(teacherId);

        post.setStatus(toStoredStatus(STATUS_PENDING));
        post.setRejectReason(null);
        post.setReviewedBy(teacher);
        post.setReviewedAt(LocalDateTime.now(ZONE_SEOUL));

        return toTeacherPostResponse(responseRepository.save(post), teacherId);
    }

    private IndividualBookChatPostResponse toTeacherPostResponse(Response post, Long teacherId) {
        SchoolClass schoolClass = findTeacherClass(teacherId);
        long[] counts = countChoicesByPost(List.of(post.getId()))
            .getOrDefault(post.getId(), new long[2]);

        return IndividualBookChatPostResponse.fromPost(
            post, false, schoolClass.getId(), counts[0], counts[1]);
    }

    private Response findTeacherManagedPost(Long teacherId, Long postId) {
        Response post = responseRepository
            .findByIdAndModeAndContentTypeAndDeletedAtIsNull(postId, MODE_INDIVIDUAL, CONTENT_TYPE_CHAT_POST)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "책수다 글을 찾을 수 없습니다. postId=" + postId
            ));

        SchoolClass schoolClass = findTeacherClass(teacherId);
        ClassStudent writerMembership = classStudentRepository
            .findByStudentId(post.getStudent().getId())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "담당 학급 학생의 글만 처리할 수 있습니다."
            ));

        if (!writerMembership.getSchoolClass().getId().equals(schoolClass.getId())) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "담당 학급 학생의 글만 처리할 수 있습니다."
            );
        }

        return post;
    }

    private SchoolClass findTeacherClass(Long teacherId) {
        return schoolClassRepository.findByTeacherId(teacherId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "교사의 학급 정보를 찾을 수 없습니다. teacherId=" + teacherId
            ));
    }

    private User findTeacher(Long teacherId) {
        User teacher = userRepository.findById(teacherId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "교사 정보를 찾을 수 없습니다. teacherId=" + teacherId
            ));

        if (!"teacher".equals(teacher.getRole())) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "교사 계정만 처리할 수 있습니다."
            );
        }

        return teacher;
    }

    private List<Long> classmateStudentIdsForClass(Long classId) {
        return classStudentRepository
            .findBySchoolClassId(classId)
            .stream()
            .map(classStudent -> classStudent.getStudent().getId())
            .toList();
    }

    private boolean isVisibleToStudent(Response post, Long viewerStudentId) {
        return post.getStudent().getId().equals(viewerStudentId) || isApproved(post);
    }

    private boolean isApproved(Response post) {
        return STATUS_APPROVED.equals(normalizeStatus(post.getStatus()));
    }

    private String normalizeStatusOrAll(String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            return "ALL";
        }

        return normalizeStatus(status);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return STATUS_PENDING;
        }

        String normalized = status.trim().toUpperCase();

        if (!STATUS_PENDING.equals(normalized)
                && !STATUS_APPROVED.equals(normalized)
                && !STATUS_REJECTED.equals(normalized)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "moderationStatus는 PENDING, APPROVED, REJECTED 중 하나여야 합니다."
            );
        }

        return normalized;
    }

    private String toStoredStatus(String status) {
        return normalizeStatus(status).toLowerCase();
    }

    private boolean inSameClass(Long studentIdA, Long studentIdB) {
        Optional<Long> classIdA = classStudentRepository.findByStudentId(studentIdA)
            .map(classStudent -> classStudent.getSchoolClass().getId());
        Optional<Long> classIdB = classStudentRepository.findByStudentId(studentIdB)
            .map(classStudent -> classStudent.getSchoolClass().getId());

        return classIdA.isPresent() && classIdA.equals(classIdB);
    }

    private String normalizeChoice(String choice) {
        String normalized = choice == null ? "" : choice.trim().toUpperCase();

        if (!normalized.equals("A") && !normalized.equals("B")) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "choice는 A 또는 B여야 합니다."
            );
        }

        return normalized;
    }

    @Transactional
    public void deletePost(Long studentId, Long postId) {
        Response response = responseRepository
            .findByIdAndStudent_IdAndModeAndContentType(postId, studentId, MODE_INDIVIDUAL, CONTENT_TYPE_CHAT_POST)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "책수다방 글을 찾을 수 없습니다. postId=" + postId
            ));

        response.setDeletedAt(LocalDateTime.now(ZONE_SEOUL));
        responseRepository.save(response);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /*
     * 책수다방 글은 반드시 학생이 지금 진행 중인 책(readingRecord)에
     * 연결되어야 한다. bookTitle(자유 입력 텍스트)로 되짚어 추측하지 않고
     * 이 값만 신뢰한다 - 존재 여부, 본인 소유 여부, 진행 중 상태(완독 전)를
     * 순서대로 검증한다.
     */
    private ReadingRecord requireInProgressOwnedReadingRecord(Long studentId, Long readingRecordId) {
        if (readingRecordId == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "책수다방 글을 쓸 책 정보를 찾을 수 없습니다. 책읽기 화면에서 다시 시도해 주세요."
            );
        }

        ReadingRecord readingRecord = readingRecordRepository.findById(readingRecordId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "개별읽기 기록을 찾을 수 없습니다. readingRecordId=" + readingRecordId
            ));

        if (readingRecord.getStudent() == null || !readingRecord.getStudent().getId().equals(studentId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "본인의 개별읽기 기록만 사용할 수 있습니다."
            );
        }

        if (readingRecord.getFinishedAt() != null) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "완독한 책은 책수다방 글을 새로 쓸 수 없습니다. 진행 중인 책으로 다시 시도해 주세요."
            );
        }

        return readingRecord;
    }
}
