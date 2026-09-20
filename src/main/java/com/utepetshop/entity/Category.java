package com.utepetshop.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
public class Category {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String name;

  private boolean active = true;
  private LocalDateTime createdAt = LocalDateTime.now();

  public Category(String name) {
    this.name = name;
  }
}
