package com.lol.highlight.domain.highlight.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FastAPI 하이라이트 영상 생성 응답 DTO
 *
 * TODO: [FastAPI 연동]
 * FastAPI 서버의 실제 응답 형식에 맞게 수정 필요
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HighlightGenerateResponse {

    /**
     * 생성된 영상 URL
     */
    private String videoUrl;

    /**
     * 썸네일 URL
     */
    private String thumbnailUrl;

    /**
     * 처리 상태
     */
    private String status;

    /**
     * 오류 메시지 (실패 시)
     */
    private String errorMessage;
}
