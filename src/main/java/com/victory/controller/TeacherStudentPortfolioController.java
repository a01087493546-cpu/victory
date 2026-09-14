package com.victory.controller;

import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.IndividualPortfolioResponse;
import com.victory.dto.PortfolioAiAnalysisRequest;
import com.victory.dto.PortfolioAiAnalysisResponse;
import com.victory.dto.PracticePortfolioResponse;
import com.victory.service.StudentPortfolioService;
import com.victory.service.PortfolioAiAnalysisService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class TeacherStudentPortfolioController {
    private final StudentPortfolioService portfolioService;
    private final PortfolioAiAnalysisService portfolioAiAnalysisService;

    @GetMapping("/api/teachers/{teacherId}/classes/{classId}/students/{studentId}/practice-portfolio")
    public ResponseEntity<PracticePortfolioResponse> practice(
            @PathVariable Long teacherId, @PathVariable Long classId, @PathVariable Long studentId,
            @RequestParam LocalDate from, @RequestParam LocalDate to, Authentication authentication) {
        requireSelf(teacherId, authentication);
        return ResponseEntity.ok(portfolioService.getPracticePortfolio(teacherId, classId, studentId, from, to));
    }

    @GetMapping("/api/teachers/{teacherId}/classes/{classId}/students/{studentId}/individual-portfolio")
    public ResponseEntity<IndividualPortfolioResponse> individual(
            @PathVariable Long teacherId, @PathVariable Long classId, @PathVariable Long studentId,
            @RequestParam LocalDate from, @RequestParam LocalDate to, Authentication authentication) {
        requireSelf(teacherId, authentication);
        return ResponseEntity.ok(portfolioService.getIndividualPortfolio(teacherId, classId, studentId, from, to));
    }

    @PostMapping("/api/teachers/{teacherId}/classes/{classId}/students/{studentId}/practice-portfolio/ai-analysis")
    public ResponseEntity<PortfolioAiAnalysisResponse> analyzePractice(
            @PathVariable Long teacherId, @PathVariable Long classId, @PathVariable Long studentId,
            @RequestBody PortfolioAiAnalysisRequest request, Authentication authentication) {
        requireSelf(teacherId, authentication);
        return ResponseEntity.ok(portfolioAiAnalysisService.analyzePractice(
            teacherId, classId, studentId, request.from(), request.to()));
    }

    @PostMapping("/api/teachers/{teacherId}/classes/{classId}/students/{studentId}/individual-portfolio/ai-analysis")
    public ResponseEntity<PortfolioAiAnalysisResponse> analyzeIndividual(
            @PathVariable Long teacherId, @PathVariable Long classId, @PathVariable Long studentId,
            @RequestBody PortfolioAiAnalysisRequest request, Authentication authentication) {
        requireSelf(teacherId, authentication);
        return ResponseEntity.ok(portfolioAiAnalysisService.analyzeIndividual(
            teacherId, classId, studentId, request.from(), request.to()));
    }

    private void requireSelf(Long teacherId, Authentication authentication) {
        Object principal = authentication == null ? null : authentication.getPrincipal();
        if (!(principal instanceof Long id) || !id.equals(teacherId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 계정으로만 포트폴리오를 조회할 수 있습니다.");
        }
    }
}
