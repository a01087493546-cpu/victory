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

    private static final Comparator<RecommendationCandidateItem> CANDIDATE_ORDER = Comparator
        .comparingInt(RecommendationCandidateItem::getMatchScore).reversed()
        .thenComparing((RecommendationCandidateItem item) -> item.getBook().getTitle())
        .thenComparing(item -> item.getBook().getId());

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
            .sorted(CANDIDATE_ORDER)
            .limit(DEFAULT_CANDIDATE_LIMIT)
            .toList();
    }

    /*
     * AI 책 추천(AiBookRecommendationService) 전용: 이전에 보여준 책을 제외한
     * "새 후보 풀"과, 부족할 때 보충할 "이전에 보여준 책 풀"을 모두 만들려면
     * 전체 활성 도서를 점수 계산 후 제한 없이 받아야 한다. 기존 getCandidates()
     * (태그 점수 후보 API, 최대 10권 제한)는 그대로 두고, 점수 계산 로직만
     * 재사용해 제한 없는 버전을 추가한다 - 기존 점수 계산 규칙은 전혀 바뀌지
     * 않는다.
     */
    public List<RecommendationCandidateItem> getAllScoredActiveBooksByRawPreference(
            String thickness, String mood, String genre,
            String illustrationLevel, String difficulty, String purpose) {

        validatePreferenceRaw(thickness, mood, genre, illustrationLevel, difficulty, purpose);

        return recommendationBookRepository
            .findActiveBooksForGradeOrderByTitleAscIdAsc(DEFAULT_STUDENT_GRADE)
            .stream()
            .map(book -> new RecommendationCandidateItem(
                RecommendationBookItem.from(book),
                calculateMatchScore(
                    book, thickness, mood, genre, illustrationLevel, difficulty, purpose, DEFAULT_STUDENT_GRADE)))
            .sorted(CANDIDATE_ORDER)
            .toList();
    }

    int calculateMatchScore(RecommendationBook book, BookPreferenceRequest request, Integer studentGrade) {
        return calculateMatchScore(
            book,
            request.getThickness(),
            request.getMood(),
            request.getGenre(),
            request.getIllustrationLevel(),
            request.getDifficulty(),
            request.getPurpose(),
            studentGrade
        );
    }

    int calculateMatchScore(
            RecommendationBook book,
            String thickness,
            String mood,
            String genre,
            String illustrationLevel,
            String difficulty,
            String purpose,
            Integer studentGrade) {

        int score = 0;

        if (book.getThickness().equals(thickness)) {
            score += 2;
        }

        if (book.getMood().equals(mood)) {
            score += 2;
        }

        if (book.getGenre().equals(genre)) {
            score += 3;
        }

        if (book.getIllustrationLevel().equals(illustrationLevel)) {
            score += 2;
        }

        if (book.getDifficulty().equals(difficulty)) {
            score += 2;
        }

        if (book.getPurposeTags() != null && book.getPurposeTags().contains(purpose)) {
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
        validatePreferenceRaw(
            request.getThickness(),
            request.getMood(),
            request.getGenre(),
            request.getIllustrationLevel(),
            request.getDifficulty(),
            request.getPurpose()
        );
    }

    private void validatePreferenceRaw(
            String thickness, String mood, String genre,
            String illustrationLevel, String difficulty, String purpose) {

        validateAllowed("thickness", thickness, THICKNESSES);
        validateAllowed("mood", mood, MOODS);
        validateAllowed("genre", genre, GENRES);
        validateAllowed("illustrationLevel", illustrationLevel, ILLUSTRATION_LEVELS);
        validateAllowed("difficulty", difficulty, DIFFICULTIES);
        validateAllowed("purpose", purpose, PURPOSES);
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
