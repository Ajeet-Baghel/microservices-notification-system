package org.ajeet.user_service.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.Nats;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class NatsEventPublisher implements EventPublisher {

    private static final String USER_CREATED_SUBJECT = "user.created";

    private final ObjectMapper objectMapper;
    private final String natsUrl;
    private Connection connection;

    public NatsEventPublisher(ObjectMapper objectMapper, @Value("${nats.url:nats://localhost:4222}") String natsUrl) {
        this.objectMapper = objectMapper;
        this.natsUrl = natsUrl;
    }

    @Override
    public void publishUserCreated(UserCreatedEvent event) {
        try {
            getConnection().publish(USER_CREATED_SUBJECT, objectMapper.writeValueAsBytes(event));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize user.created event", exception);
        }
    }

    private synchronized Connection getConnection() {
        try {
            if (connection == null || connection.getStatus() == Connection.Status.CLOSED) {
                connection = Nats.connect(natsUrl);
            }
            return connection;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while connecting to NATS at " + natsUrl, exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to connect to NATS at " + natsUrl, exception);
        }
    }

    @PreDestroy
    public void close() throws InterruptedException {
        if (connection != null) {
            connection.close();
        }
    }
}
