package com.lol.highlight.domain.match.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "match.refresh")
@Getter
@Setter
public class MatchRefreshProperties {

    /**
     * Rate Limit 윈도우 시간 (분)
     */
    private int windowMinutes = 3;

    /**
     * 윈도우 내 최대 갱신 횟수
     */
    private int maxCount = 5;

    /**
     * 유지할 최대 매치 개수
     */
    private int keepMatchCount = 40;
}
