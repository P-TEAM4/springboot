package com.lol.highlight.domain.match.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

@Getter
@Builder
public class MatchesWithProfileResponse {
    private SummonerProfileResponse profile;
    private Page<MatchResponse> matches;
}
