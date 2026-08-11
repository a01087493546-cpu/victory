package com.victory.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.BookRecommendationClassWallResponse;
import com.victory.dto.BookRecommendationCompletedBookItem;
import com.victory.dto.BookRecommendationCreateRequest;
import com.victory.dto.BookRecommendationItem;
import com.victory.dto.BookRecommendationLikeResponse;
import com.victory.dto.BookRecommendationQuestionItem;
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

import lombok.RequiredArgsConstructor;

/*
 * 우리 반 추천 책장(학생 화면) + 교사 추천 책장(교사 화면)이 함께 쓰는
 * 서비스. 같은 content_likes 테이블을 학생/교사 구분 없이 함께 조회하므로,
 * 한쪽에서 누른 좋아요가 즉시 다른 쪽 화면의 likeCount에도 반영된다
 * (IndividualSummaryShareService와 같은 관례).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookRecommendationService {

    public static final String CONTENT_TYPE_BOOK_RECOMMENDATION = "book_recommendation";
    private static final int BEST_SIZE = 3;
    private static final int REQUIRED_TEASER_RESPONSE_COUNT = 1;
    private static final String MODE_INDIVIDUAL = "individual";
    private static final String CONTENT_TYPE_ANSWER = "answer";
    private static final String STAGE_BEFORE = "before";
    private static final String STAGE_DURING = "during";
    private static final String STAGE_AFTER = "after";

    private final BookRecommendationRepository bookRecommendationRepository;
    private final BookRecommendationRewardService rewardService;
    private final ContentLikeRepository contentLikeRepository;
    private final ClassStudentRepository classStudentRepository;
    private final ReadingRecordRepository readingRecordRepository;
    private final ResponseRepository responseRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final UserRepository userRepository;
    private final IndividualAchievementService individualAchievementService;

    @Transactional
    public BookRecommendationItem createRecommendation(Long studentId, BookRecommendationCreateRequest request) {
        User student = findUser(studentId);

        // toggleLikeAsStudent/AsTeacher와 동일한 이유로 추천 글 작성도 심사계정은 서버에 남기지 않는다.
        if (Boolean.TRUE.equals(student.getDemoAccount())) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "심사계정의 책추천 글은 브라우저에만 저장됩니다."
            );
        }

        findClassStudent(studentId);
        ReadingRecord readingRecord = requireCompletedOwnedReadingRecord(studentId, request.getReadingRecordId());
        List<Long> teaserResponseIds =
            validateTeaserResponses(studentId, readingRecord.getId(), request.getTeaserResponseIds());

        BookRecommendation recommendation = new BookRecommendation();
        recommendation.setStudent(student);
        recommendation.setReadingRecord(readingRecord);
        recommendation.setTitle(readingRecord.getBook().getTitle().trim());
        recommendation.setAuthor(trimToEmpty(readingRecord.getBook().getAuthor()));
        recommendation.setReason(request.getReason().trim());
        recommendation.setTeaserResponseIds(teaserResponseIds);

        BookRecommendation saved = bookRecommendationRepository.save(recommendation);
        BookRecommendationRewardService.RewardResult rewardResult =
            rewardService.grantRecommendationRewardOnce(student, readingRecord.getId());
        refreshFinalReadingPracticeScore(readingRecord);
        StudentStats stats = rewardResult.getStats();

        return BookRecommendationItem.of(saved, resolveTeaserQuestions(saved), 0L, false, true)
            .withReward(rewardResult.isRewardGranted(), stats == null ? null : stats.getCourage());
    }

    /*
     * calculate()는 완독 기록에서 저장된 final_reading_practice_score를
     * 우선 반환하므로, 방금 저장한 추천을 반영하려면 그 저장값 우선
     * 정책을 건너뛰는 calculateLiveReadingPracticeScore()를 써야 한다
     * (원본 활동 데이터만으로 강제 재계산). final_record_completion_score와
     * finished_at은 여기서 절대 건드리지 않는다.
     */
    private void refreshFinalReadingPracticeScore(ReadingRecord readingRecord) {
        if (readingRecord.getFinishedAt() == null) {
            return;
        }

        double liveReadingPracticeScore =
            individualAchievementService.calculateLiveReadingPracticeScore(readingRecord.getId());
        readingRecord.setFinalReadingPracticeScore((int) Math.round(liveReadingPracticeScore));
    }

    public List<BookRecommendationCompletedBookItem> getCompletedBooks(Long studentId) {
        return readingRecordRepository.findByStudent_IdAndFinishedAtIsNotNull(studentId).stream()
            .sorted(Comparator
                .comparing(ReadingRecord::getFinishedAt, Comparator.reverseOrder())
                .thenComparing(ReadingRecord::getId, Comparator.reverseOrder()))
            .map(record -> new BookRecommendationCompletedBookItem(
                record.getId(),
                record.getBook().getTitle(),
                trimToEmpty(record.getBook().getAuthor()),
                record.getFinishedAt()))
            .toList();
    }

    public List<BookRecommendationQuestionItem> getCompletedBookQuestions(Long studentId, Long readingRecordId) {
        ReadingRecord readingRecord = requireCompletedOwnedReadingRecord(studentId, readingRecordId);

        return List.of(STAGE_BEFORE, STAGE_DURING, STAGE_AFTER).stream()
            .flatMap(stage -> responseRepository
                .findByStudent_IdAndReadingRecord_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                    studentId,
                    readingRecord.getId(),
                    MODE_INDIVIDUAL,
                    CONTENT_TYPE_ANSWER,
                    stage)
                .stream()
                .filter(response -> response.getPassed() == null || Boolean.TRUE.equals(response.getPassed()))
                .filter(response -> !STAGE_AFTER.equals(stage) || Boolean.TRUE.equals(response.getPassed()))
                .filter(response -> !extractQuestion(response).isBlank())
                .map(response -> new BookRecommendationQuestionItem(
                    response.getId(),
                    stage,
                    getCategoryLabel(stage),
                    getDetailLabel(response),
                    extractQuestion(response))))
            .toList();
    }

    public BookRecommendationClassWallResponse getClassWallForStudent(Long studentId) {
        ClassStudent viewerClassStudent = findClassStudent(studentId);
        List<Long> classmateIds = studentIdsInClass(viewerClassStudent.getSchoolClass().getId());

        return buildClassWall(classmateIds, studentId, studentId);
    }

    public BookRecommendationClassWallResponse getClassWallForTeacher(Long teacherId) {
        SchoolClass teacherClass = findTeacherClass(teacherId);
        List<Long> studentIds = studentIdsInClass(teacherClass.getId());

        return buildClassWall(studentIds, teacherId, null);
    }

    @Transactional
    public BookRecommendationLikeResponse toggleLikeAsStudent(Long studentId, Long recommendationId) {
        ClassStudent viewerClassStudent = findClassStudent(studentId);
        BookRecommendation recommendation =
            requireRecommendationInClass(recommendationId, viewerClassStudent.getSchoolClass().getId());

        User viewer = findUser(studentId);
        if (Boolean.TRUE.equals(viewer.getDemoAccount())) {
            return unchangedDemoLike(recommendationId);
        }
        return toggleLike(viewer, recommendation);
    }

    @Transactional
    public BookRecommendationLikeResponse toggleLikeAsTeacher(Long teacherId, Long recommendationId) {
        SchoolClass teacherClass = findTeacherClass(teacherId);
        BookRecommendation recommendation = requireRecommendationInClass(recommendationId, teacherClass.getId());

        User viewer = findUser(teacherId);
        if (Boolean.TRUE.equals(viewer.getDemoAccount())) {
            return unchangedDemoLike(recommendationId);
        }
        return toggleLike(viewer, recommendation);
    }

    private BookRecommendationLikeResponse unchangedDemoLike(Long recommendationId) {
        long likeCount = contentLikeRepository.countByContentTypeAndContentId(
            CONTENT_TYPE_BOOK_RECOMMENDATION, recommendationId);
        return new BookRecommendationLikeResponse(recommendationId, false, likeCount);
    }

    /*
     * 좋아요 대상 검증: 실제로 존재하고, 작성자의 학급이 조회자의 학급과
     * 같아야 한다. 다른 학급 recommendationId를 직접 호출해도 403으로 막힌다.
     */
    private BookRecommendation requireRecommendationInClass(Long recommendationId, Long classId) {
        BookRecommendation recommendation = bookRecommendationRepository.findById(recommendationId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "추천 글을 찾을 수 없습니다. recommendationId=" + recommendationId
            ));

        ClassStudent ownerClassStudent = classStudentRepository
            .findByStudentId(recommendation.getStudent().getId())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "작성자의 학급 정보를 찾을 수 없습니다."
            ));

        if (!ownerClassStudent.getSchoolClass().getId().equals(classId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "같은 학급의 추천 글에만 좋아요를 누를 수 있습니다."
            );
        }

        return recommendation;
    }

    /*
     * 이미 좋아요가 있으면 삭제(취소), 없으면 추가한다 - 한 사용자(학생이든
     * 교사든 User 한 명)당 같은 추천 글에는 content_likes UNIQUE 제약으로
     * 최대 1행만 존재하므로 학생/교사 좋아요는 자연스럽게 합산된다.
     */
    private BookRecommendationLikeResponse toggleLike(User viewer, BookRecommendation recommendation) {
        Optional<ContentLike> existing = contentLikeRepository
            .findByStudent_IdAndContentTypeAndContentId(
                viewer.getId(), CONTENT_TYPE_BOOK_RECOMMENDATION, recommendation.getId());

        boolean liked;

        if (existing.isPresent()) {
            contentLikeRepository.delete(existing.get());
            liked = false;
        } else {
            ContentLike like = new ContentLike();
            like.setStudent(viewer);
            like.setContentType(CONTENT_TYPE_BOOK_RECOMMENDATION);
            like.setContentId(recommendation.getId());
            contentLikeRepository.save(like);
            liked = true;
        }

        long likeCount = contentLikeRepository
            .countByContentTypeAndContentId(CONTENT_TYPE_BOOK_RECOMMENDATION, recommendation.getId());

        return new BookRecommendationLikeResponse(recommendation.getId(), liked, likeCount);
    }

    /*
     * BEST 3: 좋아요 수 내림차순 -> 작성일 내림차순 -> id 내림차순.
     * recent: BEST에 뽑힌 글을 제외하고 작성일 내림차순 -> id 내림차순
     * (repository가 이미 이 순서로 조회하므로 필터만 하면 된다).
     */
    private BookRecommendationClassWallResponse buildClassWall(
            List<Long> studentIds, Long viewerUserId, Long mineStudentId) {

        if (studentIds.isEmpty()) {
            return new BookRecommendationClassWallResponse(List.of(), List.of());
        }

        List<BookRecommendation> recommendations =
            bookRecommendationRepository.findByStudent_IdInOrderByCreatedAtDescIdDesc(studentIds);

        if (recommendations.isEmpty()) {
            return new BookRecommendationClassWallResponse(List.of(), List.of());
        }

        List<Long> recommendationIds = recommendations.stream().map(BookRecommendation::getId).toList();

        Set<Long> likedIds = viewerUserId == null
            ? Set.of()
            : contentLikeRepository
                .findByStudent_IdAndContentTypeAndContentIdIn(
                    viewerUserId, CONTENT_TYPE_BOOK_RECOMMENDATION, recommendationIds)
                .stream()
                .map(ContentLike::getContentId)
                .collect(Collectors.toSet());

        List<BookRecommendationItem> items = recommendations.stream()
            .map(recommendation -> BookRecommendationItem.of(
                recommendation,
                resolveTeaserQuestions(recommendation),
                contentLikeRepository.countByContentTypeAndContentId(
                    CONTENT_TYPE_BOOK_RECOMMENDATION, recommendation.getId()),
                likedIds.contains(recommendation.getId()),
                mineStudentId != null && mineStudentId.equals(recommendation.getStudent().getId())))
            .toList();

        List<BookRecommendationItem> bestSorted = items.stream()
            .sorted(Comparator
                .comparingLong(BookRecommendationItem::getLikeCount).reversed()
                .thenComparing(BookRecommendationItem::getCreatedAt, Comparator.reverseOrder())
                .thenComparing(BookRecommendationItem::getRecommendationId, Comparator.reverseOrder()))
            .toList();

        List<BookRecommendationItem> best = new ArrayList<>();
        for (int i = 0; i < Math.min(BEST_SIZE, bestSorted.size()); i++) {
            best.add(bestSorted.get(i).withRank(i + 1));
        }

        Set<Long> bestIds = best.stream()
            .map(BookRecommendationItem::getRecommendationId)
            .collect(Collectors.toSet());

        List<BookRecommendationItem> recent = items.stream()
            .filter(item -> !bestIds.contains(item.getRecommendationId()))
            .toList();

        return new BookRecommendationClassWallResponse(best, recent);
    }

    private List<Long> validateTeaserResponses(Long studentId, Long readingRecordId, List<Long> requestedIds) {
        if (requestedIds == null || requestedIds.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "궁금해지는 질문은 1개만 선택할 수 있습니다."
            );
        }

        List<Long> ids = requestedIds.stream()
            .filter(id -> id != null)
            .collect(Collectors.collectingAndThen(
                Collectors.toCollection(LinkedHashSet::new),
                ArrayList::new));

        if (ids.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "궁금해지는 질문은 1개만 선택할 수 있습니다."
            );
        }

        if (ids.size() != REQUIRED_TEASER_RESPONSE_COUNT) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "궁금해지는 질문은 1개만 선택할 수 있습니다."
            );
        }

        List<Response> responses = responseRepository.findAllById(ids);
        Map<Long, Response> byId = responses.stream()
            .collect(Collectors.toMap(Response::getId, Function.identity()));

        for (Long id : ids) {
            Response response = byId.get(id);

            if (response == null) {
                throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "질문을 찾을 수 없습니다. responseId=" + id
                );
            }

            validateTeaserResponse(studentId, readingRecordId, response);
        }

        return ids;
    }

    private void validateTeaserResponse(Long studentId, Long readingRecordId, Response response) {
        if (response.getStudent() == null || !response.getStudent().getId().equals(studentId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "본인의 질문만 추천 질문으로 선택할 수 있습니다."
            );
        }

        if (response.getDeletedAt() != null
                || !MODE_INDIVIDUAL.equals(response.getMode())
                || !CONTENT_TYPE_ANSWER.equals(response.getContentType())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "추천 질문으로 선택할 수 없는 응답입니다."
            );
        }

        if (response.getReadingRecord() == null || !response.getReadingRecord().getId().equals(readingRecordId)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "선택한 책의 질문만 추천 질문으로 선택할 수 있습니다."
            );
        }

        String stage = response.getStage();
        boolean validStage = STAGE_BEFORE.equals(stage) || STAGE_DURING.equals(stage) || STAGE_AFTER.equals(stage);

        if (!validStage) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "추천 질문으로 선택할 수 없는 응답입니다."
            );
        }

        if (STAGE_AFTER.equals(stage) && !Boolean.TRUE.equals(response.getPassed())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "루미 피드백을 통과한 질문만 선택할 수 있습니다."
            );
        }

        if (Boolean.FALSE.equals(response.getPassed())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "루미 피드백을 통과한 질문만 선택할 수 있습니다."
            );
        }

        String question = extractQuestion(response);

        if (question.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "질문 내용이 비어 있는 응답은 선택할 수 없습니다."
            );
        }
    }

    private ReadingRecord requireCompletedOwnedReadingRecord(Long studentId, Long readingRecordId) {
        if (readingRecordId == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "추천할 완독 책을 선택해야 합니다."
            );
        }

        ReadingRecord readingRecord = readingRecordRepository.findById(readingRecordId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "독서 기록을 찾을 수 없습니다. readingRecordId=" + readingRecordId
            ));

        if (readingRecord.getStudent() == null || !readingRecord.getStudent().getId().equals(studentId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "본인의 완독 책만 추천할 수 있습니다."
            );
        }

        if (readingRecord.getFinishedAt() == null) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "완독한 책만 추천할 수 있습니다."
            );
        }

        return readingRecord;
    }

    private String getCategoryLabel(String stage) {
        if (STAGE_BEFORE.equals(stage)) return "읽기 전";
        if (STAGE_DURING.equals(stage)) return "읽기 중";
        if (STAGE_AFTER.equals(stage)) return "읽기 후";
        return "";
    }

    private String getDetailLabel(Response response) {
        Map<String, Object> extraData = response.getExtraData();

        if (STAGE_BEFORE.equals(response.getStage())) {
            String stepType = extraData == null || extraData.get("stepType") == null
                ? ""
                : extraData.get("stepType").toString();

            return switch (stepType) {
                case "title" -> "제목 보고 질문 만들기";
                case "contents" -> "차례 보고 질문 만들기";
                case "picture" -> "그림 보고 질문 만들기";
                case "skim" -> "훑어보고 질문 만들기";
                default -> "읽기 전 질문";
            };
        }

        if (STAGE_DURING.equals(response.getStage())) {
            String questionType = extraData == null || extraData.get("questionType") == null
                ? ""
                : extraData.get("questionType").toString();

            return switch (questionType) {
                case "find" -> "바로 찾기";
                case "infer" -> "짐작하기";
                case "feel" -> "생각·느낌";
                case "connect" -> "삶과 연결";
                default -> "읽는 중 질문";
            };
        }

        Object questionIndex = extraData == null ? null : extraData.get("questionIndex");

        return questionIndex == null ? "읽기 후 질문" : "질문 " + questionIndex;
    }

    private List<String> resolveTeaserQuestions(BookRecommendation recommendation) {
        List<Long> ids = recommendation.getTeaserResponseIds();

        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        List<Long> uniqueIds = ids.stream()
            .filter(id -> id != null)
            .collect(Collectors.collectingAndThen(
                Collectors.toCollection(LinkedHashSet::new),
                ArrayList::new));

        if (uniqueIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Response> byId = responseRepository.findAllById(uniqueIds).stream()
            .collect(Collectors.toMap(Response::getId, Function.identity()));

        return uniqueIds.stream()
            .map(byId::get)
            .filter(response -> response != null && response.getDeletedAt() == null)
            .map(this::extractQuestion)
            .filter(question -> !question.isBlank())
            .toList();
    }

    private String extractQuestion(Response response) {
        Object question = response.getExtraData() == null
            ? null
            : response.getExtraData().get("question");

        return question == null ? "" : question.toString().trim();
    }

    private String trimToEmpty(String text) {
        return text == null ? "" : text.trim();
    }

    private List<Long> studentIdsInClass(Long classId) {
        return classStudentRepository.findBySchoolClassId(classId).stream()
            .map(classStudent -> classStudent.getStudent().getId())
            .toList();
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

    private User findUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "사용자를 찾을 수 없습니다. userId=" + userId
            ));
    }

    private User findTeacher(Long teacherId) {
        User user = findUser(teacherId);

        if (!"teacher".equals(user.getRole())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "교사 계정만 사용할 수 있습니다."
            );
        }

        return user;
    }
}
