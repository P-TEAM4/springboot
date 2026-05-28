package com.lol.highlight.domain.highlight.dto.ai;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HighlightGenerateRequest {

    private String matchId;
    private String gameName;
    private String tagLine;

    @Builder.Default
    private int topHighlights = 5;

    @Builder.Default
    private int topMistakes = 3;
}
