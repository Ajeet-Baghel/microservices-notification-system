package org.ajeet.notification_service.service;

import org.ajeet.notification_service.entity.Notification;
import org.ajeet.notification_service.event.UserCreatedEvent;
import org.ajeet.notification_service.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public Notification createForUser(UserCreatedEvent event) {
        Notification notification = new Notification();
        notification.setUserId(event.id());
        notification.setRecipient(event.email());
        notification.setMessage("Welcome " + event.name() + ", your account has been created.");
        notification.setCreatedAt(LocalDateTime.now());
        return notificationRepository.save(notification);
    }
}
