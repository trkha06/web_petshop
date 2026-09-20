package com.utepetshop.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_images")
@Getter
@Setter
@NoArgsConstructor
public class ProductImage {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  private Product product;

  @Column(nullable = false, length = 1200)
  private String imageUrl;

  private String secureUrl;
  private String publicId;
  private boolean primaryImage;
}
