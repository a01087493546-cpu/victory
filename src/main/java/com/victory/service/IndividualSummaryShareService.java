package com.victory.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.IndividualSummaryLikeResponse;
import com.victory.dto.IndividualSummaryShareItem;
import com.victory.entity.ClassStudent;
import com.victory.entity.ContentLike;
import com.victory.entity.Response;
import com.victory.entity.SchoolClass;
import com.victory.entity.Summary;
import com.victory.entity.User;
import com.victory.repository.ClassStudentRepository;
import com.victory.repository.ContentLikeRepository;
import com.victory.repository.ResponseRepository;
import com.victory.repository.SchoolClassRepository;
import com.victory.repository.SummaryRepository;
import com.victory.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/*
 * 개별읽기 읽기 후 "우리 반 간추리기 모음"(학생 화면) + 간추리기 확인
 * (교사 화면)이 공유하는 서비스. 같은 content_likes 테이블을 학생/교사
 * 구분 없이 함께 조회하므로, 한쪽에서 누른 좋아요가 즉시 다른 쪽 화면의
 * likeCount에도 반영된다(캐시 컬럼 없이 매번 COUNT).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IndividualSummaryShareService {

    public static final String CONTENT_TYPE_INDIVIDUAL_SUMMARY = "individual_summary";
    private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");
    private static final String MODE_INDIVIDUAL = "individual";
    private static final String CONTENT_TYPE_ANSWER = "answer";
    private static final String STAGE_AFTER = "after";
    private static final String STATUS_APPROVED = "approved";
    private static final List<Integer> AFTER_QUESTION_INDEXES = List.of(1, 2, 3);

    private final SummaryRepository summaryRepository;
    private final ContentLikeRepository contentLikeRepository;
    private final ClassStudentRepository classStudentRepository;
    private final ResponseRepository responseRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final UserRepository userRepository;

    public List<IndividualSummaryShareItem> getClassSummariesForStudent(Long studentId, LocalDate date) {
        ClassStudent viewerClassStudent = findClassStudent(studentId);
        requireStudentReadyToViewSharedSummaries(studentId);

        List<Long> classmateIds = studentIdsInClass(viewerClassStudent.getSchoolClass().getId());

        return buildItems(classmateIds, studentId, studentId, resolveDate(date),
            Boolean.TRUE.equals(viewerClassStudent.getStudent().getDemoAccount()));
    }

    public List<IndividualSummaryShareItem> getClassSummariesForTeacher(Long teacherId, LocalDate date) {
        SchoolClass teacherClass = findTeacherClass(teacherId);
        List<Long> studentIds = studentIdsInClass(teacherClass.getId());

        /*
         * 심사 교사(tt11)도 심사 학생과 같은 브라우저에서 날짜와 무관하게
         * seed 예시 + 방금 작성한 direct 간추리기를 함께 봐야 한다
         * (getClassSummariesForStudent와 같은 원칙). 여기서는 false로
         * 고정돼 있어서 seed의 실제 생성일이 오늘이 아니면 교사 화면에서
         * 전부 걸러지고, 프론트가 별도로 병합해 둔 direct 카드만 남아
         * "seed가 사라진 것처럼" 보였다.
         */
        boolean includeAllDatesForDemo = Boolean.TRUE.equals(findTeacher(teacherId).getDemoAccount());

        return buildItems(studentIds, teacherId, null, resolveDate(date), includeAllDatesForDemo);
    }

    @Transactional
    public IndividualSummaryLikeResponse toggleLikeAsStudent(Long studentId, Long summaryId) {
        ClassStudent viewerClassStudent = findClassStudent(studentId);
        Summary summary = requireSharedSummaryInClass(summaryId, viewerClassStudent.getSchoolClass().getId());
        User viewer = findUser(studentId);
        if (Boolean.TRUE.equals(viewer.getDemoAccount())) {
            return new IndividualSummaryLikeResponse(summaryId, false,
                contentLikeRepository.countByContentTypeAndContentId(CONTENT_TYPE_INDIVIDUAL_SUMMARY, summaryId));
        }
        return toggleLike(viewer, summary);
    }

    @Transactional
    public IndividualSummaryLikeResponse toggleLikeAsTeacher(Long teacherId, Long summaryId) {
        SchoolClass teacherClass = findTeacherClass(teacherId);
        Summary summary = requireSharedSummaryInClass(summaryId, teacherClass.getId());

        User viewer = findUser(teacherId);
        if (Boolean.TRUE.equals(viewer.getDemoAccount())) {
            return new IndividualSummaryLikeResponse(summaryId, false,
                contentLikeRepository.countByContentTypeAndContentId(CONTENT_TYPE_INDIVIDUAL_SUMMARY, summaryId));
        }
        return toggleLike(viewer, summary);
    }

    /*
     * 좋아요 대상 검증: 실제로 존재하고, 개별읽기 간추리기이며(readingRecord
     * 존재), 공유 조건(AI 통과 + 승인 상태)을 충족하고, 조회자의 학급과
     * 작성자의 학급이 같아야 한다. 다른 학급 summaryId를 직접 호출해도
     * 403으로 막힌다.
     */
    private Summary requireSharedSummaryInClass(Long summaryId, Long classId) {
        Summary summary = summaryRepository.findById(summaryId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "간추리기를 찾을 수 없습니다. summaryId=" + summaryId
            ));

        if (summary.getReadingRecord() == null
                || !Boolean.TRUE.equals(summary.getAiPassed())
                || !STATUS_APPROVED.equals(summary.getStatus())) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "공유된 개별읽기 간추리기가 아닙니다. summaryId=" + summaryId
            );
        }

        ClassStudent ownerClassStudent = classStudentRepository
            .findByStudentId(summary.getStudent().getId())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "작성자의 학급 정보를 찾을 수 없습니다."
            ));

        if (!ownerClassStudent.getSchoolClass().getId().equals(classId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "같은 학급의 간추리기에만 좋아요를 누를 수 있습니다."
            );
        }

        return summary;
    }

    /*
     * 이미 좋아요가 있으면 삭제(취소), 없으면 추가한다 - 한 사용자(학생이든
     * 교사든 User 한 명)당 같은 간추리기에는 content_likes UNIQUE 제약으로
     * 최대 1행만 존재한다.
     */
    private IndividualSummaryLikeResponse toggleLike(User viewer, Summary summary) {
        Optional<ContentLike> existing = contentLikeRepository
            .findByStudent_IdAndContentTypeAndContentId(
                viewer.getId(), CONTENT_TYPE_INDIVIDUAL_SUMMARY, summary.getId());

        boolean liked;

        if (existing.isPresent()) {
            contentLikeRepository.delete(existing.get());
            liked = false;
        } else {
            ContentLike like = new ContentLike();
            like.setStudent(viewer);
            like.setContentType(CONTENT_TYPE_INDIVIDUAL_SUMMARY);
            like.setContentId(summary.getId());
            contentLikeRepository.save(like);
            liked = true;
        }

        long likeCount = contentLikeRepository
            .countByContentTypeAndContentId(CONTENT_TYPE_INDIVIDUAL_SUMMARY, summary.getId());

        return new IndividualSummaryLikeResponse(summary.getId(), liked, likeCount);
    }

    private List<IndividualSummaryShareItem> buildItems(
            List<Long> studentIds,
            Long viewerUserId,
            Long mineStudentId,
            LocalDate date,
            boolean includeAllDatesForDemo) {
        if (studentIds.isEmpty()) {
            return List.of();
        }

        if (!includeAllDatesForDemo && date.isAfter(LocalDate.now(ZONE_SEOUL))) {
            return List.of();
        }

        LocalDateTime startAt = date.atStartOfDay();
        LocalDateTime endAt = date.plusDays(1).atStartOfDay();

        List<Summary> summaries = includeAllDatesForDemo
            ? summaryRepository.findAllSharedIndividualSummariesByStudentIds(studentIds)
            : summaryRepository.findSharedIndividualSummariesByStudentIdsAndCreatedAtBetween(studentIds, startAt, endAt);

        Set<Long> likedSummaryIds = viewerUserId == null || summaries.isEmpty()
            ? Set.of()
            : contentLikeRepository
                .findByStudent_IdAndContentTypeAndContentIdIn(
                    viewerUserId,
                    CONTENT_TYPE_INDIVIDUAL_SUMMARY,
                    summaries.stream().map(Summary::getId).toList())
                .stream()
                .map(ContentLike::getContentId)
                .collect(Collectors.toSet());

        return summaries.stream()
            .map(summary -> IndividualSummaryShareItem.of(
                summary,
                summary.getStudent().getName(),
                contentLikeRepository.countByContentTypeAndContentId(
                    CONTENT_TYPE_INDIVIDUAL_SUMMARY, summary.getId()),
                likedSummaryIds.contains(summary.getId()),
                mineStudentId != null && mineStudentId.equals(summary.getStudent().getId())))
            .sorted(Comparator
                .comparing(IndividualSummaryShareItem::isMine).reversed()
                .thenComparing(IndividualSummaryShareItem::getCreatedAt, Comparator.reverseOrder())
                .thenComparing(IndividualSummaryShareItem::getSummaryId, Comparator.reverseOrder()))
            .toList();
    }

    private LocalDate resolveDate(LocalDate date) {
        return date == null ? LocalDate.now(ZONE_SEOUL) : date;
    }

    /*
     * 학생은 자신의 읽기 후 질문 3개와 최종 간추리기를 모두 루미 통과 상태로
     * 저장한 뒤에만 친구들의 간추리기를 볼 수 있다. afterDone/완독 여부는
     * 잠금 기준이 아니다.
     */
    private void requireStudentReadyToViewSharedSummaries(Long studentId) {
        List<Summary> mySummaries = summaryRepository
            .findByStudent_IdAndReadingRecordIsNotNullAndAiPassedTrueAndStatusOrderByCreatedAtDesc(
                studentId, STATUS_APPROVED);

        boolean ready = mySummaries.stream()
            .anyMatch(this::hasThreePassedAfterResponses);

        if (!ready) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "내 간추리기를 먼저 완성해 주세요."
            );
        }
    }

    private boolean hasThreePassedAfterResponses(Summary summary) {
        if (summary.getReadingRecord() == null
                || summary.getSummaryText() == null
                || summary.getSummaryText().isBlank()) {
            return false;
        }

        List<Response> responses = responseRepository
            .findByStudent_IdAndReadingRecord_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                summary.getStudent().getId(),
                summary.getReadingRecord().getId(),
                MODE_INDIVIDUAL,
                CONTENT_TYPE_ANSWER,
                STAGE_AFTER);

        return AFTER_QUESTION_INDEXES.stream()
            .allMatch(index -> responses.stream()
                .anyMatch(response -> index.equals(extractQuestionIndex(response))
                    && response.getContent() != null
                    && !response.getContent().isBlank()
                    && Boolean.TRUE.equals(response.getPassed())));
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
