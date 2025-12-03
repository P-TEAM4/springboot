package com.lol.highlight.domain.highlight.entity;

import com.lol.highlight.domain.highlight.enums.HighlightStatus;
import com.lol.highlight.domain.highlight.enums.HighlightType;
import com.lol.highlight.domain.match.entity.Match;
import com.lol.highlight.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "highlights")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Highlight extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String videoUrl;

    private String thumbnailUrl;

    private Integer startTime;

    private Integer endTime;

    private Integer duration;

    @Enumerated(EnumType.STRING)
    private HighlightType type;

    @Enumerated(EnumType.STRING)
    private HighlightStatus status;

    @Column(columnDefinition = "TEXT")
    private String eventData;

    private Integer viewCount;

    @Builder
    public Highlight(Match match, String title, String description, String videoUrl,
                    String thumbnailUrl, Integer startTime, Integer endTime,
                    Integer duration, HighlightType type, HighlightStatus status,
                    String eventData) {
        this.match = match;
        this.title = title;
        this.description = description;
        this.videoUrl = videoUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.startTime = startTime;
        this.endTime = endTime;
        this.duration = duration;
        this.type = type;
        this.status = status != null ? status : HighlightStatus.PENDING;
        this.eventData = eventData;
        this.viewCount = 0;
    }

    public void updateVideoInfo(String videoUrl, String thumbnailUrl) {
        this.videoUrl = videoUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.status = HighlightStatus.COMPLETED;
    }

    public void updateStatus(HighlightStatus status) {
        this.status = status;
    }

    public void incrementViewCount() {
        this.viewCount++;
    }
}
