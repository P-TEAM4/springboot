package com.lol.highlight.domain.highlight.dto;

import com.lol.highlight.domain.highlight.entity.Highlight;
import com.lol.highlight.domain.highlight.enums.HighlightStatus;
import com.lol.highlight.domain.highlight.enums.HighlightType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class HighlightResponse {

    private Long id;
    private String matchId;
    private String title;
    private String description;
    private String videoUrl;
    private String thumbnailUrl;
    private Integer startTime;
    private Integer endTime;
    private Integer duration;
    private HighlightType type;
    private HighlightStatus status;
    private Integer viewCount;
    private LocalDateTime createdAt;
    private String eventData;

    public static HighlightResponse from(Highlight highlight) {
        return HighlightResponse.builder()
                .id(highlight.getId())
                .matchId(highlight.getMatch().getMatchId())
                .title(highlight.getTitle())
                .description(highlight.getDescription())
                .videoUrl(highlight.getVideoUrl())
                .thumbnailUrl(highlight.getThumbnailUrl())
                .startTime(highlight.getStartTime())
                .endTime(highlight.getEndTime())
                .duration(highlight.getDuration())
                .type(highlight.getType())
                .status(highlight.getStatus())
                .viewCount(highlight.getViewCount())
                .createdAt(highlight.getCreatedAt())
                .eventData(highlight.getEventData())
                .build();
    }
}
