package com.victory.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.BattleResultRequest;
import com.victory.dto.BattleResultResponse;
import com.victory.dto.DungeonResponse;
import com.victory.service.DungeonService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/dungeons")
@RequiredArgsConstructor
public class DungeonController {

    private final DungeonService dungeonService;

    @GetMapping
    public ResponseEntity<List<DungeonResponse>> getDungeons(Authentication authentication) {
        Long studentId = requireStudentId(authentication);

        return ResponseEntity.ok(dungeonService.getDungeonsForStudent(studentId));
    }

    @PostMapping("/{dungeonId}/battle-result")
    public ResponseEntity<BattleResultResponse> submitBattleResult(
            @PathVariable Long dungeonId,
            @Valid @RequestBody BattleResultRequest request,
            Authentication authentication) {

        Long studentId = requireStudentId(authentication);

        return ResponseEntity.ok(
            dungeonService.submitBattleResult(studentId, dungeonId, request.getResult()));
    }

    /*
     * IndividualReadingController.requireStudentId()와 동일한 관례.
     */
    private Long requireStudentId(Authentication authentication) {

        Object principal =
            authentication == null ? null : authentication.getPrincipal();

        if (!(principal instanceof Long studentId)) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "학생 로그인이 필요합니다."
            );
        }

        return studentId;
    }
}
