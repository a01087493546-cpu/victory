package com.victory.service;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.victory.dto.IndividualPortfolioResponse.ReadingCompetencies;
import com.victory.dto.ReadingCompetencyScore;

/** 심사 포트폴리오에서만 사용하는 학생별 고정 독서 역량 예시값. */
@Component
public class DemoReadingCompetencyProvider {
    private static final Map<String, Scores> SCORES_BY_LOGIN_ID = Map.ofEntries(
        Map.entry("ss01", new Scores(82, 76, 88, 64)),
        Map.entry("ss02", new Scores(68, 84, 72, 55)),
        Map.entry("demo_student_02", new Scores(68, 84, 72, 55)),
        Map.entry("ss03", new Scores(46, 71, 58, 83)),
        Map.entry("demo_student_03", new Scores(46, 71, 58, 83)),
        Map.entry("ss04", new Scores(35, 62, 74, 48)),
        Map.entry("demo_student_04", new Scores(35, 62, 74, 48)),
        Map.entry("ss05", new Scores(57, 44, 66, 38)),
        Map.entry("demo_student_05", new Scores(57, 44, 66, 38)),
        Map.entry("ss06", new Scores(76, 91, 53, 69)),
        Map.entry("demo_student_06", new Scores(76, 91, 53, 69)),
        Map.entry("ss07", new Scores(88, 67, 79, 92)),
        Map.entry("demo_student_07", new Scores(88, 67, 79, 92)),
        Map.entry("ss08", new Scores(63, 52, 41, 81)),
        Map.entry("demo_student_08", new Scores(63, 52, 41, 81))
    );
    private static final Scores DEFAULT_DEMO_SCORES = new Scores(55, 65, 50, 45);

    public ReadingCompetencies forLoginId(String loginId) {
        Scores scores = SCORES_BY_LOGIN_ID.getOrDefault(loginId, DEFAULT_DEMO_SCORES);
        return new ReadingCompetencies(
            ReadingCompetencyScore.of(scores.questionGeneration()),
            ReadingCompetencyScore.of(scores.readingPersistence()),
            ReadingCompetencyScore.of(scores.thoughtRefinement()),
            ReadingCompetencyScore.of(scores.thoughtSharing()));
    }

    private record Scores(
        int questionGeneration,
        int readingPersistence,
        int thoughtRefinement,
        int thoughtSharing) {
    }
}
