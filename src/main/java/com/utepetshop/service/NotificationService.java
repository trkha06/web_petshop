// File: src/main/java/com/utepetshop/service/NotificationService.java
package com.utepetshop.service;

import com.utepetshop.dto.NotificationDto;
import com.utepetshop.dto.WebSocketToastMessage;
import com.utepetshop.entity.Notification;
import com.utepetshop.entity.User;
import com.utepetshop.repository.NotificationRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
  private final NotificationRepository notificationRepository;
  private final SimpMessagingTemplate messagingTemplate;

  @Transactional
  public Notification sendNotification(
      User user, String title, String message, String type, Long referenceId) {
    Notification notification = new Notification(user, title, message, type, referenceId);
    notification = notificationRepository.save(notification);

    try {
      WebSocketToastMessage toast =
          WebSocketToastMessage.builder()
              .type(type)
              .title(title)
              .message(message)
              .referenceId(referenceId)
              .createdAt(LocalDateTime.now())
              .build();

      // Gửi đích danh cho user qua /user/queue/notifications
      messagingTemplate.convertAndSendToUser(user.getEmail(), "/queue/notifications", toast);
      log.info("Sent realtime notification to user: {}", user.getEmail());
    } catch (Exception e) {
      log.warn("Failed to dispatch WebSocket message to {}: {}", user.getEmail(), e.getMessage());
    }

    return notification;
  }

  @Transactional(readOnly = true)
  public List<NotificationDto> getUserNotifications(User user) {
    return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
        .map(NotificationDto::fromEntity)
        .toList();
  }

  @Transactional(readOnly = true)
  public long getUnreadCount(User user) {
    return notificationRepository.countByUserIdAndReadFalse(user.getId());
  }

  @Transactional
  public void markAsRead(Long notificationId, User user) {
    notificationRepository
        .findById(notificationId)
        .ifPresent(
            n -> {
              if (n.getUser().getId().equals(user.getId())) {
                n.setRead(true);
                notificationRepository.save(n);
              }
            });
  }

  @Transactional
  public void markAllAsRead(User user) {
    List<Notification> list = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    for (Notification n : list) {
      if (!n.isRead()) {
        n.setRead(true);
      }
    }
    notificationRepository.saveAll(list);
  }
}
