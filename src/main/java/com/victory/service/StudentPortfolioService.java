package com.victory.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.IndividualAchievementResult;
import com.victory.dto.IndividualPortfolioResponse;
import com.victory.dto.IndividualPortfolioResponse.ActivitySummary;
import com.victory.dto.IndividualPortfolioResponse.Competencies;
import com.victory.dto.IndividualPortfolioResponse.ReadingCompetencies;
import com.victory.dto.MonthlyCompletionStatsResponse;
import com.victory.dto.PortfolioActivityCount;
import com.victory.dto.PracticeAchievementResponse;
import com.victory.dto.PracticePortfolioResponse;
import com.victory.dto.PracticeStageDetail;
import com.victory.dto.ReadingCompetencyScore;
import com.victory.dto.StudentAchievementItem;
import com.victory.entity.ClassReadingBook;
import com.victory.entity.ClassStudent;
import com.victory.entity.ReadingRecord;
import com.victory.entity.Response;
import com.victory.entity.SchoolClass;
import com.victory.entity.StudentStatRewardLog;
import com.victory.entity.Summary;
import com.victory.entity.User;
import com.victory.repository.BookRecommendationRepository;
import com.victory.repository.ClassReadingBookRepository;
import com.victory.repository.ClassStudentRepository;
import com.victory.repository.ReadingRecordRepository;
import com.victory.repository.ResponseRepository;
import com.victory.repository.SchoolClassRepository;
import com.victory.repository.StudentStatRewardLogRepository;
import com.victory.repository.SummaryRepository;
import com.victory.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/** 교사용 학생 성장 포트폴리오의 읽기 전용 집계 서비스. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentPortfolioService {
    private static final String MODE_CLASS = "class";
    private static final String MODE_INDIVIDUAL = "individual";

    private final UserRepository userRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final ClassStudentRepository classStudentRepository;
    private final ClassReadingBookRepository classReadingBookRepository;
    private final ReadingRecordRepository readingRecordRepository;
    private final ResponseRepository responseRepository;
    private final SummaryRepository summaryRepository;
    private final StudentStatRewardLogRepository rewardLogRepository;
    private final PracticeAchievementService practiceAchievementService;
    private final IndividualReadingService individualReadingService;
    private final IndividualAchievementService individualAchievementService;
    private final BookRecommendationRepository bookRecommendationRepository;
    private final ReadingCompetencyCalculator readingCompetencyCalculator;
    private final DemoReadingCompetencyProvider demoReadingCompetencyProvider;
    private final PracticeStageNarrativeBuilder practiceStageNarrativeBuilder;
    private final DemoPracticeStageProvider demoPracticeStageProvider;

    public PracticePortfolioResponse getPracticePortfolio(
            Long teacherId, Long classId, Long studentId, LocalDate from, LocalDate to) {
        Context context = validate(teacherId, classId, studentId, from, to);
        ClassReadingBook currentBook = classReadingBookRepository.findBySchoolClassId(classId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "등록된 온책읽기 책이 없습니다."));
        PracticeAchievementResponse achievement =
            practiceAchievementService.getClassAchievement(teacherId, classId, currentBook.getId());
        StudentAchievementItem item = achievement.getStudents().stream()
            .filter(candidate -> studentId.equals(candidate.getStudentId()))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "성취도 대상 학생을 찾을 수 없습니다."));

        boolean demo = Boolean.TRUE.equals(context.teacher().getDemoAccount())
            || Boolean.TRUE.equals(context.student().getDemoAccount());

        PracticeStageDetail beforeStage;
        PracticeStageDetail duringStage;
        PracticeStageDetail afterStage;
        long before;
        long during;
        long after;
        long afterSummaryCount;

        if (demo) {
            /*
             * 심사계정 실제 seed는 읽기 전(before) Response가 전혀 없이
             * 읽기 중/후만 채워져 있어(DemoClassActivityInitializer),
             * 실제 쿼리를 그대로 쓰면 "읽기 전 미완료인데 읽기 중은
             * 완료"처럼 진행 순서상 앞뒤가 안 맞는 조합이 나올 수 있다.
             * DemoPracticeStageProvider가 학생별로 항상 논리적인 진행
             * 패턴만 결정적으로 돌려주므로 그것으로 대체한다.
             */
            DemoPracticeStageProvider.StageBundle bundle =
                demoPracticeStageProvider.forLoginId(context.student().getLoginId());
            beforeStage = bundle.before();
            duringStage = bundle.during();
            afterStage = bundle.after();
            before = beforeStage.completed() ? 1 : 0;
            during = duringStage.completed() ? 1 : 0;
            after = afterStage.completed() ? 1 : 0;
            afterSummaryCount = afterStage.completed() ? 1 : 0;
        } else {
            LocalDateTime startAt = from.atStartOfDay();
            LocalDateTime endAt = to.plusDays(1).atStartOfDay();
            List<Response> activities = responseRepository
                .findByStudent_IdAndModeAndDeletedAtIsNullAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByIdAsc(
                    studentId, MODE_CLASS, startAt, endAt).stream()
                .filter(response -> belongsToCurrentPracticeBook(response, currentBook.getId()))
                .toList();
            List<Response> beforeActivities = activitiesByStage(activities, "before");
            List<Response> duringActivities = activitiesByStage(activities, "during");
            List<Response> afterActivities = activitiesByStage(activities, "after");
            before = beforeActivities.size();
            during = duringActivities.size();
            after = afterActivities.size();
            List<Summary> afterSummaries = summaryRepository
                .findByStudent_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByIdAsc(studentId, startAt, endAt)
                .stream().filter(summary -> currentBook.getId().equals(summary.getClassReadingBookId())).toList();
            afterSummaryCount = afterSummaries.size();

            beforeStage = practiceStageNarrativeBuilder.buildBeforeStage(beforeActivities);
            duringStage = practiceStageNarrativeBuilder.buildDuringStage(duringActivities);
            afterStage = practiceStageNarrativeBuilder.buildAfterStage(afterActivities, afterSummaries);
        }

        return new PracticePortfolioResponse(studentId, context.student().getName(), context.schoolClass().getGrade(),
            context.schoolClass().getClassNumber(), from, to, item.getParticipationRate(), item.getComprehensionRate(),
            activity(before), activity(during), activity(after), currentBook.getBookTitle(),
            before + during + after + afterSummaryCount,
            beforeStage, duringStage, afterStage);
    }

    public IndividualPortfolioResponse getIndividualPortfolio(
            Long teacherId, Long classId, Long studentId, LocalDate from, LocalDate to) {
        Context context = validate(teacherId, classId, studentId, from, to);
        LocalDateTime startAt = from.atStartOfDay();
        LocalDateTime endAt = to.plusDays(1).atStartOfDay();
        List<ReadingRecord> completed = readingRecordRepository.findByStudent_IdAndFinishedAtIsNotNull(studentId)
            .stream().filter(record -> !record.getFinishedAt().isBefore(startAt) && record.getFinishedAt().isBefore(endAt))
            .toList();
        boolean demo = Boolean.TRUE.equals(context.teacher().getDemoAccount())
            || Boolean.TRUE.equals(context.student().getDemoAccount());
        List<Response> responses = demo
            ? responseRepository.findByStudent_IdAndModeAndDeletedAtIsNullOrderByIdAsc(studentId, MODE_INDIVIDUAL)
                .stream().filter(response -> demoActivityInPeriod(response, startAt, endAt)).toList()
            : responseRepository
                .findByStudent_IdAndModeAndDeletedAtIsNullAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByIdAsc(
                    studentId, MODE_INDIVIDUAL, startAt, endAt);
        List<Summary> summaries = demo
            ? summaryRepository.findByStudent_IdAndReadingRecordIsNotNullOrderByIdAsc(studentId).stream()
                .filter(summary -> finishedInPeriod(summary.getReadingRecord(), startAt, endAt)).toList()
            : summaryRepository
                .findByStudent_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByIdAsc(studentId, startAt, endAt)
                .stream().filter(summary -> summary.getReadingRecord() != null).toList();

        long questions = responses.stream().filter(r -> "answer".equals(r.getContentType())
            && ("before".equals(r.getStage()) || "after".equals(r.getStage()))).count();
        long thoughts = responses.stream().filter(r -> "answer".equals(r.getContentType())
            && "during".equals(r.getStage())).count();
        long sharing = responses.stream().filter(r -> "chat_post".equals(r.getContentType())
            || "chat_reply".equals(r.getContentType()) || "reply".equals(r.getContentType())).count();
        List<StudentStatRewardLog> logs = rewardLogRepository
            .findByStudent_IdAndGrantedAtGreaterThanEqualAndGrantedAtLessThan(studentId, startAt, endAt);
        Competencies competencies = logs.isEmpty() && demo
            ? deriveDemoCompetencies(responses, summaries)
            : sumLoggedCompetencies(logs);

        /*
         * 독서 실천도/기록 완성도 평균.
         * - 심사계정: 기존 seed(완독 기록에 고정 저장된 final 점수)를 그대로 쓴다.
         * - 일반계정: 평가기간 내 완독한 책 + 현재 진행 중인 책(있으면 1권)의
         *   점수를 평균한다. 예전에는 완독 책의 final 점수만 평균해서 진행 중인
         *   학생이나 final 점수가 비어 있는 과거 기록은 활동이 있어도 0점이었다.
         *   책마다 완독 책은 고정된 final 점수를, 그 외에는 실제 활동 기준
         *   실시간 점수를 쓴다(학생별 달성도 화면과 같은 계산 경로).
         */
        List<ReadingRecord> booksInScope = new ArrayList<>(completed);
        if (!demo) {
            readingRecordRepository.findByStudent_IdAndFinishedAtIsNull(studentId).ifPresent(booksInScope::add);
        }
        BigDecimal averageReadingPracticeScore;
        BigDecimal averageRecordCompletionScore;
        if (demo) {
            averageReadingPracticeScore =
                average(completed.stream().map(ReadingRecord::getFinalReadingPracticeScore).toList());
            averageRecordCompletionScore =
                average(completed.stream().map(ReadingRecord::getFinalRecordCompletionScore).toList());
        } else {
            List<BookScores> bookScores = booksInScope.stream().map(this::scoresFor).toList();
            averageReadingPracticeScore = averageScores(bookScores.stream().map(BookScores::practice).toList());
            averageRecordCompletionScore = averageScores(bookScores.stream().map(BookScores::completion).toList());
        }
        boolean hasUnattributedSharing = responses.stream().anyMatch(r -> "chat_reply".equals(r.getContentType())
            || "reply".equals(r.getContentType()));
        ReadingCompetencies readingCompetencies = demo
            ? demoReadingCompetencyProvider.forLoginId(context.student().getLoginId())
            : calculateReadingCompetencies(
                booksInScope, averageReadingPracticeScore, averageRecordCompletionScore, hasUnattributedSharing);

        /*
         * 월별 완독 기록: 심사계정은 seed 그대로. 일반계정은 위 completed(평가기간 내
         * 완독)를 월별로 세어, 완독 권수(completedBookCount)와 항상 합계가 맞게 한다.
         */
        MonthlyCompletionStatsResponse monthlyStats = demo
            ? individualReadingService.getMonthlyCompletionStats(studentId)
            : monthlyCompletionOf(completed, to.getYear());

        return new IndividualPortfolioResponse(studentId, context.student().getName(), context.schoolClass().getGrade(),
            context.schoolClass().getClassNumber(), from, to, completed.size(),
            averageReadingPracticeScore, averageRecordCompletionScore,
            new ActivitySummary(questions, thoughts, summaries.size(), sharing),
            monthlyStats, competencies, readingCompetencies, demo);
    }

    /*
     * "독서 역량 4종"의 입력 데이터를 모아 순수 계산기(ReadingCompetencyCalculator)에
     * 넘긴다. 학생 간 상대평가/반 평균 정규화는 절대 하지 않는다 - 계산기
     * 자체가 학생 개인의 절대 비율만 다룬다.
     *
     * booksInScope = 평가기간 내 완독한 책 + 현재 진행 중인 책(있으면
     * 1권). 완독 책의 책별 데이터는 완독 시점에 저장된 값(beforeDone 등,
     * final_*_score)을 그대로 쓰고, 진행 중인 책도 같은 ambient 플래그를
     * 그대로 써서 "책 단위가 평가 범위"라는 기준을 두 경우 모두 동일하게
     * 적용한다(개별 활동 하나하나의 타임스탬프까지 다시 기간으로 거르지
     * 않음 - averageReadingPracticeScore/averageRecordCompletionScore가
     * 이미 "완독 시점이 기간 내"만으로 기간을 가르는 것과 같은 원칙).
     */
    private ReadingCompetencies calculateReadingCompetencies(
            List<ReadingRecord> booksInScope,
            BigDecimal averageReadingPracticeScore, BigDecimal averageRecordCompletionScore,
            boolean hasUnattributedSharing) {

        List<Integer> completedStageCountPerBook = booksInScope.stream()
            .map(this::stageDoneCount)
            .toList();
        double questionGenerationScore =
            readingCompetencyCalculator.questionGenerationScore(completedStageCountPerBook);

        double readingPersistenceScore = readingCompetencyCalculator.readingPersistenceScore(
            averageReadingPracticeScore == null ? null : averageReadingPracticeScore.doubleValue());
        double thoughtRefinementScore = readingCompetencyCalculator.thoughtRefinementScore(
            averageRecordCompletionScore == null ? null : averageRecordCompletionScore.doubleValue());

        long booksWithAttributedSharing = booksInScope.stream().filter(this::hasBookSharingActivity).count();
        double thoughtSharingScore = readingCompetencyCalculator.thoughtSharingScore(
            (int) booksWithAttributedSharing, booksInScope.size(), hasUnattributedSharing);

        return new ReadingCompetencies(
            ReadingCompetencyScore.of(questionGenerationScore),
            ReadingCompetencyScore.of(readingPersistenceScore),
            ReadingCompetencyScore.of(thoughtRefinementScore),
            ReadingCompetencyScore.of(thoughtSharingScore));
    }

    private record BookScores(double practice, double completion) {
    }

    /*
     * 책 한 권의 독서 실천도/기록 완성도. 완독하면서 고정 저장된 final 점수가
     * 둘 다 있으면 그 값을, 진행 중이거나 final 점수가 비어 있는 과거 기록은
     * IndividualAchievementService(학생별 달성도와 같은 계산)의 값을 쓴다.
     */
    private BookScores scoresFor(ReadingRecord record) {
        boolean frozen = record.getFinishedAt() != null
            && record.getFinalReadingPracticeScore() != null
            && record.getFinalRecordCompletionScore() != null;
        if (frozen) {
            return new BookScores(record.getFinalReadingPracticeScore(), record.getFinalRecordCompletionScore());
        }
        if (record.getId() == null) {
            return new BookScores(
                record.getFinalReadingPracticeScore() == null ? 0 : record.getFinalReadingPracticeScore(),
                record.getFinalRecordCompletionScore() == null ? 0 : record.getFinalRecordCompletionScore());
        }
        IndividualAchievementResult live = individualAchievementService.calculate(record.getId());
        return new BookScores(live.getReadingPracticeScore(), live.getRecordCompletionScore());
    }

    private BigDecimal averageScores(List<Double> values) {
        if (values.isEmpty()) return BigDecimal.ZERO.setScale(2);
        return BigDecimal.valueOf(values.stream().mapToDouble(Double::doubleValue).average().orElse(0))
            .setScale(2, RoundingMode.HALF_UP);
    }

    private MonthlyCompletionStatsResponse monthlyCompletionOf(List<ReadingRecord> completed, int year) {
        int[] counts = new int[12];
        for (ReadingRecord record : completed) {
            if (record.getFinishedAt() != null) counts[record.getFinishedAt().getMonthValue() - 1]++;
        }
        List<Integer> list = new ArrayList<>();
        for (int count : counts) list.add(count);
        return new MonthlyCompletionStatsResponse(year, list);
    }

    private int stageDoneCount(ReadingRecord record) {
        return (Boolean.TRUE.equals(record.getBeforeDone()) ? 1 : 0)
            + (Boolean.TRUE.equals(record.getDuringDone()) ? 1 : 0)
            + (Boolean.TRUE.equals(record.getAfterDone()) ? 1 : 0);
    }

    /*
     * 책수다방 글 또는 친구 추천처럼 특정 책(readingRecordId)에 귀속되는
     * 나눔 활동이 있었는지. 두 조회 모두 이미 다른 기능(IndividualAchievementService)이
     * 쓰는 것과 같은 레포지토리 메서드를 그대로 재사용한다.
     */
    private boolean hasBookSharingActivity(ReadingRecord record) {
        if (record.getId() == null) {
            return false;
        }

        boolean hasChatPost = responseRepository
            .findByReadingRecord_IdAndModeAndDeletedAtIsNullOrderByIdAsc(record.getId(), MODE_INDIVIDUAL)
            .stream()
            .anyMatch(r -> "chat_post".equals(r.getContentType()));

        if (hasChatPost) {
            return true;
        }

        return !bookRecommendationRepository.findByReadingRecord_Id(record.getId()).isEmpty();
    }

    private Context validate(Long teacherId, Long classId, Long studentId, LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "올바른 평가 기간(from/to)을 입력해 주세요.");
        }
        User teacher = userRepository.findById(teacherId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "교사를 찾을 수 없습니다."));
        if (!"teacher".equalsIgnoreCase(teacher.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "교사 계정만 조회할 수 있습니다.");
        }
        SchoolClass schoolClass = schoolClassRepository.findByTeacherId(teacherId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "담당 학급을 찾을 수 없습니다."));
        if (!schoolClass.getId().equals(classId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "담당 학급만 조회할 수 있습니다.");
        }
        ClassStudent membership = classStudentRepository.findByStudentId(studentId)
            .filter(row -> classId.equals(row.getSchoolClass().getId()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "담당 학급 학생만 조회할 수 있습니다."));
        return new Context(teacher, schoolClass, membership.getStudent());
    }

    private boolean belongsToCurrentPracticeBook(Response response, Long currentBookId) {
        if (response.getExtraData() == null || !response.getExtraData().containsKey("classReadingBookId")) return true;
        Object value = response.getExtraData().get("classReadingBookId");
        return currentBookId.toString().equals(String.valueOf(value));
    }

    private List<Response> activitiesByStage(List<Response> responses, String stage) {
        return responses.stream().filter(response -> stage.equals(response.getStage())).toList();
    }

    private PortfolioActivityCount activity(long count) {
        return new PortfolioActivityCount(count, count > 0);
    }

    private BigDecimal average(List<Integer> values) {
        List<Integer> present = values.stream().filter(value -> value != null).toList();
        if (present.isEmpty()) return BigDecimal.ZERO.setScale(2);
        return BigDecimal.valueOf(present.stream().mapToInt(Integer::intValue).average().orElse(0))
            .setScale(2, RoundingMode.HALF_UP);
    }

    private Competencies sumLoggedCompetencies(List<StudentStatRewardLog> logs) {
        long magic = 0, stamina = 0, wisdom = 0, courage = 0;
        for (StudentStatRewardLog log : logs) {
            int amount = log.getAmount() == null ? 0 : log.getAmount();
            String type = log.getStatType() == null ? "" : log.getStatType();
            if (IndividualAfterReadingRewardService.REWARD_TYPE.equals(log.getRewardType())) {
                // 과거 amount=1 로그도 실제 지급 규칙(+3/+1/+1)으로 해석한다.
                stamina += 3; magic += 1; wisdom += 1;
            } else {
                if ("all".equals(type) || type.contains("magic")) magic += amount;
                if ("all".equals(type) || type.contains("stamina")) stamina += amount;
                if ("all".equals(type) || type.contains("wisdom")) wisdom += amount;
                if ("all".equals(type) || type.contains("courage")) courage += amount;
            }
        }
        return new Competencies(magic, stamina, wisdom, courage);
    }

    private Competencies deriveDemoCompetencies(List<Response> responses, List<Summary> summaries) {
        Set<String> before = completedUnits(responses, "before");
        Set<String> during = completedUnits(responses, "during");
        Set<String> after = completedUnits(responses, "after");
        long stageRewards = before.size() + during.size() + after.size();
        long sharing = responses.stream().filter(r -> "chat_post".equals(r.getContentType())
            || "chat_reply".equals(r.getContentType()) || "reply".equals(r.getContentType())).count();
        // demo seed의 완료 활동에 실제 보상 규칙(전/중 +1/+1, 후 +3/+1/+1)을 적용한다.
        return new Competencies(stageRewards, after.size() * 3L, stageRewards, sharing);
    }

    private Set<String> completedUnits(List<Response> responses, String stage) {
        Set<String> units = new HashSet<>();
        responses.stream().filter(r -> "answer".equals(r.getContentType()) && stage.equals(r.getStage()))
            .forEach(r -> units.add((r.getReadingRecord() == null ? "none" : r.getReadingRecord().getId())
                + ":" + (r.getActivityDate() == null ? "once" : r.getActivityDate())));
        return units;
    }

    private boolean demoActivityInPeriod(Response response, LocalDateTime startAt, LocalDateTime endAt) {
        ReadingRecord record = response.getReadingRecord();
        if (record == null && response.getParent() != null) record = response.getParent().getReadingRecord();
        if (record != null && record.getFinishedAt() != null) return finishedInPeriod(record, startAt, endAt);
        return response.getCreatedAt() != null && !response.getCreatedAt().isBefore(startAt)
            && response.getCreatedAt().isBefore(endAt);
    }

    private boolean finishedInPeriod(ReadingRecord record, LocalDateTime startAt, LocalDateTime endAt) {
        return record != null && record.getFinishedAt() != null && !record.getFinishedAt().isBefore(startAt)
            && record.getFinishedAt().isBefore(endAt);
    }

    private record Context(User teacher, SchoolClass schoolClass, User student) {
    }
}
