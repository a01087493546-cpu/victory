package com.victory.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.victory.dto.PracticeStageDetail;
import com.victory.entity.Response;
import com.victory.entity.Summary;

class PracticeStageNarrativeBuilderTest {
    private final PracticeStageNarrativeBuilder builder = new PracticeStageNarrativeBuilder();

    private Response response(String content, String question) {
        Response r = new Response();
        r.setContent(content);
        if (question != null) {
            r.setExtraData(Map.of("question", question));
        }
        return r;
    }

    private Response beforeStepResponse(String stepType, String question, String answer) {
        Response r = new Response();
        r.setContent(answer);
        r.setExtraData(Map.of("stepType", stepType, "question", question == null ? "" : question));
        return r;
    }

    private Response duringTypeResponse(String questionType, String question, String answer) {
        Response r = new Response();
        r.setContent(answer);
        r.setExtraData(Map.of("questionType", questionType, "question", question == null ? "" : question));
        return r;
    }

    @Test
    void buildBeforeStage_noResponses_returnsNotParticipated() {
        PracticeStageDetail detail = builder.buildBeforeStage(List.of());

        assertThat(detail.participated()).isFalse();
        assertThat(detail.completed()).isFalse();
        assertThat(detail.shortTitle()).isEqualTo("아직 참여 기록 없음");
        assertThat(detail.growthNote()).isEqualTo("현재 기록에서는 이 단계 활동 기록이 아직 없어요.");
        assertThat(detail.representativeText()).isNull();
    }

    @Test
    void buildBeforeStage_onlyQuestionNoAnswer_stillParticipatedButNoRepresentativeText() {
        Response noAnswer = response(null, "표지 그림을 보면 어떤 이야기일까?");

        PracticeStageDetail detail = builder.buildBeforeStage(List.of(noAnswer));

        assertThat(detail.participated()).isTrue();
        assertThat(detail.shortTitle()).isEqualTo("읽기 전 활동에 참여함");
        assertThat(detail.representativeText()).isNull();
    }

    @Test
    void buildBeforeStage_withAnswerText_quotesSnippetAndSetsShortTitle() {
        Response answered = response("까마귀는 왜 그랬을까?", "제목을 보고 궁금한 점은?");

        PracticeStageDetail detail = builder.buildBeforeStage(List.of(answered));

        assertThat(detail.participated()).isTrue();
        assertThat(detail.completed()).isTrue();
        assertThat(detail.shortTitle()).isEqualTo("질문 만들기에 참여함");
        assertThat(detail.growthNote()).contains("까마귀는 왜 그랬을까?").contains("답을 남겼어요");
        assertThat(detail.representativeText()).isEqualTo("까마귀는 왜 그랬을까?");
    }

    @Test
    void buildBeforeStage_longAnswer_truncatesSnippetWithEllipsis() {
        String longAnswer = "가".repeat(60);
        Response answered = response(longAnswer, null);

        PracticeStageDetail detail = builder.buildBeforeStage(List.of(answered));

        assertThat(detail.representativeText()).hasSize(51).endsWith("…");
        assertThat(detail.growthNote()).contains("…");
    }

    @Test
    void buildDuringStage_withAnswer_usesDuringSpecificShortTitleAndPhrase() {
        Response answered = response("주인공이 용감해서요", "왜 그렇게 생각해?");

        PracticeStageDetail detail = builder.buildDuringStage(List.of(answered));

        assertThat(detail.shortTitle()).isEqualTo("근거를 들어 답함");
        assertThat(detail.growthNote()).contains("주인공이 용감해서요").contains("근거로 짐작하거나 비교하며");
    }

    @Test
    void buildDuringStage_noParticipation_returnsNotParticipated() {
        PracticeStageDetail detail = builder.buildDuringStage(List.of());

        assertThat(detail.participated()).isFalse();
        assertThat(detail.shortTitle()).isEqualTo("아직 참여 기록 없음");
    }

    @Test
    void buildAfterStage_withSharedSummary_mentionsSharingInShortTitleAndNote() {
        Summary summary = new Summary();
        summary.setSummaryText("이 책은 우정의 소중함을 알려줘요");
        summary.setIsShared(true);

        PracticeStageDetail detail = builder.buildAfterStage(List.of(), List.of(summary));

        assertThat(detail.participated()).isTrue();
        assertThat(detail.shortTitle()).isEqualTo("간추리기와 공유 활동 참여");
        assertThat(detail.growthNote()).contains("이 책은 우정의 소중함을 알려줘요").contains("나눴어요");
        assertThat(detail.representativeText()).isEqualTo("이 책은 우정의 소중함을 알려줘요");
    }

    @Test
    void buildAfterStage_withUnsharedSummary_doesNotMentionSharing() {
        Summary summary = new Summary();
        summary.setSummaryText("주인공은 끝까지 포기하지 않았어요");
        summary.setIsShared(false);

        PracticeStageDetail detail = builder.buildAfterStage(List.of(), List.of(summary));

        assertThat(detail.shortTitle()).isEqualTo("간추리기 활동 참여");
        assertThat(detail.growthNote()).contains("주인공은 끝까지 포기하지 않았어요").doesNotContain("나눴어요");
    }

    @Test
    void buildAfterStage_noSummaryButHasResponses_fallsBackToResponseText() {
        Response answered = response("친구에게 이 책을 추천하고 싶어요", null);

        PracticeStageDetail detail = builder.buildAfterStage(List.of(answered), List.of());

        assertThat(detail.participated()).isTrue();
        assertThat(detail.shortTitle()).isEqualTo("읽기 후 활동에 참여함");
        assertThat(detail.growthNote()).contains("친구에게 이 책을 추천하고 싶어요");
    }

    @Test
    void buildAfterStage_noDataAtAll_returnsLimitedNotParticipatedNote() {
        PracticeStageDetail detail = builder.buildAfterStage(List.of(), List.of());

        assertThat(detail.participated()).isFalse();
        assertThat(detail.shortTitle()).isEqualTo("아직 참여 기록 없음");
        assertThat(detail.growthNote()).isEqualTo("현재 기록에서는 이 단계 활동 기록이 아직 없어요.");
        assertThat(detail.representativeText()).isNull();
    }

    @Test
    void buildBeforeStage_participatedButBlankContentAndNoQuestion_fallsBackToParticipationOnlyNote() {
        Response skipped = response("", null);

        PracticeStageDetail detail = builder.buildBeforeStage(List.of(skipped));

        assertThat(detail.participated()).isTrue();
        assertThat(detail.growthNote())
            .isEqualTo("현재 기록에서는 참여만 표시되어 있고 구체적인 글 내용은 남아 있지 않아요.");
        assertThat(detail.representativeText()).isNull();
    }

    @Test
    void buildBeforeStage_picksLongestAnswerAsRepresentative() {
        Response shorter = response("좋아요", null);
        Response longer = response("까마귀가 알을 품어주는 장면이 감동적이었어요", null);

        PracticeStageDetail detail = builder.buildBeforeStage(List.of(shorter, longer));

        assertThat(detail.representativeText()).isEqualTo("까마귀가 알을 품어주는 장면이 감동적이었어요");
    }

    @Test
    void buildBeforeStage_multipleClueTypes_namesEachClueUsedInOrder() {
        Response titleClue = beforeStepResponse("title", "표지를 보니 무슨 내용일까?", "까마귀 이야기 같아요");
        Response pictureClue = beforeStepResponse("picture", "그림 속 새는 왜 슬퍼 보일까?", "친구를 잃어서 슬퍼 보여요");

        PracticeStageDetail detail = builder.buildBeforeStage(List.of(titleClue, pictureClue));

        assertThat(detail.growthNote()).startsWith("제목·그림 단서로");
        assertThat(detail.growthNote()).contains("답을 남겼어요");
    }

    @Test
    void buildBeforeStage_singleClueType_namesThatOneClue() {
        Response contentsClue = beforeStepResponse("contents", "차례를 보니 몇 개의 이야기가 있을까?", "다섯 개 정도인 것 같아요");

        PracticeStageDetail detail = builder.buildBeforeStage(List.of(contentsClue));

        assertThat(detail.growthNote()).startsWith("차례 단서로");
    }

    @Test
    void buildBeforeStage_differentStudentsWithDifferentAnswers_produceDifferentGrowthNotes() {
        Response studentA = beforeStepResponse("title", "표지를 보니 무슨 내용일까?", "까마귀 이야기 같아요");
        Response studentB = beforeStepResponse("skim", "첫 문장은 무슨 뜻일까?", "겨울이 시작된다는 뜻 같아요");

        PracticeStageDetail detailA = builder.buildBeforeStage(List.of(studentA));
        PracticeStageDetail detailB = builder.buildBeforeStage(List.of(studentB));

        assertThat(detailA.growthNote()).isNotEqualTo(detailB.growthNote());
        assertThat(detailA.growthNote()).contains("까마귀 이야기 같아요");
        assertThat(detailB.growthNote()).contains("겨울이 시작된다는 뜻 같아요");
    }

    @Test
    void buildAfterStage_differentStudentsWithDifferentSummaries_produceDifferentGrowthNotes() {
        Summary summaryA = new Summary();
        summaryA.setSummaryText("까마귀가 알을 정성껏 품어줬어요");
        summaryA.setIsShared(true);
        Summary summaryB = new Summary();
        summaryB.setSummaryText("주인공은 끝까지 포기하지 않았어요");
        summaryB.setIsShared(false);

        PracticeStageDetail detailA = builder.buildAfterStage(List.of(), List.of(summaryA));
        PracticeStageDetail detailB = builder.buildAfterStage(List.of(), List.of(summaryB));

        assertThat(detailA.growthNote()).isNotEqualTo(detailB.growthNote());
        assertThat(detailA.growthNote()).contains("까마귀가 알을 정성껏 품어줬어요");
        assertThat(detailB.growthNote()).contains("주인공은 끝까지 포기하지 않았어요");
    }

    @Test
    void buildDuringStage_multipleQuestionTypes_namesEachTypeUsedInOrder() {
        Response directType = duringTypeResponse("direct", "주인공은 누구인가요?", "까마귀예요");
        Response connectType = duringTypeResponse("connect", "비슷한 경험이 있나요?", "저도 친구와 다툰 적이 있어요");

        PracticeStageDetail detail = builder.buildDuringStage(List.of(directType, connectType));

        assertThat(detail.growthNote()).contains("직접 답하기, 비교·연결 활동으로");
        assertThat(detail.growthNote()).contains("답을 남겼어요");
    }

    @Test
    void buildDuringStage_differentStudentsWithDifferentTypes_produceDifferentGrowthNotes() {
        Response studentA = duringTypeResponse("opinion", "왜 그렇게 생각해?", "주인공이 용감해서요");
        Response studentB = duringTypeResponse("infer", "다음에 무슨 일이 벌어질까?", "친구를 구하러 갈 것 같아요");

        PracticeStageDetail detailA = builder.buildDuringStage(List.of(studentA));
        PracticeStageDetail detailB = builder.buildDuringStage(List.of(studentB));

        assertThat(detailA.growthNote()).isNotEqualTo(detailB.growthNote());
        assertThat(detailA.growthNote()).contains("까닭 정리 활동으로");
        assertThat(detailB.growthNote()).contains("예측·추론 활동으로");
    }
}
