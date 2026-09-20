package com.utepetshop.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
public class Address {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  private User user;

  @Column(nullable = false)
  private String recipientName;

  @Column(nullable = false)
  private String phone;

  @Column(nullable = false)
  private String line1;

  private String ward;
  private String district;
  private String city;
  private boolean defaultAddress;
  private LocalDateTime createdAt = LocalDateTime.now();
}
