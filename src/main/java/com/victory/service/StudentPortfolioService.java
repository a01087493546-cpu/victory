package com.victory.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.IndividualPortfolioResponse;
import com.victory.dto.IndividualPortfolioResponse.ActivitySummary;
import com.victory.dto.IndividualPortfolioResponse.Competencies;
import com.victory.dto.PortfolioActivityCount;
import com.victory.dto.PracticeAchievementResponse;
import com.victory.dto.PracticePortfolioResponse;
import com.victory.dto.StudentAchievementItem;
import com.victory.entity.ClassReadingBook;
import com.victory.entity.ClassStudent;
import com.victory.entity.ReadingRecord;
import com.victory.entity.Response;
import com.victory.entity.SchoolClass;
import com.victory.entity.StudentStatRewardLog;
import com.victory.entity.Summary;
import com.victory.entity.User;
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

        LocalDateTime startAt = from.atStartOfDay();
        LocalDateTime endAt = to.plusDays(1).atStartOfDay();
        List<Response> activities = responseRepository
            .findByStudent_IdAndModeAndDeletedAtIsNullAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByIdAsc(
                studentId, MODE_CLASS, startAt, endAt).stream()
            .filter(response -> belongsToCurrentPracticeBook(response, currentBook.getId()))
            .toList();
        long before = stageCount(activities, "before");
        long during = stageCount(activities, "during");
        long after = stageCount(activities, "after");
        long summaries = summaryRepository
            .findByStudent_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByIdAsc(studentId, startAt, endAt)
            .stream().filter(summary -> currentBook.getId().equals(summary.getClassReadingBookId())).count();

        return new PracticePortfolioResponse(studentId, context.student().getName(), context.schoolClass().getGrade(),
            context.schoolClass().getClassNumber(), from, to, item.getParticipationRate(), item.getComprehensionRate(),
            activity(before), activity(during), activity(after), currentBook.getBookTitle(),
            before + during + after + summaries);
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

        return new IndividualPortfolioResponse(studentId, context.student().getName(), context.schoolClass().getGrade(),
            context.schoolClass().getClassNumber(), from, to, completed.size(),
            average(completed.stream().map(ReadingRecord::getFinalReadingPracticeScore).toList()),
            average(completed.stream().map(ReadingRecord::getFinalRecordCompletionScore).toList()),
            new ActivitySummary(questions, thoughts, summaries.size(), sharing),
            individualReadingService.getMonthlyCompletionStats(studentId), competencies, demo);
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

    private long stageCount(List<Response> responses, String stage) {
        return responses.stream().filter(response -> stage.equals(response.getStage())).count();
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
