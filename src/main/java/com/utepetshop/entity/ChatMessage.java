package com.utepetshop.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
    name = "chat_messages",
    indexes = {
      @Index(name = "idx_chat_message_room_created", columnList = "room_id,created_at")
    })
@Getter
@Setter
@NoArgsConstructor
public class ChatMessage {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "room_id", nullable = false)
  private ChatRoom room;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sender_user_id")
  private User sender;

  @Column(nullable = false, length = 255)
  private String senderName;

  @Column(length = 255)
  private String senderEmail;

  @Column(nullable = false, length = 30)
  private String senderRole;

  @Column(nullable = false, length = 2000)
  private String content;

  @Column(nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();
}
