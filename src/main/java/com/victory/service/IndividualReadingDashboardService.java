package com.victory.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.IndividualAchievementResult;
import com.victory.dto.TeacherIndividualReadingDashboardResponse;
import com.victory.dto.TeacherIndividualReadingStudentResponse;
import com.victory.entity.ClassStudent;
import com.victory.entity.ReadingRecord;
import com.victory.entity.SchoolClass;
import com.victory.entity.User;
import com.victory.repository.ClassStudentRepository;
import com.victory.repository.ReadingRecordRepository;
import com.victory.repository.SchoolClassRepository;
import com.victory.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/*
 * 교사용 개별읽기 대시보드 조회. 학급 학생 목록은 ClassStudent(학급 소속)
 * 기준으로 먼저 뽑고, 그 학생들에게 ReadingRecord가 있는지는 나중에
 * 확인한다 - 그래야 개별읽기를 아직 시작하지 않은 학생도 목록에서
 * 빠지지 않는다.
 *
 * 점수 계산 자체는 IndividualAchievementService.calculate()를 그대로
 * 재사용하고 여기서 다시 구현하지 않는다.
 */
@Service
@RequiredArgsConstructor
public class IndividualReadingDashboardService {

    private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");
    private static final int READING_PRACTICE_LOW_THRESHOLD = 30;
    private static final int MIN_READING_DAYS_FOR_PRACTICE_JUDGEMENT = 3;
    private static final int CONTENT_SUITABILITY_LOW_THRESHOLD = 50;

    /*
     * "미참여"는 종합달성도 등급이 아니라, 대표 ReadingRecord가 아예 없는
     * 학생에게만 이 대시보드가 붙이는 별도 상태다. IndividualAchievementLevel
     * enum(매우 우수/우수/보통/집중 지원)에는 추가하지 않는다 - 점수 등급과
     * "아직 시작 안 함"은 의미가 다르고, 공통 계산 서비스의 등급 공식을
     * 이 값 때문에 건드리고 싶지 않기 때문이다.
     */
    private static final String LEVEL_NOT_PARTICIPATING = "미참여";

    private final UserRepository userRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final ClassStudentRepository classStudentRepository;
    private final ReadingRecordRepository readingRecordRepository;
    private final IndividualAchievementService individualAchievementService;

    @Transactional(readOnly = true)
    public TeacherIndividualReadingDashboardResponse getDashboard(Long teacherId, Long classId) {
        SchoolClass schoolClass = requireTeacherClass(teacherId);
        validateRequestedClass(schoolClass, classId);

        List<ClassStudent> roster = sortedRoster(classId);
        LocalDate today = LocalDate.now(ZONE_SEOUL);

        List<TeacherIndividualReadingStudentResponse> students = new ArrayList<>();
        int individualReadingStudentCount = 0;
        int todayActiveStudentCount = 0;
        int supportNeededStudentCount = 0;

        for (ClassStudent classStudent : roster) {
            StudentComputation computation = computeForStudent(classStudent, today);
            students.add(computation.response());

            /*
             * 오늘 참여율 분모: ReadingRecord가 1건이라도 있으면 포함한다
             * (진행 중이든 완독만 했든 상관없음). ReadingRecord가 아예
             * 없는 미참여 학생만 제외된다. currentlyReading()은 이
             * 목적으로 더 이상 쓰지 않는다 - activeReadingBookCount 필드나
             * supportReasons 판정처럼 "지금 진행 중인지"가 진짜 중요한
             * 곳에서만 그대로 쓴다.
             */
            if (computation.hasStartedIndividualReading()) {
                individualReadingStudentCount++;

                if (computation.response().isActiveToday()) {
                    todayActiveStudentCount++;
                }
            }

            if (!computation.response().getSupportReasons().isEmpty()) {
                supportNeededStudentCount++;
            }
        }

        Double todayParticipationRate = individualReadingStudentCount == 0
            ? null
            : round2((todayActiveStudentCount / (double) individualReadingStudentCount) * 100.0);
        String todayParticipationMessage = individualReadingStudentCount == 0
            ? "개별읽기 참여 학생 없음"
            : null;

        return new TeacherIndividualReadingDashboardResponse(
            classId,
            schoolClass.getClassName(),
            today,
            todayParticipationRate,
            todayParticipationMessage,
            todayActiveStudentCount,
            individualReadingStudentCount,
            supportNeededStudentCount,
            students
        );
    }

    private record StudentComputation(
        TeacherIndividualReadingStudentResponse response,
        boolean currentlyReading,
        boolean hasStartedIndividualReading
    ) {
    }

    private StudentComputation computeForStudent(ClassStudent classStudent, LocalDate today) {
        User student = classStudent.getStudent();
        Long studentId = student.getId();

        Optional<ReadingRecord> inProgress = readingRecordRepository.findByStudent_IdAndFinishedAtIsNull(studentId);
        ReadingRecord representative = inProgress.orElseGet(() -> readingRecordRepository
            .findFirstByStudent_IdAndFinishedAtIsNotNullOrderByFinishedAtDesc(studentId)
            .orElse(null));

        if (representative == null) {
            TeacherIndividualReadingStudentResponse response = new TeacherIndividualReadingStudentResponse(
                studentId,
                student.getName(),
                classStudent.getStudentNumber(),
                null,
                null,
                0,
                0L,
                0,
                0.0,
                0,
                0.0,
                0,
                0.0,
                0.0,
                0,
                LEVEL_NOT_PARTICIPATING,
                null,
                false,
                List.of()
            );

            return new StudentComputation(response, false, false);
        }

        IndividualAchievementResult result = individualAchievementService.calculate(representative.getId());
        boolean currentlyReading = representative.getFinishedAt() == null;
        boolean activeToday = result.getLatestActivityDate() != null
            && result.getLatestActivityDate().equals(today);

        List<String> supportReasons = buildSupportReasons(result, representative, currentlyReading);

        TeacherIndividualReadingStudentResponse response = new TeacherIndividualReadingStudentResponse(
            studentId,
            student.getName(),
            classStudent.getStudentNumber(),
            representative.getId(),
            representative.getBook() == null ? null : representative.getBook().getTitle(),
            currentlyReading ? 1 : 0,
            result.getTotalCompletedBookCount(),
            result.getReadingDays(),
            result.getReadingPracticeScore(),
            result.getCompletedStageCount(),
            result.getRecordCompletionScore(),
            result.getInspectedItemCount(),
            result.getContentSuitabilityScore(),
            result.getOverallAchievementScore(),
            result.getRoundedOverallAchievementScore(),
            result.getAchievementLevel().getLabel(),
            result.getLatestActivityDate(),
            activeToday,
            supportReasons
        );

        return new StudentComputation(response, currentlyReading, true);
    }

    /*
     * 사유마다 "판정 가능 조건"을 먼저 확인하고, 조건을 만족하지 못하면
     * 그 사유는 아예 반환하지 않는다(아직 판단할 수 없는 사유를 억지로
     * 붙이지 않음). 한 학생이 여러 사유를 동시에 가질 수 있다. 반환 순서는
     * 1)단계 활동 미완료 2)독서실천도 부족 3)질문 참여 부족 4)생각 나누기
     * 부족 5)기록내용 적합성 부족 순으로 고정한다.
     *
     * beforeDone/duringDone/afterDone/finishedAt은 IndividualAchievementResult에
     * 없으므로(계산 서비스는 집계값 completedStageCount만 반환) 대표
     * ReadingRecord 엔티티를 그대로 받아 직접 읽는다 - 계산 서비스나 그
     * DTO는 이 판정 때문에 건드리지 않는다.
     */
    private List<String> buildSupportReasons(
            IndividualAchievementResult result, ReadingRecord representative, boolean currentlyReading) {

        List<String> reasons = new ArrayList<>();

        boolean beforeDone = Boolean.TRUE.equals(representative.getBeforeDone());
        boolean duringDone = Boolean.TRUE.equals(representative.getDuringDone());
        boolean afterDone = Boolean.TRUE.equals(representative.getAfterDone());
        boolean finished = representative.getFinishedAt() != null;

        /*
         * 1. 단계 활동 미완료. 완독 기록에는 절대 붙이지 않는다(대표가
         * 완독 기록일 때 currentlyReading은 항상 false). 읽기 전조차
         * 끝나지 않은 초기 기록(beforeDone=false)은 두 사유 중 어느
         * 조건도 만족하지 못해 자연히 제외된다 - beforeDone/duringDone은
         * 실제 저장된 활동이 있어야만 true가 되므로 "등록만 하고 아무것도
         * 안 한" 상태를 억지로 문제로 잡지 않는다.
         */
        if (currentlyReading) {
            if (beforeDone && !duringDone) {
                reasons.add("읽기 중 활동을 완료해야 해요");
            } else if (duringDone && !afterDone) {
                reasons.add("읽기 후 활동을 완료해야 해요");
            }
        }

        /*
         * 2. 독서실천도 부족. readingDays >= 3(완독 기록에도 적용 가능한
         * 일반 조건)이거나, 진행 중 기록이 이미 최소 한 단계를 마쳤다면
         * (조기 판정 조건 B) 판정 가능하다. B는 진행 중 기록에만 적용되어
         * 과거 완독 기록의 낮은 점수를 뒤늦게 경고하지 않는다.
         */
        boolean canJudgePractice = result.getReadingDays() >= MIN_READING_DAYS_FOR_PRACTICE_JUDGEMENT
            || (currentlyReading && result.getCompletedStageCount() >= 1);

        if (canJudgePractice && result.getReadingPracticeScore() < READING_PRACTICE_LOW_THRESHOLD) {
            reasons.add("독서실천도가 낮아요");
        }

        /*
         * 3. 질문 만들기 참여 부족. 인정 활동(readingDays)이 하루라도
         * 있거나 한 단계 이상 끝났다면 "질문을 전혀 안 썼다"를 문제로
         * 판단할 수 있다 - 등록 직후 활동이 전혀 없는 학생에게는 붙이지
         * 않는다.
         */
        boolean questionStageStarted = result.getReadingDays() >= 1 || result.getCompletedStageCount() >= 1;

        if (questionStageStarted && !result.isWroteQuestionActivity()) {
            reasons.add("질문 만들기 참여가 필요해요");
        }

        /*
         * 4. 생각 나누기(책수다방) 참여 부족. 읽기 중을 끝냈거나(책수다방
         * 메뉴가 열린 뒤), 읽기 후를 끝냈거나, 완독했을 때만 판단한다 -
         * 읽기 전만 끝낸 학생에게는 아직 이르므로 붙이지 않는다.
         */
        boolean canJudgeBookChat = duringDone || afterDone || finished;

        if (canJudgeBookChat && result.getBookChatPostCount() == 0) {
            reasons.add("생각 나누기 참여가 필요해요");
        }

        /*
         * 5. 기록내용 적합성 부족 - 기존 조건 그대로 유지.
         */
        if (result.getInspectedItemCount() >= 1
                && result.getContentSuitabilityScore() < CONTENT_SUITABILITY_LOW_THRESHOLD) {
            reasons.add("기록 내용을 조금 더 다듬어야 해요");
        }

        return reasons;
    }

    private SchoolClass requireTeacherClass(Long teacherId) {
        User teacher = userRepository.findById(teacherId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "교사를 찾을 수 없습니다. teacherId=" + teacherId
            ));

        if (!"teacher".equalsIgnoreCase(teacher.getRole())) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "교사 계정만 조회할 수 있습니다."
            );
        }

        return schoolClassRepository.findByTeacherId(teacherId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "교사의 담당 학급을 찾을 수 없습니다. teacherId=" + teacherId
            ));
    }

    private void validateRequestedClass(SchoolClass teacherClass, Long classId) {
        if (!teacherClass.getId().equals(classId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "담당 학급만 조회할 수 있습니다."
            );
        }
    }

    private List<ClassStudent> sortedRoster(Long classId) {
        List<ClassStudent> roster = new ArrayList<>(classStudentRepository.findBySchoolClassId(classId));

        roster.sort(
            Comparator
                .comparing(
                    (ClassStudent cs) -> cs.getStudentNumber() == null ? Integer.MAX_VALUE : cs.getStudentNumber())
                .thenComparing(cs -> cs.getStudent().getName() == null ? "" : cs.getStudent().getName())
                .thenComparing(cs -> cs.getStudent().getId())
        );

        return roster;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
