package com.utepetshop.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "branches")
@Getter
@Setter
@NoArgsConstructor
public class Branch {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String name;

  private String address;
  private boolean active = true;

  public Branch(String name, String address) {
    this.name = name;
    this.address = address;
  }
}
