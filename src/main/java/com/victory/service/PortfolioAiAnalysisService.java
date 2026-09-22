package com.victory.service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.victory.dto.IndividualPortfolioResponse;
import com.victory.dto.PortfolioAiAnalysisResponse;
import com.victory.dto.PracticePortfolioResponse;
import com.victory.dto.PracticeStageDetail;
import com.victory.entity.User;
import com.victory.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/** 집계 원본에서 AI에 허용된 필드만 전달하는 포트폴리오 분석 오케스트레이터. */
@Service
@RequiredArgsConstructor
public class PortfolioAiAnalysisService {
    static final String TYPE_PRACTICE = "practice";
    static final String TYPE_INDIVIDUAL = "individual";

    /*
     * completed=false 단계의 문구는 프롬프트로만 맡기지 않고 여기서
     * 강제한다 - AI가 그럴듯한 추측/권유 문장을 지어내도(예: "참여해보면
     * 좋겠습니다") 결과에 절대 노출되지 않도록 하기 위함.
     */
    private static final String INCOMPLETE_STAGE_TEXT = "활동 기록이 없습니다.";

    /*
     * 일반계정 학생이 해당 포트폴리오 유형 활동을 전혀 시작하지 않았을 때
     * 쓰는 빈 분석 결과 - OpenAI를 호출하지 않고 그대로 반환한다. 활동이
     * 하나도 없는데 "독서 활동이 확인되지 않습니다" 같은 그럴듯한 평가
     * 문구가 생기는 것을 막기 위함(demo/심사 계정은 이 분기 이전에 이미
     * findDemoStudent로 갈라져 여기를 타지 않는다).
     */
    private static final PortfolioAiAnalysisResponse NO_ACTIVITY_ANALYSIS =
        new PortfolioAiAnalysisResponse("", "");

    private final StudentPortfolioService portfolioService;
    private final FeedbackAiService feedbackAiService;
    private final UserRepository userRepository;
    private final DemoPracticePortfolioAiProvider demoPracticePortfolioAiProvider;
    private final DemoIndividualPortfolioAiProvider demoIndividualPortfolioAiProvider;

    public PortfolioAiAnalysisResponse analyzePractice(
            Long teacherId, Long classId, Long studentId, LocalDate from, LocalDate to) {
        return analyzePractice(teacherId, classId, studentId, from, to, 0L);
    }

    public PortfolioAiAnalysisResponse analyzePractice(
            Long teacherId, Long classId, Long studentId, LocalDate from, LocalDate to, Long regenerationVersion) {
        PracticePortfolioResponse data = portfolioService
            .getPracticePortfolio(teacherId, classId, studentId, from, to);
        Optional<User> demoStudent = findDemoStudent(studentId);
        PortfolioAiAnalysisResponse analysis = demoStudent.isPresent()
            ? demoPracticePortfolioAiProvider.forLoginId(demoStudent.get().getLoginId(), regenerationVersion)
            : hasPracticeActivity(data)
                ? callPortfolioAi(TYPE_PRACTICE, practiceInput(data, regenerationVersion), regenerationVersion)
                : NO_ACTIVITY_ANALYSIS;
        return withAuthoritativeCompletion(analysis, data);
    }

    public PortfolioAiAnalysisResponse analyzeIndividual(
            Long teacherId, Long classId, Long studentId, LocalDate from, LocalDate to) {
        return analyzeIndividual(teacherId, classId, studentId, from, to, 0L);
    }

    public PortfolioAiAnalysisResponse analyzeIndividual(
            Long teacherId, Long classId, Long studentId, LocalDate from, LocalDate to, Long regenerationVersion) {
        IndividualPortfolioResponse data = portfolioService
            .getIndividualPortfolio(teacherId, classId, studentId, from, to);
        Optional<User> demoStudent = findDemoStudent(studentId);
        if (demoStudent.isPresent()) {
            return demoIndividualPortfolioAiProvider.forLoginId(demoStudent.get().getLoginId(), regenerationVersion);
        }
        if (!hasIndividualActivity(data)) {
            return NO_ACTIVITY_ANALYSIS;
        }
        return callPortfolioAi(TYPE_INDIVIDUAL, individualInput(data, regenerationVersion), regenerationVersion);
    }

    /*
     * 최초 생성(regenerationVersion<=0)은 기존 2-인자 오버로드를 그대로
     * 호출한다 - seed=42 고정이던 기존 동작/테스트와 100% 동일하게
     * 유지하기 위함이다. "초안 다시 만들기"(version>0)일 때만 3-인자
     * 오버로드로 넘어가 seed를 회차마다 바꾼다.
     */
    private PortfolioAiAnalysisResponse callPortfolioAi(
            String portfolioType, Map<String, Object> input, Long regenerationVersion) {
        return (regenerationVersion == null || regenerationVersion <= 0)
            ? feedbackAiService.generatePortfolioAnalysis(portfolioType, input)
            : feedbackAiService.generatePortfolioAnalysis(portfolioType, input, regenerationVersion);
    }

    /*
     * 온책읽기 "활동 시작" 판정 - activityCount는 평가기간 내 읽기 전/중/후
     * Response와 읽기 후 Summary 저장 건수의 합(StudentPortfolioService)이라,
     * 책이 선택되어 있거나 기간만 설정된 것만으로는 1을 넘지 않는다.
     */
    private boolean hasPracticeActivity(PracticePortfolioResponse data) {
        return data.activityCount() > 0;
    }

    /*
     * 개별읽기 "활동 시작" 판정 - AI에 실제로 넘기는 근거 필드
     * (individualInput의 completedBookCount/activitySummary)와 동일한
     * 기준을 쓴다. 전부 0이면 AI에게 넘길 실제 근거가 아예 없다는 뜻이므로
     * 그대로 호출을 막는다.
     */
    private boolean hasIndividualActivity(IndividualPortfolioResponse data) {
        IndividualPortfolioResponse.ActivitySummary summary = data.activitySummary();
        return data.completedBookCount() > 0
            || summary.questions() > 0
            || summary.thoughtWriting() > 0
            || summary.summaries() > 0
            || summary.bookChatSharing() > 0;
    }

    /*
     * 심사/demo 계정(loginId 기준 고정 예시)은 실제 OpenAI 호출 없이
     * 학생별 고정 종합의견을 쓴다 - 원천 데이터가 얕은 심사 계정을 실제
     * AI로 "억지 계산"하지 않기 위함(요구사항 6번). 일반계정은 이 분기를
     * 타지 않고 기존 실제 AI 호출 경로를 그대로 유지한다.
     */
    private Optional<User> findDemoStudent(Long studentId) {
        return userRepository.findById(studentId)
            .filter(user -> Boolean.TRUE.equals(user.getDemoAccount()));
    }

    Map<String, Object> practiceInput(PracticePortfolioResponse data) {
        return practiceInput(data, 0L);
    }

    Map<String, Object> practiceInput(PracticePortfolioResponse data, Long regenerationVersion) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("studentName", data.studentName());
        input.put("periodStart", data.periodStart());
        input.put("periodEnd", data.periodEnd());
        input.put("participationRate", data.participationRate());
        input.put("comprehensionRate", data.comprehensionRate());
        input.put("beforeParticipation", data.beforeParticipation());
        input.put("duringParticipation", data.duringParticipation());
        input.put("afterParticipation", data.afterParticipation());
        input.put("currentBookTitle", data.currentBookTitle());
        input.put("activityCount", data.activityCount());
        /*
         * 단계별 분석(stageAnalysis)의 근거 - 학생이 실제로 남긴 질문/답
         * 텍스트(representativeText)와 규칙 기반 관찰(growthNote)을 그대로
         * 넘겨서, AI가 활동 횟수보다 단계별 성장 과정 중심으로 쓰게 한다.
         */
        input.put("beforeStage", data.beforeStage());
        input.put("duringStage", data.duringStage());
        input.put("afterStage", data.afterStage());
        input.put("rewriteRequest", rewriteRequest(regenerationVersion));
        return input;
    }

    Map<String, Object> individualInput(IndividualPortfolioResponse data) {
        return individualInput(data, 0L);
    }

    Map<String, Object> individualInput(IndividualPortfolioResponse data, Long regenerationVersion) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("studentName", data.studentName());
        input.put("periodStart", data.periodStart());
        input.put("periodEnd", data.periodEnd());
        input.put("completedBookCount", data.completedBookCount());
        input.put("averageReadingPracticeScore", data.averageReadingPracticeScore());
        input.put("averageRecordCompletionScore", data.averageRecordCompletionScore());
        input.put("activitySummary", data.activitySummary());
        input.put("monthlyCompletionStats", data.monthlyCompletionStats());
        /*
         * 게임 능력치 보상 누적값(data.competencies())은 AI 분석 근거로
         * 쓰지 않는다 - 대신 학생 개인의 평가기간 내 절대적 수행 비율로
         * 계산된 0~100 독서 역량 4종(readingCompetencies)을 넘긴다.
         */
        input.put("readingCompetencies", data.readingCompetencies());
        input.put("rewriteRequest", rewriteRequest(regenerationVersion));
        return input;
    }

    private String rewriteRequest(Long regenerationVersion) {
        long version = regenerationVersion == null ? 0L : regenerationVersion;
        return version <= 0
            ? "학생 활동 근거에 충실한 초안을 작성합니다."
            : "재작성 " + version + "회차입니다. 같은 근거를 유지하되 이전 초안과 다른 문장 구성과 표현으로 새로 작성합니다.";
    }

    /* AI가 완료 여부를 추측하거나 누락하지 않도록 DB 집계값으로 최종 보정한다. */
    private PortfolioAiAnalysisResponse withAuthoritativeCompletion(
            PortfolioAiAnalysisResponse analysis, PracticePortfolioResponse data) {
        if (analysis.stageAnalysis() == null) {
            return analysis;
        }
        PortfolioAiAnalysisResponse.StageAnalysis stages = analysis.stageAnalysis();
        return new PortfolioAiAnalysisResponse(analysis.strengthText(), analysis.improvementText(),
            new PortfolioAiAnalysisResponse.StageAnalysis(
                withCompletion(stages.before(), "읽기 전", data.beforeStage()),
                withCompletion(stages.during(), "읽기 중", data.duringStage()),
                withCompletion(stages.after(), "읽기 후", data.afterStage())));
    }

    /*
     * completed=true인데 AI가 그 단계를 통째로 빠뜨리거나(item==null)
     * strengthText를 비워서 준 드문 경우, "완료 여부를 확인해 주세요"
     * 같은 무의미한 문구 대신 항상 존재하는 결정적 growthNote
     * (PracticeStageNarrativeBuilder/DemoPracticeStageProvider가 실제
     * 근거로 만든 문장)로 대체한다 - 완료 단계는 절대 generic fallback이
     * 아니라 실제 성장 문장만 보이도록 하는 마지막 안전망.
     */
    private PortfolioAiAnalysisResponse.StageAnalysisItem withCompletion(
            PortfolioAiAnalysisResponse.StageAnalysisItem item, String title, PracticeStageDetail stageDetail) {
        if (!stageDetail.completed()) {
            return new PortfolioAiAnalysisResponse.StageAnalysisItem(title, false, INCOMPLETE_STAGE_TEXT, "");
        }
        boolean hasAiText = item != null && item.strengthText() != null && !item.strengthText().isBlank();
        if (hasAiText) {
            /*
             * growthText는 항상 비워서 돌려준다 - AI가 프롬프트 지시를
             * 어기고 별도 문장을 채워 보내도, 결과에는 절대 두 번째
             * 문단/점선 구분이 생기지 않도록 하기 위한 코드 레벨 안전망
             * (프론트는 이제 growthText를 별도 영역으로 그리지 않지만,
             * strengthText+growthText를 이어붙이면 "최대 2문장" 길이
             * 기준을 넘길 수 있어 여기서도 막는다).
             */
            return new PortfolioAiAnalysisResponse.StageAnalysisItem(title, true, item.strengthText(), "");
        }
        return new PortfolioAiAnalysisResponse.StageAnalysisItem(title, true, stageDetail.growthNote(), "");
    }
}
