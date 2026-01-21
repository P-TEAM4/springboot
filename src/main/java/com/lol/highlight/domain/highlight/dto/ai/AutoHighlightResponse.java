package com.lol.highlight.domain.highlight.dto.ai;

import com.lol.highlight.domain.highlight.enums.HighlightType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * FastAPI AI 자동 하이라이트 추출 응답 DTO
 *
 * TODO: [FastAPI 연동]
 * FastAPI 서버의 실제 응답 형식에 맞게 수정 필요
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoHighlightResponse {

    /**
     * 추출된 하이라이트 목록
     */
    private List<HighlightData> highlights;

    /**
     * 처리 상태
     */
    private String status;

    /**
     * 오류 메시지 (실패 시)
     */
    private String errorMessage;

    /**
     * 개별 하이라이트 데이터
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HighlightData {

        /**
         * 하이라이트 제목
         */
        private String title;

        /**
         * 하이라이트 설명
         */
        private String description;

        /**
         * 시작 시간 (초)
         */
        private Integer startTime;

        /**
         * 종료 시간 (초)
         */
        private Integer endTime;

        /**
         * 하이라이트 유형 (KILL, MULTI_KILL, PENTAKILL, BARON, DRAGON, TOWER_DESTROY, TEAM_FIGHT, CUSTOM)
         */
        private HighlightType type;

        /**
         * 신뢰도 점수 (0.0 ~ 1.0)
         */
        private Double confidence;
    }
}
