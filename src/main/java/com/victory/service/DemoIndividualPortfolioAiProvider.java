package com.victory.service;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.victory.dto.PortfolioAiAnalysisResponse;

/*
 * 심사 개별읽기 포트폴리오 전용 학생별 고정 AI 종합의견. 개별읽기는
 * 여러 책을 읽어온 장기 기록을 종합하는 성격이라, 온책읽기(단일 책 진행
 * 과정)와는 다른 "독서 습관/지속성" 관점으로 문장을 구성한다(랜덤 없음,
 * stageAnalysis는 개별읽기에 존재하지 않으므로 항상 null).
 */
@Component
public class DemoIndividualPortfolioAiProvider {

    private static final PortfolioAiAnalysisResponse KIM_CHORONG = new PortfolioAiAnalysisResponse(
        "여러 권의 책을 꾸준히 읽으며 자신의 독서 흐름을 안정적으로 이어가고 있습니다. "
            + "책 내용을 자신의 경험과 연결해 생각을 정리하는 힘도 함께 나타납니다.",
        "앞으로는 책과 책 사이의 내용을 서로 연결해 더 넓은 시야로 해석해보는 연습을 이어가면 좋겠습니다.");

    private static final PortfolioAiAnalysisResponse SONG_MINJEONG = new PortfolioAiAnalysisResponse(
        "읽기 흐름을 놓치지 않고 꾸준히 따라가며 중요한 내용을 정리하는 습관이 자리 잡혀 있습니다.",
        "읽은 뒤 떠오른 생각을 조금 더 오래 붙잡아 다듬어 보는 습관을 들이면 좋겠습니다.");

    private static final PortfolioAiAnalysisResponse PARK_HAMIN = new PortfolioAiAnalysisResponse(
        "친구들과 생각을 나누는 활동에 적극적으로 참여하며 다양한 관점을 자연스럽게 받아들이고 있습니다.",
        "완독한 책을 조금 더 자주 이어가면 독서 기록이 한층 더 풍성해질 수 있습니다.");

    private static final PortfolioAiAnalysisResponse LEE_JINWOO = new PortfolioAiAnalysisResponse(
        "책 속 생각쓰기 활동을 통해 읽은 내용을 자기 나름대로 정리해보려는 시도가 나타납니다.",
        "일정한 간격으로 책을 이어 읽는 습관을 만들면 독서 기록이 더 안정적으로 쌓일 수 있습니다.");

    private static final PortfolioAiAnalysisResponse KIM_MINJI = new PortfolioAiAnalysisResponse(
        "짧게라도 자신의 생각을 꾸준히 남기며 독서 기록을 이어가고 있습니다.",
        "친구들과 생각을 나누는 활동에 조금 더 참여해보면 다양한 관점을 넓히는 데 도움이 될 것입니다.");

    private static final PortfolioAiAnalysisResponse SEO_HEEWON = new PortfolioAiAnalysisResponse(
        "책을 끝까지 읽어내는 지속성이 뛰어나며, 여러 권의 책을 이어서 읽는 습관이 안정적으로 자리 잡았습니다.",
        "읽은 내용을 정리할 때 자신의 생각을 조금 더 구체적으로 표현해보는 연습을 더하면 좋겠습니다.");

    private static final PortfolioAiAnalysisResponse KIM_SUJIN = new PortfolioAiAnalysisResponse(
        "독서 참여와 생각 정리, 친구와의 공유까지 전반적으로 안정적인 독서 습관을 보이고 있습니다.",
        "지금의 습관을 유지하면서 다양한 주제의 책으로 읽기 범위를 넓혀보면 좋겠습니다.");

    private static final PortfolioAiAnalysisResponse LEE_HYEWON = new PortfolioAiAnalysisResponse(
        "친구들과 생각을 나누는 활동에서 강점을 보이며 다른 사람의 의견을 열린 태도로 받아들이고 있습니다.",
        "읽은 내용을 자신의 말로 다시 정리해보는 연습을 조금 더 이어가면 독서 기록이 한층 탄탄해질 것입니다.");

    private static final PortfolioAiAnalysisResponse DEFAULT = new PortfolioAiAnalysisResponse(
        "여러 권의 책을 이어서 읽으며 자신만의 독서 기록을 쌓아가고 있습니다.",
        "읽은 책마다 인상 깊었던 내용을 짧게라도 정리해보는 습관을 이어가면 좋겠습니다.");

    private static final Map<String, PortfolioAiAnalysisResponse> BY_LOGIN_ID = Map.ofEntries(
        Map.entry("ss01", KIM_CHORONG),
        Map.entry("ss02", SONG_MINJEONG), Map.entry("demo_student_02", SONG_MINJEONG),
        Map.entry("ss03", PARK_HAMIN), Map.entry("demo_student_03", PARK_HAMIN),
        Map.entry("ss04", LEE_JINWOO), Map.entry("demo_student_04", LEE_JINWOO),
        Map.entry("ss05", KIM_MINJI), Map.entry("demo_student_05", KIM_MINJI),
        Map.entry("ss06", SEO_HEEWON), Map.entry("demo_student_06", SEO_HEEWON),
        Map.entry("ss07", KIM_SUJIN), Map.entry("demo_student_07", KIM_SUJIN),
        Map.entry("ss08", LEE_HYEWON), Map.entry("demo_student_08", LEE_HYEWON)
    );

    public PortfolioAiAnalysisResponse forLoginId(String loginId) {
        return BY_LOGIN_ID.getOrDefault(loginId, DEFAULT);
    }
}
