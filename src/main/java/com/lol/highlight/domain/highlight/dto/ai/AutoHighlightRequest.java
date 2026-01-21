package com.lol.highlight.domain.highlight.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FastAPI AI 자동 하이라이트 추출 요청 DTO
 *
 * TODO: [FastAPI 연동]
 * FastAPI 서버의 실제 요청 형식에 맞게 수정 필요
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoHighlightRequest {

    /**
     * Riot 매치 ID (예: KR_7951942780)
     */
    private String matchId;

    /**
     * 플레이어 PUUID
     */
    private String puuid;
}
