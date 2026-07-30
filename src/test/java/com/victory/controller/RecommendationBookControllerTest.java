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

import com.victory.dto.BookPreferenceRequest;
import com.victory.dto.RecommendationBookItem;
import com.victory.service.RecommendationBookService;

class RecommendationBookControllerTest {

    private final RecommendationBookService recommendationBookService = mock(RecommendationBookService.class);
    private final RecommendationBookController controller =
        new RecommendationBookController(recommendationBookService);

    @Test
    void getActiveBooks_requiresStudentAuthentication() {
        assertThatThrownBy(() -> controller.getActiveBooks(null))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("401");
    }

    @Test
    void getActiveBooks_returnsServiceResultForStudent() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            100L, null, List.of(new SimpleGrantedAuthority("student")));
        RecommendationBookItem item = new RecommendationBookItem(
            1L,
            "긴긴밤",
            "루리",
            "설명",
            null,
            "medium",
            "touching",
            "growth",
            "medium",
            "medium",
            List.of("comfort"),
            3,
            6);
        when(recommendationBookService.getActiveBooks()).thenReturn(List.of(item));

        List<RecommendationBookItem> result = controller.getActiveBooks(authentication).getBody();

        assertThat(result).containsExactly(item);
        verify(recommendationBookService).getActiveBooks();
    }

    @Test
    void getCandidates_requiresStudentAuthentication() {
        assertThatThrownBy(() -> controller.getCandidates(new BookPreferenceRequest(), null))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("401");
    }
}
