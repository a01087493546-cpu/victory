package com.victory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.victory.dto.AiBookRecommendationItem;
import com.victory.dto.AiBookRecommendationRequest;
import com.victory.dto.AiBookRecommendationResponse;
import com.victory.dto.AiBookSelectionItem;
import com.victory.dto.AiBookSelectionResponse;
import com.victory.dto.RecommendationBookItem;
import com.victory.dto.RecommendationCandidateItem;

/*
 * recommend()가 실제로 AI를 호출하는 경로(=fallback을 유발하는 테스트들)는
 * RestTemplate을 목으로 바꿔치기해 강제로 실패시킨다 - 실제 OpenAI 서버로
 * 네트워크 요청을 보내지 않기 위함이다(AiBookRecommendationService.restTemplate은
 * 같은 패키지 테스트가 이렇게 교체할 수 있도록 package-private으로 열어 둠).
 * "AI가 정상적으로 선택한 경우"의 검증·조립 로직은 buildResponseFromAiSelections를
 * 직접 호출해 네트워크 없이 단위 테스트한다.
 *
 * getAllScoredActiveBooksByRawPreference()가 이미 매치 점수 내림차순으로 정렬된
 * 목록을 반환한다는 계약을 그대로 모킹하므로, tenCandidates()도 처음부터
 * 점수 내림차순(=id 오름차순)으로 만들어 둔다.
 */
class AiBookRecommendationServiceTest {

    private final RecommendationBookService recommendationBookService =
        mock(RecommendationBookService.class);
    private final AiBookRecommendationService service =
        new AiBookRecommendationService(recommendationBookService);

    {
        RestTemplate throwingRestTemplate = mock(RestTemplate.class);
        doThrow(new RestClientException("AI 호출 실패(테스트)"))
            .when(throwingRestTemplate)
            .postForObject(anyString(), any(), eq(Map.class));
        service.restTemplate = throwingRestTemplate;
    }

    private RecommendationCandidateItem candidate(long id, String title, String genre, int score) {
        RecommendationBookItem book = new RecommendationBookItem(
            id, title, "작가" + id, title + " 설명", null,
            "medium", "exciting", genre, "medium", "medium",
            List.of("fun"), 3, 5);
        return new RecommendationCandidateItem(book, score);
    }

    /* id 1~10, 점수 19~10(내림차순) - 실제 서비스가 반환하는 정렬 계약과 동일하게 구성 */
    private List<RecommendationCandidateItem> tenCandidates() {
        List<RecommendationCandidateItem> list = new ArrayList<>();
        for (long i = 1; i <= 10; i++) {
            list.add(candidate(i, "책" + i, "adventure", (int) (20 - i)));
        }
        return list;
    }

    private void stubAllScored(List<RecommendationCandidateItem> candidates) {
        when(recommendationBookService.getAllScoredActiveBooksByRawPreference(
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
            .thenReturn(candidates);
    }

    private AiBookRecommendationRequest preference() {
        return preference(null);
    }

    private AiBookRecommendationRequest preference(List<Long> excludedBookIds) {
        AiBookRecommendationRequest request = new AiBookRecommendationRequest();
        setField(request, "thickness", "medium");
        setField(request, "mood", "exciting");
        setField(request, "genre", "adventure");
        setField(request, "illustrationLevel", "medium");
        setField(request, "difficulty", "medium");
        setField(request, "purpose", "challenge");
        setField(request, "excludedBookIds", excludedBookIds);
        return request;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private AiBookSelectionResponse selectionResponse(Long... bookIds) {
        AiBookSelectionResponse response = new AiBookSelectionResponse();
        List<AiBookSelectionItem> items = new ArrayList<>();
        for (Long id : bookIds) {
            AiBookSelectionItem item = new AiBookSelectionItem();
            item.setBookId(id);
            item.setRecommendationReason("너에게 잘 맞는 책이야.");
            items.add(item);
        }
        response.setSelections(items);
        return response;
    }

    // 1. 후보 10권 중 AI가 정상적으로 3권 선택
    @Test
    void buildResponseFromAiSelections_returnsExactlyThreeValidSelections() {
        List<RecommendationCandidateItem> candidates = tenCandidates();
        AiBookSelectionResponse aiResponse = selectionResponse(3L, 5L, 7L);

        AiBookRecommendationResponse result =
            service.buildResponseFromAiSelections(candidates, preference(), aiResponse);

        assertThat(result.isAiUsed()).isTrue();
        assertThat(result.isFallbackUsed()).isFalse();
        assertThat(result.getRecommendations()).extracting(AiBookRecommendationItem::getBookId)
            .containsExactly(3L, 5L, 7L);
    }

    // 2. 후보에 없는 ID 제거 + 3. 중복 ID 제거 (같은 시나리오에서 함께 검증)
    @Test
    void buildResponseFromAiSelections_removesUnknownAndDuplicateIds() {
        List<RecommendationCandidateItem> candidates = tenCandidates();
        AiBookSelectionResponse aiResponse = selectionResponse(3L, 999L, 3L, 5L, 7L);

        AiBookRecommendationResponse result =
            service.buildResponseFromAiSelections(candidates, preference(), aiResponse);

        assertThat(result.getRecommendations()).extracting(AiBookRecommendationItem::getBookId)
            .containsExactly(3L, 5L, 7L);
        assertThat(result.isAiUsed()).isTrue();
    }

    // 4. AI가 2권만 반환했을 때 처리 → 후보가 더 있으면 점수 순서로 채워 3권 확보
    @Test
    void buildResponseFromAiSelections_topsUpToThreeWhenAiReturnsOnlyTwo() {
        List<RecommendationCandidateItem> candidates = tenCandidates();
        AiBookSelectionResponse aiResponse = selectionResponse(3L, 5L);

        AiBookRecommendationResponse result =
            service.buildResponseFromAiSelections(candidates, preference(), aiResponse);

        assertThat(result.getRecommendations()).hasSize(3);
        assertThat(result.getRecommendations().get(0).getBookId()).isEqualTo(3L);
        assertThat(result.getRecommendations().get(1).getBookId()).isEqualTo(5L);
        // 점수 순 후보(책1)가 다음 자리를 채운다
        assertThat(result.getRecommendations().get(2).getBookId()).isEqualTo(1L);
        assertThat(result.isAiUsed()).isTrue();
        assertThat(result.isFallbackUsed()).isFalse();
    }

    // JSON 파싱 실패 시 예외 발생 → recommend()에서 fallback으로 이어짐
    @Test
    void parseAiResponse_throwsOnInvalidJson() {
        assertThatThrownBy(() -> service.parseAiResponse("이건 JSON이 아니야"))
            .isInstanceOf(Exception.class);
    }

    // AI API 오류(여기서는 null API key로 인한 호출 실패) 시 fallback
    @Test
    void recommend_fallsBackWhenAiCallFails() {
        stubAllScored(tenCandidates());

        AiBookRecommendationResponse result = service.recommend(preference());

        assertThat(result.isAiUsed()).isFalse();
        assertThat(result.isFallbackUsed()).isTrue();
        assertThat(result.getRecommendations()).hasSize(3);
    }

    // 추천 이유 빈값/공백 처리 → 해당 선택 제외
    @Test
    void validateSelections_dropsBlankOrEmptyReason() {
        List<RecommendationCandidateItem> candidates = tenCandidates();
        AiBookSelectionResponse aiResponse = new AiBookSelectionResponse();

        AiBookSelectionItem blank = new AiBookSelectionItem();
        blank.setBookId(3L);
        blank.setRecommendationReason("   ");

        AiBookSelectionItem valid = new AiBookSelectionItem();
        valid.setBookId(5L);
        valid.setRecommendationReason("좋은 책이야.");

        aiResponse.setSelections(List.of(blank, valid));

        List<AiBookRecommendationService.ValidatedSelection> result =
            service.validateSelections(aiResponse, candidates);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).candidate().getBook().getId()).isEqualTo(5L);
    }

    // 후보 0권 → 빈 배열, AI 호출 자체를 하지 않음
    @Test
    void recommend_returnsEmptyWhenNoCandidates() {
        stubAllScored(List.of());

        AiBookRecommendationResponse result = service.recommend(preference());

        assertThat(result.getRecommendations()).isEmpty();
        assertThat(result.isAiUsed()).isFalse();
        assertThat(result.isFallbackUsed()).isFalse();
    }

    // 후보 1권
    @Test
    void recommend_returnsOneWhenOnlyOneCandidateAndAiFails() {
        stubAllScored(List.of(candidate(1L, "책1", "adventure", 10)));

        AiBookRecommendationResponse result = service.recommend(preference());

        assertThat(result.getRecommendations()).hasSize(1);
        assertThat(result.isFallbackUsed()).isTrue();
    }

    // 후보 2권
    @Test
    void recommend_returnsTwoWhenOnlyTwoCandidatesAndAiFails() {
        stubAllScored(List.of(
            candidate(1L, "책1", "adventure", 10),
            candidate(2L, "책2", "adventure", 8)));

        AiBookRecommendationResponse result = service.recommend(preference());

        assertThat(result.getRecommendations()).hasSize(2);
        assertThat(result.isFallbackUsed()).isTrue();
    }

    // 후보 3권 이상 → fallback도 최대 3권만
    @Test
    void recommend_limitsToThreeWhenManyCandidatesAndAiFails() {
        stubAllScored(tenCandidates());

        AiBookRecommendationResponse result = service.recommend(preference());

        assertThat(result.getRecommendations()).hasSize(3);
    }

    // fallback 순서가 후보 점수 순서(=서비스가 반환한 순서)와 동일
    @Test
    void recommend_fallbackOrderMatchesCandidateOrder() {
        stubAllScored(tenCandidates());

        AiBookRecommendationResponse result = service.recommend(preference());

        assertThat(result.getRecommendations()).extracting(AiBookRecommendationItem::getBookId)
            .containsExactly(1L, 2L, 3L);
    }

    // 개인정보가 AI 요청에 포함되지 않음
    @Test
    void buildUserContent_neverIncludesPersonalInformation() throws Exception {
        List<RecommendationCandidateItem> candidates = tenCandidates();

        String content = service.buildUserContent(preference(), candidates);

        assertThat(content).doesNotContain("studentId");
        assertThat(content).doesNotContain("loginId");
        assertThat(content).doesNotContain("token");
        assertThat(content).doesNotContain("className");
        assertThat(content).doesNotContain("schoolClass");
        assertThat(content).contains("thickness");
        assertThat(content).contains("candidateBooks");
    }

    // excludedBookIds 자체는 AI 프롬프트에 전달하지 않는다("제외된 책은 후보로도 전달 안 함" 원칙)
    @Test
    void buildUserContent_neverIncludesExcludedBookIds() throws Exception {
        List<RecommendationCandidateItem> candidates = tenCandidates();
        AiBookRecommendationRequest request = preference(List.of(1L, 2L, 3L));

        String content = service.buildUserContent(request, candidates);

        assertThat(content).doesNotContain("excludedBookIds");
    }

    // 1) 첫 요청(excludedBookIds 없음)에는 전체 후보에서 점수 순으로 3권 반환
    @Test
    void recommend_withoutExcludedBookIds_usesFullPool() {
        stubAllScored(tenCandidates());

        AiBookRecommendationResponse result = service.recommend(preference(null));

        assertThat(result.getRecommendations()).extracting(AiBookRecommendationItem::getBookId)
            .containsExactly(1L, 2L, 3L);
    }

    // 2/3) 제외 목록을 지정하면 새 후보 풀에서 제외되고, 이전 결과와 겹치지 않음
    @Test
    void recommend_excludesShownBooksFromFreshPool() {
        stubAllScored(tenCandidates());
        AiBookRecommendationRequest request = preference(List.of(1L, 2L, 3L));

        AiBookRecommendationResponse result = service.recommend(request);

        assertThat(result.getRecommendations()).extracting(AiBookRecommendationItem::getBookId)
            .containsExactly(4L, 5L, 6L);
        assertThat(result.getRecommendations())
            .extracting(AiBookRecommendationItem::getBookId)
            .doesNotContain(1L, 2L, 3L);
    }

    // 세 번째 요청: 앞선 6권을 모두 제외해도 남은 후보가 충분하면 겹치지 않음(중복 0권)
    @Test
    void recommend_excludesAllPreviouslyShownBooksAcrossMultipleRounds() {
        stubAllScored(tenCandidates());
        AiBookRecommendationRequest request = preference(List.of(1L, 2L, 3L, 4L, 5L, 6L));

        AiBookRecommendationResponse result = service.recommend(request);

        assertThat(result.getRecommendations()).extracting(AiBookRecommendationItem::getBookId)
            .containsExactly(7L, 8L, 9L);
    }

    // 5) 남은 새 후보가 1~2권일 때만 이전에 보여준 책을 점수 순으로 보충(3권 이상이면 절대 재사용 안 함)
    @Test
    void recommend_reusesShownBooksOnlyWhenFreshPoolInsufficient() {
        stubAllScored(tenCandidates());
        // 1~8을 제외하면 새 후보는 9, 10 두 권만 남는다.
        AiBookRecommendationRequest request =
            preference(List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L));

        AiBookRecommendationResponse result = service.recommend(request);

        assertThat(result.getRecommendations()).extracting(AiBookRecommendationItem::getBookId)
            .containsExactly(9L, 10L, 1L);
        assertThat(result.isFallbackUsed()).isTrue();
    }

    // 후보가 부족해 과거 책을 재사용해도 오류로 처리하지 않고, 후보 0권이 아니면 그대로 3권 채움
    @Test
    void recommend_reusesShownBooksWhenFreshPoolIsCompletelyEmpty() {
        List<RecommendationCandidateItem> three = List.of(
            candidate(1L, "책1", "adventure", 30),
            candidate(2L, "책2", "adventure", 20),
            candidate(3L, "책3", "adventure", 10));
        stubAllScored(three);
        AiBookRecommendationRequest request = preference(List.of(1L, 2L, 3L));

        AiBookRecommendationResponse result = service.recommend(request);

        assertThat(result.getRecommendations()).extracting(AiBookRecommendationItem::getBookId)
            .containsExactly(1L, 2L, 3L);
        assertThat(result.isFallbackUsed()).isTrue();
    }

    // excludedBookIds 정리: null/음수/0/중복/최대 개수 처리
    @Test
    void sanitizeExcludedBookIds_handlesNullNegativeDuplicateAndCap() {
        assertThat(service.sanitizeExcludedBookIds(null)).isEmpty();

        List<Long> raw = new ArrayList<>();
        raw.add(-5L);
        raw.add(0L);
        raw.add(null);
        raw.add(3L);
        raw.add(3L);
        for (long i = 100; i < 140; i++) {
            raw.add(i);
        }

        Set<Long> sanitized = service.sanitizeExcludedBookIds(raw);

        assertThat(sanitized).doesNotContain(-5L, 0L);
        assertThat(sanitized).contains(3L);
        assertThat(sanitized).hasSizeLessThanOrEqualTo(30);
    }

    // recommendation_books에 없는 ID가 섞여 있어도 안전하게 무시된다(존재하는 후보만 걸러짐)
    @Test
    void recommend_ignoresExcludedIdsThatAreNotRealBooks() {
        stubAllScored(tenCandidates());
        AiBookRecommendationRequest request = preference(List.of(1L, 9999L));

        AiBookRecommendationResponse result = service.recommend(request);

        assertThat(result.getRecommendations()).extracting(AiBookRecommendationItem::getBookId)
            .containsExactly(2L, 3L, 4L);
    }
}
