// File: src/main/java/com/utepetshop/entity/Notification.java
package com.utepetshop.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
public class Notification {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false, length = 1000)
  private String message;

  @Column(nullable = false)
  private String type;

  private Long referenceId;

  @Column(name = "is_read", nullable = false)
  private boolean read = false;

  private LocalDateTime createdAt = LocalDateTime.now();

  public Notification(User user, String title, String message, String type, Long referenceId) {
    this.user = user;
    this.title = title;
    this.message = message;
    this.type = type;
    this.referenceId = referenceId;
    this.read = false;
    this.createdAt = LocalDateTime.now();
  }
}
