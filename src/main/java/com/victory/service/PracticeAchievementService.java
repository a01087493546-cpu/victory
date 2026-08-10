package com.victory.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.PracticeAchievementHistoryItem;
import com.victory.dto.PracticeAchievementHistoryResponse;
import com.victory.dto.PracticeAchievementResponse;
import com.victory.dto.StudentAchievementItem;
import com.victory.dto.SupportReasonItem;
import com.victory.entity.AiEvaluationAttempt;
import com.victory.entity.ClassReadingBook;
import com.victory.entity.ClassStudent;
import com.victory.entity.PracticeAchievementSnapshot;
import com.victory.entity.PracticeProgress;
import com.victory.entity.Response;
import com.victory.entity.SchoolClass;
import com.victory.entity.User;
import com.victory.repository.AiEvaluationAttemptRepository;
import com.victory.repository.ClassReadingBookRepository;
import com.victory.repository.ClassStudentRepository;
import com.victory.repository.PracticeAchievementSnapshotRepository;
import com.victory.repository.PracticeProgressRepository;
import com.victory.repository.ResponseRepository;
import com.victory.repository.SchoolClassRepository;
import com.victory.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/*
 * 교사용 연습읽기 성취도/그래프 계산. 기존 responses/practice_progress/
 * class_reading_books/class_students/classes 테이블을 그대로 읽기만 하고,
 * 새로 쓰는 테이블은 ai_evaluation_attempts(이해도 시도 기록)와
 * practice_achievement_snapshots(일별 누적 스냅샷) 두 개뿐이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PracticeAchievementService {

    private static final String MODE_CLASS = "class";
    private static final String CONTENT_TYPE_ANSWER = "answer";
    private static final String STAGE_BEFORE = "before";
    private static final String STAGE_AFTER = "after";
    private static final String ACTIVITY_TYPE_AFTER_QUESTION = "after_reading_question";
    private static final String ACTIVITY_TYPE_BOOK_THOUGHT = "book_thought";

    private static final double SUPPORT_PARTICIPATION_THRESHOLD = 30.0;
    private static final double SUPPORT_COMPREHENSION_THRESHOLD = 50.0;

    /*
     * 요구사항이 명시적으로 "서버의 한국 시간대 Asia/Seoul 날짜 기준"이라고
     * 못박았으므로, 배포 환경의 JVM 기본 시간대에 의존하지 않고 이 클래스가
     * 쓰는 "오늘"/스냅샷 날짜는 항상 이 ZoneId로 명시적으로 계산한다. 기존
     * 엔티티들의 LocalDateTime.now()는 JVM 기본 시간대를 그대로 쓰는데,
     * 이 서버는 JDBC URL에도 serverTimezone=Asia/Seoul을 쓰고 있어 같은
     * 벽시계 값이라는 전제하에 두 값을 그대로 비교한다.
     */
    private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");

    private final SchoolClassRepository schoolClassRepository;
    private final ClassStudentRepository classStudentRepository;
    private final ClassReadingBookRepository classReadingBookRepository;
    private final PracticeProgressRepository practiceProgressRepository;
    private final ResponseRepository responseRepository;
    private final UserRepository userRepository;
    private final AiEvaluationAttemptRepository aiEvaluationAttemptRepository;
    private final PracticeAchievementSnapshotRepository snapshotRepository;
    private final PracticeAchievementCalculator calculator;

    public PracticeAchievementResponse getClassAchievement(
            Long teacherId,
            Long classId,
            Long classReadingBookIdParam) {

        SchoolClass schoolClass = findTeacherClass(teacherId);
        validateRequestedClass(schoolClass, classId);

        ClassReadingBook classReadingBook = findClassReadingBook(classId);
        validateRequestedClassReadingBook(classReadingBook, classReadingBookIdParam);

        List<ClassStudent> roster = sortedRoster(classId);
        LocalDate today = LocalDate.now(ZONE_SEOUL);

        if (isDemoTeacher(teacherId)) {
            return buildDemoClassAchievement(classId, classReadingBook.getId(), roster);
        }

        List<StudentAchievementItem> items = new ArrayList<>();
        int todayParticipatingCount = 0;
        int supportNeededCount = 0;

        for (ClassStudent classStudent : roster) {
            StudentComputation computation =
                computeForStudent(classStudent, classReadingBook, today);

            items.add(computation.item());

            if (computation.participatedToday()) {
                todayParticipatingCount++;
            }

            if (computation.needsSupport()) {
                supportNeededCount++;
            }
        }

        int totalStudentCount = roster.size();
        double todayParticipationRate = totalStudentCount == 0
            ? 0.0
            : (todayParticipatingCount / (double) totalStudentCount) * 100.0;

        return new PracticeAchievementResponse(
            classId,
            classReadingBook.getId(),
            totalStudentCount,
            todayParticipatingCount,
            calculator.round2(todayParticipationRate),
            supportNeededCount,
            items
        );
    }

    public PracticeAchievementHistoryResponse getAchievementHistory(
            Long teacherId,
            Long classId,
            Long classReadingBookIdParam,
            Long studentId,
            LocalDate from,
            LocalDate to) {

        SchoolClass schoolClass = findTeacherClass(teacherId);
        validateRequestedClass(schoolClass, classId);

        ClassReadingBook classReadingBook = findClassReadingBook(classId);
        validateRequestedClassReadingBook(classReadingBook, classReadingBookIdParam);

        ClassStudent classStudent = classStudentRepository.findByStudentId(studentId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "학생을 찾을 수 없습니다. studentId=" + studentId
            ));

        if (!classStudent.getSchoolClass().getId().equals(classId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "담당 학급 학생만 조회할 수 있습니다."
            );
        }

        if (isDemoTeacher(teacherId)) {
            return buildDemoHistory(classStudent, from, to);
        }

        List<PracticeAchievementSnapshot> snapshots = snapshotRepository
            .findByStudentIdAndClassReadingBookIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(
                studentId, classReadingBook.getId(), from, to);

        List<PracticeAchievementHistoryItem> history = snapshots.stream()
            .map(snapshot -> new PracticeAchievementHistoryItem(
                snapshot.getSnapshotDate(),
                snapshot.getParticipationRate(),
                snapshot.getComprehensionRate(),
                snapshot.getAchievementRate(),
                snapshot.getFinalReadingProgress()
            ))
            .toList();

        return new PracticeAchievementHistoryResponse(
            studentId,
            classStudent.getStudent().getName(),
            history
        );
    }

    /*
     * 스케줄러가 호출하는 스냅샷 저장. 이미 그 날짜 스냅샷이 있으면(과거든
     * 오늘 재실행이든) 아무것도 하지 않는다 - "과거 스냅샷은 새 계산으로
     * 덮어쓰지 않는다"와 "중복 실행에도 중복 레코드가 생기지 않는다"를
     * 함께 만족시킨다. DB UNIQUE 제약을 최후 방어선으로 함께 둔다.
     */
    @Transactional
    public void saveSnapshotIfAbsent(
            Long studentId,
            Long classReadingBookId,
            ClassReadingBook classReadingBook,
            LocalDate snapshotDate) {

        boolean alreadyExists = snapshotRepository
            .findByStudentIdAndClassReadingBookIdAndSnapshotDate(
                studentId, classReadingBookId, snapshotDate)
            .isPresent();

        if (alreadyExists) {
            return;
        }

        RawMetrics metrics = computeRawMetrics(studentId, classReadingBook);

        PracticeAchievementSnapshot snapshot = new PracticeAchievementSnapshot();
        snapshot.setStudentId(studentId);
        snapshot.setClassReadingBookId(classReadingBookId);
        snapshot.setSnapshotDate(snapshotDate);
        snapshot.setReadingActivityCompletionRate(calculator.round2(metrics.finalReadingProgress));
        snapshot.setQuestionParticipationRate(calculator.round2(metrics.questionParticipationRate));
        snapshot.setThoughtSharingParticipationRate(calculator.round2(metrics.thoughtSharingParticipationRate));
        snapshot.setParticipationRate(calculator.round2(metrics.participationRate));
        snapshot.setComprehensionRate(calculator.round2(metrics.comprehensionRate));
        snapshot.setAchievementRate(calculator.round2(metrics.achievementRate));
        snapshot.setFinalReadingProgress(calculator.round2(metrics.finalReadingProgress));
        snapshot.setHasAiEvaluation(metrics.hasAiEvaluation);

        try {
            snapshotRepository.save(snapshot);
        } catch (DataIntegrityViolationException e) {
            /*
             * 스케줄러가 여러 인스턴스에서 동시에 실행되는 등 경쟁 상황에서
             * UNIQUE 제약에 걸린 것 - 이미 다른 트랜잭션이 저장했다는
             * 뜻이므로 무시하고 넘어간다(idempotent 저장).
             */
        }
    }

    public Optional<LocalDate> findLastSnapshotDate(Long studentId, Long classReadingBookId) {
        return snapshotRepository
            .findTopByStudentIdAndClassReadingBookIdOrderBySnapshotDateDesc(studentId, classReadingBookId)
            .map(PracticeAchievementSnapshot::getSnapshotDate);
    }

    public List<ClassStudent> findAllActiveClassStudents() {
        return classStudentRepository.findAll();
    }

    public Optional<ClassReadingBook> findClassReadingBookForClass(Long classId) {
        return classReadingBookRepository.findBySchoolClassId(classId);
    }

    private StudentComputation computeForStudent(
            ClassStudent classStudent,
            ClassReadingBook classReadingBook,
            LocalDate today) {

        Long studentId = classStudent.getStudent().getId();
        RawMetrics metrics = computeRawMetrics(studentId, classReadingBook);

        List<SupportReasonItem> reasons = new ArrayList<>();

        if (metrics.participationRate < SUPPORT_PARTICIPATION_THRESHOLD) {
            reasons.add(SupportReasonItem.LOW_READING_PARTICIPATION);
        }

        if (metrics.hasUnmetOpenQuestionStage) {
            reasons.add(SupportReasonItem.NO_QUESTION_SUBMISSION);
        }

        if (metrics.bookChatStageOpen && metrics.bookChatPostCount == 0) {
            reasons.add(SupportReasonItem.NO_THOUGHT_SHARING);
        }

        if (metrics.hasAiEvaluation && metrics.comprehensionRate < SUPPORT_COMPREHENSION_THRESHOLD) {
            reasons.add(SupportReasonItem.LOW_COMPREHENSION);
        }

        boolean needsSupport = !reasons.isEmpty();

        StudentAchievementItem item = new StudentAchievementItem(
            studentId,
            classStudent.getStudentNumber(),
            classStudent.getStudent().getName(),
            calculator.round2(metrics.pageProgress),
            calculator.round2(metrics.reviewScore),
            calculator.round2(metrics.finalReadingProgress),
            calculator.round2(metrics.finalReadingProgress),
            calculator.round2(metrics.questionParticipationRate),
            calculator.round2(metrics.thoughtSharingParticipationRate),
            calculator.round2(metrics.participationRate),
            metrics.hasAiEvaluation,
            metrics.aiEvaluatedQuestionCount,
            metrics.passedWithinThreeAttemptsCount,
            calculator.round2(metrics.comprehensionRate),
            calculator.round2(metrics.achievementRate),
            needsSupport,
            reasons
        );

        boolean participatedToday = metrics.participatedToday(today);

        return new StudentComputation(item, participatedToday, needsSupport);
    }

    /*
     * IndividualReadingDashboardService의 같은 이름 상수와 동일한 이유다:
     * 심사 학급에 남아 있는 ss02~ss08(demo_student_02~08와 이름이 겹치는
     * 예전 계정) 때문에 로스터가 8명보다 커져서, 이름이 중복 표시되고
     * rates 배열이 Math.min(i, ...)로 클램프되어 뒤쪽 학생들이 전부 같은
     * 달성도(70% 근처)로 보이던 문제를 여기서도 심사 seed 8명으로
     * 한정해서 막는다. DB 행은 건드리지 않는다.
     */
    private static final Set<String> DEMO_SEED_STUDENT_LOGIN_IDS = Set.of(
        "ss01", "demo_student_02", "demo_student_03", "demo_student_04",
        "demo_student_05", "demo_student_06", "demo_student_07", "demo_student_08");

    private PracticeAchievementResponse buildDemoClassAchievement(
            Long classId, Long classReadingBookId, List<ClassStudent> roster) {
        List<ClassStudent> seedRoster = roster.stream()
            .filter(member -> DEMO_SEED_STUDENT_LOGIN_IDS.contains(member.getStudent().getLoginId()))
            .toList();

        /*
         * 개별읽기 대시보드와 값이 완전히 겹치지 않도록 학생별로 다른
         * 달성도를 쓴다(순서: 김초롱/송민정/박하민/이진우/김민지/서희원/김수진/이혜원).
         * value = {pageProgress, questionParticipationRate, thoughtSharingParticipationRate,
         * participationRate, comprehensionRate}. achievement = (participationRate + comprehensionRate) / 2.
         */
        double[][] rates = {
            {95, 95, 90, 92, 90}, {90, 85, 80, 88, 82},
            {82, 78, 72, 80, 78}, {75, 70, 65, 74, 68},
            {78, 72, 68, 76, 70}, {58, 50, 45, 55, 49},
            {50, 42, 35, 48, 40}, {44, 38, 30, 42, 36}
        };
        List<StudentAchievementItem> students = new ArrayList<>();
        int supportCount = 0;
        for (int i = 0; i < seedRoster.size(); i++) {
            ClassStudent member = seedRoster.get(i);
            double[] value = rates[Math.min(i, rates.length - 1)];
            /*
             * 사유가 학생마다 다르도록 구성한다(이해도/읽기 참여+질문 미제출/
             * 생각 나누기 부족을 서로 다른 학생에게 분산).
             */
            List<SupportReasonItem> reasons = i == 5 ? List.of(SupportReasonItem.NO_THOUGHT_SHARING)
                : i == 6 ? List.of(SupportReasonItem.LOW_READING_PARTICIPATION, SupportReasonItem.NO_QUESTION_SUBMISSION)
                : i == 7 ? List.of(SupportReasonItem.LOW_COMPREHENSION) : List.of();
            if (!reasons.isEmpty()) supportCount++;
            double achievement = calculator.round2(value[3] * 0.5 + value[4] * 0.5);
            students.add(new StudentAchievementItem(
                member.getStudent().getId(), member.getStudentNumber(), member.getStudent().getName(),
                value[0], value[0], value[0], value[0], value[1], value[2], value[3],
                true, 5, (int) Math.round(value[4] / 20), value[4], achievement,
                !reasons.isEmpty(), reasons));
        }
        int todayCount = Math.max(0, seedRoster.size() - 2);
        return new PracticeAchievementResponse(classId, classReadingBookId, seedRoster.size(), todayCount,
            seedRoster.isEmpty() ? 0.0 : calculator.round2(todayCount * 100.0 / seedRoster.size()), supportCount, students);
    }

    private PracticeAchievementHistoryResponse buildDemoHistory(
            ClassStudent classStudent, LocalDate from, LocalDate to) {
        LocalDate end = to == null ? LocalDate.now(ZONE_SEOUL) : to;
        LocalDate start = from == null ? end.minusDays(6) : from;
        if (start.isBefore(end.minusDays(13))) start = end.minusDays(13);
        int studentIndex = classStudent.getStudentNumber() == null ? 0 : classStudent.getStudentNumber() - 1;
        double base = Math.max(24, 88 - studentIndex * 7);
        List<PracticeAchievementHistoryItem> history = new ArrayList<>();
        int day = 0;
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            double participation = Math.min(100, base - 14 + day * 2.6);
            double comprehension = Math.min(100, base - 8 + day * 1.8 + (day % 2 == 0 ? 2 : -1));
            history.add(new PracticeAchievementHistoryItem(date,
                calculator.round2(participation), calculator.round2(comprehension),
                calculator.round2((participation + comprehension) * 0.5),
                calculator.round2(Math.min(100, participation + 3))));
            day++;
        }
        return new PracticeAchievementHistoryResponse(
            classStudent.getStudent().getId(), classStudent.getStudent().getName(), history);
    }

    /*
     * 한 학생의 원시(반올림 전) 지표를 전부 계산한다. 실시간 조회 API와
     * 스냅샷 저장(스케줄러) 양쪽에서 같은 로직을 재사용해서 화면 값과
     * 그래프 값이 어긋나지 않게 한다.
     */
    private RawMetrics computeRawMetrics(Long studentId, ClassReadingBook classReadingBook) {

        PracticeProgress progress = practiceProgressRepository
            .findByStudent_Id(studentId)
            .orElse(null);

        boolean beforeDone = progress != null && Boolean.TRUE.equals(progress.getBeforeDone());
        boolean classReadDone = progress != null && Boolean.TRUE.equals(progress.getClassReadDone());

        double pageProgress = calculator.pageProgress(
            classReadingBook.getCurrentPage(), classReadingBook.getTotalPages());
        double reviewScore = calculator.reviewScore(
            progress == null ? null : progress.getDuringTypeProgress());
        double finalReadingProgress = calculator.finalReadingProgress(reviewScore, pageProgress);

        List<Response> preReadingResponses = fetchResponses(studentId, STAGE_BEFORE, null);
        List<Response> bookThoughtResponses = fetchResponses(studentId, "during", ACTIVITY_TYPE_BOOK_THOUGHT);
        List<Response> afterQuestionResponses = fetchResponses(studentId, STAGE_AFTER, ACTIVITY_TYPE_AFTER_QUESTION);

        boolean wroteBeforeQuestion = !preReadingResponses.isEmpty();
        boolean wroteBookThoughtQuestion = !bookThoughtResponses.isEmpty();
        boolean wroteAfterQuestion = !afterQuestionResponses.isEmpty();

        double questionParticipationRate = calculator.questionParticipationRate(
            wroteBeforeQuestion, wroteBookThoughtQuestion, wroteAfterQuestion);

        int bookChatPostCount = bookThoughtResponses.size();
        double thoughtSharingParticipationRate =
            calculator.thoughtSharingParticipationRate(bookChatPostCount);

        double participationRate = calculator.participationRate(
            finalReadingProgress, questionParticipationRate, thoughtSharingParticipationRate);

        /*
         * 3-1. 학급 진도(이 학생 본인의 practice_progress 기준)에 따른 활성화
         * 판단 - 읽기 전은 항상 열려 있고, 읽기 중(책 속 생각쓰기/책수다방)은
         * beforeDone 이후, 읽기 후(간추리기 질문)는 classReadDone 이후에만
         * 열린다. 아직 열리지 않은 단계는 미제출이어도 지원 사유에 넣지 않는다.
         */
        boolean hasUnmetOpenQuestionStage =
            !wroteBeforeQuestion
                || (beforeDone && !wroteBookThoughtQuestion)
                || (classReadDone && !wroteAfterQuestion);

        boolean bookChatStageOpen = beforeDone;

        /*
         * 반드시 classReadingBookId로도 함께 걸러야 한다 - studentId만으로
         * 조회하면 이 학생이 나중에 다른 학급(다른 온책읽기 책)에 들어가도
         * 예전 책의 AI 평가 기록까지 이해도 계산에 섞여 버린다.
         */
        List<AiEvaluationAttempt> attempts = aiEvaluationAttemptRepository
            .findByStudentIdInAndClassReadingBookIdOrderByEvaluatedAtAsc(
                List.of(studentId), classReadingBook.getId());

        /*
         * 질문 단위 = evaluationKey. evaluationKey가 없는 레코드(옛 방식
         * 호출이거나 인증 학생 API를 거치지 않은 시도)는 어떤 질문의
         * 재시도인지 정확히 알 수 없으므로 임의로 묶지 않고 이해도 계산
         * 대상에서 완전히 제외한다.
         */
        Map<String, List<AiEvaluationAttempt>> attemptsByQuestionKey = new HashMap<>();

        for (AiEvaluationAttempt attempt : attempts) {
            String key = attempt.getEvaluationKey();

            if (key == null || key.isBlank()) {
                continue;
            }

            attemptsByQuestionKey
                .computeIfAbsent(key, k -> new ArrayList<>())
                .add(attempt);
        }

        int aiEvaluatedQuestionCount = attemptsByQuestionKey.size();
        int passedWithinThreeAttemptsCount = 0;

        for (List<AiEvaluationAttempt> questionAttempts : attemptsByQuestionKey.values()) {
            List<String> orderedStatuses = questionAttempts.stream()
                .sorted(Comparator.comparing(
                    AiEvaluationAttempt::getAttemptNumber,
                    Comparator.nullsLast(Comparator.naturalOrder())))
                .map(AiEvaluationAttempt::getStatus)
                .toList();

            if (calculator.passedWithinAttemptLimit(orderedStatuses)) {
                passedWithinThreeAttemptsCount++;
            }
        }

        boolean hasAiEvaluation = aiEvaluatedQuestionCount > 0;
        double comprehensionRate = calculator.comprehensionRate(
            passedWithinThreeAttemptsCount, aiEvaluatedQuestionCount);

        double achievementRate = calculator.achievementRate(participationRate, comprehensionRate);

        LocalDate lastTouchedBefore = latestLocalDate(preReadingResponses);
        LocalDate lastTouchedThought = latestCreatedDate(bookThoughtResponses);
        LocalDate lastTouchedAfter = latestLocalDate(afterQuestionResponses);

        return new RawMetrics(
            pageProgress,
            reviewScore,
            finalReadingProgress,
            questionParticipationRate,
            thoughtSharingParticipationRate,
            bookChatPostCount,
            participationRate,
            hasAiEvaluation,
            aiEvaluatedQuestionCount,
            passedWithinThreeAttemptsCount,
            comprehensionRate,
            achievementRate,
            hasUnmetOpenQuestionStage,
            bookChatStageOpen,
            lastTouchedBefore,
            lastTouchedThought,
            lastTouchedAfter
        );
    }

    private List<Response> fetchResponses(Long studentId, String stage, String activityTypeFilter) {

        List<Response> responses = responseRepository
            .findByStudent_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                studentId, MODE_CLASS, CONTENT_TYPE_ANSWER, stage);

        if (activityTypeFilter == null) {
            return responses;
        }

        return responses.stream()
            .filter(r -> activityTypeFilter.equals(extractExtraField(r, "activityType")))
            .toList();
    }

    /*
     * "오늘 새로 작성"은 처음 저장(createdAt)뿐 아니라 오늘 다시 고쳐 쓴
     * 경우(updatedAt)도 포함한다 - 읽기 전 질문처럼 유형별 upsert되는
     * 화면은 편집 후 재저장 시 updatedAt만 바뀌고 createdAt은 그대로라서,
     * updatedAt까지 봐야 "오늘 활동했음"을 놓치지 않는다.
     */
    private LocalDate latestLocalDate(List<Response> responses) {
        return responses.stream()
            .map(r -> r.getUpdatedAt() != null ? r.getUpdatedAt() : r.getCreatedAt())
            .filter(Objects::nonNull)
            .map(LocalDateTime::toLocalDate)
            .max(Comparator.naturalOrder())
            .orElse(null);
    }

    /*
     * 책 속 생각 쓰기(책수다방 글)는 매번 새로 INSERT되는 화면이라
     * createdAt만으로 충분하다(같은 글을 중복 제출하면 기존 행을 그대로
     * 반환하고 새로 만들지 않으므로 updatedAt으로 갱신일을 볼 필요가 없다).
     */
    private LocalDate latestCreatedDate(List<Response> responses) {
        return responses.stream()
            .map(Response::getCreatedAt)
            .filter(Objects::nonNull)
            .map(LocalDateTime::toLocalDate)
            .max(Comparator.naturalOrder())
            .orElse(null);
    }

    private String extractExtraField(Response response, String key) {
        if (response.getExtraData() == null) {
            return null;
        }

        Object value = response.getExtraData().get(key);
        return value == null ? null : value.toString();
    }

    private List<ClassStudent> sortedRoster(Long classId) {
        List<ClassStudent> roster = new ArrayList<>(
            classStudentRepository.findBySchoolClassId(classId));

        roster.sort(
            Comparator
                .comparing(
                    (ClassStudent cs) -> cs.getStudentNumber() == null
                        ? Integer.MAX_VALUE
                        : cs.getStudentNumber())
                .thenComparing(cs -> cs.getStudent().getName() == null ? "" : cs.getStudent().getName())
                .thenComparing(cs -> cs.getStudent().getId())
        );

        return roster;
    }

    private SchoolClass findTeacherClass(Long teacherId) {

        User teacher = userRepository.findById(teacherId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "교사를 찾을 수 없습니다. teacherId=" + teacherId
            ));

        if (!"teacher".equalsIgnoreCase(teacher.getRole())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "교사 계정만 처리할 수 있습니다."
            );
        }

        return schoolClassRepository.findByTeacherId(teacherId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "교사의 담당 학급을 찾을 수 없습니다. teacherId=" + teacherId
            ));
    }

    private boolean isDemoTeacher(Long teacherId) {
        return userRepository.findById(teacherId)
            .map(user -> Boolean.TRUE.equals(user.getDemoAccount()))
            .orElse(false);
    }

    private void validateRequestedClass(SchoolClass teacherClass, Long classId) {

        if (!teacherClass.getId().equals(classId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "담당 학급만 조회할 수 있습니다."
            );
        }
    }

    private ClassReadingBook findClassReadingBook(Long classId) {

        return classReadingBookRepository.findBySchoolClassId(classId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "등록된 온책읽기 책 정보가 없습니다. classId=" + classId
            ));
    }

    private void validateRequestedClassReadingBook(
            ClassReadingBook classReadingBook,
            Long classReadingBookIdParam) {

        if (classReadingBookIdParam != null
                && !classReadingBook.getId().equals(classReadingBookIdParam)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "요청한 classReadingBookId가 이 학급의 현재 온책읽기 책과 다릅니다."
            );
        }
    }

    private record StudentComputation(
        StudentAchievementItem item,
        boolean participatedToday,
        boolean needsSupport
    ) {
    }

    private static final class RawMetrics {
        private final double pageProgress;
        private final double reviewScore;
        private final double finalReadingProgress;
        private final double questionParticipationRate;
        private final double thoughtSharingParticipationRate;
        private final int bookChatPostCount;
        private final double participationRate;
        private final boolean hasAiEvaluation;
        private final int aiEvaluatedQuestionCount;
        private final int passedWithinThreeAttemptsCount;
        private final double comprehensionRate;
        private final double achievementRate;
        private final boolean hasUnmetOpenQuestionStage;
        private final boolean bookChatStageOpen;
        private final LocalDate lastTouchedBefore;
        private final LocalDate lastTouchedThought;
        private final LocalDate lastTouchedAfter;

        private RawMetrics(
                double pageProgress,
                double reviewScore,
                double finalReadingProgress,
                double questionParticipationRate,
                double thoughtSharingParticipationRate,
                int bookChatPostCount,
                double participationRate,
                boolean hasAiEvaluation,
                int aiEvaluatedQuestionCount,
                int passedWithinThreeAttemptsCount,
                double comprehensionRate,
                double achievementRate,
                boolean hasUnmetOpenQuestionStage,
                boolean bookChatStageOpen,
                LocalDate lastTouchedBefore,
                LocalDate lastTouchedThought,
                LocalDate lastTouchedAfter) {
            this.pageProgress = pageProgress;
            this.reviewScore = reviewScore;
            this.finalReadingProgress = finalReadingProgress;
            this.questionParticipationRate = questionParticipationRate;
            this.thoughtSharingParticipationRate = thoughtSharingParticipationRate;
            this.bookChatPostCount = bookChatPostCount;
            this.participationRate = participationRate;
            this.hasAiEvaluation = hasAiEvaluation;
            this.aiEvaluatedQuestionCount = aiEvaluatedQuestionCount;
            this.passedWithinThreeAttemptsCount = passedWithinThreeAttemptsCount;
            this.comprehensionRate = comprehensionRate;
            this.achievementRate = achievementRate;
            this.hasUnmetOpenQuestionStage = hasUnmetOpenQuestionStage;
            this.bookChatStageOpen = bookChatStageOpen;
            this.lastTouchedBefore = lastTouchedBefore;
            this.lastTouchedThought = lastTouchedThought;
            this.lastTouchedAfter = lastTouchedAfter;
        }

        private boolean participatedToday(LocalDate today) {
            return today.equals(lastTouchedBefore)
                || today.equals(lastTouchedThought)
                || today.equals(lastTouchedAfter);
        }
    }
}
