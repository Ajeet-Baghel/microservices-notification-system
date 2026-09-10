package org.ajeet.user_service.event;

public interface EventTransport {

    void publish(String subject, String payload, String messageId);
}
