package org.ajeet.user_service.outbox;

import org.ajeet.user_service.event.EventTransport;
import org.ajeet.user_service.event.NatsEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxRelayTests {

    @Test
    void marksEventPublishedAfterJetStreamAcknowledgesIt() {
        OutboxEventRepository repository = mock(OutboxEventRepository.class);
        EventTransport publisher = mock(EventTransport.class);
        OutboxEvent event = event();
        when(repository.findByPublishedAtIsNullOrderByCreatedAtAsc(any(Pageable.class)))
                .thenReturn(List.of(event));

        new OutboxRelay(repository, publisher).publishPendingEvents();

        verify(publisher).publish(event.getSubject(), event.getPayload(), event.getId().toString());
        assertThat(event.getPublishedAt()).isNotNull();
        assertThat(event.getAttempts()).isZero();
    }

    @Test
    void leavesFailedEventPendingForRetry() {
        OutboxEventRepository repository = mock(OutboxEventRepository.class);
        EventTransport publisher = mock(EventTransport.class);
        OutboxEvent event = event();
        when(repository.findByPublishedAtIsNullOrderByCreatedAtAsc(any(Pageable.class)))
                .thenReturn(List.of(event));
        doThrow(new IllegalStateException("NATS unavailable"))
                .when(publisher).publish(event.getSubject(), event.getPayload(), event.getId().toString());

        new OutboxRelay(repository, publisher).publishPendingEvents();

        assertThat(event.getPublishedAt()).isNull();
        assertThat(event.getAttempts()).isEqualTo(1);
    }

    private OutboxEvent event() {
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setSubject("user.created");
        event.setPayload("{}");
        event.setCreatedAt(LocalDateTime.now());
        return event;
    }
}
