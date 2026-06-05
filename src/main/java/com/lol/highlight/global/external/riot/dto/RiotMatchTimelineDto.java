package com.lol.highlight.global.external.riot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RiotMatchTimelineDto {

    private Info info;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Info {
        private List<Frame> frames;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Frame {
        private Long timestamp;
        private List<Event> events;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Event {
        private String type;
        private Long timestamp;
        private Integer participantId;
        private Integer itemId;
        private Integer skillSlot;  // 1=Q, 2=W, 3=E, 4=R
        private Integer levelUpType;
    }
}
