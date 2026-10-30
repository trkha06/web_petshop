package com.utepetshop.controller.api;

import com.utepetshop.dto.CheckoutRequest;
import com.utepetshop.dto.OrderResponse;
import com.utepetshop.entity.CustomerOrder;
import com.utepetshop.repository.OrderRepository;
import com.utepetshop.service.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderApiController {
  private final OrderRepository orders;
  private final CurrentUser current;
  private final StoreService store;

  public OrderApiController(OrderRepository orders, CurrentUser current, StoreService store) {
    this.orders = orders;
    this.current = current;
    this.store = store;
  }

  @GetMapping
  public List<OrderResponse> list() {
    return orders.findByCustomerIdOrderByCreatedAtDesc(current.required().getId()).stream()
        .map(OrderResponse::fromEntity)
        .toList();
  }

  @PostMapping
  public ResponseEntity<OrderResponse> checkout(
      @Valid @RequestBody CheckoutRequest req,
      @RequestHeader("Idempotency-Key") String idempotencyKey) {
    CustomerOrder order = store.checkout(current.required(), req, idempotencyKey);
    return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.fromEntity(order));
  }

  @PostMapping("/{id}/cancel")
  ResponseEntity<?> cancel(@PathVariable Long id) {
    store.cancelByCustomer(current.required(), id);
    return ResponseEntity.ok(Map.of("message", "Đã hủy đơn và hoàn tồn kho"));
  }
}
