package com.epam.finaltask;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "app.url.resetPassword=http://localhost:8080/auth/reset-password?token=",
                "spring.sql.init.mode=never",
                "app.db.seed=false"
        }
)
@ExtendWith(SpringExtension.class)
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
class ApplicationTests {

    @Test
    void contextLoads() {
    }
}
