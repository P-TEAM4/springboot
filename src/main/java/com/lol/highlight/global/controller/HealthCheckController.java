package com.lol.highlight.global.controller;

import com.lol.highlight.global.external.datadragon.DataDragonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class HealthCheckController {

    private final DataDragonService dataDragonService;

    @GetMapping("/")
    public ResponseEntity<String> rootCheck() {
        return ResponseEntity.ok("Service is running");
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("service", "lol-highlight-backend");

        return ResponseEntity.ok(health);
    }

    @GetMapping("/api/datadragon/version")
    public ResponseEntity<Map<String, String>> getDataDragonVersion() {
        Map<String, String> response = new HashMap<>();
        response.put("version", dataDragonService.getActiveVersion());
        return ResponseEntity.ok(response);
    }
}
