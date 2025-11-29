package com.lol.highlight.global.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlaskMatchAnalysisRequest {
    private String matchId;
    private String summonerName;
    private String tagLine;
}
