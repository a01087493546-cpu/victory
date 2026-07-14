package com.victory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.victory.dto.AiFeedbackRequest;
import com.victory.dto.AiFeedbackResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class FeedbackAiService {

    private static final String OPENAI_CHAT_COMPLETIONS_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-4o-mini";

    private static final String SUMMARY_TYPE_KEYWORD = "summary";

    // 질문/답 평가용 프롬프트. summaryText 기반 요약 평가에는 사용하지 않는다.
    private static final String SYSTEM_PROMPT_QUESTION = """
            너는 초등학생 대상 독서 활동 앱의 캐릭터 '루미'야.
            학생이 만든 질문이 책 유형에 맞는 방식으로 작성되었는지 평가해.

            책 유형별 좋은 질문 기준:
            - 이야기 책: 시간의 흐름이나 장소의 변화에 따라 등장인물에게
              일어난 사건(행동, 대화, 겪은 일, 문제와 해결 과정 포함)을
              다루면 통과 기준에 맞아. '제안했다', '말했다', '부탁했다' 등의
              표현도 인물의 행동(사건)에 해당해. 감정이나 개인적 느낌 자체만
              다룰 때만 기준에 안 맞는 것으로 봐. 애매하면 항상 기준에
              맞는 것으로 판단해.
            - 정보를 담은 책: 문단이나 각 장의 중심 내용(정보)에 대한 질문이어야 함
            - 주장을 담은 책: 글쓴이의 주장과 그 이유에 대한 질문이어야 함

            책 유형 정보가 비어 있거나 알 수 없을 때는(책 유형: (특정되지
            않음)), 질문이 이야기(사건)/정보(지식)/주장(의견) 중 어느
            하나의 기준에라도 논리적으로 맞으면 통과 기준에 맞는 것으로
            판단해. 세 기준을 모두 만족할 필요는 없어.

            답변은 정답을 채점하는 게 아니라, 질문에 대한 논리적인 생각으로
            이어지는지만 확인해. 답에 특별한 오류가 없다면 통과시켜.

            질문이 위에서 설명한 책 유형별 기준(이야기 책의 경우 행동/대화/
            제안/부탁 포함)에 맞고 답이 질문과 논리적으로 연결되면 반드시
            good으로 판정해. 위 기준에 명백히 안 맞을 때만 need로 판정해.
            애매하면 항상 good으로 판단해.

            반드시 아래 JSON 형식으로만 답해:
            {"status": "good" 또는 "need", "message": "피드백"}

            message 작성 규칙:
            - status가 good이면 한 문장으로 짧게 격려만 해줘.
              예: '질문이 정확해! 다음으로 넘어가자.'
            - status가 need면 두 문장 이내로 핵심만 짚어줘.
              무엇이 문제인지 + 어떻게 고치면 좋을지만.
              예시(참고용, 실제 상황에 맞게 자유롭게 작성):
              - '질문이 느낌이나 의견을 묻고 있어. 무슨 일이 있었는지로
                바꿔볼까?'
              - '질문이 이 책 유형과 안 맞아. 다른 방향으로 질문해볼까?'
              - '질문이 너무 짧거나 불명확해. 조금 더 구체적으로 써볼까?'
              실제 이유에 맞는 문장을 새로 만들어서 써도 되고, 위 예시를
              그대로 따라 하지 않아도 돼.
            - 절대 3문장 이상 쓰지 마.
            """;

    // 요약(간추리기) 평가 전용 프롬프트. "질문"이라는 단어를 아예 쓰지 않는다.
    private static final String SYSTEM_PROMPT_SUMMARY = """
            너는 초등학생 대상 독서 활동 앱의 캐릭터 '루미'야.
            학생이 책을 읽고 쓴 간추리기(요약문)를 평가해.

            책 유형별로 간추리기에 꼭 담겨야 할 핵심 요소:
            - 이야기 책: 시간의 흐름이나 장소의 변화에 따른 사건의 흐름
            - 정보를 담은 책: 문단이나 장의 중심 내용(정보)
            - 주장을 담은 책: 글쓴이의 주장과 그 이유

            간추리기가 앞뒤가 통하게 정리되어 있고 책 유형에 맞는 핵심
            요소가 담겨 있으면 반드시 good으로 판정해. 핵심 요소가
            빠져 있거나 내용이 너무 짧아 알아보기 어려울 때만 need로
            판정해. 애매하면 항상 good으로 판단해.

            반드시 아래 JSON 형식으로만 답해:
            {"status": "good" 또는 "need", "message": "피드백"}

            message 작성 규칙:
            - status가 good이면 한 문장으로 짧게 격려만 해줘.
              예: '간추리기가 잘 정리됐어! 다음으로 넘어가자.'
            - status가 need면 두 문장 이내로 핵심만 짚어줘.
              무엇이 부족한지 + 어떻게 보완하면 좋을지만.
              예: '중요한 사건이 빠진 것 같아. 처음, 가운데, 마지막에
              있었던 일을 조금 더 자세히 써볼까?'
            - 절대 3문장 이상 쓰지 마.
            """;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openai.api.key}")
    private String openaiApiKey;

    public AiFeedbackResponse getFeedback(AiFeedbackRequest request) {
        try {
            Map<String, Object> requestBody = buildRequestBody(request);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            Map<?, ?> response = restTemplate.postForObject(OPENAI_CHAT_COMPLETIONS_URL, entity, Map.class);

            String content = extractContent(response);
            return objectMapper.readValue(content, AiFeedbackResponse.class);
        } catch (Exception e) {
            log.error("AI 피드백 생성 실패", e);
            return new AiFeedbackResponse("need", "지금은 피드백을 확인할 수 없어, 잠시 후 다시 시도해줘");
        }
    }

    private Map<String, Object> buildRequestBody(AiFeedbackRequest request) {
        boolean isSummary = isSummaryType(request);

        Map<String, Object> systemMessage = new LinkedHashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", isSummary ? SYSTEM_PROMPT_SUMMARY : SYSTEM_PROMPT_QUESTION);

        Map<String, Object> userMessage = new LinkedHashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", isSummary ? buildSummaryUserContent(request) : buildQuestionUserContent(request));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", MODEL);
        body.put("messages", List.of(systemMessage, userMessage));
        body.put("response_format", Map.of("type", "json_object"));
        body.put("temperature", 0.2);
        body.put("seed", 42);
        return body;
    }

    private boolean isSummaryType(AiFeedbackRequest request) {
        String type = request.getType();
        return type != null && type.toLowerCase().contains(SUMMARY_TYPE_KEYWORD);
    }

    private String buildQuestionUserContent(AiFeedbackRequest request) {
        StringBuilder sb = new StringBuilder();
        String bookType = request.getBookType();
        String bookTypeDisplay = (bookType == null || bookType.isBlank())
                ? "(특정되지 않음)" : bookType;

        sb.append("활동 유형: ").append(request.getType()).append("\n");
        sb.append("책 유형: ").append(bookTypeDisplay).append("\n");
        sb.append("질문과 답변 목록:\n");

        List<AiFeedbackRequest.QAItem> qaList = request.getQaList();
        if (qaList != null) {
            for (int i = 0; i < qaList.size(); i++) {
                AiFeedbackRequest.QAItem qa = qaList.get(i);
                sb.append(i + 1).append(". 질문: ").append(qa.getQuestion())
                        .append(" / 답변: ").append(qa.getAnswer()).append("\n");
            }
        }

        return sb.toString();
    }

    private String buildSummaryUserContent(AiFeedbackRequest request) {
        return "다음은 학생이 책을 읽고 쓴 간추리기(요약문)입니다.\n\n"
                + "[책 유형]\n" + request.getBookType() + "\n\n"
                + "[간추리기 내용]\n" + request.getSummaryText() + "\n\n"
                + "이 요약이 책 유형 기준에 맞게 사건의 흐름, 중심 내용, 또는 주장과 이유를 "
                + "잘 담고 있는지 평가해 주세요. 이것은 질문이 아니라 완성된 요약 문단입니다.";
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<?, ?> response) {
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> firstChoice = choices.get(0);
        Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
        return (String) message.get("content");
    }
}
