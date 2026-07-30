package com.victory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.BookPreferenceRequest;
import com.victory.dto.RecommendationBookItem;
import com.victory.dto.RecommendationCandidateItem;
import com.victory.entity.RecommendationBook;
import com.victory.repository.RecommendationBookRepository;

class RecommendationBookServiceTest {

    private final RecommendationBookRepository recommendationBookRepository =
        mock(RecommendationBookRepository.class);
    private final RecommendationBookService service =
        new RecommendationBookService(recommendationBookRepository);

    private RecommendationBook book(
            Long id,
            String title,
            String author,
            boolean active,
            String thickness,
            String mood,
            String genre,
            String illustrationLevel,
            String difficulty,
            List<String> purposeTags,
            Integer minGrade,
            Integer maxGrade) {

        RecommendationBook book = new RecommendationBook();
        book.setId(id);
        book.setTitle(title);
        book.setAuthor(author);
        book.setDescription(title + " 설명");
        book.setActive(active);
        book.setThickness(thickness);
        book.setMood(mood);
        book.setGenre(genre);
        book.setIllustrationLevel(illustrationLevel);
        book.setDifficulty(difficulty);
        book.setPurposeTags(purposeTags);
        book.setRecommendedGradeMin(minGrade);
        book.setRecommendedGradeMax(maxGrade);
        return book;
    }

    private RecommendationBook matchingBook(Long id, String title) {
        return book(
            id,
            title,
            "작가",
            true,
            "medium",
            "exciting",
            "adventure",
            "many",
            "easy",
            List.of("fun", "imagination"),
            3,
            5);
    }

    private BookPreferenceRequest preference(
            String thickness,
            String mood,
            String genre,
            String illustrationLevel,
            String difficulty,
            String purpose) {

        BookPreferenceRequest request = new BookPreferenceRequest();
        setField(request, "thickness", thickness);
        setField(request, "mood", mood);
        setField(request, "genre", genre);
        setField(request, "illustrationLevel", illustrationLevel);
        setField(request, "difficulty", difficulty);
        setField(request, "purpose", purpose);
        return request;
    }

    private BookPreferenceRequest matchingPreference() {
        return preference("medium", "exciting", "adventure", "many", "easy", "fun");
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

    @Test
    void getActiveBooks_returnsActiveBooksFromRepository() {
        RecommendationBook active = matchingBook(1L, "긴긴밤");
        when(recommendationBookRepository.findByActiveTrueOrderByTitleAscIdAsc())
            .thenReturn(List.of(active));

        List<RecommendationBookItem> result = service.getActiveBooks();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("긴긴밤");
        verify(recommendationBookRepository).findByActiveTrueOrderByTitleAscIdAsc();
    }

    @Test
    void getBooksByIds_returnsSortedItems() {
        RecommendationBook b = matchingBook(2L, "바다");
        RecommendationBook a = matchingBook(1L, "가람");
        when(recommendationBookRepository.findByIdIn(anyCollection()))
            .thenReturn(List.of(b, a));

        List<RecommendationBookItem> result = service.getBooksByIds(List.of(2L, 1L));

        assertThat(result).extracting(RecommendationBookItem::getTitle)
            .containsExactly("가람", "바다");
    }

    @Test
    void duplicateChecks_delegateToRepository() {
        when(recommendationBookRepository.existsByTitle("긴긴밤")).thenReturn(true);
        when(recommendationBookRepository.existsByTitleAndAuthor("긴긴밤", "루리")).thenReturn(true);

        assertThat(service.existsByTitle("긴긴밤")).isTrue();
        assertThat(service.existsByTitleAndAuthor("긴긴밤", "루리")).isTrue();
    }

    @Test
    void calculateMatchScore_addsThicknessGenrePurposeAndAllMatches() {
        RecommendationBook fullMatch = matchingBook(1L, "완전 일치");
        BookPreferenceRequest request = matchingPreference();

        assertThat(service.calculateMatchScore(fullMatch, request, 4)).isEqualTo(14);

        RecommendationBook thicknessOnly = book(
            2L, "두께만", "작가", true, "medium", "calm", "history",
            "few", "hard", List.of("comfort"), 1, 2);
        assertThat(service.calculateMatchScore(thicknessOnly, request, 4)).isEqualTo(2);

        RecommendationBook genreOnly = book(
            3L, "장르만", "작가", true, "thin", "calm", "adventure",
            "few", "hard", List.of("comfort"), 1, 2);
        assertThat(service.calculateMatchScore(genreOnly, request, 4)).isEqualTo(3);

        RecommendationBook purposeOnly = book(
            4L, "목적만", "작가", true, "thin", "calm", "history",
            "few", "hard", List.of("fun"), 1, 2);
        assertThat(service.calculateMatchScore(purposeOnly, request, 4)).isEqualTo(2);
    }

    @Test
    void getCandidates_sortsByScoreThenTitleAndIdAndLimitsToTen() {
        RecommendationBook top = matchingBook(12L, "가장 잘 맞는 책");
        RecommendationBook sameScoreA = book(
            2L, "가나다", "작가", true, "medium", "calm", "history",
            "few", "hard", List.of("comfort"), 1, 6);
        RecommendationBook sameScoreB = book(
            1L, "가나다", "작가2", true, "medium", "calm", "history",
            "few", "hard", List.of("comfort"), 1, 6);
        RecommendationBook low = book(
            3L, "낮은 점수", "작가", true, "thin", "calm", "history",
            "few", "hard", List.of("comfort"), 1, 2);
        List<RecommendationBook> books = new java.util.ArrayList<>(
            List.of(low, sameScoreA, sameScoreB, top));
        for (long i = 4; i <= 15; i++) {
            books.add(book(
                i,
                "후보" + i,
                "작가",
                true,
                "thin",
                "calm",
                "history",
                "few",
                "hard",
                List.of("comfort"),
                1,
                2));
        }
        when(recommendationBookRepository.findActiveBooksForGradeOrderByTitleAscIdAsc(4))
            .thenReturn(books);

        List<RecommendationCandidateItem> result = service.getCandidates(matchingPreference());

        assertThat(result).hasSize(10);
        assertThat(result.get(0).getBook().getTitle()).isEqualTo("가장 잘 맞는 책");
        assertThat(result.get(1).getBook().getId()).isEqualTo(1L);
        assertThat(result.get(2).getBook().getId()).isEqualTo(2L);
        assertThat(result.get(0).getMatchScore()).isEqualTo(14);
    }

    @Test
    void getCandidates_rejectsUnknownPreferenceValue() {
        BookPreferenceRequest invalid = preference("wide", "exciting", "adventure", "many", "easy", "fun");

        assertThatThrownBy(() -> service.getCandidates(invalid))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400")
            .hasMessageContaining("thickness");
    }
}
