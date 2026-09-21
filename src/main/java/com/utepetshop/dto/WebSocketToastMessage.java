// File: src/main/java/com/utepetshop/dto/WebSocketToastMessage.java
package com.utepetshop.dto;

import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebSocketToastMessage {
  private String type; // e.g. "BOOKING_CONFIRMED", "ORDER_SHIPPED", etc.
  private String title;
  private String message;
  private Long referenceId;
  private LocalDateTime createdAt;
}
