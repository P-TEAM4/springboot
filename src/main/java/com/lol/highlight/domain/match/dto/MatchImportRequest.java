package com.lol.highlight.domain.match.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MatchImportRequest {

    @NotBlank(message = "Match ID is required")
    private String matchId;
}
