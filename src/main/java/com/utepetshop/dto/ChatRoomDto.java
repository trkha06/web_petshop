package com.utepetshop.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomDto {
  private String roomId;
  private String customerName;
  private String customerEmail;
  private String lastMessage;
  private String lastTime;
  private int unreadCount;
  private long lastUpdated;
}
