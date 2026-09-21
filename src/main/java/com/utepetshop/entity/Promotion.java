// File: src/main/java/com/utepetshop/entity/Promotion.java
package com.utepetshop.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "promotions")
@Getter
@Setter
@NoArgsConstructor
public class Promotion {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String code;

  @Enumerated(EnumType.STRING)
  @Column(name = "discount_type", nullable = false)
  private DiscountType discountType = DiscountType.PERCENTAGE;

  @Column(name = "discount_value", nullable = false, precision = 15, scale = 2)
  private BigDecimal discountValue;

  @Column(name = "min_order_value", precision = 15, scale = 2)
  private BigDecimal minOrderValue = BigDecimal.ZERO;

  @Column(name = "max_discount_amount", precision = 15, scale = 2)
  private BigDecimal maxDiscountAmount;

  @Column(name = "start_date")
  private LocalDateTime startDate;

  @Column(name = "end_date")
  private LocalDateTime endDate;

  @Column(name = "usage_limit", nullable = false)
  private int usageLimit = 100;

  @Column(name = "used_count", nullable = false)
  private int usedCount = 0;

  private boolean active = true;
}
