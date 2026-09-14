package com.victory.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

/*
 * 심사 포트폴리오에서만 사용하는 학생별 고정 월별 완독 권수(1~12월).
 * 심사계정은 실제 장기간 완독 기록이 쌓이지 않아 그래프가 밋밋해 보이므로,
 * 학생마다 서로 다른 deterministic 12개월 값을 준다(랜덤 없음 - 같은
 * 학생은 항상 같은 값, 서버 재시작 후에도 동일). DemoReadingCompetencyProvider와
 * 같은 패턴: loginId로 조회하고, ss0N/demo_studentN 별칭을 함께 등록한다.
 */
@Component
public class DemoMonthlyCompletionProvider {
    private static final Map<String, List<Integer>> COUNTS_BY_LOGIN_ID = Map.ofEntries(
        Map.entry("ss01", List.of(1, 2, 1, 2, 3, 2, 3, 2, 4, 2, 3, 2)),       // 김초롱
        Map.entry("ss02", List.of(0, 1, 2, 2, 1, 3, 2, 3, 2, 4, 3, 2)),
        Map.entry("demo_student_02", List.of(0, 1, 2, 2, 1, 3, 2, 3, 2, 4, 3, 2)), // 송민정
        Map.entry("ss03", List.of(2, 1, 0, 1, 2, 1, 3, 2, 1, 2, 3, 1)),
        Map.entry("demo_student_03", List.of(2, 1, 0, 1, 2, 1, 3, 2, 1, 2, 3, 1)), // 박하민
        Map.entry("ss04", List.of(0, 1, 0, 2, 1, 1, 2, 1, 0, 2, 1, 2)),
        Map.entry("demo_student_04", List.of(0, 1, 0, 2, 1, 1, 2, 1, 0, 2, 1, 2)), // 이진우
        Map.entry("ss05", List.of(1, 0, 1, 1, 2, 0, 1, 2, 1, 1, 2, 1)),
        Map.entry("demo_student_05", List.of(1, 0, 1, 1, 2, 0, 1, 2, 1, 1, 2, 1)), // 김민지
        Map.entry("ss06", List.of(2, 3, 2, 3, 2, 4, 3, 2, 4, 3, 3, 4)),
        Map.entry("demo_student_06", List.of(2, 3, 2, 3, 2, 4, 3, 2, 4, 3, 3, 4)), // 서희원
        Map.entry("ss07", List.of(3, 2, 4, 3, 4, 3, 4, 4, 3, 4, 3, 4)),
        Map.entry("demo_student_07", List.of(3, 2, 4, 3, 4, 3, 4, 4, 3, 4, 3, 4)), // 김수진
        Map.entry("ss08", List.of(1, 2, 1, 0, 1, 2, 1, 3, 2, 1, 2, 3)),
        Map.entry("demo_student_08", List.of(1, 2, 1, 0, 1, 2, 1, 3, 2, 1, 2, 3))  // 이혜원
    );

    private static final List<Integer> DEFAULT_MONTHLY_COUNTS =
        List.of(1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2);

    public List<Integer> forLoginId(String loginId) {
        return COUNTS_BY_LOGIN_ID.getOrDefault(loginId, DEFAULT_MONTHLY_COUNTS);
    }
}
