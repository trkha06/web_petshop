package com.utepetshop.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDto {
  private String roomId;
  private String senderName;
  private String senderEmail;
  private String senderRole;
  private String content;
  private String time;
  private boolean staff;
  private boolean bot;

  public static ChatMessageDto of(String roomId, String senderName, String senderEmail, String senderRole, String content) {
    boolean isStaff = "STAFF".equalsIgnoreCase(senderRole) 
        || "MANAGER".equalsIgnoreCase(senderRole) 
        || "ADMIN".equalsIgnoreCase(senderRole);
    boolean isBot = "BOT".equalsIgnoreCase(senderRole);
    return ChatMessageDto.builder()
        .roomId(roomId)
        .senderName(senderName)
        .senderEmail(senderEmail)
        .senderRole(senderRole)
        .content(content)
        .time(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")))
        .staff(isStaff)
        .bot(isBot)
        .build();
  }
}
