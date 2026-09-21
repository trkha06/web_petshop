// File: src/main/java/com/utepetshop/controller/api/NotificationApiController.java
package com.utepetshop.controller.api;

import com.utepetshop.dto.NotificationDto;
import com.utepetshop.entity.User;
import com.utepetshop.service.CurrentUser;
import com.utepetshop.service.NotificationService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationApiController {
  private final NotificationService notificationService;
  private final CurrentUser currentUser;

  @GetMapping
  public ResponseEntity<Map<String, Object>> getNotifications() {
    User user = currentUser.required();
    List<NotificationDto> list = notificationService.getUserNotifications(user);
    long unreadCount = notificationService.getUnreadCount(user);
    return ResponseEntity.ok(Map.of("notifications", list, "unreadCount", unreadCount));
  }

  @PostMapping("/{id}/read")
  public ResponseEntity<Map<String, String>> markRead(@PathVariable Long id) {
    User user = currentUser.required();
    notificationService.markAsRead(id, user);
    return ResponseEntity.ok(Map.of("message", "Đã đánh dấu đã đọc"));
  }

  @PostMapping("/read-all")
  public ResponseEntity<Map<String, String>> markAllRead() {
    User user = currentUser.required();
    notificationService.markAllAsRead(user);
    return ResponseEntity.ok(Map.of("message", "Đã đánh dấu tất cả là đã đọc"));
  }
}
