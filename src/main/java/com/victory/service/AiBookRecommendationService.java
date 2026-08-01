package com.victory.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.victory.dto.AiBookRecommendationItem;
import com.victory.dto.AiBookRecommendationRequest;
import com.victory.dto.AiBookRecommendationResponse;
import com.victory.dto.AiBookSelectionItem;
import com.victory.dto.AiBookSelectionResponse;
import com.victory.dto.RecommendationBookItem;
import com.victory.dto.RecommendationCandidateItem;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/*
 * 학생 취향 6개 + 태그 점수 후보를 OpenAI에 전달해 정확히 3권을 고르게 하는
 * 전용 서비스. FeedbackAiService(질문/간추리기 평가)와는 완전히 분리된
 * 프롬프트·DTO를 쓴다.
 *
 * AI가 후보에 없는 책을 지어내거나(hallucination) 잘못된 형식으로 응답할 수
 * 있다는 전제로, bookId 기준으로만 DB 후보와 다시 연결하고 title/author/
 * description 등 AI가 되돌려준 다른 필드는 전부 무시한다.
 *
 * "다른 책 추천받기"를 눌러 excludedBookIds(이전에 이미 보여준 책)가 오면,
 * 그 책들을 새 후보 풀과 AI 프롬프트에서 완전히 제외한다. 다만 제외 후 남은
 * 책이 3권보다 적을 때만 예외적으로 이전 책을 점수 순으로 재사용해 3권을
 * 채운다(요청 사항: "후보가 부족해 재사용한 경우에도 오류로 처리하지 않음").
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiBookRecommendationService {

    private static final String OPENAI_CHAT_COMPLETIONS_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-4o-mini";
    private static final int RECOMMENDATION_COUNT = 3;
    private static final int CANDIDATE_LIMIT = 10;
    private static final int MAX_REASON_LENGTH = 150;
    private static final int MAX_EXCLUDED_BOOK_IDS = 30;

    private static final String SYSTEM_PROMPT = """
            너는 초등학교 4학년 어린이에게 책을 추천하는 독서 도우미야.

            아래 규칙을 반드시 지켜:
            1. 반드시 제공된 후보 도서 목록 안에서만 책을 선택해.
            2. 후보 목록에 없는 책을 새로 만들거나 언급하지 마.
            3. 각 후보의 bookId를 정확하게 그대로 사용해.
            4. 가능하면 서로 다른 특징을 가진 책 3권을 골라.
            5. 학생이 고른 6개 취향(두께, 분위기, 장르, 그림 비중, 난이도,
               읽는 목적)을 가장 중요하게 고려해.
            6. 후보에 있는 매치 점수(matchScore)도 참고해.
            7. 학생 취향과 크게 어긋나거나 지나치게 어려운 책은 피해.
            8. 추천 이유는 책마다 1~2문장으로 짧게 써.
            9. 초등학교 4학년이 이해할 수 있는 쉬운 한국어 문장을 사용해.
            10. 추천 이유에서 책의 결말이나 중요한 반전을 알려주지 마.
            11. "AI", "점수", "데이터", "알고리즘" 같은 표현을 추천 이유에 쓰지 마.
            12. 정확히 3권을 선택해(후보가 3권보다 적으면 있는 만큼만 선택해).
            13. 다른 설명 없이 반드시 아래 JSON 형식으로만 응답해:
            {"selections": [{"bookId": 13, "recommendationReason": "..."}, {"bookId": 18, "recommendationReason": "..."}, {"bookId": 23, "recommendationReason": "..."}]}
            """;

    private final RecommendationBookService recommendationBookService;
    /*
     * 패키지 접근(빈 default 접근자)으로 열어 두어 같은 패키지의 단위 테스트가
     * 실제 OpenAI 네트워크 호출 없이 RestTemplate을 모킹해 "AI 호출 실패"
     * 경로를 재현할 수 있게 한다(운영 코드에서는 buildRestTemplate()로만 생성됨).
     */
    RestTemplate restTemplate = buildRestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openai.api.key}")
    private String openaiApiKey;

    private static RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(20_000);
        return new RestTemplate(factory);
    }

    public AiBookRecommendationResponse recommend(AiBookRecommendationRequest request) {
        List<RecommendationCandidateItem> allScored = recommendationBookService.getAllScoredActiveBooksByRawPreference(
            request.getThickness(),
            request.getMood(),
            request.getGenre(),
            request.getIllustrationLevel(),
            request.getDifficulty(),
            request.getPurpose()
        );

        if (allScored.isEmpty()) {
            return new AiBookRecommendationResponse(List.of(), false, false);
        }

        Set<Long> excludedIds = sanitizeExcludedBookIds(request.getExcludedBookIds());

        List<RecommendationCandidateItem> freshCandidates = allScored.stream()
            .filter(candidate -> !excludedIds.contains(candidate.getBook().getId()))
            .limit(CANDIDATE_LIMIT)
            .toList();

        List<RecommendationCandidateItem> reusePool = allScored.stream()
            .filter(candidate -> excludedIds.contains(candidate.getBook().getId()))
            .toList();

        AiBookRecommendationResponse primary = buildPrimaryResponse(freshCandidates, request);

        return topUpFromReusePoolIfNeeded(primary, reusePool, request);
    }

    /*
     * excludedBookIds 정리: null → 빈 목록, null 원소/0 이하 값 제거, 중복
     * 제거, 최대 30개까지만 허용. recommendation_books에 실제로 없는 ID는
     * 여기서 걸러낼 필요가 없다 - 어차피 allScored의 bookId와 대조해서만
     * 쓰이므로 자동으로 무시된다.
     */
    Set<Long> sanitizeExcludedBookIds(List<Long> rawExcludedBookIds) {
        if (rawExcludedBookIds == null) {
            return Set.of();
        }

        return rawExcludedBookIds.stream()
            .filter(Objects::nonNull)
            .filter(id -> id > 0)
            .distinct()
            .limit(MAX_EXCLUDED_BOOK_IDS)
            .collect(java.util.stream.Collectors.toSet());
    }

    /*
     * 제외 후 남은 "새 후보 풀"이 비어 있으면 AI를 아예 호출하지 않는다
     * (제공할 후보가 없으므로). 후보가 있으면 기존과 동일하게 AI를 호출하고,
     * 실패 시 그 새 후보 풀 안에서만 fallback한다.
     */
    private AiBookRecommendationResponse buildPrimaryResponse(
            List<RecommendationCandidateItem> freshCandidates, AiBookRecommendationRequest request) {

        if (freshCandidates.isEmpty()) {
            return new AiBookRecommendationResponse(List.of(), false, true);
        }

        try {
            AiBookSelectionResponse aiResponse = callAiForSelections(request, freshCandidates);
            return buildResponseFromAiSelections(freshCandidates, request, aiResponse);
        } catch (Exception e) {
            log.error("AI 책 추천 생성 실패", e);
            return buildFallbackResponse(freshCandidates, request);
        }
    }

    /*
     * 새 후보 풀만으로 3권을 채우지 못했을 때만(즉 남은 후보 자체가 3권보다
     * 적었을 때만) 이전에 보여준 책(reusePool)을 점수 순으로 보충한다.
     * 새 후보가 3권 이상이었다면 이 메서드는 아무 것도 하지 않는다 -
     * 이전 책은 절대 섞이지 않는다.
     */
    private AiBookRecommendationResponse topUpFromReusePoolIfNeeded(
            AiBookRecommendationResponse primary,
            List<RecommendationCandidateItem> reusePool,
            AiBookRecommendationRequest request) {

        if (primary.getRecommendations().size() >= RECOMMENDATION_COUNT || reusePool.isEmpty()) {
            return primary;
        }

        List<AiBookRecommendationItem> items = new ArrayList<>(primary.getRecommendations());
        Set<Long> usedIds = new HashSet<>();
        for (AiBookRecommendationItem item : items) {
            usedIds.add(item.getBookId());
        }

        boolean reused = false;
        for (RecommendationCandidateItem candidate : reusePool) {
            if (items.size() >= RECOMMENDATION_COUNT) {
                break;
            }

            Long bookId = candidate.getBook().getId();
            if (usedIds.contains(bookId)) {
                continue;
            }

            items.add(toItem(candidate, buildFallbackReason(candidate, request)));
            usedIds.add(bookId);
            reused = true;
        }

        return new AiBookRecommendationResponse(items, primary.isAiUsed(), primary.isFallbackUsed() || reused);
    }

    /*
     * AI 호출 결과(파싱 완료된 selections)를 검증하고 최종 응답을 만든다.
     * 네트워크/파싱 단계와 분리해 두면 이 로직만 단위 테스트할 수 있다.
     */
    AiBookRecommendationResponse buildResponseFromAiSelections(
            List<RecommendationCandidateItem> candidates,
            AiBookRecommendationRequest request,
            AiBookSelectionResponse aiResponse) {

        List<ValidatedSelection> valid = validateSelections(aiResponse, candidates);

        if (valid.isEmpty()) {
            return buildFallbackResponse(candidates, request);
        }

        List<AiBookRecommendationItem> items = topUpWithFallback(valid, candidates, request);
        return new AiBookRecommendationResponse(items, true, false);
    }

    /*
     * AI 응답 검증:
     * 1) selections null → 빈 목록으로 취급
     * 2) bookId가 숫자가 아니거나 null → 제외
     * 3) 후보 목록 안에 없는 id → 제외(제외된 책은 애초에 이 candidates에
     *    들어있지 않으므로 자동으로 걸러짐)
     * 4) 중복 id → 첫 번째만 사용
     * 5) 추천 이유 null/빈 문자열/HTML 포함 → 제외
     * 6) 최대 3개까지만 사용
     */
    List<ValidatedSelection> validateSelections(
            AiBookSelectionResponse aiResponse, List<RecommendationCandidateItem> candidates) {

        if (aiResponse == null || aiResponse.getSelections() == null) {
            return List.of();
        }

        Map<Long, RecommendationCandidateItem> candidatesById = new LinkedHashMap<>();
        for (RecommendationCandidateItem candidate : candidates) {
            candidatesById.put(candidate.getBook().getId(), candidate);
        }

        List<ValidatedSelection> result = new ArrayList<>();
        Set<Long> seenIds = new HashSet<>();

        for (AiBookSelectionItem selection : aiResponse.getSelections()) {
            if (result.size() >= RECOMMENDATION_COUNT) {
                break;
            }

            if (selection == null || selection.getBookId() == null) {
                continue;
            }

            Long bookId = selection.getBookId();
            RecommendationCandidateItem candidate = candidatesById.get(bookId);

            if (candidate == null || !seenIds.add(bookId)) {
                continue;
            }

            String reason = sanitizeReason(selection.getRecommendationReason());
            if (reason == null) {
                continue;
            }

            result.add(new ValidatedSelection(candidate, reason));
        }

        return result;
    }

    private String sanitizeReason(String rawReason) {
        if (rawReason == null) {
            return null;
        }

        String trimmed = rawReason.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        if (trimmed.contains("<") || trimmed.contains(">")) {
            return null;
        }

        if (trimmed.length() > MAX_REASON_LENGTH) {
            return trimmed.substring(0, MAX_REASON_LENGTH);
        }

        return trimmed;
    }

    /*
     * AI가 3권 미만을 유효하게 선택했을 때, 넘겨받은 candidates(=이번에
     * 호출한 새 후보 풀) 안에 더 있다면 매치 점수 순서로 나머지 자리를
     * 채운다. AI가 고른 것도 아니고 완전 실패도 아니므로 aiUsed=true,
     * fallbackUsed=false로 취급한다.
     */
    private List<AiBookRecommendationItem> topUpWithFallback(
            List<ValidatedSelection> valid,
            List<RecommendationCandidateItem> candidates,
            AiBookRecommendationRequest request) {

        List<AiBookRecommendationItem> items = new ArrayList<>();
        Set<Long> usedIds = new HashSet<>();

        for (ValidatedSelection selection : valid) {
            items.add(toItem(selection.candidate(), selection.reason()));
            usedIds.add(selection.candidate().getBook().getId());
        }

        for (RecommendationCandidateItem candidate : candidates) {
            if (items.size() >= RECOMMENDATION_COUNT) {
                break;
            }

            Long bookId = candidate.getBook().getId();
            if (usedIds.contains(bookId)) {
                continue;
            }

            items.add(toItem(candidate, buildFallbackReason(candidate, request)));
            usedIds.add(bookId);
        }

        return items;
    }

    private AiBookRecommendationResponse buildFallbackResponse(
            List<RecommendationCandidateItem> candidates, AiBookRecommendationRequest request) {

        List<AiBookRecommendationItem> items = candidates.stream()
            .limit(RECOMMENDATION_COUNT)
            .map(candidate -> toItem(candidate, buildFallbackReason(candidate, request)))
            .toList();

        return new AiBookRecommendationResponse(items, false, true);
    }

    private String buildFallbackReason(RecommendationCandidateItem candidate, AiBookRecommendationRequest request) {
        String genre = candidate.getBook().getGenre();

        if (genre != null && genre.equals(request.getGenre())) {
            return "네가 좋아하는 이야기와 비슷해서 재미있게 읽을 수 있는 책이야.";
        }

        return "네가 고른 책 취향과 잘 맞는 책이야.";
    }

    private AiBookRecommendationItem toItem(RecommendationCandidateItem candidate, String reason) {
        RecommendationBookItem book = candidate.getBook();

        return new AiBookRecommendationItem(
            book.getId(),
            book.getTitle(),
            book.getAuthor(),
            book.getDescription(),
            book.getCoverImage(),
            candidate.getMatchScore(),
            reason
        );
    }

    private AiBookSelectionResponse callAiForSelections(
            AiBookRecommendationRequest request, List<RecommendationCandidateItem> candidates) throws Exception {

        Map<String, Object> requestBody = buildRequestBody(request, candidates);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openaiApiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        Map<?, ?> response = restTemplate.postForObject(OPENAI_CHAT_COMPLETIONS_URL, entity, Map.class);

        String content = extractContent(response);
        return parseAiResponse(content);
    }

    AiBookSelectionResponse parseAiResponse(String content) throws Exception {
        return objectMapper.readValue(extractJsonObject(content), AiBookSelectionResponse.class);
    }

    /*
     * OpenAI가 response_format=json_object를 지켜도, 방어적으로 JSON 앞뒤에
     * 붙을 수 있는 설명문을 걷어낸다.
     */
    private String extractJsonObject(String content) {
        if (content == null) {
            throw new IllegalArgumentException("AI 응답이 비어 있습니다.");
        }

        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');

        if (start < 0 || end < start) {
            throw new IllegalArgumentException("AI 응답에서 JSON을 찾을 수 없습니다.");
        }

        return content.substring(start, end + 1);
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<?, ?> response) {
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> firstChoice = choices.get(0);
        Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
        return (String) message.get("content");
    }

    Map<String, Object> buildRequestBody(
            AiBookRecommendationRequest request, List<RecommendationCandidateItem> candidates)
            throws Exception {

        Map<String, Object> systemMessage = new LinkedHashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", SYSTEM_PROMPT);

        Map<String, Object> userMessage = new LinkedHashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", buildUserContent(request, candidates));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", MODEL);
        body.put("messages", List.of(systemMessage, userMessage));
        body.put("response_format", Map.of("type", "json_object"));
        body.put("temperature", 0.1);
        return body;
    }

    /*
     * AI에 전달하는 정보는 학생 취향 6개와 (이미 제외 처리된) 후보 도서
     * 정보뿐이다. excludedBookIds 자체는 전달하지 않는다 - 제외된 책은
     * candidates 목록에 애초에 들어있지 않으므로 AI에게 굳이 "이 책들은
     * 빼라"고 알릴 필요가 없다(원칙: 제외된 책은 후보 목록으로도 전달하지
     * 않는다). 학생 이름, loginId, studentId, 학급 정보, JWT, 다른 독서
     * 기록은 여기에 절대 포함하지 않는다.
     */
    String buildUserContent(AiBookRecommendationRequest request, List<RecommendationCandidateItem> candidates)
            throws Exception {

        Map<String, Object> preference = new LinkedHashMap<>();
        preference.put("thickness", request.getThickness());
        preference.put("mood", request.getMood());
        preference.put("genre", request.getGenre());
        preference.put("illustrationLevel", request.getIllustrationLevel());
        preference.put("difficulty", request.getDifficulty());
        preference.put("purpose", request.getPurpose());

        List<Map<String, Object>> candidateList = candidates.stream()
            .map(candidate -> {
                RecommendationBookItem book = candidate.getBook();
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("bookId", book.getId());
                item.put("title", book.getTitle());
                item.put("author", book.getAuthor());
                item.put("description", book.getDescription());
                item.put("thickness", book.getThickness());
                item.put("mood", book.getMood());
                item.put("genre", book.getGenre());
                item.put("illustrationLevel", book.getIllustrationLevel());
                item.put("difficulty", book.getDifficulty());
                item.put("purposeTags", book.getPurposeTags());
                item.put("matchScore", candidate.getMatchScore());
                return item;
            })
            .toList();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("studentPreference", preference);
        payload.put("candidateBooks", candidateList);

        return "학생 취향과 후보 도서 목록이야. 이 후보 안에서만 정확히 3권을 골라 JSON으로 응답해줘.\n"
            + objectMapper.writeValueAsString(payload);
    }

    record ValidatedSelection(RecommendationCandidateItem candidate, String reason) {
    }
}
