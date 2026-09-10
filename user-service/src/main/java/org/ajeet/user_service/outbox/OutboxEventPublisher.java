package org.ajeet.user_service.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ajeet.user_service.event.EventPublisher;
import org.ajeet.user_service.event.UserCreatedEvent;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class OutboxEventPublisher implements EventPublisher {

    private static final String USER_CREATED_SUBJECT = "user.created";

    private final ObjectMapper objectMapper;
    private final OutboxEventRepository repository;

    public OutboxEventPublisher(ObjectMapper objectMapper, OutboxEventRepository repository) {
        this.objectMapper = objectMapper;
        this.repository = repository;
    }

    @Override
    public void publishUserCreated(UserCreatedEvent event) {
        try {
            OutboxEvent outboxEvent = new OutboxEvent();
            outboxEvent.setId(UUID.randomUUID());
            outboxEvent.setSubject(USER_CREATED_SUBJECT);
            outboxEvent.setPayload(objectMapper.writeValueAsString(event));
            outboxEvent.setCreatedAt(LocalDateTime.now());
            outboxEvent.setAttempts(0);
            repository.save(outboxEvent);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize user.created event", exception);
        }
    }
}
