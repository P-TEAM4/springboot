package com.lol.highlight.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RiotAccountLinkRequest {

    @NotBlank(message = "Summoner name is required")
    private String summonerName;

    @NotBlank(message = "Tag line is required")
    private String tagLine;
}
