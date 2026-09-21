// File: src/main/java/com/utepetshop/dto/NotificationDto.java
package com.utepetshop.dto;

import com.utepetshop.entity.Notification;
import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDto {
  private Long id;
  private String title;
  private String message;
  private String type;
  private Long referenceId;
  private boolean read;
  private LocalDateTime createdAt;

  public static NotificationDto fromEntity(Notification n) {
    return NotificationDto.builder()
        .id(n.getId())
        .title(n.getTitle())
        .message(n.getMessage())
        .type(n.getType())
        .referenceId(n.getReferenceId())
        .read(n.isRead())
        .createdAt(n.getCreatedAt())
        .build();
  }
}
