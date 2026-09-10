package org.ajeet.user_service.outbox;


import org.ajeet.user_service.event.EventTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class OutboxRelay {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxEventRepository repository;
    private final EventTransport eventTransport;

    public OutboxRelay(OutboxEventRepository repository, EventTransport eventTransport) {
        this.repository = repository;
        this.eventTransport = eventTransport;
    }

    @Scheduled(fixedDelayString = "${outbox.relay-delay-ms:1000}")
    @Transactional
    public void publishPendingEvents() {
        for (OutboxEvent event : repository.findByPublishedAtIsNullOrderByCreatedAtAsc(PageRequest.of(0, 100))) {
            try {
                eventTransport.publish(event.getSubject(), event.getPayload(), event.getId().toString());
                event.setPublishedAt(LocalDateTime.now());
            } catch (RuntimeException exception) {
                event.setAttempts(event.getAttempts() + 1);
                LOGGER.warn("Failed to relay outbox event {}; it will be retried", event.getId(), exception);
            }
        }
    }
}
