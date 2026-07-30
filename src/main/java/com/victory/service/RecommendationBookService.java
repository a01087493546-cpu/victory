package com.victory.service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.BookPreferenceRequest;
import com.victory.dto.RecommendationBookItem;
import com.victory.dto.RecommendationCandidateItem;
import com.victory.entity.RecommendationBook;
import com.victory.repository.RecommendationBookRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationBookService {

    private static final int DEFAULT_STUDENT_GRADE = 4;
    private static final int DEFAULT_CANDIDATE_LIMIT = 10;

    public static final Set<String> THICKNESSES = Set.of("thin", "medium", "thick");
    public static final Set<String> ILLUSTRATION_LEVELS = Set.of("many", "medium", "few");
    public static final Set<String> DIFFICULTIES = Set.of("easy", "medium", "hard");
    public static final Set<String> MOODS = Set.of(
        "exciting", "funny", "touching", "mysterious", "calm", "imaginative");
    public static final Set<String> GENRES = Set.of(
        "adventure", "fantasy", "daily_life", "mystery", "science",
        "history", "nature", "friendship", "growth");
    public static final Set<String> PURPOSES = Set.of(
        "fun", "comfort", "knowledge", "imagination", "challenge");

    private final RecommendationBookRepository recommendationBookRepository;

    public List<RecommendationBookItem> getActiveBooks() {
        return recommendationBookRepository.findByActiveTrueOrderByTitleAscIdAsc().stream()
            .map(RecommendationBookItem::from)
            .toList();
    }

    public List<RecommendationBookItem> getBooksByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        return recommendationBookRepository.findByIdIn(new HashSet<>(ids)).stream()
            .sorted(Comparator
                .comparing(RecommendationBook::getTitle)
                .thenComparing(RecommendationBook::getId))
            .map(RecommendationBookItem::from)
            .toList();
    }

    public boolean existsByTitle(String title) {
        return recommendationBookRepository.existsByTitle(title);
    }

    public boolean existsByTitleAndAuthor(String title, String author) {
        return recommendationBookRepository.existsByTitleAndAuthor(title, author);
    }

    public List<RecommendationCandidateItem> getCandidates(BookPreferenceRequest request) {
        validatePreference(request);

        return recommendationBookRepository
            .findActiveBooksForGradeOrderByTitleAscIdAsc(DEFAULT_STUDENT_GRADE)
            .stream()
            .map(book -> new RecommendationCandidateItem(
                RecommendationBookItem.from(book),
                calculateMatchScore(book, request, DEFAULT_STUDENT_GRADE)))
            .sorted(Comparator
                .comparingInt(RecommendationCandidateItem::getMatchScore).reversed()
                .thenComparing(item -> item.getBook().getTitle())
                .thenComparing(item -> item.getBook().getId()))
            .limit(DEFAULT_CANDIDATE_LIMIT)
            .toList();
    }

    int calculateMatchScore(RecommendationBook book, BookPreferenceRequest request, Integer studentGrade) {
        int score = 0;

        if (book.getThickness().equals(request.getThickness())) {
            score += 2;
        }

        if (book.getMood().equals(request.getMood())) {
            score += 2;
        }

        if (book.getGenre().equals(request.getGenre())) {
            score += 3;
        }

        if (book.getIllustrationLevel().equals(request.getIllustrationLevel())) {
            score += 2;
        }

        if (book.getDifficulty().equals(request.getDifficulty())) {
            score += 2;
        }

        if (book.getPurposeTags() != null && book.getPurposeTags().contains(request.getPurpose())) {
            score += 2;
        }

        if (isRecommendedForGrade(book, studentGrade)) {
            score += 1;
        }

        return score;
    }

    private boolean isRecommendedForGrade(RecommendationBook book, Integer studentGrade) {
        if (studentGrade == null) {
            return false;
        }

        return (book.getRecommendedGradeMin() == null || book.getRecommendedGradeMin() <= studentGrade)
            && (book.getRecommendedGradeMax() == null || book.getRecommendedGradeMax() >= studentGrade);
    }

    private void validatePreference(BookPreferenceRequest request) {
        validateAllowed("thickness", request.getThickness(), THICKNESSES);
        validateAllowed("mood", request.getMood(), MOODS);
        validateAllowed("genre", request.getGenre(), GENRES);
        validateAllowed("illustrationLevel", request.getIllustrationLevel(), ILLUSTRATION_LEVELS);
        validateAllowed("difficulty", request.getDifficulty(), DIFFICULTIES);
        validateAllowed("purpose", request.getPurpose(), PURPOSES);
    }

    private void validateAllowed(String fieldName, String value, Set<String> allowedValues) {
        if (value == null || value.isBlank() || !allowedValues.contains(value)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "알 수 없는 추천 선택값입니다. field=" + fieldName
            );
        }
    }
}
