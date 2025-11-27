package com.lol.highlight.domain.analysis.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AnalysisCreateRequest {

    @NotNull(message = "Match ID is required")
    private Long matchId;
}
