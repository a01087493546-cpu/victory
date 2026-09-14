package com.victory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.IndividualPortfolioResponse;
import com.victory.dto.MonthlyCompletionStatsResponse;
import com.victory.dto.PortfolioActivityCount;
import com.victory.dto.PortfolioAiAnalysisResponse;
import com.victory.dto.PracticePortfolioResponse;
import com.victory.dto.PracticeStageDetail;
import com.victory.entity.User;
import com.victory.repository.UserRepository;

class PortfolioAiAnalysisServiceTest {
    private final StudentPortfolioService portfolioService = mock(StudentPortfolioService.class);
    private final FeedbackAiService feedbackAiService = mock(FeedbackAiService.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final DemoPracticePortfolioAiProvider demoPracticePortfolioAiProvider = new DemoPracticePortfolioAiProvider();
    private final DemoIndividualPortfolioAiProvider demoIndividualPortfolioAiProvider = new DemoIndividualPortfolioAiProvider();
    private final PortfolioAiAnalysisService service = new PortfolioAiAnalysisService(
        portfolioService, feedbackAiService, userRepository,
        demoPracticePortfolioAiProvider, demoIndividualPortfolioAiProvider);
    private final LocalDate from = LocalDate.of(2026, 8, 1);
    private final LocalDate to = LocalDate.of(2026, 8, 31);

    private User demoUser(String loginId) {
        User user = new User();
        user.setDemoAccount(true);
        user.setLoginId(loginId);
        return user;
    }

    @Test
    @SuppressWarnings("unchecked")
    void practiceAnalysis_sendsOnlyPracticeAggregateFields() {
        PracticeStageDetail beforeStage = new PracticeStageDetail(true, true, "질문 만들기에 참여함",
            "\"까마귀는 왜 그랬을까?\"라는 질문을 스스로 만들었어요.", "까마귀는 왜 그랬을까?");
        PracticeStageDetail duringStage = new PracticeStageDetail(true, true, "근거를 들어 답함",
            "\"주인공이 용감해서요\"처럼 책 내용을 근거로 짐작하거나 비교하며 생각을 정리하고 답을 남겼어요.", "주인공이 용감해서요");
        PracticeStageDetail afterStage = new PracticeStageDetail(false, false, "아직 참여 기록 없음",
            "현재 기록에서는 이 단계 활동 기록이 아직 없어요.", null);
        PracticePortfolioResponse aggregate = new PracticePortfolioResponse(2L, "김학생", 4, 2, from, to,
            82d, 91d, new PortfolioActivityCount(1, true), new PortfolioActivityCount(2, true),
            new PortfolioActivityCount(0, false), "마당을 나온 암탉", 3,
            beforeStage, duringStage, afterStage);
        when(portfolioService.getPracticePortfolio(1L, 10L, 2L, from, to)).thenReturn(aggregate);
        when(feedbackAiService.generatePortfolioAnalysis(eq("practice"), org.mockito.ArgumentMatchers.anyMap()))
            .thenReturn(new PortfolioAiAnalysisResponse("꾸준히 참여했어요.", "읽기 후 활동을 이어가면 좋아요.",
                new PortfolioAiAnalysisResponse.StageAnalysis(
                    new PortfolioAiAnalysisResponse.StageAnalysisItem("읽기 전", false, "읽기 전 분석", "다음 활동"),
                    new PortfolioAiAnalysisResponse.StageAnalysisItem("읽기 중", false, "읽기 중 분석", "다음 활동"),
                    new PortfolioAiAnalysisResponse.StageAnalysisItem("읽기 후", true, "읽기 후 분석", "다음 활동"))));

        PortfolioAiAnalysisResponse result = service.analyzePractice(1L, 10L, 2L, from, to);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(feedbackAiService).generatePortfolioAnalysis(eq("practice"), captor.capture());
        assertThat(captor.getValue()).containsKeys("participationRate", "comprehensionRate", "beforeParticipation",
            "duringParticipation", "afterParticipation", "currentBookTitle", "activityCount",
            "beforeStage", "duringStage", "afterStage");
        assertThat(captor.getValue()).doesNotContainKeys("completedBookCount", "competencies", "monthlyCompletionStats");
        assertThat(captor.getValue().get("beforeStage")).isSameAs(beforeStage);
        assertThat(result.strengthText()).isEqualTo("꾸준히 참여했어요.");
        assertThat(result.stageAnalysis().before().completed()).isTrue();
        assertThat(result.stageAnalysis().before().strengthText()).isEqualTo("읽기 전 분석");
        assertThat(result.stageAnalysis().during().completed()).isTrue();
        assertThat(result.stageAnalysis().after().completed()).isFalse();
        /*
         * AI가 completed=false 단계에 그럴듯한 문구("읽기 후 분석"/"다음
         * 활동")를 지어내도, DB 집계값(afterStage.completed()==false)에
         * 따라 무조건 고정 문구로 덮어써야 한다.
         */
        assertThat(result.stageAnalysis().after().strengthText()).isEqualTo("활동 기록이 없습니다.");
        assertThat(result.stageAnalysis().after().growthText()).isEqualTo("");
    }

    @Test
    @SuppressWarnings("unchecked")
    void practiceAnalysis_incompleteStageForcesFixedTextEvenWhenAiOmitsTheStageEntirely() {
        PracticeStageDetail beforeStage = new PracticeStageDetail(false, false, "아직 참여 기록 없음",
            "현재 기록에서는 이 단계 활동 기록이 아직 없어요.", null);
        PracticeStageDetail duringStage = new PracticeStageDetail(true, true, "근거를 들어 답함",
            "\"주인공이 용감해서요\"처럼 답을 남겼어요.", "주인공이 용감해서요");
        PracticeStageDetail afterStage = new PracticeStageDetail(true, true, "간추리기 활동 참여",
            "\"책 내용을 간추렸어요\"", "책 내용을 간추렸어요");
        PracticePortfolioResponse aggregate = new PracticePortfolioResponse(2L, "김학생", 4, 2, from, to,
            50d, 60d, new PortfolioActivityCount(0, false), new PortfolioActivityCount(1, true),
            new PortfolioActivityCount(1, true), "마당을 나온 암탉", 2,
            beforeStage, duringStage, afterStage);
        when(portfolioService.getPracticePortfolio(1L, 10L, 2L, from, to)).thenReturn(aggregate);
        // AI가 completed=false인 before 단계를 통째로 null로 반환하는 극단적 케이스
        when(feedbackAiService.generatePortfolioAnalysis(eq("practice"), org.mockito.ArgumentMatchers.anyMap()))
            .thenReturn(new PortfolioAiAnalysisResponse("꾸준히 참여했어요.", "다음에도 이어가면 좋아요.",
                new PortfolioAiAnalysisResponse.StageAnalysis(
                    null,
                    new PortfolioAiAnalysisResponse.StageAnalysisItem("읽기 중", true, "읽기 중 분석", "다음 활동"),
                    new PortfolioAiAnalysisResponse.StageAnalysisItem("읽기 후", true, "읽기 후 분석", "다음 활동"))));

        PortfolioAiAnalysisResponse result = service.analyzePractice(1L, 10L, 2L, from, to);

        assertThat(result.stageAnalysis().before().completed()).isFalse();
        assertThat(result.stageAnalysis().before().strengthText()).isEqualTo("활동 기록이 없습니다.");
        assertThat(result.stageAnalysis().before().growthText()).isEqualTo("");
        assertThat(result.stageAnalysis().before().title()).isEqualTo("읽기 전");
    }

    /*
     * completed=true인데 AI가 그 단계를 통째로 빠뜨리거나(null) 빈
     * 문자열을 주면, "완료 여부를 확인해 주세요" 같은 의미 없는 문구
     * 대신 항상 존재하는 실제 growthNote로 대체돼야 한다 - 절대 generic
     * fallback이 노출되면 안 된다는 요구사항의 핵심 안전망.
     */
    @Test
    @SuppressWarnings("unchecked")
    void practiceAnalysis_completedStageFallsBackToGrowthNoteWhenAiOmitsOrBlanksIt() {
        PracticeStageDetail beforeStage = new PracticeStageDetail(true, true, "질문 만들기에 참여함",
            "\"까마귀는 왜 그랬을까?\"라는 질문을 스스로 만들었어요.", "까마귀는 왜 그랬을까?");
        PracticeStageDetail duringStage = new PracticeStageDetail(true, true, "근거를 들어 답함",
            "\"주인공이 용감해서요\"처럼 답을 남겼어요.", "주인공이 용감해서요");
        PracticeStageDetail afterStage = new PracticeStageDetail(true, true, "간추리기 활동 참여",
            "\"책 내용을 간추렸어요\"처럼 정리했어요.", "책 내용을 간추렸어요");
        PracticePortfolioResponse aggregate = new PracticePortfolioResponse(2L, "김학생", 4, 2, from, to,
            80d, 85d, new PortfolioActivityCount(1, true), new PortfolioActivityCount(1, true),
            new PortfolioActivityCount(1, true), "마당을 나온 암탉", 3,
            beforeStage, duringStage, afterStage);
        when(portfolioService.getPracticePortfolio(1L, 10L, 2L, from, to)).thenReturn(aggregate);
        // before는 AI가 통째로 누락(null), during은 strengthText가 빈 문자열, after만 정상 AI 텍스트
        when(feedbackAiService.generatePortfolioAnalysis(eq("practice"), org.mockito.ArgumentMatchers.anyMap()))
            .thenReturn(new PortfolioAiAnalysisResponse("꾸준히 참여했어요.", "다음에도 이어가면 좋아요.",
                new PortfolioAiAnalysisResponse.StageAnalysis(
                    null,
                    new PortfolioAiAnalysisResponse.StageAnalysisItem("읽기 중", true, "", ""),
                    new PortfolioAiAnalysisResponse.StageAnalysisItem("읽기 후", true, "정상적인 AI 문장입니다.", "다음 활동"))));

        PortfolioAiAnalysisResponse result = service.analyzePractice(1L, 10L, 2L, from, to);

        assertThat(result.stageAnalysis().before().completed()).isTrue();
        assertThat(result.stageAnalysis().before().strengthText()).isEqualTo(beforeStage.growthNote());
        assertThat(result.stageAnalysis().during().completed()).isTrue();
        assertThat(result.stageAnalysis().during().strengthText()).isEqualTo(duringStage.growthNote());
        // AI가 정상 텍스트를 준 단계는 그대로 유지(growthNote로 덮어쓰지 않음)
        assertThat(result.stageAnalysis().after().strengthText()).isEqualTo("정상적인 AI 문장입니다.");
    }

    @Test
    @SuppressWarnings("unchecked")
    void individualAnalysis_sendsOnlyIndividualAggregateFieldsIncludingDemoDerivedValues() {
        IndividualPortfolioResponse aggregate = new IndividualPortfolioResponse(2L, "심사학생", 4, 2, from, to,
            2, new BigDecimal("88.00"), new BigDecimal("92.00"),
            new IndividualPortfolioResponse.ActivitySummary(5, 4, 2, 1),
            new MonthlyCompletionStatsResponse(2026, List.of(0,0,0,0,0,0,0,2,0,0,0,0)),
            new IndividualPortfolioResponse.Competencies(3, 6, 3, 1),
            new IndividualPortfolioResponse.ReadingCompetencies(
                com.victory.dto.ReadingCompetencyScore.of(72), com.victory.dto.ReadingCompetencyScore.of(85),
                com.victory.dto.ReadingCompetencyScore.of(63), com.victory.dto.ReadingCompetencyScore.of(48)),
            true);
        when(portfolioService.getIndividualPortfolio(1L, 10L, 2L, from, to)).thenReturn(aggregate);
        when(feedbackAiService.generatePortfolioAnalysis(eq("individual"), org.mockito.ArgumentMatchers.anyMap()))
            .thenReturn(new PortfolioAiAnalysisResponse("기록을 잘 이어가고 있어요.", "생각을 더 나눠 보면 좋아요."));

        service.analyzeIndividual(1L, 10L, 2L, from, to);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(feedbackAiService).generatePortfolioAnalysis(eq("individual"), captor.capture());
        assertThat(captor.getValue()).containsKeys("completedBookCount", "averageReadingPracticeScore",
            "averageRecordCompletionScore", "activitySummary", "monthlyCompletionStats", "readingCompetencies");
        assertThat(captor.getValue()).doesNotContainKeys(
            "participationRate", "comprehensionRate", "currentBookTitle", "competencies");
    }

    @Test
    void practiceAnalysis_demoAccount_usesFixedProviderTextInsteadOfCallingAi() {
        PracticeStageDetail beforeStage = new PracticeStageDetail(true, true, "질문 만들기에 참여함", "근거", "대표 텍스트");
        PracticeStageDetail duringStage = new PracticeStageDetail(true, true, "근거를 들어 답함", "근거", "대표 텍스트");
        PracticeStageDetail afterStage = new PracticeStageDetail(true, true, "간추리기 활동 참여", "근거", "대표 텍스트");
        PracticePortfolioResponse aggregate = new PracticePortfolioResponse(2L, "김초롱", 4, 2, from, to,
            90d, 88d, new PortfolioActivityCount(1, true), new PortfolioActivityCount(1, true),
            new PortfolioActivityCount(1, true), "마당을 나온 암탉", 3,
            beforeStage, duringStage, afterStage);
        when(portfolioService.getPracticePortfolio(1L, 10L, 2L, from, to)).thenReturn(aggregate);
        when(userRepository.findById(2L)).thenReturn(java.util.Optional.of(demoUser("ss01")));

        PortfolioAiAnalysisResponse result = service.analyzePractice(1L, 10L, 2L, from, to);

        verify(feedbackAiService, never()).generatePortfolioAnalysis(
            org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyMap());
        assertThat(result).isEqualTo(demoPracticePortfolioAiProvider.forLoginId("ss01"));
    }

    /* demo 고정 문구를 쓰더라도, 실제로 completed=false인 단계는 여전히 고정 문구로 강제돼야 한다. */
    @Test
    void practiceAnalysis_demoAccount_stillForcesIncompleteStageTextWhenStageNotActuallyCompleted() {
        PracticeStageDetail beforeStage = new PracticeStageDetail(false, false, "아직 참여 기록 없음", "근거", null);
        PracticeStageDetail duringStage = new PracticeStageDetail(true, true, "근거를 들어 답함", "근거", "대표 텍스트");
        PracticeStageDetail afterStage = new PracticeStageDetail(true, true, "간추리기 활동 참여", "근거", "대표 텍스트");
        PracticePortfolioResponse aggregate = new PracticePortfolioResponse(2L, "이진우", 4, 2, from, to,
            40d, 50d, new PortfolioActivityCount(0, false), new PortfolioActivityCount(1, true),
            new PortfolioActivityCount(1, true), "마당을 나온 암탉", 2,
            beforeStage, duringStage, afterStage);
        when(portfolioService.getPracticePortfolio(1L, 10L, 2L, from, to)).thenReturn(aggregate);
        when(userRepository.findById(2L)).thenReturn(java.util.Optional.of(demoUser("demo_student_04")));

        PortfolioAiAnalysisResponse result = service.analyzePractice(1L, 10L, 2L, from, to);

        assertThat(result.stageAnalysis().before().completed()).isFalse();
        assertThat(result.stageAnalysis().before().strengthText()).isEqualTo("활동 기록이 없습니다.");
        assertThat(result.stageAnalysis().before().growthText()).isEqualTo("");
    }

    @Test
    void individualAnalysis_demoAccount_usesFixedProviderTextInsteadOfCallingAi() {
        IndividualPortfolioResponse aggregate = new IndividualPortfolioResponse(2L, "김수진", 4, 2, from, to,
            5, new BigDecimal("90.00"), new BigDecimal("88.00"),
            new IndividualPortfolioResponse.ActivitySummary(5, 4, 2, 1),
            new MonthlyCompletionStatsResponse(2026, List.of(3,2,4,3,2,4,3,4,2,3,4,3)),
            new IndividualPortfolioResponse.Competencies(3, 6, 3, 1),
            new IndividualPortfolioResponse.ReadingCompetencies(
                com.victory.dto.ReadingCompetencyScore.of(88), com.victory.dto.ReadingCompetencyScore.of(67),
                com.victory.dto.ReadingCompetencyScore.of(79), com.victory.dto.ReadingCompetencyScore.of(92)),
            true);
        when(portfolioService.getIndividualPortfolio(1L, 10L, 2L, from, to)).thenReturn(aggregate);
        when(userRepository.findById(2L)).thenReturn(java.util.Optional.of(demoUser("demo_student_07")));

        PortfolioAiAnalysisResponse result = service.analyzeIndividual(1L, 10L, 2L, from, to);

        verify(feedbackAiService, never()).generatePortfolioAnalysis(
            org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyMap());
        assertThat(result).isEqualTo(demoIndividualPortfolioAiProvider.forLoginId("demo_student_07"));
        assertThat(result.stageAnalysis()).isNull();
    }

    /* demo 학생 8명이 서로 다른 온책 종합의견을 갖는지(이름만 바뀐 동일 템플릿이 아닌지) 확인. */
    @Test
    void demoPracticeProvider_allEightStudentsHaveDistinctOverallText() {
        List<String> loginIds = List.of("ss01", "demo_student_02", "demo_student_03", "demo_student_04",
            "demo_student_05", "demo_student_06", "demo_student_07", "demo_student_08");
        List<String> strengthTexts = loginIds.stream()
            .map(id -> demoPracticePortfolioAiProvider.forLoginId(id).strengthText())
            .distinct().toList();
        assertThat(strengthTexts).hasSize(loginIds.size());
    }

    /* demo 학생 8명이 서로 다른 개별읽기 종합의견을 갖는지 확인. */
    @Test
    void demoIndividualProvider_allEightStudentsHaveDistinctOverallText() {
        List<String> loginIds = List.of("ss01", "demo_student_02", "demo_student_03", "demo_student_04",
            "demo_student_05", "demo_student_06", "demo_student_07", "demo_student_08");
        List<String> strengthTexts = loginIds.stream()
            .map(id -> demoIndividualPortfolioAiProvider.forLoginId(id).strengthText())
            .distinct().toList();
        assertThat(strengthTexts).hasSize(loginIds.size());
    }

    /*
     * 일반계정 학생이 온책읽기 활동을 전혀 시작하지 않았을 때(before/during/
     * after 전부 미완료, activityCount==0) AI를 호출하지 않고 빈 값을
     * 돌려줘야 한다 - "현재 기록에서는 독서 활동이 확인되지 않습니다"
     * 같은 그럴듯한 평가 문구가 활동 전 학생에게 생기는 것을 막기 위함.
     */
    @Test
    void practiceAnalysis_generalAccountWithZeroActivity_skipsAiCallAndReturnsEmpty() {
        PracticeStageDetail beforeStage = new PracticeStageDetail(false, false, "아직 참여 기록 없음",
            "활동 기록이 없습니다.", null);
        PracticeStageDetail duringStage = new PracticeStageDetail(false, false, "아직 참여 기록 없음",
            "활동 기록이 없습니다.", null);
        PracticeStageDetail afterStage = new PracticeStageDetail(false, false, "아직 참여 기록 없음",
            "활동 기록이 없습니다.", null);
        PracticePortfolioResponse aggregate = new PracticePortfolioResponse(2L, "김학생", 4, 2, from, to,
            0d, 0d, new PortfolioActivityCount(0, false), new PortfolioActivityCount(0, false),
            new PortfolioActivityCount(0, false), "마당을 나온 암탉", 0,
            beforeStage, duringStage, afterStage);
        when(portfolioService.getPracticePortfolio(1L, 10L, 2L, from, to)).thenReturn(aggregate);
        when(userRepository.findById(2L)).thenReturn(java.util.Optional.empty());

        PortfolioAiAnalysisResponse result = service.analyzePractice(1L, 10L, 2L, from, to);

        verify(feedbackAiService, never()).generatePortfolioAnalysis(
            org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyMap());
        assertThat(result.strengthText()).isEqualTo("");
        assertThat(result.improvementText()).isEqualTo("");
        assertThat(result.stageAnalysis()).isNull();
    }

    /* 활동이 하나라도(예: 읽기 중만 완료) 있으면 일반계정도 정상적으로 AI를 호출해야 한다. */
    @Test
    void practiceAnalysis_generalAccountWithSomeActivity_stillCallsAi() {
        PracticeStageDetail beforeStage = new PracticeStageDetail(false, false, "아직 참여 기록 없음",
            "활동 기록이 없습니다.", null);
        PracticeStageDetail duringStage = new PracticeStageDetail(true, true, "근거를 들어 답함",
            "\"주인공이 용감해서요\"처럼 답을 남겼어요.", "주인공이 용감해서요");
        PracticeStageDetail afterStage = new PracticeStageDetail(false, false, "아직 참여 기록 없음",
            "활동 기록이 없습니다.", null);
        PracticePortfolioResponse aggregate = new PracticePortfolioResponse(2L, "김학생", 4, 2, from, to,
            30d, 40d, new PortfolioActivityCount(0, false), new PortfolioActivityCount(1, true),
            new PortfolioActivityCount(0, false), "마당을 나온 암탉", 1,
            beforeStage, duringStage, afterStage);
        when(portfolioService.getPracticePortfolio(1L, 10L, 2L, from, to)).thenReturn(aggregate);
        when(userRepository.findById(2L)).thenReturn(java.util.Optional.empty());
        when(feedbackAiService.generatePortfolioAnalysis(eq("practice"), org.mockito.ArgumentMatchers.anyMap()))
            .thenReturn(new PortfolioAiAnalysisResponse("읽기 중 활동에 참여했어요.", "읽기 전/후 활동도 이어가면 좋아요.",
                new PortfolioAiAnalysisResponse.StageAnalysis(
                    new PortfolioAiAnalysisResponse.StageAnalysisItem("읽기 전", false, "", ""),
                    new PortfolioAiAnalysisResponse.StageAnalysisItem("읽기 중", true, "읽기 중 분석", ""),
                    new PortfolioAiAnalysisResponse.StageAnalysisItem("읽기 후", false, "", ""))));

        PortfolioAiAnalysisResponse result = service.analyzePractice(1L, 10L, 2L, from, to);

        verify(feedbackAiService).generatePortfolioAnalysis(eq("practice"), org.mockito.ArgumentMatchers.anyMap());
        assertThat(result.strengthText()).isEqualTo("읽기 중 활동에 참여했어요.");
    }

    /*
     * 일반계정 학생이 개별읽기 활동을 전혀 시작하지 않았을 때(완독 0,
     * 질문/생각쓰기/간추리기/책수다방 전부 0) AI를 호출하지 않고 빈 값을
     * 돌려줘야 한다.
     */
    @Test
    void individualAnalysis_generalAccountWithZeroActivity_skipsAiCallAndReturnsEmpty() {
        IndividualPortfolioResponse aggregate = new IndividualPortfolioResponse(2L, "김학생", 4, 2, from, to,
            0, null, null,
            new IndividualPortfolioResponse.ActivitySummary(0, 0, 0, 0),
            new MonthlyCompletionStatsResponse(2026, List.of(0,0,0,0,0,0,0,0,0,0,0,0)),
            new IndividualPortfolioResponse.Competencies(0, 0, 0, 0),
            new IndividualPortfolioResponse.ReadingCompetencies(
                com.victory.dto.ReadingCompetencyScore.of(0), com.victory.dto.ReadingCompetencyScore.of(0),
                com.victory.dto.ReadingCompetencyScore.of(0), com.victory.dto.ReadingCompetencyScore.of(0)),
            false);
        when(portfolioService.getIndividualPortfolio(1L, 10L, 2L, from, to)).thenReturn(aggregate);
        when(userRepository.findById(2L)).thenReturn(java.util.Optional.empty());

        PortfolioAiAnalysisResponse result = service.analyzeIndividual(1L, 10L, 2L, from, to);

        verify(feedbackAiService, never()).generatePortfolioAnalysis(
            org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyMap());
        assertThat(result.strengthText()).isEqualTo("");
        assertThat(result.improvementText()).isEqualTo("");
        assertThat(result.stageAnalysis()).isNull();
    }

    /* 완독은 없어도 질문/생각쓰기 등 다른 활동이 하나라도 있으면 AI를 호출해야 한다. */
    @Test
    void individualAnalysis_generalAccountWithOnlyQuestionActivity_stillCallsAi() {
        IndividualPortfolioResponse aggregate = new IndividualPortfolioResponse(2L, "김학생", 4, 2, from, to,
            0, null, null,
            new IndividualPortfolioResponse.ActivitySummary(2, 0, 0, 0),
            new MonthlyCompletionStatsResponse(2026, List.of(0,0,0,0,0,0,0,0,0,0,0,0)),
            new IndividualPortfolioResponse.Competencies(0, 0, 0, 0),
            new IndividualPortfolioResponse.ReadingCompetencies(
                com.victory.dto.ReadingCompetencyScore.of(0), com.victory.dto.ReadingCompetencyScore.of(0),
                com.victory.dto.ReadingCompetencyScore.of(0), com.victory.dto.ReadingCompetencyScore.of(0)),
            false);
        when(portfolioService.getIndividualPortfolio(1L, 10L, 2L, from, to)).thenReturn(aggregate);
        when(userRepository.findById(2L)).thenReturn(java.util.Optional.empty());
        when(feedbackAiService.generatePortfolioAnalysis(eq("individual"), org.mockito.ArgumentMatchers.anyMap()))
            .thenReturn(new PortfolioAiAnalysisResponse("질문을 만들며 읽기 시작했어요.", "완독까지 이어가면 좋아요."));

        PortfolioAiAnalysisResponse result = service.analyzeIndividual(1L, 10L, 2L, from, to);

        verify(feedbackAiService).generatePortfolioAnalysis(eq("individual"), org.mockito.ArgumentMatchers.anyMap());
        assertThat(result.strengthText()).isEqualTo("질문을 만들며 읽기 시작했어요.");
    }

    @Test
    void aggregateAuthorizationFailure_isPropagatedBeforeAiCall() {
        ResponseStatusException forbidden = new ResponseStatusException(HttpStatus.FORBIDDEN, "담당 학급 학생만 조회");
        when(portfolioService.getPracticePortfolio(1L, 10L, 99L, from, to)).thenThrow(forbidden);
        assertThatThrownBy(() -> service.analyzePractice(1L, 10L, 99L, from, to)).isSameAs(forbidden);
        verify(feedbackAiService, never()).generatePortfolioAnalysis(eq("practice"), org.mockito.ArgumentMatchers.anyMap());
    }
}
