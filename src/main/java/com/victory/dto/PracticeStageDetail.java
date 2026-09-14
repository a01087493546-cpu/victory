package com.victory.dto;

/*
 * 온책읽기 성장 포트폴리오의 읽기 전/중/후 한 단계에 대한 상세 정보.
 * 양적 참여 여부(participated/completed)와 함께, 학생이 실제로 남긴
 * 질문/답/간추리기 텍스트를 근거로 한 짧은 설명(shortTitle)과 성장
 * 관찰 문장(growthNote), 대표 텍스트 1개(representativeText)를 담는다.
 * 값은 모두 실제 저장된 텍스트에서만 파생되며 추측하거나 지어내지
 * 않는다 - 데이터가 부족하면 growthNote가 "현재 기록에서는..." 식의
 * 제한적 문장이 된다(PracticeStageNarrativeBuilder 참고).
 */
public record PracticeStageDetail(
    boolean participated,
    boolean completed,
    String shortTitle,
    String growthNote,
    String representativeText) {
}
