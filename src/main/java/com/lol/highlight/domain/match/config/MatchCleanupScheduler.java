package com.lol.highlight.domain.match.config;

import com.lol.highlight.domain.match.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchCleanupScheduler {

    private final MatchRepository matchRepository;
    private final MatchRefreshProperties refreshProperties;

    /**
     * 매일 새벽 4시에 오래된 전적 정리
     * 사용자 활동이 적은 시간대에 실행하여 Race Condition 방지
     */
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void scheduledCleanup() {
        log.info("Starting scheduled match cleanup...");
        
        // 모든 고유 puuid 조회
        List<String> puuids = matchRepository.findDistinctPuuids();
        
        for (String puuid : puuids) {
            matchRepository.deleteOldMatchesKeepRecent(
                puuid, 
                refreshProperties.getKeepMatchCount()
            );
        }
        
        log.info("Scheduled cleanup completed for {} users", puuids.size());
    }
}
