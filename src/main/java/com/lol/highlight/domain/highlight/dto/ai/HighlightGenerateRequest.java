package com.lol.highlight.domain.highlight.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FastAPI 하이라이트 영상 생성 요청 DTO
 *
 * TODO: [FastAPI 연동]
 * FastAPI 서버의 실제 요청 형식에 맞게 수정 필요
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HighlightGenerateRequest {

    /**
     * Riot 매치 ID (예: KR_7951942780)
     */
    private String matchId;

    /**
     * 하이라이트 ID (DB ID)
     */
    private Long highlightId;

    /**
     * 시작 시간 (초)
     */
    private Integer startTime;

    /**
     * 종료 시간 (초)
     */
    private Integer endTime;

    /**
     * 플레이어 PUUID (선택)
     */
    private String puuid;
}
