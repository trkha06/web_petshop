package com.utepetshop.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
public class OrderItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  private CustomerOrder order;

  @ManyToOne(optional = false)
  private Product product;

  private String productName;

  @Column(precision = 15, scale = 2)
  private BigDecimal unitPrice;

  private int quantity;
}
