package com.utepetshop.dto;

import com.utepetshop.entity.Product;
import java.math.BigDecimal;

public record ProductResponse(
    Long id,
    String sku,
    String name,
    String description,
    BigDecimal price,
    String category,
    String brand,
    String petType,
    String imageUrl) {
  public static ProductResponse of(Product p) {
    return new ProductResponse(
        p.getId(),
        p.getSku(),
        p.getName(),
        p.getDescription(),
        p.getPrice(),
        p.getCategory().getName(),
        p.getBrand().getName(),
        p.getPetType().name(),
        p.getImageUrl());
  }
}
