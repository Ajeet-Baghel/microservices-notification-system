package org.ajeet.notification_service.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.JetStream;
import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamManagement;
import io.nats.client.Nats;
import io.nats.client.PushSubscribeOptions;
import io.nats.client.api.AckPolicy;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.ajeet.notification_service.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

@Component
@ConditionalOnProperty(name = "nats.enabled", havingValue = "true", matchIfMissing = true)
public class NatsUserCreatedConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(NatsUserCreatedConsumer.class);
    private static final String USER_EVENTS_STREAM = "USER_EVENTS";
    private static final String USER_CREATED_SUBJECT = "user.created";
    private static final String DURABLE_CONSUMER = "notification-service";

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
    public void subscribe() throws IOException, InterruptedException, JetStreamApiException {
        connection = Nats.connect(natsUrl);
        ensureStream(connection.jetStreamManagement());
        JetStream jetStream = connection.jetStream();
        Dispatcher dispatcher = connection.createDispatcher();
        ConsumerConfiguration consumerConfiguration = ConsumerConfiguration.builder()
                .durable(DURABLE_CONSUMER)
                .ackPolicy(AckPolicy.Explicit)
                .ackWait(Duration.ofSeconds(10))
                .maxDeliver(5)
                .build();
        PushSubscribeOptions options = PushSubscribeOptions.builder()
                .stream(USER_EVENTS_STREAM)
                .configuration(consumerConfiguration)
                .build();
        jetStream.subscribe(USER_CREATED_SUBJECT, dispatcher, message -> {
            try {
                UserCreatedEvent event = objectMapper.readValue(message.getData(), UserCreatedEvent.class);
                notificationService.createForUser(event);
                message.ack();
            } catch (Exception exception) {
                LOGGER.error("Failed to process user.created event; requesting redelivery", exception);
                message.nakWithDelay(Duration.ofSeconds(5));
            }
        }, false, options);
    }

    private void ensureStream(JetStreamManagement management) throws IOException, JetStreamApiException {
        try {
            management.getStreamInfo(USER_EVENTS_STREAM);
        } catch (JetStreamApiException exception) {
            management.addStream(StreamConfiguration.builder()
                    .name(USER_EVENTS_STREAM)
                    .subjects(USER_CREATED_SUBJECT)
                    .storageType(StorageType.File)
                    .build());
        }
    }

    @PreDestroy
    public void close() throws InterruptedException {
        if (connection != null) {
            connection.close();
        }
    }
}
