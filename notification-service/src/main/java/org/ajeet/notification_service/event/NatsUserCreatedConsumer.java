package org.ajeet.notification_service.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.ajeet.notification_service.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@ConditionalOnProperty(name = "nats.enabled", havingValue = "true", matchIfMissing = true)
public class NatsUserCreatedConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(NatsUserCreatedConsumer.class);
    private static final String USER_CREATED_SUBJECT = "user.created";
    private static final String QUEUE_GROUP = "notification-service";

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final String natsUrl;
    private Connection connection;

    public NatsUserCreatedConsumer(ObjectMapper objectMapper, NotificationService notificationService,
                                   @Value("${nats.url:nats://localhost:4222}") String natsUrl) {
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
        this.natsUrl = natsUrl;
    }

    @PostConstruct
    public void subscribe() throws IOException, InterruptedException {
        connection = Nats.connect(natsUrl);
        Dispatcher dispatcher = connection.createDispatcher(message -> {
            try {
                UserCreatedEvent event = objectMapper.readValue(message.getData(), UserCreatedEvent.class);
                notificationService.createForUser(event);
            } catch (Exception exception) {
                LOGGER.error("Failed to process user.created event", exception);
            }
        });
        dispatcher.subscribe(USER_CREATED_SUBJECT, QUEUE_GROUP);
    }

    @PreDestroy
    public void close() throws InterruptedException {
        if (connection != null) {
            connection.close();
        }
    }
}
