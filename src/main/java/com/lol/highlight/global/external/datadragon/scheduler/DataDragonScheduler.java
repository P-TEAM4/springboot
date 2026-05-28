package com.lol.highlight.global.external.datadragon.scheduler;

import com.lol.highlight.global.external.datadragon.DataDragonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataDragonScheduler {

    private final DataDragonService dataDragonService;

    /**
     * 애플리케이션 시작 시 Data Dragon 버전 초기화
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Updating Data Dragon version on application startup");
        dataDragonService.updateDataDragonVersion();
    }

    /**
     * 매주 목요일 오전 9시, 11시, 1시(13시)에 Data Dragon 버전 업데이트
     * CRON: 초 분 시 일 월 요일
     * 0 0 9,11,13 ? * THU
     */
    @Scheduled(cron = "0 0 9,11,13 ? * THU", zone = "Asia/Seoul")
    public void updateDataDragonVersion() {
        log.info("Scheduled Data Dragon version update started");
        dataDragonService.updateDataDragonVersion();
    }
}
