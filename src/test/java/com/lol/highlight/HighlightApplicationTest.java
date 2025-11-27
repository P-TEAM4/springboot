package com.lol.highlight;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=test-secret-key-for-jwt-token-test-minimum-32-characters-long",
        "jwt.expiration=3600000"
})
class HighlightApplicationTest {

    @Test
    @DisplayName("Application Context Loads Successfully")
    void contextLoads() {
        // This test will fail if there are any configuration issues
    }
}
