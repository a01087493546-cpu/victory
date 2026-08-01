package com.victory.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * 던전 전투 결과 제출 요청(POST /api/dungeons/{dungeonId}/battle-result).
 * victory/defeat/timeout 외 값은 서비스에서 400으로 거부한다.
 */
@Getter
@NoArgsConstructor
public class BattleResultRequest {

    @NotBlank
    private String result;
}
