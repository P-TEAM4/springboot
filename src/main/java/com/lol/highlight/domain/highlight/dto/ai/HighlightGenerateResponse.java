package com.lol.highlight.domain.highlight.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
public class HighlightGenerateResponse {

    @JsonProperty("match_id")
    private String matchId;

    private Map<String, String> player;

    @JsonProperty("video_path")
    private String videoPath;

    @JsonProperty("total_clips")
    private Integer totalClips;

    private List<ClipInfo> highlights;

    private List<ClipInfo> mistakes;

    @Getter
    @NoArgsConstructor
    public static class ClipInfo {

        @JsonProperty("clip_path")
        private String clipPath;

        private Double timestamp;

        private String type;

        @JsonProperty("base_importance")
        private Double baseImportance;

        @JsonProperty("impact_score")
        private Double impactScore;

        @JsonProperty("combined_importance")
        private Double combinedImportance;

        private String description;

        @JsonProperty("impact_description")
        private String impactDescription;

        private String coaching;

        private Map<String, Object> details;
    }
}
