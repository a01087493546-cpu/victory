package com.victory.service;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.victory.dto.PortfolioAiAnalysisResponse;
import com.victory.dto.PortfolioAiAnalysisResponse.StageAnalysis;
import com.victory.dto.PortfolioAiAnalysisResponse.StageAnalysisItem;

/*
 * 심사 온책읽기 포트폴리오 전용 학생별 고정 AI 종합의견 + 단계별 분석.
 * 심사계정은 원천 데이터가 얕아 실제 OpenAI 호출로는 억지스러운 결과가
 * 나올 수 있어, 학생마다 서로 다른 성격의 고정 예시 텍스트를 쓴다(랜덤
 * 없음). completed 여부/미완료 문구는 여기서 정하지 않는다 -
 * PortfolioAiAnalysisService.withAuthoritativeCompletion()이 실제
 * PracticeStageDetail.completed() 값으로 항상 최종 보정하므로, 이 클래스는
 * "완료됐다고 가정했을 때의 문장"만 책임진다.
 *
 * 각 단계는 strengthText 하나만으로 완결된 문단이고 growthText는 항상
 * 빈 문자열이다 - "차례도 함께 살펴보면..." 같은 별도 권유 문장을 점선
 * 구분선으로 나눠 보여주던 예전 UI를 없애면서, 그 문장을 growthText에
 * 남겨 둘 이유도 함께 사라졌다(프론트가 strengthText 하나만 표시).
 */
@Component
public class DemoPracticePortfolioAiProvider {

    private static final PortfolioAiAnalysisResponse KIM_CHORONG = response(
        "한 권의 책을 읽는 동안 활동에 꾸준히 참여하며 책의 흐름을 놓치지 않고 따라가는 모습이 나타났습니다. "
            + "읽은 내용을 자신의 생각과 연결해 정리하려는 태도도 안정적으로 이어지고 있습니다.",
        "앞으로는 책의 여러 장면을 서로 연결해 보며 작품 전체의 의미를 더 깊게 이해하는 연습을 이어가면 좋겠습니다.",
        stage("읽기 전",
            "표지와 제목을 살펴보며 이야기에 대한 궁금증을 자연스럽게 떠올렸습니다. 자신이 겪어본 일과 연결해 질문을 구체화하는 모습도 보입니다."),
        stage("읽기 중",
            "책을 읽는 동안 활동에 빠지지 않고 꾸준히 참여했습니다. 이야기 속 상황을 자신의 경험과 연결해 생각을 넓혀가고 있습니다."),
        stage("읽기 후",
            "읽은 내용을 자신의 말로 정리하며 이야기를 마무리했습니다. 친구들과 생각을 나누는 활동에도 자연스럽게 참여하고 있습니다."));

    private static final PortfolioAiAnalysisResponse SONG_MINJEONG = response(
        "책의 흐름을 잘 따라가며 중요한 내용을 놓치지 않고 정리하는 모습이 꾸준히 나타나고 있습니다.",
        "읽은 뒤 떠오른 생각을 그 순간에 그치지 않고 조금 더 오래 붙잡아 다듬어보는 습관을 들이면 좋겠습니다.",
        stage("읽기 전",
            "차례를 살펴보며 어떤 이야기가 이어질지 예상해보았습니다. 궁금한 점을 스스로 떠올리며 읽을 준비를 하는 모습이 나타났습니다."),
        stage("읽기 중",
            "이야기의 흐름을 놓치지 않고 따라가며 중요한 장면을 확인했습니다. 책 속 사건을 순서대로 이해하려는 태도가 나타났습니다."),
        stage("읽기 후",
            "읽은 내용 중 중요한 부분을 골라 정리했습니다. 이야기의 전체 흐름을 자신의 말로 되짚어보는 모습이 나타났습니다."));

    private static final PortfolioAiAnalysisResponse PARK_HAMIN = response(
        "책을 읽는 동안 장면 하나하나에 집중하며 그 의미를 파악하는 힘이 안정적으로 나타나고 있습니다.",
        "다 읽은 뒤에는 인상 깊었던 부분을 자신의 말로 정리해보는 습관을 조금씩 더해가면 좋겠습니다.",
        stage("읽기 전",
            "표지와 제목을 보며 이야기에 대한 궁금증을 자연스럽게 드러냈습니다. 짧지만 스스로 질문을 만들어보려는 시도가 나타났습니다."),
        stage("읽기 중",
            "장면 하나하나에 집중하며 그 의미를 파악하려는 태도가 안정적으로 이어졌습니다. 인물의 행동에 담긴 까닭을 짐작해보는 모습도 나타났습니다."),
        stage("읽기 후",
            "읽은 이야기에 대해 짧게라도 생각을 남겼습니다. 이야기를 끝까지 읽어냈다는 점도 꾸준히 확인됩니다."));

    private static final PortfolioAiAnalysisResponse LEE_JINWOO = response(
        "이해가 잘 안 되는 부분을 그냥 넘기지 않고 다시 확인하며 읽으려는 모습이 나타나고 있습니다.",
        "책을 읽기 시작할 때와 마무리할 때 모두 조금 더 꾸준히 활동에 참여해보면 독서 습관이 더 안정적으로 자리 잡을 것입니다.",
        stage("읽기 전",
            "제목을 보고 궁금한 점을 간단하게 떠올렸습니다. 짧은 문장이지만 스스로 질문을 만들어보는 모습이 나타났습니다."),
        stage("읽기 중",
            "이해가 잘 안 되는 부분을 다시 확인하며 읽으려는 모습이 나타났습니다."),
        stage("읽기 후",
            "이야기를 다 읽었다는 것을 짧게 표시했습니다."));

    private static final PortfolioAiAnalysisResponse KIM_MINJI = response(
        "책 내용을 어느 정도 안정적으로 이해하며 짧게라도 자신의 생각을 남기는 모습이 나타나고 있습니다.",
        "정리한 생각을 친구들과 나누는 활동을 조금 더 지속적으로 이어가면 좋겠습니다.",
        stage("읽기 전",
            "표지를 보며 이야기 내용을 짐작해보는 모습이 나타났습니다."),
        stage("읽기 중",
            "책 내용을 어느 정도 안정적으로 이해하며 읽어갔습니다. 중요한 장면에서는 속도를 늦춰 다시 확인하기도 했습니다."),
        stage("읽기 후",
            "읽은 내용에 대해 짧게 생각을 남겼습니다."));

    private static final PortfolioAiAnalysisResponse SEO_HEEWON = response(
        "한 권의 책을 끝까지 놓치지 않고 읽어가는 지속성이 돋보이며, 친구의 생각도 열린 태도로 받아들이고 있습니다.",
        "자신의 생각을 표현할 때 좀 더 구체적인 예를 들어보는 연습을 더하면 좋겠습니다.",
        stage("읽기 전",
            "제목과 그림을 살펴보며 이야기에 대한 기대를 나타내는 모습이 보였습니다."),
        stage("읽기 중",
            "책을 읽어가는 흐름을 끝까지 놓치지 않고 이어갔습니다. 다음 이야기를 예측해보려는 시도도 나타났습니다."),
        stage("읽기 후",
            "친구의 생각을 살펴보고 자연스럽게 받아들였습니다. 자신의 생각도 짧게나마 함께 남기는 모습이 나타났습니다."));

    private static final PortfolioAiAnalysisResponse KIM_SUJIN = response(
        "책을 읽고 생각을 정리하며 친구들과 나누는 활동까지 전반적으로 안정적인 독서 습관을 보이고 있습니다.",
        "지금의 습관을 유지하면서 다양한 주제나 장르의 책으로 읽기 범위를 넓혀보면 좋겠습니다.",
        stage("읽기 전",
            "제목, 그림, 차례를 두루 살펴보며 이야기를 다양하게 예상해보는 모습이 나타났습니다."),
        stage("읽기 중",
            "여러 인물과 사건을 서로 연결하며 생각을 넓혀갔습니다. 근거를 들어 자신의 생각을 설명하는 모습도 나타났습니다."),
        stage("읽기 후",
            "읽은 내용을 정리하고 친구들과 생각을 나누는 활동에 적극적으로 참여했습니다."));

    private static final PortfolioAiAnalysisResponse LEE_HYEWON = response(
        "쉬운 내용부터 차근히 이해하며 책 읽기를 꾸준히 이어가려는 모습이 나타나고 있습니다.",
        "읽기 전, 중, 후 활동을 서로 연결해가며 꾸준한 독서 습관을 만들어가면 좋겠습니다.",
        stage("읽기 전",
            "제목을 보고 짧게라도 궁금한 점을 떠올렸습니다."),
        stage("읽기 중",
            "쉬운 내용부터 차근히 따라가며 이해하려는 모습이 나타났습니다."),
        stage("읽기 후",
            "이야기를 끝까지 읽었다는 기록을 남겼습니다."));

    private static final PortfolioAiAnalysisResponse DEFAULT = response(
        "책 읽기 활동에 참여하며 이야기 내용을 따라가려는 모습이 나타나고 있습니다.",
        "읽기 전, 중, 후 활동에 조금씩 더 꾸준히 참여해보면 독서 습관이 안정적으로 자리 잡을 것입니다.",
        stage("읽기 전", "책에 대한 궁금증을 스스로 떠올려보는 모습이 나타났습니다."),
        stage("읽기 중", "이야기 내용을 이해하며 읽어가는 모습이 나타났습니다."),
        stage("읽기 후", "읽은 내용을 짧게 정리하는 모습이 나타났습니다."));

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

    private static PortfolioAiAnalysisResponse response(
            String strengthText, String improvementText,
            StageAnalysisItem before, StageAnalysisItem during, StageAnalysisItem after) {
        return new PortfolioAiAnalysisResponse(strengthText, improvementText,
            new StageAnalysis(before, during, after));
    }

    private static StageAnalysisItem stage(String title, String strengthText) {
        return new StageAnalysisItem(title, true, strengthText, "");
    }
}
