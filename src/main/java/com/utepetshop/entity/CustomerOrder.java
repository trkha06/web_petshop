package com.utepetshop.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import lombok.*;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class CustomerOrder {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  private User customer;

  @ManyToOne(optional = false)
  private Branch branch;

  @Enumerated(EnumType.STRING)
  private OrderStatus status = OrderStatus.PENDING;

  @Enumerated(EnumType.STRING)
  private PaymentMethod paymentMethod = PaymentMethod.COD;

  @Enumerated(EnumType.STRING)
  private DeliveryType deliveryType = DeliveryType.DELIVERY;

  @Column(precision = 15, scale = 2)
  private BigDecimal totalAmount;

  private LocalDateTime createdAt = LocalDateTime.now();

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
  private List<OrderItem> items = new ArrayList<>();
}
