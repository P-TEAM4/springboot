package com.lol.highlight.domain.highlight.dto;

import com.lol.highlight.domain.highlight.enums.HighlightType;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class HighlightCreateRequest {

    @NotBlank(message = "Match ID is required")
    private String matchId;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private HighlightType type;

    private Double gameStartOffset;
}
