package org.ajeet.notification_service.repository;

import org.ajeet.notification_service.entity.Notification;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class NotificationRepositoryTests {

    @Autowired
    private NotificationRepository repository;

    @Test
    void enforcesUniqueUserId() {
        repository.saveAndFlush(notificationFor(42L));
        assertThat(repository.existsByUserId(42L)).isTrue();

        assertThatThrownBy(() -> repository.saveAndFlush(notificationFor(42L)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Notification notificationFor(Long userId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setRecipient("user@example.com");
        notification.setMessage("Welcome");
        notification.setCreatedAt(LocalDateTime.now());
        return notification;
    }
}
