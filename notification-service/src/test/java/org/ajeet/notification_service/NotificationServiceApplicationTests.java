package org.ajeet.notification_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "nats.enabled=false")
class NotificationServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
