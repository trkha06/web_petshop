package com.utepetshop.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
    name = "chat_rooms",
    indexes = {@Index(name = "idx_chat_room_last_message_at", columnList = "last_message_at")})
@Getter
@Setter
@NoArgsConstructor
public class ChatRoom {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 80)
  private String roomId;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "customer_id", nullable = false, unique = true)
  private User customer;

  @Column(length = 2000)
  private String lastMessage;

  private LocalDateTime lastMessageAt;

  @Column(nullable = false)
  private int unreadCount;
}
