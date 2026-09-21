// File: src/main/java/com/utepetshop/entity/Service.java
package com.utepetshop.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "services")
@Getter
@Setter
@NoArgsConstructor
public class Service {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(length = 2000)
  private String description;

  @Column(nullable = false, precision = 15, scale = 2)
  private BigDecimal price;

  @Column(nullable = false)
  private int durationMinutes = 60;

  private String imageUrl;

  @Enumerated(EnumType.STRING)
  private PetType petType;

  private boolean active = true;

  private LocalDateTime createdAt = LocalDateTime.now();
}
