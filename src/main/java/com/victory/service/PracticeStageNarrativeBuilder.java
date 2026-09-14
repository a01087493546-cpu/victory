package com.victory.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.victory.dto.PracticeStageDetail;
import com.victory.entity.Response;
import com.victory.entity.Summary;

/*
 * 온책읽기 성장 포트폴리오의 읽기 전/중/후 단계 설명을 학생이 실제로
 * 남긴 질문/답/간추리기 텍스트에서만 만든다(추측·랜덤 없음, AI도
 * 호출하지 않는다 - 그래서 aggregate endpoint에서 항상 즉시/결정적으로
 * 쓸 수 있다). 데이터가 없으면 "현재 기록에서는..." 식으로만 말하고,
 * 있으면 실제 텍스트 일부를 인용해 단순 "완료했습니다" 반복을 피한다.
 *
 * 읽기 전/중 growthNote는 어떤 단서·유형을 실제로 활용했는지도 반영한다
 * (extra_data.stepType: title/contents/picture/skim, extra_data.questionType:
 * direct/opinion/connect/infer - ResponseService.savePreReadingResponse /
 * saveDuringPracticeResponse / saveDuringReviewResponse / saveBookThoughtResponse
 * 참고). 이 메타데이터가 없는 옛 데이터/테스트 픽스처는 단서 언급 없이
 * 기존 문구로 자연스럽게 대체된다(하위 호환).
 */
@Component
public class PracticeStageNarrativeBuilder {

    private static final int SNIPPET_MAX_LENGTH = 50;

    private static final List<String> BEFORE_CLUE_ORDER = List.of("title", "contents", "picture", "skim");
    private static final Map<String, String> BEFORE_CLUE_LABELS = Map.of(
        "title", "제목", "contents", "차례", "picture", "그림", "skim", "글");

    private static final List<String> DURING_TYPE_ORDER = List.of("direct", "opinion", "connect", "infer");
    private static final Map<String, String> DURING_TYPE_LABELS = Map.of(
        "direct", "직접 답하기", "opinion", "까닭 정리", "connect", "비교·연결", "infer", "예측·추론");

    public PracticeStageDetail buildBeforeStage(List<Response> beforeResponses) {
        Response representative = pickRepresentativeAnswer(beforeResponses);
        String question = representative == null ? null : stringField(representative, "question");
        String answer = representative == null ? null : blankToNull(representative.getContent());
        boolean participated = !beforeResponses.isEmpty();
        List<String> clues = usedLabels(beforeResponses, "stepType", BEFORE_CLUE_ORDER, BEFORE_CLUE_LABELS);

        String shortTitle = participated
            ? (question != null ? "질문 만들기에 참여함" : "읽기 전 활동에 참여함")
            : "아직 참여 기록 없음";
        String representativeText = representativeText(question, answer);
        String growthNote = beforeGrowthNote(participated, clues, question, answer);

        return new PracticeStageDetail(participated, participated, shortTitle, growthNote, representativeText);
    }

    public PracticeStageDetail buildDuringStage(List<Response> duringResponses) {
        Response representative = pickRepresentativeAnswer(duringResponses);
        String question = representative == null ? null : stringField(representative, "question");
        String answer = representative == null ? null : blankToNull(representative.getContent());
        boolean participated = !duringResponses.isEmpty();
        List<String> types = usedLabels(duringResponses, "questionType", DURING_TYPE_ORDER, DURING_TYPE_LABELS);

        String shortTitle = participated
            ? (answer != null ? "근거를 들어 답함" : "읽기 중 활동에 참여함")
            : "아직 참여 기록 없음";
        String representativeText = representativeText(question, answer);
        String growthNote = duringGrowthNote(participated, types, question, answer);

        return new PracticeStageDetail(participated, participated, shortTitle, growthNote, representativeText);
    }

    private String beforeGrowthNote(boolean participated, List<String> clues, String question, String answer) {
        if (!participated) {
            return "현재 기록에서는 이 단계 활동 기록이 아직 없어요.";
        }
        String cluePhrase = clues.isEmpty()
            ? "제목·차례·그림 같은 단서로"
            : String.join("·", clues) + " 단서로";
        if (answer != null) {
            return cluePhrase + " 스스로 질문을 만들고, \"" + snippet(answer) + "\"처럼 답을 남겼어요.";
        }
        if (question != null) {
            return cluePhrase + " \"" + snippet(question) + "\"라는 질문을 스스로 만들었어요.";
        }
        return "현재 기록에서는 참여만 표시되어 있고 구체적인 글 내용은 남아 있지 않아요.";
    }

    private String duringGrowthNote(boolean participated, List<String> types, String question, String answer) {
        if (!participated) {
            return "현재 기록에서는 이 단계 활동 기록이 아직 없어요.";
        }
        String actionPhrase = types.isEmpty()
            ? "책 내용을 근거로 짐작하거나 비교하며 생각을 정리하고"
            : String.join(", ", types) + " 활동으로 책 내용을 근거로 생각을 정리하고";
        if (answer != null) {
            return "\"" + snippet(answer) + "\"처럼 " + actionPhrase + " 답을 남겼어요.";
        }
        if (question != null) {
            return "\"" + snippet(question) + "\"라는 질문을 스스로 만들었어요.";
        }
        return "현재 기록에서는 참여만 표시되어 있고 구체적인 글 내용은 남아 있지 않아요.";
    }

    /*
     * order에 나열된 각 키(stepType/questionType 값)에 대해, 실제로 내용
     * 있는(content 또는 question이 비어있지 않은) 응답이 하나라도 있으면
     * 그 라벨을 결과에 포함한다. order 순서를 그대로 유지해 "제목, 차례,
     * 그림" 처럼 사람이 읽기 자연스러운 순서로 나열되게 한다.
     */
    private List<String> usedLabels(
            List<Response> responses, String extraKey, List<String> order, Map<String, String> labels) {
        List<String> present = new ArrayList<>();
        for (String key : order) {
            boolean found = responses.stream().anyMatch(r ->
                key.equals(stringField(r, extraKey))
                    && (blankToNull(r.getContent()) != null || stringField(r, "question") != null));
            if (found) {
                present.add(labels.get(key));
            }
        }
        return present;
    }

    public PracticeStageDetail buildAfterStage(List<Response> afterResponses, List<Summary> afterSummaries) {
        Summary summary = afterSummaries.isEmpty() ? null : afterSummaries.get(0);
        String summaryText = summary == null ? null : blankToNull(summary.getSummaryText());
        boolean shared = summary != null && Boolean.TRUE.equals(summary.getIsShared());
        Response representative = pickRepresentativeAnswer(afterResponses);
        String question = representative == null ? null : stringField(representative, "question");
        String answer = representative == null ? null : blankToNull(representative.getContent());
        boolean participated = !afterResponses.isEmpty() || summaryText != null;

        String shortTitle;
        if (summaryText != null) {
            shortTitle = shared ? "간추리기와 공유 활동 참여" : "간추리기 활동 참여";
        } else if (participated) {
            shortTitle = "읽기 후 활동에 참여함";
        } else {
            shortTitle = "아직 참여 기록 없음";
        }

        String representativeText = summaryText != null ? snippet(summaryText) : representativeText(question, answer);

        String growthNote;
        if (summaryText != null) {
            growthNote = shared
                ? "\"" + snippet(summaryText) + "\"처럼 책 내용을 자기 말로 간추려 친구들과 나눴어요."
                : "\"" + snippet(summaryText) + "\"처럼 책 내용을 자기 말로 간추려 기록했어요.";
        } else {
            growthNote = qaGrowthNote(participated, question, answer, "읽은 내용을 자기 말로 정리하고");
        }

        return new PracticeStageDetail(participated, participated, shortTitle, growthNote, representativeText);
    }

    private Response pickRepresentativeAnswer(List<Response> responses) {
        return responses.stream()
            .filter(r -> blankToNull(r.getContent()) != null)
            .max(Comparator.comparingInt(r -> r.getContent().length()))
            .orElse(null);
    }

    private String representativeText(String question, String answer) {
        if (answer != null) {
            return snippet(answer);
        }
        if (question != null) {
            return snippet(question);
        }
        return null;
    }

    private String qaGrowthNote(boolean participated, String question, String answer, String actionPhrase) {
        if (!participated) {
            return "현재 기록에서는 이 단계 활동 기록이 아직 없어요.";
        }
        if (answer != null) {
            return "\"" + snippet(answer) + "\"처럼 " + actionPhrase + " 답을 남겼어요.";
        }
        if (question != null) {
            return "\"" + snippet(question) + "\"라는 질문을 스스로 만들었어요.";
        }
        return "현재 기록에서는 참여만 표시되어 있고 구체적인 글 내용은 남아 있지 않아요.";
    }

    private String stringField(Response response, String key) {
        if (response.getExtraData() == null) return null;
        Object value = response.getExtraData().get(key);
        return value == null ? null : blankToNull(String.valueOf(value));
    }

    private String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String snippet(String text) {
        String trimmed = text.trim();
        if (trimmed.length() <= SNIPPET_MAX_LENGTH) {
            return trimmed;
        }
        return trimmed.substring(0, SNIPPET_MAX_LENGTH).trim() + "…";
    }
}
