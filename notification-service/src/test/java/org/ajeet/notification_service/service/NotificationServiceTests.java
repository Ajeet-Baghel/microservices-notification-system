package org.ajeet.notification_service.service;

import org.ajeet.notification_service.entity.Notification;
import org.ajeet.notification_service.event.UserCreatedEvent;
import org.ajeet.notification_service.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTests {

    @Test
    void createsNotificationFromUserCreatedEvent() {
        NotificationRepository repository = mock(NotificationRepository.class);
        NotificationService service = new NotificationService(repository);
        when(repository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UserCreatedEvent event = new UserCreatedEvent(42L, "Test User", "user@example.com", LocalDateTime.now());

        service.createForUser(event);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(notificationCaptor.capture());
        Notification notification = notificationCaptor.getValue();
        assertThat(notification.getUserId()).isEqualTo(42L);
        assertThat(notification.getRecipient()).isEqualTo("user@example.com");
        assertThat(notification.getMessage()).contains("Test User");
        assertThat(notification.getCreatedAt()).isNotNull();
    }
}
