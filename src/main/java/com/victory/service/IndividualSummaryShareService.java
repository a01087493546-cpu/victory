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
    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_REJECTED = "rejected";
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

        List<IndividualSummaryShareItem> approved = buildItems(classmateIds, studentId, studentId, resolveDate(date),
            Boolean.TRUE.equals(viewerClassStudent.getStudent().getDemoAccount()));
        List<IndividualSummaryShareItem> mine = summaryRepository
            .findAllReviewableIndividualSummariesByStudentIds(List.of(studentId)).stream()
            .filter(summary -> !STATUS_APPROVED.equals(normalizeStatus(summary.getStatus())))
            .map(summary -> toItem(summary, studentId, studentId))
            .toList();
        return java.util.stream.Stream.concat(mine.stream(), approved.stream())
            .distinct().toList();
    }

    public List<IndividualSummaryShareItem> getReviewSummariesForTeacher(Long teacherId, String status) {
        SchoolClass teacherClass = findTeacherClass(teacherId);
        String normalized = status == null || status.isBlank() ? null : normalizeStatus(status);
        return summaryRepository.findAllReviewableIndividualSummariesByStudentIds(
                studentIdsInClass(teacherClass.getId())).stream()
            .filter(summary -> normalized == null || normalized.equals(normalizeStatus(summary.getStatus())))
            .map(summary -> toItem(summary, teacherId, null))
            .toList();
    }

    @Transactional
    public IndividualSummaryShareItem approve(Long teacherId, Long summaryId) {
        Summary summary = requireTeacherOwnedSummary(teacherId, summaryId);
        summary.setStatus(STATUS_APPROVED);
        summary.setRejectionReason(null);
        return toItem(summaryRepository.save(summary), teacherId, null);
    }

    @Transactional
    public IndividualSummaryShareItem reject(Long teacherId, Long summaryId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "거절 사유를 입력해야 합니다.");
        }
        Summary summary = requireTeacherOwnedSummary(teacherId, summaryId);
        summary.setStatus(STATUS_REJECTED);
        summary.setRejectionReason(reason.trim());
        return toItem(summaryRepository.save(summary), teacherId, null);
    }

    /*
     * 교사가 APPROVED/REJECTED 상태를 다시 PENDING(대기)으로 되돌린다
     * ("승인 취소"/"대기로 돌리기"). approve/reject와 마찬가지로 현재 상태를
     * 검사하지 않고 같은 Summary 행을 UPDATE만 하므로, 좋아요(ContentLike)는
     * 전혀 건드리지 않아 그대로 보존된다.
     */
    @Transactional
    public IndividualSummaryShareItem returnToPending(Long teacherId, Long summaryId) {
        Summary summary = requireTeacherOwnedSummary(teacherId, summaryId);
        summary.setStatus(STATUS_PENDING);
        summary.setRejectionReason(null);
        return toItem(summaryRepository.save(summary), teacherId, null);
    }

    /*
     * requireOwnedRejectedSummary가 이미 REJECTED 상태만 통과시키므로
     * 여기 도달한 시점의 이전 status는 항상 rejected다 - 남은 조건은
     * "본문이 실제로 바뀌었는가"뿐이다. 본문이 바뀐 재제출일 때만 기존
     * 좋아요를 전부 지운다(수정 전 글에 대한 반응이므로 수정본에 그대로
     * 이어지면 안 됨) - approve/reject/returnToPending처럼 상태만 바뀌는
     * 경로는 이 메서드를 타지 않으므로 좋아요가 그대로 보존된다.
     */
    @Transactional
    public IndividualSummaryShareItem resubmit(Long studentId, Long summaryId, String summaryText) {
        Summary summary = requireOwnedRejectedSummary(studentId, summaryId);
        if (summaryText == null || summaryText.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "간추리기 내용을 입력해야 합니다.");
        }
        String trimmed = summaryText.trim();
        boolean textChanged = !trimmed.equals(summary.getSummaryText());
        summary.setSummaryText(trimmed);
        summary.setStatus(STATUS_PENDING);
        summary.setRejectionReason(null);
        Summary saved = summaryRepository.save(summary);
        if (textChanged) {
            contentLikeRepository.deleteByContentTypeAndContentId(CONTENT_TYPE_INDIVIDUAL_SUMMARY, saved.getId());
        }
        return toItem(saved, studentId, studentId);
    }

    @Transactional
    public void deleteRejected(Long studentId, Long summaryId) {
        summaryRepository.delete(requireOwnedRejectedSummary(studentId, summaryId));
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

    private IndividualSummaryShareItem toItem(Summary summary, Long viewerUserId, Long mineStudentId) {
        boolean liked = viewerUserId != null && contentLikeRepository
            .findByStudent_IdAndContentTypeAndContentId(viewerUserId, CONTENT_TYPE_INDIVIDUAL_SUMMARY, summary.getId())
            .isPresent();
        return IndividualSummaryShareItem.of(summary, summary.getStudent().getName(),
            contentLikeRepository.countByContentTypeAndContentId(CONTENT_TYPE_INDIVIDUAL_SUMMARY, summary.getId()),
            liked, mineStudentId != null && mineStudentId.equals(summary.getStudent().getId()));
    }

    private Summary requireTeacherOwnedSummary(Long teacherId, Long summaryId) {
        SchoolClass teacherClass = findTeacherClass(teacherId);
        Summary summary = summaryRepository.findById(summaryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "간추리기를 찾을 수 없습니다."));
        if (summary.getReadingRecord() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "개별읽기 간추리기가 아닙니다.");
        }
        ClassStudent owner = findClassStudent(summary.getStudent().getId());
        if (!teacherClass.getId().equals(owner.getSchoolClass().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "담당 학급의 간추리기만 관리할 수 있습니다.");
        }
        return summary;
    }

    private Summary requireOwnedRejectedSummary(Long studentId, Long summaryId) {
        Summary summary = summaryRepository.findById(summaryId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "간추리기를 찾을 수 없습니다."));
        if (!studentId.equals(summary.getStudent().getId()) || !STATUS_REJECTED.equals(normalizeStatus(summary.getStatus()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "거절된 내 간추리기만 수정하거나 삭제할 수 있습니다.");
        }
        return summary;
    }

    private String normalizeStatus(String status) {
        return status == null ? STATUS_PENDING : status.trim().toLowerCase();
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
        if (mySummaries.isEmpty()) {
            mySummaries = summaryRepository.findAllReviewableIndividualSummariesByStudentIds(List.of(studentId));
        }

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
