package org.ajeet.user_service.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.ajeet.user_service.event.UserCreatedEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OutboxEventPublisherTests {

    @Test
    void storesSerializedUserCreatedEvent() {
        OutboxEventRepository repository = mock(OutboxEventRepository.class);
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        OutboxEventPublisher publisher = new OutboxEventPublisher(objectMapper, repository);

        publisher.publishUserCreated(new UserCreatedEvent(
                42L, "Test User", "user@example.com", LocalDateTime.of(2026, 9, 10, 12, 0)));

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(captor.capture());
        OutboxEvent event = captor.getValue();
        assertThat(event.getId()).isNotNull();
        assertThat(event.getSubject()).isEqualTo("user.created");
        assertThat(event.getPayload()).contains("\"id\":42", "\"email\":\"user@example.com\"");
        assertThat(event.getCreatedAt()).isNotNull();
        assertThat(event.getPublishedAt()).isNull();
        assertThat(event.getAttempts()).isZero();
    }
}
