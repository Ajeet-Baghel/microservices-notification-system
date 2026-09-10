package org.ajeet.notification_service.repository;

import org.ajeet.notification_service.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    boolean existsByUserId(Long userId);
}
