package com.lol.highlight;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class LolHighlightApplication {

    public static void main(String[] args) {
        SpringApplication.run(LolHighlightApplication.class, args);
    }

}
