package org.ajeet.user_service.event;

public interface EventPublisher {

    void publishUserCreated(UserCreatedEvent event);
}
