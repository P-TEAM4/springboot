package com.lol.highlight.global.external.riot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RiotMatchDto {

    private Metadata metadata;
    private Info info;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Metadata {
        private String matchId;
        private List<String> participants;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Info {
        private Long gameCreation;
        private Long gameDuration;
        private String gameVersion;
        private List<Participant> participants;
        private List<Team> teams;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Participant {
        private String puuid;
        private String summonerName;
        private String riotIdGameName;
        private String riotIdTagline;
        private String championName;
        private Integer championId;
        private Integer kills;
        private Integer deaths;
        private Integer assists;
        private Integer totalDamageDealtToChampions;
        private Integer totalDamageTaken;
        private Integer goldEarned;
        private Integer totalMinionsKilled;
        private Integer neutralMinionsKilled;
        private Integer visionScore;
        private Integer item0;
        private Integer item1;
        private Integer item2;
        private Integer item3;
        private Integer item4;
        private Integer item5;
        private Integer item6;
        private Boolean win;
        private Integer teamId;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Team {
        private Integer teamId;
        private Boolean win;
        private Objectives objectives;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Objectives {
        private Objective baron;
        private Objective dragon;
        private Objective tower;
        private Objective inhibitor;
        private Objective riftHerald;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Objective {
        private Integer kills;
    }
}
