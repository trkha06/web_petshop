package com.utepetshop.dto;

import com.utepetshop.entity.CustomerOrder;
import com.utepetshop.entity.DeliveryType;
import com.utepetshop.entity.OrderItem;
import com.utepetshop.entity.OrderStatus;
import com.utepetshop.entity.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
    Long id,
    Long branchId,
    String branchName,
    String branchAddress,
    OrderStatus status,
    PaymentMethod paymentMethod,
    DeliveryType deliveryType,
    String recipientName,
    String recipientEmail,
    String recipientPhone,
    String shippingAddress,
    String customerNote,
    String promotionCode,
    BigDecimal subtotalAmount,
    BigDecimal discountAmount,
    BigDecimal totalAmount,
    LocalDateTime createdAt,
    List<OrderItemResponse> items) {
  public static OrderResponse fromEntity(CustomerOrder order) {
    return new OrderResponse(
        order.getId(),
        order.getBranch().getId(),
        order.getBranch().getName(),
        order.getBranch().getAddress(),
        order.getStatus(),
        order.getPaymentMethod(),
        order.getDeliveryType(),
        order.getRecipientName(),
        order.getRecipientEmail(),
        order.getRecipientPhone(),
        order.getShippingAddress(),
        order.getCustomerNote(),
        order.getPromotionCode(),
        order.getSubtotalAmount(),
        order.getDiscountAmount(),
        order.getTotalAmount(),
        order.getCreatedAt(),
        order.getItems().stream().map(OrderItemResponse::fromEntity).toList());
  }

  public record OrderItemResponse(
      Long id,
      Long productId,
      String productName,
      BigDecimal unitPrice,
      int quantity,
      BigDecimal lineTotal) {
    public static OrderItemResponse fromEntity(OrderItem item) {
      return new OrderItemResponse(
          item.getId(),
          item.getProduct().getId(),
          item.getProductName(),
          item.getUnitPrice(),
          item.getQuantity(),
          item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
    }
  }
}
