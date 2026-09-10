package org.ajeet.user_service.event;

import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamManagement;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.nats.client.PublishOptions;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class NatsEventPublisher implements EventTransport {

    private static final String USER_EVENTS_STREAM = "USER_EVENTS";

    private final String natsUrl;
    private final String natsUsername;
    private final String natsPassword;
    private Connection connection;
    private JetStream jetStream;

    public NatsEventPublisher(@Value("${nats.url:nats://localhost:4222}") String natsUrl,
                              @Value("${nats.username}") String natsUsername,
                              @Value("${nats.password}") String natsPassword) {
        this.natsUrl = natsUrl;
        this.natsUsername = natsUsername;
        this.natsPassword = natsPassword;
    }

    @Override
    public void publish(String subject, String payload, String messageId) {
        try {
            PublishOptions options = PublishOptions.builder().messageId(messageId).build();
            getJetStream().publish(subject, payload.getBytes(StandardCharsets.UTF_8), options);
        } catch (IOException | JetStreamApiException exception) {
            throw new IllegalStateException("Failed to publish outbox event " + messageId, exception);
        }
    }

    private synchronized JetStream getJetStream() {
        if (jetStream == null) {
            try {
                connection = Nats.connect(Options.builder()
                        .server(natsUrl)
                        .userInfo(natsUsername.toCharArray(), natsPassword.toCharArray())
                        .build());
                JetStreamManagement management = connection.jetStreamManagement();
                try {
                    management.getStreamInfo(USER_EVENTS_STREAM);
                } catch (JetStreamApiException exception) {
                    management.addStream(StreamConfiguration.builder()
                            .name(USER_EVENTS_STREAM)
                            .subjects("user.created")
                            .storageType(StorageType.File)
                            .build());
                }
                jetStream = connection.jetStream();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while connecting to NATS at " + natsUrl, exception);
            } catch (IOException | JetStreamApiException exception) {
                throw new IllegalStateException("Failed to initialize JetStream at " + natsUrl, exception);
            }
        }
        return jetStream;
    }

    @PreDestroy
    public void close() throws InterruptedException {
        if (connection != null) {
            connection.close();
        }
    }
}
