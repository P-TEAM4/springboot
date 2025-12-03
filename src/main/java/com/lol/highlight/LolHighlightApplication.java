package com.lol.highlight;

import com.lol.highlight.global.config.AiClientConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@EnableConfigurationProperties(AiClientConfig.class)
@SpringBootApplication
public class LolHighlightApplication {

    public static void main(String[] args) {
        SpringApplication.run(LolHighlightApplication.class, args);
    }

}
