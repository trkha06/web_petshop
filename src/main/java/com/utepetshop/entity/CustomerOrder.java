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

  @Enumerated(EnumType.STRING)
  private ShippingZone shippingZone;

  @Column(length = 50)
  private String shippingCarrier;

  @Column(unique = true, length = 40)
  private String trackingCode;

  @Column(name = "checkout_request_key", unique = true, length = 80, updatable = false)
  private String checkoutRequestKey;

  @Column(length = 100)
  private String recipientName;

  @Column(length = 150)
  private String recipientEmail;

  @Column(length = 20)
  private String recipientPhone;

  @Column(length = 500)
  private String shippingAddress;

  @Column(length = 500)
  private String customerNote;

  @Column(length = 500)
  private String returnReason;

  private LocalDateTime returnRequestedAt;

  @Column(length = 50)
  private String promotionCode;

  @Column(precision = 15, scale = 2)
  private BigDecimal subtotalAmount;

  @Column(precision = 15, scale = 2)
  private BigDecimal discountAmount = BigDecimal.ZERO;

  @Column(precision = 15, scale = 2)
  private BigDecimal totalAmount;

  private LocalDateTime createdAt = LocalDateTime.now();

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
  private List<OrderItem> items = new ArrayList<>();
}
