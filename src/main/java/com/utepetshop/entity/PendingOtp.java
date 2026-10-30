package com.utepetshop.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
    name = "pending_otps",
    uniqueConstraints = {@UniqueConstraint(name = "uk_pending_otp_email_purpose", columnNames = {"email", "purpose"})},
    indexes = {@Index(name = "idx_pending_otp_expiry", columnList = "expires_at")})
@Getter
@Setter
@NoArgsConstructor
public class PendingOtp {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 255)
  private String email;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private OtpPurpose purpose;

  @Column(length = 255)
  private String fullName;

  @Column(length = 255)
  private String passwordHash;

  @Column(nullable = false, length = 255)
  private String otpHash;

  @Column(nullable = false)
  private LocalDateTime expiresAt;

  @Column(nullable = false)
  private int failedAttempts;

  @Column(nullable = false)
  private int sendCount;

  @Column(nullable = false)
  private LocalDateTime sendWindowStartedAt;

  @Column(nullable = false)
  private LocalDateTime lastSentAt;

  @Column(nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();
}
