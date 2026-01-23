package com.lol.highlight.domain.analysis.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AnalysisCreateRequest {

    @NotBlank(message = "Match ID is required")
    private String matchId;
}
