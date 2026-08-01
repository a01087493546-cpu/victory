package com.victory.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.AiBookRecommendationItem;
import com.victory.dto.AiBookRecommendationRequest;
import com.victory.dto.AiBookRecommendationResponse;
import com.victory.service.AiBookRecommendationService;

/*
 * teacher 접근 차단(403), 잘못된 선택값(400)은 SecurityConfig(hasAuthority("student"))와
 * RecommendationBookService.getCandidates()의 기존 검증(ResponseStatusException 400)이
 * 이미 처리하므로, RecommendationBookControllerTest와 같은 방식으로 컨트롤러
 * 계층에서는 인증 여부(401)와 정상 위임/응답 DTO 구조만 확인한다.
 */
class BookRecommendationAiControllerTest {

    private final AiBookRecommendationService aiBookRecommendationService =
        mock(AiBookRecommendationService.class);
    private final BookRecommendationAiController controller =
        new BookRecommendationAiController(aiBookRecommendationService);

    @Test
    void getAiRecommendations_requiresStudentAuthentication() {
        assertThatThrownBy(() -> controller.getAiRecommendations(new AiBookRecommendationRequest(), null))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("401");
    }

    @Test
    void getAiRecommendations_returnsServiceResultForStudent() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            100L, null, List.of(new SimpleGrantedAuthority("student")));

        AiBookRecommendationRequest request = new AiBookRecommendationRequest();

        AiBookRecommendationItem item = new AiBookRecommendationItem(
            13L, "푸른 사자 와니니 1", "이현", "설명", null, 14,
            "신나는 모험을 좋아하는 너에게 잘 맞는 책이야.");
        AiBookRecommendationResponse expected =
            new AiBookRecommendationResponse(List.of(item), true, false);

        when(aiBookRecommendationService.recommend(request)).thenReturn(expected);

        AiBookRecommendationResponse result =
            controller.getAiRecommendations(request, authentication).getBody();

        assertThat(result).isSameAs(expected);
        assertThat(result.getRecommendations()).hasSize(1);
        assertThat(result.isAiUsed()).isTrue();
        verify(aiBookRecommendationService).recommend(request);
    }

    @Test
    void getAiRecommendations_doesNotAcceptStudentIdFromRequest() {
        // AiBookRecommendationRequest에는 애초에 studentId 필드 자체가 없다(리플렉션으로
        // 필드 목록을 확인해 회귀를 방지).
        java.lang.reflect.Field[] fields = AiBookRecommendationRequest.class.getDeclaredFields();
        boolean hasStudentIdField = java.util.Arrays.stream(fields)
            .anyMatch(field -> field.getName().toLowerCase().contains("studentid"));

        assertThat(hasStudentIdField).isFalse();
    }
}
