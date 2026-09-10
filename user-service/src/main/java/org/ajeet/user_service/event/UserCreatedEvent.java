package org.ajeet.user_service.event;

import java.time.LocalDateTime;

public record UserCreatedEvent(Long id, String name, String email, LocalDateTime createdAt) {
}
