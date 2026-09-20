package com.utepetshop.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String sku;

  @Column(nullable = false)
  private String name;

  @Column(length = 2000)
  private String description;

  @Column(nullable = false, precision = 15, scale = 2)
  private BigDecimal price;

  @ManyToOne(optional = false)
  private Category category;

  @ManyToOne(optional = false)
  private Brand brand;

  @Enumerated(EnumType.STRING)
  private PetType petType;

  private String imageUrl;
  private boolean active = true;

  @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
  private java.util.List<ProductImage> images = new java.util.ArrayList<>();

  private LocalDateTime createdAt = LocalDateTime.now();
  private LocalDateTime updatedAt = LocalDateTime.now();
}
